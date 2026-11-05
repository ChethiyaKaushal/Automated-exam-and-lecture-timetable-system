import { proxySetup } from "@/services/server/timetableProxy";

export async function GET(request: Request) {
  return proxySetup("academic-years", request);
}
