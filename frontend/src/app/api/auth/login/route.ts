import { login } from "@/services/server/timetableProxy";

export async function POST(request: Request) {
  return login(request);
}
