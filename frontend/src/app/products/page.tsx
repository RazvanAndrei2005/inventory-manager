"use client";

import React, { useCallback, useEffect, useState } from "react";
import { PackagePlus, Pencil, Plus, Search, Trash2, X } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { useAuth } from "@/context/AuthContext";
import { ApiError, apiFetch } from "@/lib/api";
import type { Category, PageResponse, Product } from "@/types/api";

export default function ProductsPage() {
  const { isAdmin, isManagerOrAdmin } = useAuth();
  const [products, setProducts] = useState<PageResponse<Product> | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [page, setPage] = useState(0);
  const [name, setName] = useState("");
  const [sku, setSku] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Create / Edit modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [formName, setFormName] = useState("");
  const [formSku, setFormSku] = useState("");
  const [formPrice, setFormPrice] = useState("");
  const [formStock, setFormStock] = useState("");
  const [formCategoryId, setFormCategoryId] = useState("");
  const [saving, setSaving] = useState(false);

  // Restock modal state
  const [restockTarget, setRestockTarget] = useState<Product | null>(null);
  const [restockQty, setRestockQty] = useState("10");

  const loadProducts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ page: String(page), size: "10", sort: "id,asc" });
      if (name.trim()) params.set("name", name.trim());
      if (sku.trim()) params.set("sku", sku.trim());
      if (categoryId) params.set("categoryId", categoryId);
      if (minPrice) params.set("minPrice", minPrice);
      if (maxPrice) params.set("maxPrice", maxPrice);
      const data = await apiFetch<PageResponse<Product>>(`/api/products?${params.toString()}`);
      setProducts(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load products");
    } finally {
      setLoading(false);
    }
  }, [page, name, sku, categoryId, minPrice, maxPrice]);

  useEffect(() => {
    void loadProducts();
  }, [loadProducts]);

  useEffect(() => {
    apiFetch<PageResponse<Category>>("/api/categories?size=100")
      .then((res) => setCategories(res.content))
      .catch(() => {});
  }, []);

  const openCreate = () => {
    setEditing(null);
    setFormName("");
    setFormSku("");
    setFormPrice("");
    setFormStock("0");
    setFormCategoryId(categories[0] ? String(categories[0].id) : "");
    setModalOpen(true);
  };

  const openEdit = (p: Product) => {
    setEditing(p);
    setFormName(p.name);
    setFormSku(p.sku);
    setFormPrice(String(p.price));
    setFormStock(String(p.stockQuantity));
    setFormCategoryId(p.category ? String(p.category.id) : "");
    setModalOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const body = JSON.stringify({
        name: formName,
        sku: formSku,
        price: Number(formPrice),
        stockQuantity: Number(formStock),
        categoryId: Number(formCategoryId),
      });
      if (editing) {
        await apiFetch<Product>(`/api/products/${editing.id}`, { method: "PUT", body });
      } else {
        await apiFetch<Product>("/api/products", { method: "POST", body });
      }
      setModalOpen(false);
      await loadProducts();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save product");
    } finally {
      setSaving(false);
    }
  };

  const handleRestock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!restockTarget) return;
    setError(null);
    try {
      await apiFetch<Product>(
        `/api/products/${restockTarget.id}/restock?quantity=${encodeURIComponent(restockQty)}`,
        { method: "PATCH" }
      );
      setRestockTarget(null);
      await loadProducts();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Restock failed");
    }
  };

  const handleDelete = async (p: Product) => {
    if (!confirm(`Delete product "${p.name}"?`)) return;
    setError(null);
    try {
      await apiFetch<void>(`/api/products/${p.id}`, { method: "DELETE" });
      await loadProducts();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Delete failed");
    }
  };

  return (
    <AppShell>
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-white">Products</h1>
            <p className="text-sm text-slate-400">Filter, manage, and restock inventory items.</p>
          </div>
          {isManagerOrAdmin && (
            <button
              onClick={openCreate}
              className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 px-4 py-2 text-sm font-semibold text-white transition cursor-pointer"
            >
              <Plus className="h-4 w-4" />
              New Product
            </button>
          )}
        </div>

        {error && (
          <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3 rounded-xl border border-slate-800 bg-slate-900/70 p-4">
          <div className="relative">
            <Search className="h-4 w-4 text-slate-500 absolute left-3 top-3" />
            <input
              value={name}
              onChange={(e) => {
                setPage(0);
                setName(e.target.value);
              }}
              placeholder="Search name..."
              className="w-full rounded-lg border border-slate-700 bg-slate-950 pl-9 pr-3 py-2 text-sm text-white placeholder-slate-500 focus:border-indigo-500 focus:outline-none"
            />
          </div>
          <input
            value={sku}
            onChange={(e) => {
              setPage(0);
              setSku(e.target.value);
            }}
            placeholder="Exact SKU..."
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white placeholder-slate-500 focus:border-indigo-500 focus:outline-none"
          />
          <select
            value={categoryId}
            onChange={(e) => {
              setPage(0);
              setCategoryId(e.target.value);
            }}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
          <input
            type="number"
            step="0.01"
            min="0"
            value={minPrice}
            onChange={(e) => {
              setPage(0);
              setMinPrice(e.target.value);
            }}
            placeholder="Min price"
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white placeholder-slate-500 focus:border-indigo-500 focus:outline-none"
          />
          <input
            type="number"
            step="0.01"
            min="0"
            value={maxPrice}
            onChange={(e) => {
              setPage(0);
              setMaxPrice(e.target.value);
            }}
            placeholder="Max price"
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white placeholder-slate-500 focus:border-indigo-500 focus:outline-none"
          />
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/70 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-900 text-slate-400 text-xs uppercase">
                <tr>
                  <th className="px-5 py-3">Name</th>
                  <th className="px-5 py-3">SKU</th>
                  <th className="px-5 py-3">Category</th>
                  <th className="px-5 py-3 text-right">Price</th>
                  <th className="px-5 py-3 text-right">Stock</th>
                  {isManagerOrAdmin && <th className="px-5 py-3 text-right">Actions</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {loading ? (
                  <tr>
                    <td colSpan={6} className="px-5 py-8 text-center text-slate-400">
                      Loading products...
                    </td>
                  </tr>
                ) : !products || products.content.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-5 py-8 text-center text-slate-400">
                      No products found.
                    </td>
                  </tr>
                ) : (
                  products.content.map((p) => (
                    <tr key={p.id} className="hover:bg-slate-800/40">
                      <td className="px-5 py-3.5 font-medium text-white">{p.name}</td>
                      <td className="px-5 py-3.5 font-mono text-xs text-slate-300">{p.sku}</td>
                      <td className="px-5 py-3.5 text-slate-300">{p.category?.name ?? "—"}</td>
                      <td className="px-5 py-3.5 text-right text-slate-200">${p.price.toFixed(2)}</td>
                      <td className="px-5 py-3.5 text-right">
                        <span
                          className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                            p.stockQuantity <= 5
                              ? "bg-amber-500/20 text-amber-300 border border-amber-500/30"
                              : "bg-emerald-500/20 text-emerald-300 border border-emerald-500/30"
                          }`}
                        >
                          {p.stockQuantity}
                        </span>
                      </td>
                      {isManagerOrAdmin && (
                        <td className="px-5 py-3.5 text-right space-x-2 whitespace-nowrap">
                          <button
                            onClick={() => {
                              setRestockTarget(p);
                              setRestockQty("10");
                            }}
                            title="Restock"
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs bg-emerald-600/20 text-emerald-300 hover:bg-emerald-600/30 cursor-pointer"
                          >
                            <PackagePlus className="h-3.5 w-3.5" />
                            Restock
                          </button>
                          <button
                            onClick={() => openEdit(p)}
                            title="Edit"
                            className="inline-flex items-center p-1.5 rounded-md text-slate-300 hover:bg-slate-700 cursor-pointer"
                          >
                            <Pencil className="h-3.5 w-3.5" />
                          </button>
                          {isAdmin && (
                            <button
                              onClick={() => void handleDelete(p)}
                              title="Delete"
                              className="inline-flex items-center p-1.5 rounded-md text-red-400 hover:bg-red-500/20 cursor-pointer"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </button>
                          )}
                        </td>
                      )}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {products && products.totalPages > 1 && (
            <div className="px-5 py-3 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
              <span>
                Page {products.number + 1} of {products.totalPages} ({products.totalElements} total)
              </span>
              <div className="space-x-2">
                <button
                  disabled={products.first}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="px-3 py-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-slate-200 cursor-pointer"
                >
                  Previous
                </button>
                <button
                  disabled={products.last}
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

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-2xl">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-white">
                {editing ? "Edit Product" : "Create Product"}
              </h2>
              <button onClick={() => setModalOpen(false)} className="text-slate-400 hover:text-white cursor-pointer">
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleSave} className="space-y-3.5">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Name</label>
                <input
                  required
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">SKU</label>
                <input
                  required
                  value={formSku}
                  onChange={(e) => setFormSku(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Price</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={formPrice}
                    onChange={(e) => setFormPrice(e.target.value)}
                    className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Stock</label>
                  <input
                    type="number"
                    min="0"
                    required
                    value={formStock}
                    onChange={(e) => setFormStock(e.target.value)}
                    className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                  />
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Category</label>
                <select
                  required
                  value={formCategoryId}
                  onChange={(e) => setFormCategoryId(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                >
                  <option value="">Select category</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="flex justify-end gap-2 pt-2">
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
                  {saving ? "Saving..." : "Save"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {restockTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="w-full max-w-sm rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-2xl">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-white">Restock {restockTarget.name}</h2>
              <button onClick={() => setRestockTarget(null)} className="text-slate-400 hover:text-white cursor-pointer">
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleRestock} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">
                  Quantity to add (current: {restockTarget.stockQuantity})
                </label>
                <input
                  type="number"
                  min="1"
                  required
                  value={restockQty}
                  onChange={(e) => setRestockQty(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                />
              </div>
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setRestockTarget(null)}
                  className="px-4 py-2 rounded-lg text-sm bg-slate-800 text-slate-200 hover:bg-slate-700 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-lg text-sm font-semibold bg-emerald-600 hover:bg-emerald-500 text-white cursor-pointer"
                >
                  Add Stock
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  );
}
