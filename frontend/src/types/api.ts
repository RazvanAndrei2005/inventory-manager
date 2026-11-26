export type Role = "ROLE_ADMIN" | "ROLE_MANAGER" | "ROLE_USER";

export type StockMovementType = "INITIAL_STOCK" | "RESTOCK" | "ADJUSTMENT" | "SALE";

export type OrderStatus = "PENDING" | "CONFIRMED" | "SHIPPED" | "DELIVERED" | "CANCELLED";

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status: number;
  error?: string;
  message: string;
  path?: string;
}

export interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  role: Role;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  role: Role;
  enabled: boolean;
}

export interface Category {
  id: number;
  name: string;
  description: string | null;
}

export interface Product {
  id: number;
  name: string;
  sku: string;
  price: number;
  stockQuantity: number;
  category: Category | null;
}

export interface StockMovement {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  type: StockMovementType;
  quantityChange: number;
  quantityAfter: number;
  reason: string | null;
  createdById: number;
  createdByUsername: string;
  createdAt: string;
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Order {
  id: number;
  userId: number;
  username: string;
  status: OrderStatus;
  totalAmount: number;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}
