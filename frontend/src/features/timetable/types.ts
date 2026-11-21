export type AcademicYear = { academicYear: string };

export type Semester = { semesterId: number; semesterName: string };

export type Module = {
  moduleId: number;
  moduleCode: string;
  moduleName: string;
  curriculum: string;
  semesterId: number;
  semesterName: string;
  departmentId: number;
  departmentName: string;
};

export type ExamModuleRow = Module & {
  hasExam: boolean;
  examDurationMinutes: number;
};

export type Engine = "gemini" | "java";

export type ExamPeriod = {
  periodId: number;
  name: string | null;
  start: string;
  end: string;
};

export type Timeslot = { id: number; date: string; time: string };
export type DraftVersion = { id: number; version: number };
export type Assignment = { requirementId: number; startTimeslotId: number | null; venueIds?: number[] };
export type ExamSetup = {
  period: ExamPeriod;
  exams: { moduleId: number; enabled: boolean; durationMinutes: number; semester: string }[];
  slots: Timeslot[];
  drafts: DraftVersion[];
};

export type ScheduledExam = {
  requirementId: number;
  moduleCode: string;
  moduleName: string;
  curriculum: string;
  durationMinutes: number;
  studentCount: number;
  date: string;
  startTime: string;
  endTime: string;
  venues: { venueId: number; venueName: string; allocatedCount: number; examCapacity: number }[];
};

export type ReviewIssue = {
  severity: "ERROR" | "WARNING";
  code: string;
  description: string;
  requirementIds: number[];
};

export type Review = {
  valid: boolean;
  studentMembershipsComplete: boolean;
  issues: ReviewIssue[];
  exams: ScheduledExam[];
};

export type Preview = { assignments: Assignment[]; review: Review };
export type SavedDraft = { timetableId: number; version: number; review: Review };
