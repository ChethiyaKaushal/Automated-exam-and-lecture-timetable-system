import { Suspense } from "react";
import ExamTimetablePage from "@/app/_pages/admin/ExamTimetablePage";

export default function Page() {
  return <Suspense fallback={<p role="status" className="p-8">Loading exam timetable...</p>}>
    <ExamTimetablePage />
  </Suspense>;
}
