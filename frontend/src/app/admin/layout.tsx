import AdminHeader from "@/components/layout/AdminHeader";
import AdminSidebar from "@/components/layout/AdminSidebar";

export default function AdminLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <div className="min-h-screen bg-[#fcf9f2] text-slate-800">
      <AdminSidebar />
      <AdminHeader />

      <main className="min-h-screen pt-16">
        {children}
      </main>
    </div>
  );
}
