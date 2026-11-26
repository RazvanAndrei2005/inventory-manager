"use client";

import React, { useCallback, useEffect, useState } from "react";
import { Ban, CheckCircle2, Plus, Trash2, X } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { useAuth } from "@/context/AuthContext";
import { ApiError, apiFetch } from "@/lib/api";
import type { Order, OrderStatus, PageResponse, Product } from "@/types/api";

const NEXT_STATUS: Partial<Record<OrderStatus, OrderStatus>> = {
  PENDING: "CONFIRMED",
  CONFIRMED: "SHIPPED",
  SHIPPED: "DELIVERED",
};

const STATUS_STYLES: Record<OrderStatus, string> = {
  PENDING: "bg-amber-500/20 text-amber-300 border-amber-500/30",
  CONFIRMED: "bg-sky-500/20 text-sky-300 border-sky-500/30",
  SHIPPED: "bg-indigo-500/20 text-indigo-300 border-indigo-500/30",
  DELIVERED: "bg-emerald-500/20 text-emerald-300 border-emerald-500/30",
  CANCELLED: "bg-red-500/20 text-red-300 border-red-500/30",
};

interface DraftItem {
  productId: string;
  quantity: string;
}

export default function OrdersPage() {
  const { isManagerOrAdmin } = useAuth();
  const [scope, setScope] = useState<"my" | "all">("my");
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [page, setPage] = useState(0);
  const [orders, setOrders] = useState<PageResponse<Order> | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [draftItems, setDraftItems] = useState<DraftItem[]>([{ productId: "", quantity: "1" }]);
  const [submitting, setSubmitting] = useState(false);

  const loadOrders = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const endpoint = scope === "all" && isManagerOrAdmin ? "/api/orders" : "/api/orders/my";
      const params = new URLSearchParams({ page: String(page), size: "10", sort: "createdAt,desc" });
      if (statusFilter) params.set("status", statusFilter);
      const data = await apiFetch<PageResponse<Order>>(`${endpoint}?${params.toString()}`);
      setOrders(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load orders");
    } finally {
      setLoading(false);
    }
  }, [scope, isManagerOrAdmin, page, statusFilter]);

  useEffect(() => {
    void loadOrders();
  }, [loadOrders]);

  useEffect(() => {
    apiFetch<PageResponse<Product>>("/api/products?size=100")
      .then((res) => setProducts(res.content))
      .catch(() => {});
  }, []);

  const handleAdvanceStatus = async (order: Order, nextStatus: OrderStatus) => {
    setError(null);
    try {
      await apiFetch<Order>(`/api/orders/${order.id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status: nextStatus }),
      });
      await loadOrders();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Status update failed");
    }
  };

  const handleCancel = async (order: Order) => {
    if (!confirm(`Cancel order #${order.id}? Stock will be restored.`)) return;
    setError(null);
    try {
      await apiFetch<Order>(`/api/orders/${order.id}/cancel`, { method: "POST" });
      await loadOrders();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Cancel failed");
    }
  };

  const handleCreateOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const items = draftItems.map((i) => ({
        productId: Number(i.productId),
        quantity: Number(i.quantity),
      }));
      await apiFetch<Order>("/api/orders", {
        method: "POST",
        body: JSON.stringify({ items }),
      });
      setModalOpen(false);
      setDraftItems([{ productId: "", quantity: "1" }]);
      await loadOrders();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create order");
    } finally {
      setSubmitting(false);
    }
  };

  const estimatedTotal = draftItems.reduce((sum, item) => {
    const product = products.find((p) => String(p.id) === item.productId);
    const qty = Number(item.quantity) || 0;
    return sum + (product ? product.price * qty : 0);
  }, 0);

  return (
    <AppShell>
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-white">Orders</h1>
            <p className="text-sm text-slate-400">
              Place orders with automatic stock deduction, price lock, and status workflow.
            </p>
          </div>
          <button
            onClick={() => {
              setDraftItems([{ productId: products[0] ? String(products[0].id) : "", quantity: "1" }]);
              setModalOpen(true);
            }}
            className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 px-4 py-2 text-sm font-semibold text-white transition cursor-pointer"
          >
            <Plus className="h-4 w-4" />
            Place Order
          </button>
        </div>

        {error && (
          <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
            {error}
          </div>
        )}

        <div className="flex flex-wrap items-center justify-between gap-3">
          {isManagerOrAdmin && (
            <div className="inline-flex rounded-lg border border-slate-800 bg-slate-900 p-1 text-xs font-medium">
              <button
                onClick={() => {
                  setScope("my");
                  setPage(0);
                }}
                className={`px-3 py-1.5 rounded-md transition cursor-pointer ${
                  scope === "my" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"
                }`}
              >
                My Orders
              </button>
              <button
                onClick={() => {
                  setScope("all");
                  setPage(0);
                }}
                className={`px-3 py-1.5 rounded-md transition cursor-pointer ${
                  scope === "all" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"
                }`}
              >
                All Orders
              </button>
            </div>
          )}

          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-white"
          >
            <option value="">All Statuses</option>
            {(["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"] as OrderStatus[]).map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-4">
          {loading ? (
            <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-8 text-center text-sm text-slate-400">
              Loading orders...
            </div>
          ) : !orders || orders.content.length === 0 ? (
            <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-8 text-center text-sm text-slate-400">
              No orders found.
            </div>
          ) : (
            orders.content.map((order) => {
              const nextStatus = NEXT_STATUS[order.status];
              const canCancel = order.status === "PENDING" || order.status === "CONFIRMED";
              return (
                <div
                  key={order.id}
                  className="rounded-xl border border-slate-800 bg-slate-900/70 overflow-hidden"
                >
                  <div className="px-5 py-4 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <span className="font-bold text-white">Order #{order.id}</span>
                      <span
                        className={`text-xs font-semibold px-2.5 py-0.5 rounded-full border ${STATUS_STYLES[order.status]}`}
                      >
                        {order.status}
                      </span>
                      <span className="text-xs text-slate-400">
                        by <strong className="text-slate-200">{order.username}</strong> &middot;{" "}
                        {new Date(order.createdAt).toLocaleString()}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-white mr-2">
                        ${order.totalAmount.toFixed(2)}
                      </span>
                      {isManagerOrAdmin && nextStatus && (
                        <button
                          onClick={() => void handleAdvanceStatus(order, nextStatus)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white cursor-pointer"
                        >
                          <CheckCircle2 className="h-3.5 w-3.5" />
                          Mark {nextStatus}
                        </button>
                      )}
                      {canCancel && (
                        <button
                          onClick={() => void handleCancel(order)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-red-600/20 text-red-300 hover:bg-red-600/30 border border-red-500/30 cursor-pointer"
                        >
                          <Ban className="h-3.5 w-3.5" />
                          Cancel
                        </button>
                      )}
                    </div>
                  </div>

                  <div className="px-5 py-3">
                    <table className="w-full text-xs text-left">
                      <thead className="text-slate-400">
                        <tr>
                          <th className="py-1.5">Product</th>
                          <th className="py-1.5">SKU</th>
                          <th className="py-1.5 text-right">Qty</th>
                          <th className="py-1.5 text-right">Unit Price</th>
                          <th className="py-1.5 text-right">Line Total</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-800/70">
                        {order.items.map((item) => (
                          <tr key={item.id}>
                            <td className="py-2 text-slate-200 font-medium">{item.productName}</td>
                            <td className="py-2 font-mono text-slate-400">{item.productSku}</td>
                            <td className="py-2 text-right text-slate-200">{item.quantity}</td>
                            <td className="py-2 text-right text-slate-300">${item.unitPrice.toFixed(2)}</td>
                            <td className="py-2 text-right text-white font-medium">${item.lineTotal.toFixed(2)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {orders && orders.totalPages > 1 && (
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>
              Page {orders.number + 1} of {orders.totalPages}
            </span>
            <div className="space-x-2">
              <button
                disabled={orders.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-slate-200 cursor-pointer"
              >
                Previous
              </button>
              <button
                disabled={orders.last}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-slate-200 cursor-pointer"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="w-full max-w-lg rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-2xl">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-white">Place New Order</h2>
              <button onClick={() => setModalOpen(false)} className="text-slate-400 hover:text-white cursor-pointer">
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleCreateOrder} className="space-y-4">
              <div className="space-y-2.5 max-h-72 overflow-y-auto pr-1">
                {draftItems.map((item, idx) => (
                  <div key={idx} className="flex items-center gap-2">
                    <select
                      required
                      value={item.productId}
                      onChange={(e) => {
                        const copy = [...draftItems];
                        copy[idx].productId = e.target.value;
                        setDraftItems(copy);
                      }}
                      className="flex-1 rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                    >
                      <option value="">Select product</option>
                      {products.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name} (${p.price.toFixed(2)} — stock: {p.stockQuantity})
                        </option>
                      ))}
                    </select>
                    <input
                      type="number"
                      min="1"
                      required
                      value={item.quantity}
                      onChange={(e) => {
                        const copy = [...draftItems];
                        copy[idx].quantity = e.target.value;
                        setDraftItems(copy);
                      }}
                      className="w-20 rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                    />
                    {draftItems.length > 1 && (
                      <button
                        type="button"
                        onClick={() => setDraftItems(draftItems.filter((_, i) => i !== idx))}
                        className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg cursor-pointer"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    )}
                  </div>
                ))}
              </div>

              <button
                type="button"
                onClick={() => setDraftItems([...draftItems, { productId: "", quantity: "1" }])}
                className="text-xs font-medium text-indigo-400 hover:text-indigo-300 cursor-pointer"
              >
                + Add another product
              </button>

              <div className="flex items-center justify-between border-t border-slate-800 pt-4">
                <span className="text-sm text-slate-300">
                  Estimated Total: <strong className="text-white">${estimatedTotal.toFixed(2)}</strong>
                </span>
                <div className="space-x-2">
                  <button
                    type="button"
                    onClick={() => setModalOpen(false)}
                    className="px-4 py-2 rounded-lg text-sm bg-slate-800 text-slate-200 hover:bg-slate-700 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="px-4 py-2 rounded-lg text-sm font-semibold bg-indigo-600 hover:bg-indigo-500 text-white disabled:opacity-50 cursor-pointer"
                  >
                    {submitting ? "Placing..." : "Submit Order"}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  );
}
