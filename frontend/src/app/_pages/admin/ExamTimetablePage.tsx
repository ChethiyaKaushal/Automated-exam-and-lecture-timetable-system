"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { errorMessages } from "@/features/timetable/api";
import { ExamTimetableSheet } from "@/features/timetable/components/ExamTimetableSheet";
import type { TimetableTarget } from "@/features/timetable/components/useTimetableDrag";
import { examApi, rejectedReview, restorePreview, storePreview } from "@/features/timetable/examApi";
import type { Assignment, DraftVersion, ExamPeriod, ExamSetup, Preview, ReviewIssue, ScheduledExam } from "@/features/timetable/types";

const panel = "space-y-4 rounded-2xl border border-[#ebdcb9] bg-white p-5 sm:p-6";
const field = "mt-1 w-full rounded-xl border border-[#d9c59f] bg-white p-2.5 text-sm";
const button = "rounded-xl bg-[#4b2a0a] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#633a12] disabled:opacity-50";
const secondary = "rounded-xl border border-[#d9c59f] px-4 py-2.5 text-sm font-semibold text-[#4b2a0a] disabled:opacity-50";
const periodLabel = (period: ExamPeriod) => period.name || `Exam period #${period.periodId}`;
const dashboardUrl = (periodId: number, draftId?: number) =>
  `/admin/exam-timetable?periodId=${periodId}${draftId ? `&draftId=${draftId}` : ""}`;

export default function ExamTimetablePage() {
  const router = useRouter();
  const query = useSearchParams();
  const requestedPeriod = Number(query.get("periodId"));
  const draftId = Number(query.get("draftId")) || undefined;
  const [periods, setPeriods] = useState<ExamPeriod[] | null>(null);
  const [errors, setErrors] = useState<string[]>([]);

  useEffect(() => {
    let active = true;
    examApi.periods().then(rows => { if (active) setPeriods(rows); })
      .catch(error => { if (active) setErrors(errorMessages(error)); });
    return () => { active = false; };
  }, []);

  const period = requestedPeriod ? periods?.find(item => item.periodId === requestedPeriod) : periods?.[0];

  return <div className="mx-auto max-w-7xl space-y-6 px-4 py-8 sm:px-8">
    <header className="flex flex-wrap items-end justify-between gap-4 border-b border-[#ebdcb9] pb-5">
      <h1 className="text-3xl font-bold text-[#4b2a0a]">Exam timetable</h1>
      <Link href="/admin/generate" className={secondary}>Timetable setup</Link>
    </header>
    {errors.length > 0 && <Issues messages={errors} issues={[]} />}
    {periods === null && !errors.length && <p role="status">Loading exam periods...</p>}
    {periods?.length === 0 && <p className={panel}>Create an exam period in Timetable Setup to get started.</p>}
    {Boolean(periods?.length) && <section className={panel}>
      <label className="block max-w-xl text-sm font-semibold">Exam period
        <select className={field} value={period?.periodId ?? ""} onChange={event => router.push(dashboardUrl(Number(event.target.value)))}>
          {!period && <option value="" disabled>Choose an exam period</option>}
          {periods?.map(item => <option key={item.periodId} value={item.periodId}>{periodLabel(item)} · {item.start} to {item.end}</option>)}
        </select>
      </label>
    </section>}
    {period && <PeriodDashboard key={`${period.periodId}:${draftId ?? "working"}`} period={period} draftId={draftId} />}
  </div>;
}

