import Link from "next/link";
import { CalendarDays } from "lucide-react";
export default function ScheduleUnavailable({ kind }: { kind: string }) {
  return <div className="mx-auto max-w-5xl px-4 py-8 sm:px-8">
    <h1 className="text-3xl font-bold text-[#4b2a0a]">{kind} timetable</h1>
    <section className="mt-6 rounded-2xl border border-[#ebdcb9] bg-white px-6 py-16 text-center">
      <CalendarDays size={42} className="mx-auto text-[#b88935]" />
      <h2 className="mt-5 text-xl font-bold text-[#4b2a0a]">Schedules are not available yet</h2>
      <p className="mx-auto mt-3 max-w-lg text-sm leading-relaxed text-slate-600">You can save academic periods and exam requirements now. The current service does not provide timetable generation or published schedule retrieval.</p>
      <Link href="/admin/generate" className="mt-6 inline-block rounded-xl bg-[#4b2a0a] px-5 py-3 text-sm font-semibold text-white">Set up an academic period</Link>
    </section>
  </div>;
}
