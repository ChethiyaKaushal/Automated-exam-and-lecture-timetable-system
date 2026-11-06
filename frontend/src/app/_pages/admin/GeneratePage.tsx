"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { CalendarRange, CheckCircle2, Loader2 } from "lucide-react";
import { api, errorMessages } from "@/features/timetable/api";
import { examApi, storePreview } from "@/features/timetable/examApi";
import type { AcademicYear, Engine, Module, Semester, ExamModuleRow } from "@/features/timetable/types";
import ExamModuleForm from "@/features/timetable/components/ExamModuleForm";
import { formatSemesterName } from "@/features/timetable/utils/formatSemesterName";

type Row = ExamModuleRow;
const input = "mt-2 w-full rounded-xl border border-[#d9c59f] bg-white px-3 py-2.5 text-sm";
const button = "rounded-xl bg-[#4b2a0a] px-5 py-3 text-sm font-semibold text-white hover:bg-[#633a12] disabled:opacity-50 disabled:cursor-not-allowed";

export default function GeneratePage() {
  const router = useRouter();
  const [semesters, setSemesters] = useState<Semester[]>([]);
  const [semesterIds, setSemesterIds] = useState<number[]>([]);
  const [academicYear, setAcademicYear] = useState("");
  const [academicYears, setAcademicYears] = useState<string[]>([]);
  const [loadingYears, setLoadingYears] = useState(true);
  const [yearsError, setYearsError] = useState("");
  const [startingDate, setStartingDate] = useState("");
  const [endingDate, setEndingDate] = useState("");
  const [holiday, setHoliday] = useState("");
  const [holidayDates, setHolidayDates] = useState<string[]>([]);
  const [eventType, setEventType] = useState("EXAM");
  const [modules, setModules] = useState<Row[]>([]);
  const [stage, setStage] = useState<"setup" | "review" | "saved">("setup");
  const [periodId, setPeriodId] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [busyMessage, setBusyMessage] = useState("");
  const [engine, setEngine] = useState<Engine>("gemini");
  const [requirementsSaved, setRequirementsSaved] = useState(false);
  const [previewReady, setPreviewReady] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);
  const setError = (message: string) => setErrors(message ? [message] : []);
  const inFlight = useRef(false);
  const semesterRequest = useRef(0);

  async function loadSemesters(year: string) {
    const requestId = ++semesterRequest.current;
    setSemesters([]); setSemesterIds([]); setModules([]);
    setLoading(Boolean(year)); setError("");
    if (!year) return;
    try {
      const query = new URLSearchParams({ academicYear: year });
      const rows = await api<Semester[]>(`semesters?${query}`);
      if (requestId === semesterRequest.current) setSemesters(rows);
    } catch (e) {
      if (requestId === semesterRequest.current) setError(e instanceof Error ? e.message : "Unable to load semesters.");
    } finally {
      if (requestId === semesterRequest.current) setLoading(false);
    }
  }
  function changeAcademicYear(year: string) {
    setAcademicYear(year);
    void loadSemesters(year);
  }
  function loadAcademicYears(isCurrent: () => boolean = () => true) {
    return api<AcademicYear[]>("academic-years").then(data => {
      if (!isCurrent()) return;
      const years = [...new Set(data.map(item => item.academicYear).filter(year => typeof year === "string" && year.trim()))];
      setAcademicYears(years);
      setAcademicYear(current => years.includes(current) ? current : "");
    }).catch((e: unknown) => {
      if (!isCurrent()) return;
      setAcademicYears([]);
      setAcademicYear("");
      setYearsError(e instanceof Error ? e.message : "Unable to load academic years.");
    }).finally(() => { if (isCurrent()) setLoadingYears(false); });
  }
  useEffect(() => {
    let active = true;
    void loadAcademicYears(() => active);
    return () => { active = false; };
  }, []);

  async function review(e: React.FormEvent) {
    e.preventDefault();
    if (inFlight.current) return;
    if (loading || !semesterIds.length || semesterIds.some(id => !semesters.some(semester => semester.semesterId === id)) || !academicYears.includes(academicYear) || !startingDate || !endingDate || endingDate < startingDate) {
      setError("Select an academic year, a valid date range, and at least one semester."); return;
    }
    inFlight.current = true; setBusy(true); setError("");
    try {
      const rows = await api<Module[]>("modules", { semesterIds });
      setModules(rows.map(m => ({ ...m, hasExam: false, examDurationMinutes: 180 })));
      setStage("review");
    } catch (e) { setError(e instanceof Error ? e.message : "Unable to load modules."); }
    finally { setBusy(false); inFlight.current = false; }
  }

  async function save() {
    if (inFlight.current) return;
    const selectedExams = modules.filter(module => module.hasExam);
    if (eventType === "EXAM" && !selectedExams.length) {
      setError("Select at least one module that has an exam.");
      return;
    }
    const durationErrors = selectedExams.filter(({ examDurationMinutes: minutes }) =>
      !Number.isInteger(minutes) || minutes < 30 || minutes > 270 || minutes % 30 !== 0)
      .map(module => `${module.moduleCode} ${module.moduleName}: enter 30 to 270 minutes in steps of 30.`);
    if (eventType === "EXAM" && durationErrors.length) {
      setErrors(durationErrors);
      return;
    }

    inFlight.current = true; setBusy(true); setError("");
    try {
      setBusyMessage("Saving the calendar period...");
      let id = periodId;
      if (id === null) {
        const result = await api<{ periodId: number; eventType: string }>("setup", {
          startingDate, endingDate, academicYear: academicYear.trim(), eventType, semesterIds,
          holidayDates: eventType === "EXAM" ? holidayDates.filter(date => date >= startingDate && date <= endingDate) : [],
        });
        id = result.periodId;
        setPeriodId(id);
      }
      if (eventType === "LECTURE") {
        setStage("saved");
        return;
      }

      if (!requirementsSaved) {
        setBusyMessage("Saving exam requirements...");
        try {
          await api<void>("requirements", {
            periodIds: [id],
            modules: modules.map(module => ({
              moduleId: module.moduleId,
              hasExam: module.hasExam,
              examDurationMinutes: module.hasExam ? module.examDurationMinutes : null,
            })),
          });
        } catch (error) {
          // A request can finish in Spring after the browser receives a timeout.
          const existing = await examApi.setup(id).catch(() => null);
          const active = existing?.exams.filter(exam => exam.enabled);
          if (active?.length !== selectedExams.length || !selectedExams.every(module =>
            active.some(exam => exam.moduleId === module.moduleId && exam.durationMinutes === module.examDurationMinutes))) {
            throw error;
          }
        }
        setRequirementsSaved(true);
      }

      setBusyMessage(engine === "gemini" ? "Gemini is generating the timetable. This can take several minutes." : "Generating the timetable...");
      const preview = await examApi.preview(id, engine);
      storePreview(id, preview);
      setPreviewReady(true);
      const dashboard = `/admin/exam-timetable?periodId=${id}`;
      if (preview.review.valid && preview.review.studentMembershipsComplete) {
        setBusyMessage("Saving the draft timetable...");
        const saved = await examApi.save(id, preview.assignments, false);
        storePreview(id, null);
        router.push(`${dashboard}&draftId=${saved.timetableId}`);
      } else {
        router.push(dashboard);
      }
    } catch (error) {
      setErrors(errorMessages(error));
    } finally {
      setBusy(false);
      inFlight.current = false;
    }
  }

  const selectedNames = semesters.filter(s => semesterIds.includes(s.semesterId)).map(s => formatSemesterName(s.semesterName)).join(", ");
  return <div className="mx-auto max-w-6xl space-y-6 px-4 py-8 sm:px-8">
    <div className="border-b border-[#ebdcb9] pb-5">
      <p className="text-xs font-bold uppercase tracking-widest text-[#916528]">Academic planning</p>
      <h1 className="mt-2 text-3xl font-bold text-[#4b2a0a]">Timetable setup</h1>
      <p className="mt-2 text-sm text-[#7a4a1d]">Choose an academic period, set exam durations, and generate a draft timetable.</p>
    </div>
    <ol className="flex flex-wrap gap-3 text-sm text-[#4b2a0a]" aria-label="Setup progress">{["Academic period", "Review modules", "Saved"].map((label, i) => <li key={label} aria-current={i === ["setup", "review", "saved"].indexOf(stage) ? "step" : undefined} className={`rounded-full border px-4 py-2 ${i === ["setup", "review", "saved"].indexOf(stage) ? "bg-[#f5c04a] border-[#f5c04a] font-bold" : "border-[#ebdcb9]"}`}>{i + 1}. {label}</li>)}</ol>
    {errors.length > 0 && <section role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800"><h2 className="font-bold">What needs attention</h2><ul className="mt-2 list-disc space-y-1 pl-5">{errors.map(message => <li key={message}>{message}</li>)}</ul></section>}
    {stage === "setup" && <form onSubmit={review} className="space-y-6 rounded-2xl border border-[#ebdcb9] bg-white p-5 sm:p-8">
      <h2 className="flex items-center gap-2 text-lg font-bold text-[#4b2a0a]"><CalendarRange size={22} /> Academic period</h2>
      <fieldset disabled={busy} className="grid gap-5 sm:grid-cols-2">
        <div>
          <label htmlFor="academicYear" className="text-sm font-semibold">Academic year</label>
          <select
            id="academicYear"
            className={input}
            required
            value={academicYear}
            disabled={loadingYears || !academicYears.length}
            aria-describedby={yearsError || (!loadingYears && !academicYears.length) ? "academicYearStatus" : undefined}
            onChange={e => changeAcademicYear(e.target.value)}
          >
            <option value="" disabled>{loadingYears ? "Loading academic years..." : "Select academic year"}</option>
            {academicYears.map(year => <option key={year} value={year}>{year}</option>)}
          </select>
          {!loadingYears && (yearsError || !academicYears.length) && <div id="academicYearStatus" className="mt-2 text-xs">
            <p role={yearsError ? "alert" : "status"} className={yearsError ? "text-rose-700" : "text-slate-500"}>{yearsError ? `Unable to load academic years. ${yearsError}` : "No academic years available."}</p>
            <button type="button" className="mt-2 underline" onClick={() => { setLoadingYears(true); setYearsError(""); void loadAcademicYears(); }}>Reload academic years</button>
          </div>}
        </div>
        <label className="text-sm font-semibold">Period type<select className={input} value={eventType} onChange={e => setEventType(e.target.value)}><option value="EXAM">Examinations</option><option value="LECTURE">Lectures</option></select></label>
        <label className="text-sm font-semibold">Start date<input className={input} type="date" required value={startingDate} onChange={e => setStartingDate(e.target.value)} /></label>
        <label className="text-sm font-semibold">End date<input className={input} type="date" required min={startingDate} value={endingDate} onChange={e => setEndingDate(e.target.value)} /></label>
      </fieldset>
      {eventType === "EXAM" && <fieldset disabled={busy} className="space-y-2">
        <legend className="text-sm font-semibold">Additional holidays (optional)</legend>
        <div className="flex flex-wrap items-end gap-2">
          <label className="text-xs text-slate-600">Holiday date<input aria-label="Holiday date" className={input} type="date" min={startingDate} max={endingDate} value={holiday} onChange={event => setHoliday(event.target.value)} /></label>
          <button type="button" className="rounded-xl border border-[#d9c59f] px-4 py-2.5 text-sm disabled:opacity-50" disabled={!startingDate || !endingDate || !holiday || holiday < startingDate || holiday > endingDate || holidayDates.includes(holiday)} onClick={() => { setHolidayDates(dates => [...dates, holiday].sort()); setHoliday(""); }}>Exclude date</button>
        </div>
        {holidayDates.length > 0 && <ul className="flex flex-wrap gap-2">{holidayDates.map(date => <li key={date}><button type="button" className="rounded-full border border-[#d9c59f] px-3 py-1 text-sm" aria-label={`Remove holiday ${date}`} onClick={() => setHolidayDates(dates => dates.filter(item => item !== date))}>{date} ×</button></li>)}</ul>}
      </fieldset>}
      <fieldset disabled={busy || loading}><legend className="mb-3 text-sm font-bold">Semesters <span className="font-normal text-slate-500">({semesterIds.length} selected)</span></legend>
        {!academicYear ? <p className="text-sm text-slate-500">Select an academic year to view its semesters.</p> : loading ? <p role="status" className="text-sm">Loading semesters for {academicYear}...</p> : semesters.length ? <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">{semesters.map(s => <label key={s.semesterId} className={`flex cursor-pointer items-center gap-3 rounded-xl border p-4 text-sm ${semesterIds.includes(s.semesterId) ? "border-[#4b2a0a] bg-[#fbf7ee]" : "border-[#ebdcb9]"}`}><input type="checkbox" className="h-4 w-4 accent-[#4b2a0a]" checked={semesterIds.includes(s.semesterId)} onChange={e => setSemesterIds(ids => e.target.checked ? [...ids, s.semesterId] : ids.filter(id => id !== s.semesterId))} />{formatSemesterName(s.semesterName)}</label>)}</div> : <p className="text-sm text-slate-500">No semesters available for {academicYear}. Reload to try again.</p>}
      </fieldset>
      <div className="flex flex-wrap items-center gap-4"><button className={button} disabled={busy || loading || loadingYears || !semesters.length || !academicYears.length}>{busy ? "Loading modules..." : "Review modules"}</button><button type="button" className="text-sm underline" disabled={busy || loading || !academicYear} onClick={() => void loadSemesters(academicYear)}>Reload semesters</button></div>
    </form>}
    {stage !== "setup" && <section className="space-y-5 rounded-2xl border border-[#ebdcb9] bg-white p-5 sm:p-8">
      <div className="flex flex-wrap justify-between gap-4"><div><h2 className="text-xl font-bold text-[#4b2a0a]">{eventType === "EXAM" ? "Exam requirements" : "Lecture period"}</h2><p className="mt-1 text-sm text-slate-600">{academicYear} · {startingDate} to {endingDate}</p><p className="mt-1 text-sm text-slate-500">Semesters: {selectedNames}</p></div>{stage === "review" && periodId === null && <button className="text-sm underline" disabled={busy} onClick={() => { setStage("setup"); setError(""); }}>Change selection</button>}</div>
      {periodId !== null && <p className="rounded-lg bg-[#fbf7ee] p-3 text-sm">Calendar period #{periodId} saved.{requirementsSaved && " Exam requirements saved. You can retry generation for this same period."}</p>}
      {stage === "saved" ? <div role="status" className="space-y-3 rounded-xl border border-emerald-200 bg-emerald-50 p-6 text-emerald-900"><CheckCircle2 /><h3 className="text-lg font-bold">Lecture period saved</h3><p className="text-sm">Calendar period #{periodId} saved successfully.</p><button className={button} onClick={() => { setStage("setup"); setPeriodId(null); setRequirementsSaved(false); setPreviewReady(false); setModules([]); setError(""); }}>Create another setup</button></div> : <>
        {eventType === "EXAM" && <><p className="text-sm text-slate-600">Select modules that have an exam and enter their duration in minutes. {modules.filter(m => m.hasExam).length} of {modules.length} selected.</p><ExamModuleForm
          semesters={semesters.filter(semester => semesterIds.includes(semester.semesterId))}
          modules={modules}
          disabled={busy || requirementsSaved}
          onSelectAll={selected => setModules(rows => rows.map(row => ({ ...row, hasExam: selected })))}
          onChange={(moduleId, patch) => setModules(rows => rows.map(row => row.moduleId === moduleId ? { ...row, ...patch } : row))}
        />{!modules.length && <p className="text-sm">No modules match these semesters. Change the selection or add module records before saving exam requirements.</p>}</>}
        {eventType === "LECTURE" && <p className="text-sm text-slate-600">This creates a lecture calendar period for the selected semesters. Lecture requirements and automatic allocation are not available yet.</p>}
        {eventType === "EXAM" && <label className="block max-w-xs text-sm font-semibold">Generate with
          <select className={input} value={engine} disabled={busy} onChange={event => setEngine(event.target.value as Engine)}>
            <option value="gemini">Gemini</option><option value="java">Java scheduler</option>
          </select>
        </label>}
        <div className="flex flex-wrap items-center gap-4">
          <button type="button" className={button} disabled={busy || (eventType === "EXAM" && !modules.some(m => m.hasExam))} onClick={() => void save()}>
            {busy ? <span role="status" className="flex items-center gap-2"><Loader2 size={16} className="animate-spin" />{busyMessage}</span> : eventType === "EXAM" ? "Generate timetable" : "Save lecture period"}
          </button>
          {previewReady && <Link className="text-sm underline" href={`/admin/exam-timetable?periodId=${periodId}`}>Open generated preview</Link>}
        </div>
      </>}
    </section>}
  </div>;
}
