import { proxyExams } from "@/services/server/timetableProxy";

type Context = { params: Promise<{ segments: string[] }> };
const positiveId = (value: string) => /^[1-9][0-9]*$/.test(value);

export async function GET(request: Request, { params }: Context) {
  const { segments } = await params;
  const [periodId, section, draftId] = segments;
  if ((segments.length === 1 && (periodId === "periods" || positiveId(periodId)))
    || (segments.length === 3 && positiveId(periodId) && section === "drafts" && positiveId(draftId))) {
    return proxyExams(segments.join("/"), request);
  }
  return Response.json({ message: "Exam timetable page not found." }, { status: 404 });
}

export async function POST(request: Request, { params }: Context) {
  const { segments } = await params;
  const [periodId, action] = segments;
  if (segments.length !== 2 || !positiveId(periodId) || !["preview", "review", "drafts"].includes(action)) {
    return Response.json({ message: "Exam timetable action not found." }, { status: 404 });
  }
  if (action === "preview") {
    const engine = new URL(request.url).searchParams.get("engine") ?? "java";
    if (engine !== "java" && engine !== "gemini") {
      return Response.json({ message: "Choose Java or Gemini to generate a timetable." }, { status: 400 });
    }
    return proxyExams(`${periodId}/preview?engine=${engine}`, request, engine === "gemini");
  }
  return proxyExams(segments.join("/"), request);
}
