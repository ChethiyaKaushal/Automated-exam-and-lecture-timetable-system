"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import {
  CalendarDays,
  Sparkles,
  GraduationCap,
  Settings,
  LogOut,
  X,
  Menu,
} from "lucide-react";
import { useState } from "react";

// Exactly the 3 main required navigation items
const menuItems = [
  {
    name: "Lecture Timetable",
    href: "/admin/timetable",
    icon: CalendarDays,
    description: "Weekly lecture sessions & batches",
  },
  {
    name: "Exam Timetable",
    href: "/admin/exam-timetable",
    icon: GraduationCap,
    description: "Examination hall schedules",
  },
  {
    name: "Timetable Setup",
    href: "/admin/generate",
    icon: Sparkles,
    description: "Academic periods & requirements",
  },
];

export default function AdminSidebar() {
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <>
      {/* Mobile menu trigger */}
      <div className="fixed top-3 left-4 z-40 md:hidden">
        <button
          type="button"
          aria-label="Open navigation menu"
          onClick={() => setMobileOpen(true)}
          className="flex h-10 w-10 items-center justify-center rounded-lg border border-[#f5c04a]/40 bg-[#4b2a0a] text-[#f5c04a] shadow-md transition hover:bg-[#3a2007]"
        >
          <Menu size={22} />
        </button>
      </div>

      {/* Mobile overlay */}
      {mobileOpen && (
        <div
          role="presentation"
          onClick={() => setMobileOpen(false)}
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-xs md:hidden"
        />
      )}

      {/* Main Sidebar */}
      <aside
        className={`fixed left-0 top-0 z-50 flex h-screen w-64 flex-col border-r border-[#e8d7b8] bg-[#4b2a0a] text-white shadow-xl transition-transform duration-300 md:translate-x-0 ${
          mobileOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        {/* Brand Header */}
        <div className="flex items-center gap-3 border-b border-[#633a12] bg-[#3e2106] px-5 py-4">
          <Image
            src="/images/university-logo.png"
            alt="University of Ruhuna"
            width={48}
            height={48}
            className="h-12 w-auto shrink-0 drop-shadow rounded-full bg-white/10 p-1"
          />

          <div className="min-w-0 flex-1">
            <h2 className="text-sm font-bold tracking-tight text-[#f5c04a] truncate">
              University of Ruhuna
            </h2>
            <p className="text-xs font-semibold text-gray-200 truncate">
              Faculty of Engineering
            </p>
            <p className="mt-0.5 text-[10px] font-medium tracking-wider uppercase text-amber-200/80">
              MIS Timetable Portal
            </p>
          </div>

          <button
            type="button"
            aria-label="Close navigation menu"
            onClick={() => setMobileOpen(false)}
            className="rounded-lg p-1 text-amber-200/80 hover:bg-white/10 md:hidden"
          >
            <X size={20} />
          </button>
        </div>

        {/* Section Label */}
        <div className="px-5 pt-5 pb-2">
          <p className="text-[11px] font-bold tracking-wider uppercase text-[#f5c04a]/90">
            Timetable Management
          </p>
        </div>

        {/* Main 3 Navigation Items */}
        <nav className="flex-1 space-y-1.5 px-3 py-1 overflow-y-auto">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const active = pathname === item.href;

            return (
              <Link
                key={item.name}
                href={item.href}
                onClick={() => setMobileOpen(false)}
                aria-current={active ? "page" : undefined}
                className={`group flex items-center gap-3 rounded-xl px-3.5 py-3 text-sm font-medium transition-all duration-150 ${
                  active
                    ? "bg-[#f5c04a] text-[#4b2a0a] font-bold shadow-md shadow-black/20"
                    : "text-amber-100/90 hover:bg-[#603710] hover:text-white"
                }`}
              >
                <div
                  className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg transition-colors ${
                    active
                      ? "bg-[#4b2a0a] text-[#f5c04a]"
                      : "bg-[#3a2007] text-[#f5c04a] group-hover:bg-[#4b2a0a]"
                  }`}
                >
                  <Icon size={19} />
                </div>
                <div className="min-w-0">
                  <div className="truncate">{item.name}</div>
                  <div
                    className={`text-[10px] font-normal truncate ${
                      active ? "text-[#4b2a0a]/80" : "text-amber-200/60"
                    }`}
                  >
                    {item.description}
                  </div>
                </div>
              </Link>
            );
          })}
        </nav>

        {/* System & Settings Footer */}
        <div className="border-t border-[#633a12] bg-[#3e2106]/70 p-3 space-y-1">
          <Link
            href="/admin/settings"
            onClick={() => setMobileOpen(false)}
            aria-current={pathname === "/admin/settings" ? "page" : undefined}
            className={`flex items-center gap-3 rounded-lg px-3 py-2 text-xs font-medium transition ${
              pathname === "/admin/settings"
                ? "bg-[#f5c04a] text-[#4b2a0a] font-bold"
                : "text-amber-200/80 hover:bg-white/10 hover:text-white"
            }`}
          >
            <Settings size={16} />
            <span>Service status</span>
          </Link>

          <Link
            href="/"
            onClick={() => setMobileOpen(false)}
            className="flex items-center gap-3 rounded-lg px-3 py-2 text-xs font-medium text-amber-200/80 transition hover:bg-rose-900/50 hover:text-rose-200"
          >
            <LogOut size={16} />
            <span>Back to home</span>
          </Link>

          <div className="pt-2 px-1 text-center">
            <p className="text-[10px] text-amber-200/50">
              UOR Faculty of Engineering &copy; 2026
            </p>
          </div>
        </div>
      </aside>
    </>
  );
}
