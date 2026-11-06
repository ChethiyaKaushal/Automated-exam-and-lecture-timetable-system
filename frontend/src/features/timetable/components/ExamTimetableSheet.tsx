import { useState } from "react";
import type { ScheduledExam, Timeslot } from "../types";
import { useTimetableDrag, type TimetableTarget } from "./useTimetableDrag";

function dateLabel(date: string) {
  return date.split("-").reverse().join(".");
}

function timeLabel(time: string) {
  const [hour, minute] = time.split(":");
  const hours = Number(hour);
  if (hours === 12 && minute === "00") return "12 noon";
  return (hours % 12 || 12) + ":" + minute + (hours < 12 ? " a.m." : " p.m.");
}

function groupExams(exams: ScheduledExam[]) {
  const days = new Map<string, Map<string, ScheduledExam[]>>();
  const ordered = [...exams].sort((a, b) =>
    a.date.localeCompare(b.date) ||
    a.startTime.localeCompare(b.startTime) ||
    a.endTime.localeCompare(b.endTime) ||
    a.moduleCode.localeCompare(b.moduleCode) ||
    a.curriculum.localeCompare(b.curriculum)
  );

  for (const exam of ordered) {
    const sessions = days.get(exam.date) ?? new Map<string, ScheduledExam[]>();
    const timeRange = exam.startTime + "-" + exam.endTime;
    const session = sessions.get(timeRange) ?? [];
    session.push(exam);
    sessions.set(timeRange, session);
    days.set(exam.date, sessions);
  }
  return days;
}

const cell = "border border-[#e8d7b8] px-2.5 py-2 align-middle";
const slotButton = "rounded-md border border-[#d9c59f] bg-white px-2.5 py-2 text-sm text-[#4b2a0a] enabled:hover:bg-amber-50 disabled:opacity-60";

