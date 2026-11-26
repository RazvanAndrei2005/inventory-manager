"use client";

import React, { useCallback, useEffect, useState } from "react";
import { Pencil, Plus, Trash2, X } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { useAuth } from "@/context/AuthContext";
import { ApiError, apiFetch } from "@/lib/api";
import type { Category, PageResponse } from "@/types/api";

export default function CategoriesPage() {
  const { isAdmin, isManagerOrAdmin } = useAuth();
  const [data, setData] = useState<PageResponse<Category> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<PageResponse<Category>>(`/api/categories?page=${page}&size=10&sort=name,asc`);
      setData(res);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load categories");
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    setName("");
    setDescription("");
    setModalOpen(true);
  };

  const openEdit = (cat: Category) => {
    setEditing(cat);
    setName(cat.name);
    setDescription(cat.description ?? "");
    setModalOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const body = JSON.stringify({ name, description });
      if (editing) {
        await apiFetch<Category>(`/api/categories/${editing.id}`, { method: "PUT", body });
      } else {
        await apiFetch<Category>("/api/categories", { method: "POST", body });
      }
      setModalOpen(false);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save category");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (cat: Category) => {
    if (!confirm(`Delete category "${cat.name}"?`)) return;
    setError(null);
    try {
      await apiFetch<void>(`/api/categories/${cat.id}`, { method: "DELETE" });
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Delete failed");
    }
  };

  return (
    <AppShell>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white">Categories</h1>
            <p className="text-sm text-slate-400">Organize products into categories.</p>
          </div>
          {isManagerOrAdmin && (
            <button
              onClick={openCreate}
              className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 px-4 py-2 text-sm font-semibold text-white transition cursor-pointer"
            >
              <Plus className="h-4 w-4" />
              New Category
            </button>
          )}
        </div>

        {error && (
          <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
            {error}
          </div>
        )}

        <div className="rounded-xl border border-slate-800 bg-slate-900/70 overflow-hidden">
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-900 text-slate-400 text-xs uppercase">
              <tr>
                <th className="px-5 py-3">ID</th>
                <th className="px-5 py-3">Name</th>
                <th className="px-5 py-3">Description</th>
                {isManagerOrAdmin && <th className="px-5 py-3 text-right">Actions</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {loading ? (
                <tr>
                  <td colSpan={4} className="px-5 py-8 text-center text-slate-400">
                    Loading categories...
                  </td>
                </tr>
              ) : !data || data.content.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-5 py-8 text-center text-slate-400">
                    No categories yet.
                  </td>
                </tr>
              ) : (
                data.content.map((c) => (
                  <tr key={c.id} className="hover:bg-slate-800/40">
                    <td className="px-5 py-3.5 text-slate-400">#{c.id}</td>
                    <td className="px-5 py-3.5 font-medium text-white">{c.name}</td>
                    <td className="px-5 py-3.5 text-slate-300">{c.description || "—"}</td>
                    {isManagerOrAdmin && (
                      <td className="px-5 py-3.5 text-right space-x-2">
                        <button
                          onClick={() => openEdit(c)}
                          className="inline-flex items-center p-1.5 rounded-md text-slate-300 hover:bg-slate-700 cursor-pointer"
                        >
                          <Pencil className="h-3.5 w-3.5" />
                        </button>
                        {isAdmin && (
                          <button
                            onClick={() => void handleDelete(c)}
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

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-2xl">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-white">
                {editing ? "Edit Category" : "Create Category"}
              </h2>
              <button onClick={() => setModalOpen(false)} className="text-slate-400 hover:text-white cursor-pointer">
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleSave} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Name</label>
                <input
                  required
                  maxLength={100}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Description</label>
                <textarea
                  rows={3}
                  maxLength={255}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
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
                  {saving ? "Saving..." : "Save"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  );
}