function PeriodDashboard({ period, draftId }: { period: ExamPeriod; draftId?: number }) {
  const router = useRouter();
  const periodId = period.periodId;
  const [setup, setSetup] = useState<ExamSetup | null>(null);
  const [preview, setPreview] = useState<Preview | null>(null);
  const [opened, setOpened] = useState<DraftVersion | null>(null);
  const [hasChanges, setHasChanges] = useState(false);
  const [issues, setIssues] = useState<ReviewIssue[] | null>(null);
  const [errors, setErrors] = useState<string[]>([]);
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [acknowledged, setAcknowledged] = useState(false);
  const [examId, setExamId] = useState("");
  const [slotId, setSlotId] = useState("");
  const [pickedExam, setPickedExam] = useState<number | null>(null);
  const feedback = useRef<HTMLDivElement>(null);
  const inFlight = useRef(false);

  useEffect(() => {
    let active = true;
    async function load() {
      const data = await examApi.setup(periodId);
      if (!active) return;
      setSetup(data);
      const working = restorePreview(periodId, draftId);
      if (working) {
        setPreview(working);
        setHasChanges(true);
        return;
      }
      const version = draftId ? data.drafts.find(draft => draft.id === draftId) : data.drafts[0];
      if (draftId && !version) throw new Error("That draft could not be found for this exam period.");
      if (!version) return;
      const saved = await examApi.open(periodId, version.id);
      if (!active) return;
      setOpened(version);
      setPreview(saved);
    }
    void load().catch(error => { if (active) setErrors(errorMessages(error)); });
    return () => { active = false; };
  }, [draftId, periodId]);

  async function action(run: () => Promise<void>) {
    if (inFlight.current) return;
    inFlight.current = true;
    setBusy(true);
    setErrors([]);
    setIssues(null);
    setNotice("");
    try { await run(); }
    catch (error) {
      const review = rejectedReview(error);
      if (review?.issues.length) setIssues(review.issues);
      else setErrors(errorMessages(error));
      revealFeedback();
    }
    finally { inFlight.current = false; setBusy(false); }
  }

  function revealFeedback() {
    requestAnimationFrame(() => feedback.current?.scrollIntoView({ block: "nearest" }));
  }

  async function checkEdit(assignments: Assignment[]) {
    const review = await examApi.review(periodId, assignments);
    if (!review.valid && preview?.review.valid) {
      setIssues(review.issues);
      setNotice("Couldn’t apply this change. Your timetable stays as it was.");
      revealFeedback();
      return;
    }
    const next = { assignments, review };
    storePreview(periodId, next, draftId);
    setPreview(next);
    setHasChanges(true);
    setAcknowledged(false);
    setNotice(review.valid ? "Change applied. Keep editing or choose Save all changes when you’re ready." : "Review the remaining problems before saving.");
    if (!review.valid) revealFeedback();
  }

  function pickExam(id: number | null) {
    setPickedExam(id);
    if (id !== null) setExamId(String(id));
  }

  function applyChange(source: number, destination: TimetableTarget) {
    if (!preview || inFlight.current) return;
    const first = preview.assignments.find(item => item.requirementId === source);
    const second = destination.kind === "swap"
      ? preview.assignments.find(item => item.requirementId === destination.id) : undefined;
    const nextStart = destination.kind === "move" ? destination.id : second?.startTimeslotId;
    if (!first || nextStart == null || (second && first.startTimeslotId === null)) return;
    setExamId(String(source));
    setPickedExam(null);
    if (destination.kind === "move") setSlotId(String(destination.id));
    if (first.startTimeslotId === nextStart) {
      setIssues(null);
      setErrors([]);
      setNotice(second ? "These exams already start at the same time." : "This exam is already at that time.");
      return;
    }
    // Let the validator choose suitable rooms at the new time for each edited exam.
    const assignments = preview.assignments.map(item => {
      if (item === first) return { requirementId: source, startTimeslotId: nextStart };
      if (item === second) return { requirementId: item.requirementId, startTimeslotId: first.startTimeslotId };
      return item;
    });
    void action(() => checkEdit(assignments));
  }

  function saveDraft() {
    if (!preview || !preview.review.valid || !hasChanges) return;
    const assignments = preview.assignments;
    void action(async () => {
      const saved = await examApi.save(periodId, assignments, acknowledged);
      const version = { id: saved.timetableId, version: saved.version };
      storePreview(periodId, null, draftId);
      setPreview({ assignments, review: saved.review });
      setOpened(version);
      setSetup(current => current && { ...current, drafts: [version, ...current.drafts.filter(item => item.id !== version.id)] });
      setHasChanges(false);
      setAcknowledged(false);
      router.replace(dashboardUrl(periodId, version.id));
    });
  }

  const labels = new Map(preview?.review.exams.map(exam => [exam.requirementId, `${exam.moduleCode} · ${exam.moduleName}${exam.curriculum ? ` (${exam.curriculum})` : ""}`]));
  const options = preview?.assignments.map(item => ({
    ...item, label: labels.get(item.requirementId) ?? `Unscheduled exam #${item.requirementId}`,
  })) ?? [];
  const selected = options.find(item => item.requirementId === Number(examId));
  const requiresReview = preview?.review.studentMembershipsComplete === false;

  return <>
    {!preview && <Issues messages={errors} issues={[]} />}
    {!setup && !errors.length && <p role="status">Loading timetable...</p>}
    {setup && <section className={panel}>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          {preview && <p className="mt-2 text-sm font-semibold">{hasChanges ? "Unsaved changes" : `Saved draft version ${opened?.version}`}</p>}
        </div>
        {setup.drafts.length > 0 && <label className="text-sm font-semibold">Saved drafts
          <select className={field} disabled={busy} value={hasChanges ? "" : opened?.id ?? ""} onChange={event => router.push(dashboardUrl(periodId, Number(event.target.value)))}>
            <option value="" disabled>Open a saved draft</option>
            {setup.drafts.map(draft => <option key={draft.id} value={draft.id}>Version {draft.version}</option>)}
          </select>
        </label>}
      </div>
      {!preview && !errors.length && <p className="text-sm text-slate-600">There is no draft for this period yet. Generate one from Timetable Setup.</p>}
    </section>}
    {setup && preview && <>
      <section className={panel}>
        <h2 className="text-lg font-bold text-[#4b2a0a]">Edit exam times</h2>
        <fieldset disabled={busy} className="grid gap-4 lg:grid-cols-2">
          <label className="text-sm font-semibold">Exam
            <select className={field} value={examId} onChange={event => { setExamId(event.target.value); setPickedExam(null); }}>
              <option value="">Choose an exam</option>
              {options.map(item => <option key={item.requirementId} value={item.requirementId}>{item.label}</option>)}
            </select>
          </label>
          <label className="text-sm font-semibold">New time
            <select className={field} value={slotId} onChange={event => setSlotId(event.target.value)}>
              <option value="">Choose a date and time</option>
              {setup.slots.map(slot => <option key={slot.id} value={slot.id}>{slot.date} at {slot.time}</option>)}
            </select>
          </label>
          <div className="flex flex-wrap gap-2 lg:col-span-2">
            <button type="button" className={secondary} disabled={!selected || !slotId || selected.startTimeslotId === Number(slotId)} onClick={() => applyChange(Number(examId), { kind: "move", id: Number(slotId) })}>Check move</button>
          </div>
        </fieldset>
        <div ref={feedback} className="scroll-mt-24 space-y-2 empty:hidden">
          {notice && <p role="status" className="rounded-lg bg-[#fbf7ee] px-3 py-2 text-sm text-[#4b2a0a]">{notice}</p>}
          <Issues messages={errors} issues={issues ?? preview.review.issues} exams={preview.review.exams} />
        </div>
        {hasChanges && requiresReview && <label className="flex items-start gap-2 rounded-xl bg-amber-50 p-3 text-sm text-amber-900">
          <input type="checkbox" className="mt-1 accent-[#4b2a0a]" disabled={busy} checked={acknowledged} onChange={event => setAcknowledged(event.target.checked)} />
          I reviewed any reported individual and repeat-student clashes for this draft.
        </label>}
        <button type="button" className={button} disabled={busy || !hasChanges || !preview.review.valid || (requiresReview && !acknowledged)} onClick={saveDraft}>Save all changes</button>
        {busy && <span role="status" className="ml-3 text-sm text-slate-600">Working...</span>}
      </section>
      <ExamTimetableSheet
        exams={preview.review.exams}
        slots={setup.slots}
        selectedExam={pickedExam}
        busy={busy}
        onDrop={applyChange}
        onPick={pickExam}
      />
    </>}
  </>;
}

