import { proxySetup } from "@/services/server/timetableProxy";
export async function GET(request: Request) {
  const academicYear = new URL(request.url).searchParams.get("academicYear")?.trim();
  if (!academicYear) {
    return Response.json({ message: "Select an academic year to load semesters." }, { status: 400 });
  }
  const query = new URLSearchParams({ academicYear });
  return proxySetup(`semesters?${query}`, request);
}
