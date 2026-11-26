"use client";

import React, { useCallback, useEffect, useState } from "react";
import { Plus, X } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { useAuth } from "@/context/AuthContext";
import { ApiError, apiFetch } from "@/lib/api";
import type { PageResponse, Product, StockMovement, StockMovementType } from "@/types/api";

const TYPE_STYLES: Record<StockMovementType, string> = {
  INITIAL_STOCK: "bg-sky-500/20 text-sky-300 border-sky-500/30",
  RESTOCK: "bg-emerald-500/20 text-emerald-300 border-emerald-500/30",
  SALE: "bg-indigo-500/20 text-indigo-300 border-indigo-500/30",
  ADJUSTMENT: "bg-amber-500/20 text-amber-300 border-amber-500/30",
};

export default function StockMovementsPage() {
  const { isManagerOrAdmin } = useAuth();
  const [data, setData] = useState<PageResponse<StockMovement> | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [page, setPage] = useState(0);
  const [productId, setProductId] = useState("");
  const [type, setType] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [adjProductId, setAdjProductId] = useState("");
  const [adjChange, setAdjChange] = useState("-1");
  const [adjReason, setAdjReason] = useState("");
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    if (!isManagerOrAdmin) return;
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ page: String(page), size: "15", sort: "createdAt,desc" });
      if (productId) params.set("productId", productId);
      if (type) params.set("type", type);
      const res = await apiFetch<PageResponse<StockMovement>>(`/api/stock-movements?${params.toString()}`);
      setData(res);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load stock movements");
    } finally {
      setLoading(false);
    }
  }, [isManagerOrAdmin, page, productId, type]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    apiFetch<PageResponse<Product>>("/api/products?size=100")
      .then((res) => setProducts(res.content))
      .catch(() => {});
  }, []);

  const handleAdjust = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await apiFetch<StockMovement>("/api/stock-movements/adjustments", {
        method: "POST",
        body: JSON.stringify({
          productId: Number(adjProductId),
          quantityChange: Number(adjChange),
          reason: adjReason,
        }),
      });
      setModalOpen(false);
      setAdjReason("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Adjustment failed");
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppShell>
      {!isManagerOrAdmin ? (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-6 text-sm text-red-300">
          Access denied. Stock movements are restricted to ADMIN and MANAGER roles.
        </div>
      ) : (
        <div className="space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <h1 className="text-2xl font-bold text-white">Stock Movements</h1>
              <p className="text-sm text-slate-400">Audit trail of every stock change across all products.</p>
            </div>
            <button
              onClick={() => {
                setAdjProductId(products[0] ? String(products[0].id) : "");
                setAdjChange("-1");
                setAdjReason("");
                setModalOpen(true);
              }}
              className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 px-4 py-2 text-sm font-semibold text-white transition cursor-pointer"
            >
              <Plus className="h-4 w-4" />
              Manual Adjustment
            </button>
          </div>

          {error && (
            <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
              {error}
            </div>
          )}

          <div className="flex flex-wrap gap-3">
            <select
              value={productId}
              onChange={(e) => {
                setProductId(e.target.value);
                setPage(0);
              }}
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-white"
            >
              <option value="">All Products</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.sku})
                </option>
              ))}
            </select>

            <select
              value={type}
              onChange={(e) => {
                setType(e.target.value);
                setPage(0);
              }}
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-white"
            >
              <option value="">All Types</option>
              {(["INITIAL_STOCK", "RESTOCK", "SALE", "ADJUSTMENT"] as StockMovementType[]).map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>

          <div className="rounded-xl border border-slate-800 bg-slate-900/70 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="bg-slate-900 text-slate-400 text-xs uppercase">
                  <tr>
                    <th className="px-5 py-3">Date</th>
                    <th className="px-5 py-3">Product</th>
                    <th className="px-5 py-3">Type</th>
                    <th className="px-5 py-3 text-right">Change</th>
                    <th className="px-5 py-3 text-right">After</th>
                    <th className="px-5 py-3">Reason</th>
                    <th className="px-5 py-3">By</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {loading ? (
                    <tr>
                      <td colSpan={7} className="px-5 py-8 text-center text-slate-400">
                        Loading movements...
                      </td>
                    </tr>
                  ) : !data || data.content.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="px-5 py-8 text-center text-slate-400">
                        No stock movements found.
                      </td>
                    </tr>
                  ) : (
                    data.content.map((m) => (
                      <tr key={m.id} className="hover:bg-slate-800/40">
                        <td className="px-5 py-3 text-xs text-slate-400 whitespace-nowrap">
                          {new Date(m.createdAt).toLocaleString()}
                        </td>
                        <td className="px-5 py-3 font-medium text-white">
                          {m.productName}{" "}
                          <span className="font-mono text-xs text-slate-400">({m.productSku})</span>
                        </td>
                        <td className="px-5 py-3">
                          <span
                            className={`text-xs font-semibold px-2 py-0.5 rounded-full border ${TYPE_STYLES[m.type]}`}
                          >
                            {m.type}
                          </span>
                        </td>
                        <td
                          className={`px-5 py-3 text-right font-semibold ${
                            m.quantityChange > 0 ? "text-emerald-400" : "text-red-400"
                          }`}
                        >
                          {m.quantityChange > 0 ? `+${m.quantityChange}` : m.quantityChange}
                        </td>
                        <td className="px-5 py-3 text-right text-slate-200">{m.quantityAfter}</td>
                        <td className="px-5 py-3 text-slate-300">{m.reason || "—"}</td>
                        <td className="px-5 py-3 text-slate-300">{m.createdByUsername}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {data && data.totalPages > 1 && (
              <div className="px-5 py-3 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
                <span>
                  Page {data.number + 1} of {data.totalPages}
                </span>
                <div className="space-x-2">
                  <button
                    disabled={data.first}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    className="px-3 py-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-slate-200 cursor-pointer"
                  >
                    Previous
                  </button>
                  <button
                    disabled={data.last}
                    onClick={() => setPage((p) => p + 1)}
                    className="px-3 py-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-slate-200 cursor-pointer"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-2xl">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-white">Manual Stock Adjustment</h2>
              <button onClick={() => setModalOpen(false)} className="text-slate-400 hover:text-white cursor-pointer">
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleAdjust} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Product</label>
                <select
                  required
                  value={adjProductId}
                  onChange={(e) => setAdjProductId(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                >
                  <option value="">Select product</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} (stock: {p.stockQuantity})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">
                  Quantity Change (positive or negative, non-zero)
                </label>
                <input
                  type="number"
                  required
                  value={adjChange}
                  onChange={(e) => setAdjChange(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Reason</label>
                <input
                  required
                  maxLength={255}
                  value={adjReason}
                  onChange={(e) => setAdjReason(e.target.value)}
                  placeholder="e.g., Damaged in warehouse"
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                />
              </div>
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 rounded-lg text-sm bg-slate-800 text-slate-200 hover:bg-slate-700 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 rounded-lg text-sm font-semibold bg-indigo-600 hover:bg-indigo-500 text-white disabled:opacity-50 cursor-pointer"
                >
                  {saving ? "Saving..." : "Apply Adjustment"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  );
}
