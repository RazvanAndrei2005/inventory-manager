package com.inventory.manager.service;

import com.inventory.manager.dto.OrderItemRequestDto;
import com.inventory.manager.dto.OrderItemResponseDto;
import com.inventory.manager.dto.OrderRequestDto;
import com.inventory.manager.dto.OrderResponseDto;
import com.inventory.manager.entity.Order;
import com.inventory.manager.entity.OrderItem;
import com.inventory.manager.entity.OrderStatus;
import com.inventory.manager.entity.Product;
import com.inventory.manager.entity.StockMovementType;
import com.inventory.manager.entity.User;
import com.inventory.manager.exception.BadRequestException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.OrderRepository;
import com.inventory.manager.repository.ProductRepository;
import com.inventory.manager.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StockMovementService stockMovementService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        StockMovementService stockMovementService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public OrderResponseDto createOrder(Long userId, OrderRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Set<Long> seenProductIds = new HashSet<>();
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> createdItems = new ArrayList<>();

        List<OrderItemRequestDto> sortedItems = request.getItems().stream()
                .sorted(Comparator.comparing(OrderItemRequestDto::getProductId))
                .toList();

        for (OrderItemRequestDto itemReq : sortedItems) {
            if (!seenProductIds.add(itemReq.getProductId())) {
                throw new BadRequestException("Duplicate product ID in order: " + itemReq.getProductId());
            }
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product '" + product.getName()
                        + "': available=" + product.getStockQuantity() + ", requested=" + itemReq.getQuantity());
            }

            int newStock = product.getStockQuantity() - itemReq.getQuantity();
            product.setStockQuantity(newStock);
            productRepository.save(product);

            BigDecimal unitPrice = product.getPrice();
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity())));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            order.addItem(orderItem);
            createdItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : createdItems) {
            Product product = item.getProduct();
            stockMovementService.recordMovement(
                    product,
                    StockMovementType.SALE,
                    -item.getQuantity(),
                    product.getStockQuantity(),
                    "Order #" + savedOrder.getId(),
                    user
            );
        }

        return mapToDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getMyOrders(Long userId, OrderStatus status, Pageable pageable) {
        Page<Order> page = status != null
                ? orderRepository.findByUserIdAndStatus(userId, status, pageable)
                : orderRepository.findByUserId(userId, pageable);
        return page.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<Order> page = status != null
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return page.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long orderId, Long currentUserId, boolean isPrivileged) {
        Order order = findOrder(orderId);
        if (!isPrivileged && !order.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to view this order");
        }
        return mapToDto(order);
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatus newStatus, Long actorUserId) {
        Order order = findOrder(orderId);
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new BadRequestException("Invalid order status transition from " + order.getStatus() + " to " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restoreStock(order, actorUserId);
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        return mapToDto(updated);
    }

    @Transactional
    public OrderResponseDto cancelOrder(Long orderId, Long currentUserId, boolean isPrivileged) {
        Order order = findOrder(orderId);
        if (!isPrivileged && !order.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to cancel this order");
        }
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BadRequestException("Invalid order status transition from " + order.getStatus() + " to CANCELLED");
        }

        restoreStock(order, currentUserId);
        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        return mapToDto(updated);
    }

    private void restoreStock(Order order, Long actorUserId) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + actorUserId));
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int newStock = product.getStockQuantity() + item.getQuantity();
            product.setStockQuantity(newStock);
            productRepository.save(product);
            stockMovementService.recordMovement(
                    product,
                    StockMovementType.ADJUSTMENT,
                    item.getQuantity(),
                    newStock,
                    "Order #" + order.getId() + " cancelled",
                    actor
            );
        }
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private OrderResponseDto mapToDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> {
                    BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new OrderItemResponseDto(
                            item.getId(),
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getProduct().getSku(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            lineTotal
                    );
                })
                .toList();

        return new OrderResponseDto(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getUsername(),
                order.getStatus(),
                order.getTotalAmount(),
                itemDtos,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