function issueMessage(issue: ReviewIssue, exams: ScheduledExam[]) {
  const affected = issue.requirementIds.map(id => exams.find(exam => exam.requirementId === id));
  const labels = affected.flatMap(exam => exam ? [exam.moduleCode + (exam.curriculum ? " (" + exam.curriculum + ")" : "")] : []);
  const exam = labels[0] ?? "This exam";
  switch (issue.code) {
    case "EXAM_DOES_NOT_FIT":
      return exam + " won’t fit at that start time. Choose an earlier time or another day so the full exam fits before the break or closing time.";
    case "STUDENT_OR_GROUP_CLASH":
      return labels.length === 2 ? "Some students need to sit both " + labels.join(" and ") + ". Choose times that don’t overlap." : issue.description;
    case "SAME_DAY_WORKLOAD":
      return labels.length === 2 ? "Some students have both " + labels.join(" and ") + " on the same day. Consider moving one to another day." : issue.description;
    case "VENUES_UNAVAILABLE":
      return "There aren’t enough free seats for " + (labels[0] ?? "this exam") + " at that time. Choose another time or day.";
    case "INSUFFICIENT_VENUE_CAPACITY":
      return "The assigned halls don’t have enough seats for " + (labels[0] ?? "this exam") + ". Choose another time with more room.";
    case "STUDENT_CLASHES_UNVERIFIED":
      return "Student lists are incomplete. Check any reported repeat-student clashes before saving.";
    case "INVALID_VENUE_SELECTION":
      return "The rooms assigned to " + (labels[0] ?? "this exam") + " could not be used. Move the exam to choose rooms again.";
    default:
      return issue.description;
  }
}

function Issues({ messages, issues, exams = [] }: { messages: string[]; issues: ReviewIssue[]; exams?: ScheduledExam[] }) {
  if (!messages.length && !issues.length) return null;
  return <section role={messages.length || issues.some(issue => issue.severity === "ERROR") ? "alert" : "status"} className="space-y-2 rounded-xl border border-[#ebdcb9] bg-white p-3" aria-labelledby="timetable-issues">
    <h2 id="timetable-issues" className="text-lg font-bold text-[#4b2a0a]">Edit feedback</h2>
    <ul className="space-y-2 text-sm">
      {messages.map(message => <li key={message} className="rounded-lg bg-rose-50 px-3 py-2 text-rose-900">{message}</li>)}
      {issues.map((issue, index) => <li key={`${issue.code}-${index}`} className={`rounded-lg px-3 py-2 ${issue.severity === "ERROR" ? "bg-rose-50 text-rose-900" : "bg-amber-50 text-amber-900"}`}>
        <strong>{issue.severity === "ERROR" ? "Problem" : "Review"}</strong><p>{issueMessage(issue, exams)}</p>
      </li>)}
    </ul>
  </section>;
}
