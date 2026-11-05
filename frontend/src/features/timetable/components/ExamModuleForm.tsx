"use client";

import type { ExamModuleRow, Semester } from "@/features/timetable/types";
import { formatSemesterName } from "@/features/timetable/utils/formatSemesterName";



type Props = {
  semesters: Semester[];
  modules: ExamModuleRow[];
  disabled: boolean;
  onSelectAll: (selected: boolean) => void;
  onChange: (moduleId: number, patch: Partial<Pick<ExamModuleRow, "hasExam" | "examDurationMinutes">>) => void;
};

const getDepartmentName = (module: ExamModuleRow) => module.departmentName?.trim() || "Unnamed department";

export default function ExamModuleForm({ semesters, modules, disabled, onChange, onSelectAll }: Props) {
  const selectedCount = modules.filter(module => module.hasExam).length;
  const allSelected = modules.length > 0 && selectedCount === modules.length;
  return <div className="space-y-6">
    <label className="flex cursor-pointer items-center gap-3 rounded-xl border border-[#d9c59f] bg-[#fbf7ee] px-4 py-3 text-sm font-semibold text-[#4b2a0a]">
      <input
        type="checkbox"
        checked={allSelected}
        ref={node => { if (node) node.indeterminate = selectedCount > 0 && !allSelected; }}
        disabled={disabled || !modules.length}
        onChange={event => onSelectAll(event.target.checked)}
        className="h-4 w-4 accent-[#4b2a0a]"
      />
      Select all modules
      <span className="ml-auto text-xs font-normal text-[#7a4a1d]">{selectedCount} / {modules.length} selected</span>
    </label>
    {semesters.map(semester => {
      const semesterModules = modules.filter(module => module.semesterId === semester.semesterId);
      const departmentNames = [...new Set(semesterModules.map(getDepartmentName))].sort((a, b) => a.localeCompare(b));
      const heading = formatSemesterName(semesterModules.find(module => module.semesterName?.trim())?.semesterName.trim()
        || semester.semesterName?.trim()
        || `Semester ${semester.semesterId}`);

      return <section
        key={semester.semesterId}
        aria-labelledby={`semester-${semester.semesterId}-heading`}
        className="overflow-hidden rounded-2xl border border-[#d9c59f]"
      >
        <header className="flex flex-wrap items-center justify-between gap-3 bg-[#4b2a0a] px-4 py-4 text-white sm:px-5">
          <div>
            <p className="text-[10px] font-bold uppercase tracking-widest text-[#f5c04a]">Semester</p>
            <h3 id={`semester-${semester.semesterId}-heading`} className="mt-1 text-lg font-bold">{heading}</h3>
          </div>
          <span className="rounded-full bg-white/10 px-3 py-1 text-xs text-amber-100">
            {semesterModules.filter(module => module.hasExam).length} / {semesterModules.length} exams selected
          </span>
        </header>

        <div className="space-y-5 bg-[#fcf9f2] p-3 sm:p-5">
          {!semesterModules.length && <p className="py-3 text-sm text-slate-500">No modules available for this semester.</p>}
          {departmentNames.map(departmentName => {
            const departmentModules = semesterModules
              .filter(module => getDepartmentName(module) === departmentName)
              .sort((a, b) => a.moduleCode.localeCompare(b.moduleCode, undefined, { numeric: true })
                || (a.curriculum || "").localeCompare(b.curriculum || "")
                || a.moduleId - b.moduleId);
            const headingId = `semester-${semester.semesterId}-department-${encodeURIComponent(departmentName)}-heading`;

            return <section key={departmentName} aria-labelledby={headingId} className="overflow-hidden rounded-xl border border-[#ebdcb9] bg-white">
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[#ebdcb9] bg-[#f4e7c8] px-4 py-3">
                <div>
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-[#7a4a1d]">Department</p>
                  <h4 id={headingId} className="mt-0.5 text-sm font-bold text-[#4b2a0a]">{departmentName}</h4>
                </div>
                <span className="text-xs text-[#7a4a1d]">{departmentModules.length} {departmentModules.length === 1 ? "module" : "modules"}</span>
              </div>

              <div className="overflow-x-auto">
                <table aria-labelledby={headingId} className="w-full min-w-[640px] table-fixed text-left text-sm">
                  <colgroup>
                    <col className="w-28" />
                    <col />
                    <col className="w-24" />
                    <col className="w-24" />
                    <col className="w-36" />
                  </colgroup>
                  <thead className="border-b border-[#ebdcb9] bg-[#fbf7ee] text-xs text-[#7a4a1d]">
                    <tr>
                      <th scope="col" className="px-4 py-3">Module code</th>
                      <th scope="col" className="px-4 py-3">Module name</th>
                      <th scope="col" className="px-4 py-3">Curriculum</th>
                      <th scope="col" className="px-4 py-3 text-center">Has exam</th>
                      <th scope="col" className="px-4 py-3 text-center">Duration (min)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#ebdcb9]">
                    {departmentModules.map(module => {
                      const label = `${module.moduleCode}, ${module.moduleName}, ${module.curriculum || "no curriculum"}, ${heading}`;
                      return <tr key={module.moduleId} className={module.hasExam ? "bg-amber-50/40" : ""}>
                        <th scope="row" className="px-4 py-3 align-middle font-semibold text-[#4b2a0a] [overflow-wrap:anywhere]">{module.moduleCode}</th>
                        <td className="whitespace-normal px-4 py-3 align-middle [overflow-wrap:anywhere]">{module.moduleName}</td>
                        <td className="px-4 py-3 align-middle text-xs text-slate-500 [overflow-wrap:anywhere]">{module.curriculum || "—"}</td>
                        <td className="px-4 py-3 text-center align-middle">
                          <input
                            aria-label={`Has exam for ${label}`}
                            type="checkbox"
                            disabled={disabled}
                            checked={module.hasExam}
                            className="h-4 w-4 align-middle accent-[#4b2a0a]"
                            onChange={event => onChange(module.moduleId, { hasExam: event.target.checked })}
                          />
                        </td>
                        <td className="px-4 py-3 text-center align-middle">
                          <input
                            aria-label={`Exam duration for ${label}`}
                            type="number"
                            min="30"
                            max="270"
                            step="30"
                            className="w-24 rounded-lg border border-[#d9c59f] p-2 align-middle disabled:bg-slate-100"
                            disabled={disabled || !module.hasExam}
                            value={module.hasExam ? module.examDurationMinutes || "" : ""}
                            onChange={event => onChange(module.moduleId, { examDurationMinutes: Number(event.target.value) })}
                          />
                        </td>
                      </tr>;
                    })}
                  </tbody>
                </table>
              </div>
            </section>;
          })}
        </div>
      </section>;
    })}
  </div>;
}
