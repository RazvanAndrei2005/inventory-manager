"use client";

import React, { useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Activity,
  Box,
  FolderTree,
  LayoutDashboard,
  LogOut,
  PackageCheck,
  ShoppingCart,
  Users,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  requiresManager?: boolean;
  requiresAdmin?: boolean;
}

const NAV_ITEMS: NavItem[] = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/products", label: "Products", icon: Box },
  { href: "/categories", label: "Categories", icon: FolderTree },
  { href: "/orders", label: "Orders", icon: ShoppingCart },
  { href: "/stock-movements", label: "Stock Movements", icon: Activity, requiresManager: true },
  { href: "/admin/users", label: "Users", icon: Users, requiresAdmin: true },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const { user, loading, isAdmin, isManagerOrAdmin, logout } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, user, router]);

  if (loading || !user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-950 text-slate-400">
        <div className="flex items-center gap-3">
          <div className="h-5 w-5 animate-spin rounded-full border-2 border-indigo-500 border-t-transparent" />
          <span>Loading workspace...</span>
        </div>
      </div>
    );
  }

  const visibleNav = NAV_ITEMS.filter((item) => {
    if (item.requiresAdmin) return isAdmin;
    if (item.requiresManager) return isManagerOrAdmin;
    return true;
  });

  const roleBadge = user.role.replace("ROLE_", "");

  return (
    <div className="min-h-screen flex bg-slate-950 text-slate-100">
      <aside className="w-64 shrink-0 border-r border-slate-800 bg-slate-900/70 flex flex-col">
        <div className="h-16 px-5 flex items-center gap-2.5 border-b border-slate-800">
          <div className="h-9 w-9 rounded-lg bg-indigo-600 flex items-center justify-center">
            <PackageCheck className="h-5 w-5 text-white" />
          </div>
          <div>
            <p className="font-semibold text-sm leading-tight text-white">Inventory Manager</p>
            <p className="text-xs text-slate-400">Spring Boot 4 + Neon</p>
          </div>
        </div>

        <nav className="flex-1 p-3 space-y-1">
          {visibleNav.map((item) => {
            const Icon = item.icon;
            const active = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                  active
                    ? "bg-indigo-600 text-white shadow-sm"
                    : "text-slate-300 hover:bg-slate-800 hover:text-white"
                }`}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <div className="min-w-0">
              <p className="text-sm font-medium text-white truncate">{user.username}</p>
              <p className="text-xs text-slate-400 truncate">{user.email}</p>
            </div>
            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
              {roleBadge}
            </span>
          </div>
          <button
            onClick={() => void logout()}
            className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-sm font-medium bg-slate-800 hover:bg-slate-700 text-slate-200 transition cursor-pointer"
          >
            <LogOut className="h-4 w-4" />
            Sign Out
          </button>
        </div>
      </aside>

      <main className="flex-1 min-w-0 overflow-auto">
        <div className="max-w-7xl mx-auto p-6 md:p-8">{children}</div>
      </main>
    </div>
  );
}
