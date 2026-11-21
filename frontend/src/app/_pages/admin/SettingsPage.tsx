"use client";
import { useEffect, useState } from "react";
import { api } from "@/features/timetable/api";
import type { AcademicYear } from "@/features/timetable/types";
export default function SettingsPage() {
  const [status, setStatus] = useState("Checking connection...");
  const [busy, setBusy] = useState(true);
  function check(isCurrent: () => boolean = () => true) {
    return api<AcademicYear[]>("academic-years")
      .then(years => { if (isCurrent()) setStatus(`Connected to timetable service. ${years.length} academic years available.`); })
      .catch((e: unknown) => { if (isCurrent()) setStatus(e instanceof Error ? e.message : "Connection failed."); })
      .finally(() => { if (isCurrent()) setBusy(false); });
  }
  useEffect(() => {
    let active = true;
    void check(() => active);
    return () => { active = false; };
  }, []);
  return <div className="mx-auto max-w-4xl px-4 py-8 sm:px-8"><h1 className="text-3xl font-bold text-[#4b2a0a]">Service status</h1>
    <section className="mt-6 space-y-5 rounded-2xl border border-[#ebdcb9] bg-white p-6"><h2 className="font-bold text-[#4b2a0a]">Timetable service</h2><p role="status" className="text-sm text-slate-600">{status}</p><button disabled={busy} onClick={() => { setBusy(true); setStatus("Checking connection..."); void check(); }} className="rounded-xl bg-[#4b2a0a] px-5 py-3 text-sm font-semibold text-white disabled:opacity-50">Check connection</button>
    <p className="border-t border-[#ebdcb9] pt-4 text-sm text-slate-600">Academic dates and exam durations are entered for each setup. Institution-wide scheduling preferences are not available yet.</p></section>
  </div>;
}
