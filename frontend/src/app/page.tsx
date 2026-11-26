"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { AlertTriangle, Box, FolderTree, ShoppingCart } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { useAuth } from "@/context/AuthContext";
import { apiFetch } from "@/lib/api";
import type { Category, Order, PageResponse, Product } from "@/types/api";

export default function DashboardPage() {
  const { user, isManagerOrAdmin } = useAuth();
  const [totalProducts, setTotalProducts] = useState<number>(0);
  const [totalCategories, setTotalCategories] = useState<number>(0);
  const [totalOrders, setTotalOrders] = useState<number>(0);
  const [lowStock, setLowStock] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    const load = async () => {
      try {
        const [productsPage, categoriesPage, lowStockList, ordersPage] = await Promise.all([
          apiFetch<PageResponse<Product>>("/api/products?size=1"),
          apiFetch<PageResponse<Category>>("/api/categories?size=1"),
          apiFetch<Product[]>("/api/products/low-stock?threshold=5"),
          apiFetch<PageResponse<Order>>(isManagerOrAdmin ? "/api/orders?size=1" : "/api/orders/my?size=1"),
        ]);
        setTotalProducts(productsPage.totalElements);
        setTotalCategories(categoriesPage.totalElements);
        setLowStock(lowStockList);
        setTotalOrders(ordersPage.totalElements);
      } catch {
        // handled gracefully
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [user, isManagerOrAdmin]);

  const cards = [
    { label: "Total Products", value: totalProducts, icon: Box, href: "/products", color: "text-indigo-400" },
    { label: "Categories", value: totalCategories, icon: FolderTree, href: "/categories", color: "text-emerald-400" },
    {
      label: isManagerOrAdmin ? "All Orders" : "My Orders",
      value: totalOrders,
      icon: ShoppingCart,
      href: "/orders",
      color: "text-sky-400",
    },
    { label: "Low Stock (<= 5)", value: lowStock.length, icon: AlertTriangle, href: "/products", color: "text-amber-400" },
  ];

  return (
    <AppShell>
      <div className="space-y-8">
        <div>
          <h1 className="text-2xl font-bold text-white">Dashboard</h1>
          <p className="text-sm text-slate-400 mt-1">
            Live overview connected to your Spring Boot API + Neon PostgreSQL.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {cards.map((card) => {
            const Icon = card.icon;
            return (
              <Link
                key={card.label}
                href={card.href}
                className="rounded-xl border border-slate-800 bg-slate-900/70 p-5 hover:border-slate-700 transition"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium uppercase tracking-wider text-slate-400">
                    {card.label}
                  </span>
                  <Icon className={`h-5 w-5 ${card.color}`} />
                </div>
                <p className="mt-3 text-3xl font-bold text-white">
                  {loading ? "—" : card.value}
                </p>
              </Link>
            );
          })}
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/70 overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-800 flex items-center justify-between">
            <h2 className="font-semibold text-white flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-amber-400" />
              Low-Stock Alerts (threshold &le; 5)
            </h2>
            <Link href="/products" className="text-xs text-indigo-400 hover:text-indigo-300">
              View all products &rarr;
            </Link>
          </div>
          {loading ? (
            <div className="p-6 text-sm text-slate-400">Loading...</div>
          ) : lowStock.length === 0 ? (
            <div className="p-6 text-sm text-slate-400">All products have healthy stock levels.</div>
          ) : (
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-900 text-slate-400 text-xs uppercase">
                <tr>
                  <th className="px-5 py-3">Product</th>
                  <th className="px-5 py-3">SKU</th>
                  <th className="px-5 py-3">Category</th>
                  <th className="px-5 py-3 text-right">Stock</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {lowStock.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-800/40">
                    <td className="px-5 py-3 font-medium text-white">{p.name}</td>
                    <td className="px-5 py-3 font-mono text-xs text-slate-300">{p.sku}</td>
                    <td className="px-5 py-3 text-slate-300">{p.category?.name ?? "—"}</td>
                    <td className="px-5 py-3 text-right">
                      <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/20 text-amber-300 border border-amber-500/30">
                        {p.stockQuantity}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </AppShell>
  );
}
