import { proxyVenues } from "@/services/server/timetableProxy";
type Context = { params: Promise<{ segments?: string[] }> };
async function handle(request: Request, { params }: Context) {
  const s = (await params).segments ?? [];
  const path = s.join("/");
  const method = request.method;
  const valid = (path === "" && ["GET","POST"].includes(method)) ||
    (path === "timeslots" && method === "GET") ||
    (/^[1-9]\d*$/.test(path) && ["GET","PUT","DELETE"].includes(method)) ||
    (/^[1-9]\d*\/blocks$/.test(path) && method === "GET") ||
    (/^[1-9]\d*\/blocks\/[1-9]\d*$/.test(path) && ["PUT","DELETE"].includes(method));
  if (!valid) return Response.json({message:"Not found"},{status:404});
  return proxyVenues(path ? `/${path}` : "", request);
}
export { handle as GET, handle as POST, handle as PUT, handle as DELETE };