export function ExamTimetableSheet({
  exams, slots, selectedExam, busy, onDrop, onPick,
}: {
  exams: ScheduledExam[];
  slots: Timeslot[];
  selectedExam: number | null;
  busy: boolean;
  onDrop: (source: number, target: TimetableTarget) => void;
  onPick: (examId: number | null) => void;
}) {
  const [moveDate, setMoveDate] = useState(slots[0]?.date ?? "");
  const dates = [...new Set(slots.map(slot => slot.date))];
  const slotByStart = new Map(slots.map(slot => [slot.date + " " + slot.time, slot.id]));
  const days = groupExams(exams);
  const selected = exams.find(exam => exam.requirementId === selectedExam);

  function selectExam(id: number) {
    if (selectedExam === id) onPick(null);
    else if (selected) onDrop(selected.requirementId, { kind: "swap", id });
    else onPick(id);
  }

  const { bind, dragging, target, scroller } = useTimetableDrag(selectExam, onDrop);
  const highlight = (kind: TimetableTarget["kind"], id?: number) =>
    target?.kind === kind && target.id === id ? " bg-amber-100 outline outline-2 outline-inset outline-[#916528]" : "";

  function moveTo(id: number) {
    if (selectedExam !== null) onDrop(selectedExam, { kind: "move", id });
  }

  return <section aria-label="Timetable" className="rounded-2xl border border-[#ebdcb9] bg-white p-4 sm:p-6">
    {slots.length > 0 && <div className="mb-4 space-y-2 rounded-xl border border-[#ebdcb9] bg-[#fbf7ee] p-3">
      <div className="flex flex-wrap items-center gap-3 text-sm">
        <label className="flex items-center gap-2 font-semibold text-[#4b2a0a]">Move to date
          <select className={slotButton} value={moveDate} disabled={busy || dragging} onChange={event => setMoveDate(event.target.value)}>
            {dates.map(date => <option key={date} value={date}>{dateLabel(date)}</option>)}
          </select>
        </label>
        {selected && <button type="button" className={slotButton} disabled={busy} onClick={() => onPick(null)}>Clear selection</button>}
      </div>
      <p className="text-xs text-[#7a4a1d]">{selected
        ? selected.moduleCode + " selected. Choose a time to move it, or another exam to swap."
        : "Drag an exam onto another exam to swap, or onto a time to move. You can also select an exam and then choose its destination."}</p>
      <div className="flex flex-wrap gap-2">
        {slots.filter(slot => slot.date === moveDate).map(slot => <button
          key={slot.id} type="button" data-move-to={slot.id} className={slotButton + highlight("move", slot.id)}
          aria-disabled={busy || selectedExam === null} onClick={() => { if (!busy) moveTo(slot.id); }}
        >{timeLabel(slot.time)}</button>)}
      </div>
    </div>}
    {exams.length === 0 ? <p className="text-sm text-slate-600">No exams are placed yet. Check the messages by the edit controls.</p> :
      <div ref={scroller} className="overflow-x-auto" tabIndex={0} role="region" aria-label="Scheduled exam timetable">
        <table className="w-full min-w-[720px] table-fixed border-collapse text-sm">
          <thead className="bg-[#fbf7ee] text-xs uppercase tracking-wide text-[#4b2a0a]">
            <tr>
              <th scope="col" className={cell + " w-[11%]"}>Date</th>
              <th scope="col" className={cell + " w-[17%]"}>Time</th>
              <th scope="col" className={cell + " w-[14%]"}>Module No.</th>
              <th scope="col" className={cell + " w-[10%]"}>Student count</th>
              <th scope="col" className={cell + " w-[17%]"}>Venue</th>
              <th scope="col" className={cell}>Module name</th>
            </tr>
          </thead>
          {Array.from(days, ([date, sessions]) => {
            const dateRows = Array.from(sessions.values()).reduce((count, session) => count + session.length, 0);
            return <tbody key={date} className="border-t-2 border-[#d9c59f]">
              {Array.from(sessions.values()).flatMap((session, sessionIndex) => session.map((exam, index) => {
                const startSlotId = slotByStart.get(exam.date + " " + exam.startTime);
                const timeRange = timeLabel(exam.startTime) + " – " + timeLabel(exam.endTime);
                const id = exam.requirementId;
                return <tr key={id} data-swap-with={id} className={highlight("swap", id)}>
                  {sessionIndex === 0 && index === 0 && <th scope="rowgroup" rowSpan={dateRows} className={cell + " bg-[#fbf7ee] text-center font-normal tabular-nums"}>{dateLabel(date)}</th>}
                  {index === 0 && <td rowSpan={session.length} data-move-to={startSlotId} className={cell + " text-center tabular-nums" + highlight("move", startSlotId)}>
                    {startSlotId === undefined ? timeRange : <button type="button" className="w-full rounded-sm py-1 enabled:hover:underline"
                      disabled={busy || selectedExam === null} onClick={() => moveTo(startSlotId)}>{timeRange}</button>}
                  </td>}
                  <th scope="row" className={cell + " text-center font-medium"}>
                    <button type="button" {...bind(id)} disabled={busy} aria-pressed={selectedExam === id}
                      aria-label={exam.moduleCode + " " + exam.moduleName + (exam.curriculum ? " (" + exam.curriculum + ")" : "")}
                      className={"inline-flex min-h-10 w-full touch-none select-none items-center justify-center gap-1 rounded-md border px-2 py-1 font-semibold text-[#4b2a0a] disabled:opacity-60 "
                        + (dragging ? "cursor-grabbing " : "cursor-grab ")
                        + (selectedExam === id ? "border-[#916528] bg-amber-100" : "border-[#e8d7b8] bg-[#fbf7ee] hover:bg-amber-50")}>
                      <span aria-hidden="true">⋮⋮</span> {exam.moduleCode}
                    </button>
                  </th>
                  <td className={cell + " text-center tabular-nums"}>{exam.studentCount}</td>
                  <td className="border border-[#e8d7b8] p-0">
                    <ul className="divide-y divide-[#ebdcb9]">
                      {exam.venues.map(venue => <li key={venue.venueId} className="px-2.5 py-1.5 break-words">
                        {venue.venueName} – <span title={venue.allocatedCount + " allocated / " + venue.examCapacity + " capacity"} className="tabular-nums">{venue.allocatedCount}</span>
                      </li>)}
                    </ul>
                  </td>
                  <td className={cell + " break-words"}>{exam.moduleName}{exam.curriculum && " (" + exam.curriculum + ")"}</td>
                </tr>;
              }))}
            </tbody>;
          })}
        </table>
      </div>}
  </section>;
}
