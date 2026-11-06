import { proxySetup } from "@/services/server/timetableProxy";
export async function POST(request: Request) { return proxySetup("filtermodule", request); }
