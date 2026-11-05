import { api, ApiError } from "./api";
import type { Assignment, Engine, ExamPeriod, ExamSetup, Preview, Review, SavedDraft } from "./types";

export const examApi = {
  periods: () => api<ExamPeriod[]>("exams/periods"),
  setup: (periodId: number) => api<ExamSetup>(`exams/${periodId}`),
  preview: (periodId: number, engine: Engine) => api<Preview>(`exams/${periodId}/preview?engine=${engine}`, {}),
  review: (periodId: number, assignments: Assignment[]) => api<Review>(`exams/${periodId}/review`, { assignments }),
  open: (periodId: number, timetableId: number) => api<Preview>(`exams/${periodId}/drafts/${timetableId}`),
  save: (periodId: number, assignments: Assignment[], manualStudentReview: boolean) =>
    api<SavedDraft>(`exams/${periodId}/drafts`, { assignments, manualStudentReview }),
};

export function rejectedReview(error: unknown): Review | undefined {
  if (!(error instanceof ApiError) || error.status !== 422 || !error.data || typeof error.data !== "object"
      || !("review" in error.data)) return undefined;
  return error.data.review as Review;
}

const previewKey = (periodId: number, draftId?: number) => `exam-timetable-preview:${periodId}${draftId ? ":draft:" + draftId : ""}`;

export function storePreview(periodId: number, preview: Preview | null, draftId?: number) {
  if (preview) sessionStorage.setItem(previewKey(periodId, draftId), JSON.stringify(preview));
  else sessionStorage.removeItem(previewKey(periodId, draftId));
}

export function restorePreview(periodId: number, draftId?: number): Preview | null {
  try {
    const data = sessionStorage.getItem(previewKey(periodId, draftId));
    return data ? JSON.parse(data) as Preview : null;
  } catch {
    return null;
  }
}
