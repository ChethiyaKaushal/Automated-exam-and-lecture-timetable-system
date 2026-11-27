export default function AdminHeader() {
  return <header className="fixed left-0 right-0 top-0 z-30 flex h-16 items-center justify-between gap-3 border-b border-[#ebdcb9] bg-[#fbf7ee] px-4 shadow-sm md:px-8">
    <div className="pl-12"><p className="text-sm font-bold text-[#4b2a0a] sm:text-lg">Timetable Management System</p><p className="hidden text-xs text-[#7a4a1d] sm:block">Faculty of Engineering · University of Ruhuna</p></div>
    <span className="hidden rounded-full border border-[#ebdcb9] px-3 py-1 text-xs text-[#7a4a1d] sm:block">Planning workspace</span>
  </header>;
}
