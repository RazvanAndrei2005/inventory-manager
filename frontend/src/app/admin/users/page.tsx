"use client";

import React, { useCallback, useEffect, useState } from "react";
import { AppShell } from "@/components/AppShell";
import { useAuth } from "@/context/AuthContext";
import { ApiError, apiFetch } from "@/lib/api";
import type { PageResponse, Role, UserResponse } from "@/types/api";

export default function AdminUsersPage() {
  const { user: currentUser, isAdmin } = useAuth();
  const [data, setData] = useState<PageResponse<UserResponse> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!isAdmin) return;
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<PageResponse<UserResponse>>(`/api/admin/users?page=${page}&size=10&sort=username,asc`);
      setData(res);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load users");
    } finally {
      setLoading(false);
    }
  }, [isAdmin, page]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleRoleChange = async (target: UserResponse, role: Role) => {
    setError(null);
    try {
      await apiFetch<UserResponse>(`/api/admin/users/${target.id}/role`, {
        method: "PATCH",
        body: JSON.stringify({ role }),
      });
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update role");
    }
  };

  const handleToggleStatus = async (target: UserResponse) => {
    setError(null);
    try {
      await apiFetch<UserResponse>(`/api/admin/users/${target.id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ enabled: !target.enabled }),
      });
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update status");
    }
  };

  return (
    <AppShell>
      {!isAdmin ? (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-6 text-sm text-red-300">
          Access denied. User administration is restricted to ADMIN role.
        </div>
      ) : (
        <div className="space-y-6">
          <div>
            <h1 className="text-2xl font-bold text-white">User Management</h1>
            <p className="text-sm text-slate-400">
              Manage user roles and account status (disabling a user revokes their active tokens).
            </p>
          </div>

          {error && (
            <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
              {error}
            </div>
          )}

          <div className="rounded-xl border border-slate-800 bg-slate-900/70 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="bg-slate-900 text-slate-400 text-xs uppercase">
                  <tr>
                    <th className="px-5 py-3">ID</th>
                    <th className="px-5 py-3">Username</th>
                    <th className="px-5 py-3">Email</th>
                    <th className="px-5 py-3">Role</th>
                    <th className="px-5 py-3">Status</th>
                    <th className="px-5 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {loading ? (
                    <tr>
                      <td colSpan={6} className="px-5 py-8 text-center text-slate-400">
                        Loading users...
                      </td>
                    </tr>
                  ) : !data || data.content.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="px-5 py-8 text-center text-slate-400">
                        No users found.
                      </td>
                    </tr>
                  ) : (
                    data.content.map((u) => {
                      const isSelf = currentUser?.id === u.id;
                      return (
                        <tr key={u.id} className="hover:bg-slate-800/40">
                          <td className="px-5 py-3.5 text-slate-400">#{u.id}</td>
                          <td className="px-5 py-3.5 font-medium text-white">
                            {u.username}{" "}
                            {isSelf && (
                              <span className="ml-1 text-[10px] px-1.5 py-0.5 rounded bg-indigo-500/20 text-indigo-300">
                                YOU
                              </span>
                            )}
                          </td>
                          <td className="px-5 py-3.5 text-slate-300">{u.email}</td>
                          <td className="px-5 py-3.5">
                            <select
                              disabled={isSelf}
                              value={u.role}
                              onChange={(e) => void handleRoleChange(u, e.target.value as Role)}
                              className="rounded-md border border-slate-700 bg-slate-950 px-2.5 py-1 text-xs text-white disabled:opacity-50"
                            >
                              <option value="ROLE_USER">ROLE_USER</option>
                              <option value="ROLE_MANAGER">ROLE_MANAGER</option>
                              <option value="ROLE_ADMIN">ROLE_ADMIN</option>
                            </select>
                          </td>
                          <td className="px-5 py-3.5">
                            <span
                              className={`text-xs font-semibold px-2.5 py-0.5 rounded-full border ${
                                u.enabled
                                  ? "bg-emerald-500/20 text-emerald-300 border-emerald-500/30"
                                  : "bg-red-500/20 text-red-300 border-red-500/30"
                              }`}
                            >
                              {u.enabled ? "Active" : "Disabled"}
                            </span>
                          </td>
                          <td className="px-5 py-3.5 text-right">
                            <button
                              disabled={isSelf}
                              onClick={() => void handleToggleStatus(u)}
                              className={`px-3 py-1 rounded-md text-xs font-semibold transition disabled:opacity-40 cursor-pointer ${
                                u.enabled
                                  ? "bg-red-600/20 text-red-300 hover:bg-red-600/30"
                                  : "bg-emerald-600/20 text-emerald-300 hover:bg-emerald-600/30"
                              }`}
                            >
                              {u.enabled ? "Disable" : "Enable"}
                            </button>
                          </td>
                        </tr>
                      );
                    })
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
    </AppShell>
  );
}
