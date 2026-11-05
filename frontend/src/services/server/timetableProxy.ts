import { NextResponse } from "next/server";

const setupPath = "/api/admin/timetable-setup";
const backendCookies = new Set(["JSESSIONID", "SESSION", "XSRF-TOKEN", "remember-me"]);
class ServiceError extends Error {
  constructor(message: string, readonly status: number) { super(message); }
}

// Cookie jars are isolated per incoming request, never shared between users.
class BackendSession {
  private cookies = new Map<string, string>();
  private updates = new Map<string, string>();
  private base = (process.env.SPRING_BOOT_API_URL || "http://localhost:8081").replace(/\/$/, "");
  constructor(request?: Request) {
    for (const part of (request?.headers.get("cookie") || "").split(";")) {
      const index = part.indexOf("=");
      const name = part.slice(0, index).trim();
      if (index > 0 && backendCookies.has(name)) this.cookies.set(name, part.slice(index + 1).trim());
    }
  }
  async fetch(path: string, init: RequestInit = {}, timeoutMs = 15_000) {
    const headers = new Headers(init.headers);
    if (!headers.has("Accept")) headers.set("Accept", "application/json");
    if (this.cookies.size) headers.set("Cookie", [...this.cookies].map(([name, value]) => `${name}=${value}`).join("; "));
    const response = await fetch(`${this.base}${path}`, {
      ...init, headers, cache: "no-store", redirect: "manual", signal: AbortSignal.timeout(timeoutMs),
    });
    for (const cookie of response.headers.getSetCookie()) {
      const parts = cookie.split(";").map(part => part.trim());
      const index = parts[0].indexOf("=");
      const name = parts[0].slice(0, index);
      if (index < 1 || !backendCookies.has(name)) continue;
      const value = parts[0].slice(index + 1);
      const expired = parts.some(part => /^max-age=0$/i.test(part)
        || (/^expires=/i.test(part) && Date.parse(part.slice(8)) <= Date.now()));
      if (expired) this.cookies.delete(name);
      else this.cookies.set(name, value);
      // Rewrite backend scope to the browser-facing Next.js origin, retaining security attributes.
      const attributes = parts.slice(1).filter(part => !/^(domain|path)=/i.test(part));
      if (!attributes.some(part => /^samesite=/i.test(part))) attributes.push("SameSite=Lax");
      if (name !== "XSRF-TOKEN" && !attributes.some(part => /^httponly$/i.test(part))) attributes.push("HttpOnly");
      this.updates.set(name, [parts[0], "Path=/", ...attributes].join("; "));
    }
    return response;
  }
  finish(response: Response) {
    response.headers.set("Cache-Control", "no-store");
    for (const cookie of this.updates.values()) response.headers.append("Set-Cookie", cookie);
    return response;
  }
}

function checkOrigin(request: Request) {
  const origin = request.headers.get("origin");
  const url = new URL(request.url);
  const host = request.headers.get("host");
  const expectedOrigin = host ? `${url.protocol}//${host}` : url.origin;
  if ((origin && origin !== expectedOrigin) || request.headers.get("sec-fetch-site") === "cross-site") {
    throw new ServiceError("Cross-site timetable requests are not allowed.", 403);
  }
}
function checkAuthentication(response: Response) {
  if (response.status === 401 || (response.status >= 300 && response.status < 400)) {
    throw new ServiceError("Your session has expired or you are not signed in. Please log in with an administrator account.", 401);
  }
}
async function readJson(response: Response): Promise<unknown> {
  checkAuthentication(response);
  const raw = await response.text();
  try { return JSON.parse(raw); }
  catch {
    if (response.status === 403) throw new ServiceError("Access denied. An administrator account and a valid security token are required. Please sign in again.", 403);
    if (/name=["'](?:username|password)["']/i.test(raw)) throw new ServiceError("Please sign in with an administrator account.", 401);
    throw new ServiceError("The timetable service returned an unexpected non-JSON response.", 502);
  }
}
async function getCsrf(session: BackendSession) {
  const response = await session.fetch(`${setupPath}/csrf`, { method: "GET" });
  const data = await readJson(response);
  if (!response.ok) throw new ServiceError(response.status === 403
    ? "Access denied. Please sign in with an administrator account."
    : "Unable to obtain a security token from the timetable service.", response.status);
  if (!data || typeof data !== "object" || !("header" in data) || !("token" in data)
    || typeof data.header !== "string" || !/^X-[A-Za-z0-9-]+$/i.test(data.header)
    || typeof data.token !== "string" || !data.token.trim()) {
    throw new ServiceError("The timetable service returned an invalid security token.", 502);
  }
  return { header: data.header, token: data.token };
}
function failure(error: unknown) {
  return NextResponse.json({ message: error instanceof ServiceError ? error.message
    : "Cannot reach the timetable service. Check that Spring Boot is running. If a save timed out, check whether it was saved before trying again." },
  { status: error instanceof ServiceError ? error.status : 502 });
}
async function proxyTimetable(path: string, request?: Request, timeoutMs?: number) {
  const session = new BackendSession(request);
  try {
    const isPost = request?.method === "POST";
    let body: string | undefined;
    const headers: Record<string, string> = {};
    if (isPost) {
      checkOrigin(request);
      try { body = JSON.stringify(await request.json()); }
      catch { throw new ServiceError("Invalid JSON request.", 400); }
      const csrf = await getCsrf(session);
      headers[csrf.header] = csrf.token;
      headers["Content-Type"] = "application/json";
    }
    const response = await session.fetch(path, { method: isPost ? "POST" : "GET", headers, body }, timeoutMs);
    checkAuthentication(response);
    if (response.status === 204) return session.finish(new Response(null, { status: 204 }));
    const data = await readJson(response);
    return session.finish(NextResponse.json(data, { status: response.status }));
  } catch (error) { return session.finish(failure(error)); }
}

export function proxySetup(path: string, request?: Request) {
  return proxyTimetable(`${setupPath}/${path}`, request);
}

export function proxyExams(path: string, request: Request, longRunning = false) {
  return proxyTimetable(`/api/admin/exam-timetables/${path}`, request, longRunning ? 11 * 60_000 : undefined);
}


// Spring's generated login form supplies the pre-authentication CSRF input.
// The protected timetable CSRF endpoint is used after authentication succeeds.
export async function login(request: Request) {
  const session = new BackendSession(request);
  try {
    checkOrigin(request);
    let input;
    try { input = await request.json(); }
    catch { throw new ServiceError("Invalid JSON request.", 400); }
    if (!input || typeof input.username !== "string" || !input.username.trim()
      || typeof input.password !== "string" || !input.password) {
      throw new ServiceError("Enter your username and password.", 400);
    }
    const page = await session.fetch("/login", { headers: { Accept: "text/html" }, method: "GET" });
    if (!page.ok) throw new ServiceError("Unable to load the timetable service login form.", 502);
    const html = await page.text();
    let token = "";
    for (const tag of html.match(/<input\b[^>]*>/gi) || []) {
      if (/\bname\s*=\s*["']_csrf["']/i.test(tag)) token = tag.match(/\bvalue\s*=\s*["']([^"']*)["']/i)?.[1] || "";
    }
    if (!token) throw new ServiceError("The backend login form did not provide a CSRF token.", 502);
    const form = new URLSearchParams({ username: input.username, password: input.password, _csrf: token });
    if (input.rememberMe === true) form.set("remember-me", "on");
    const result = await session.fetch("/login", {
      method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded", Accept: "text/html" }, body: form.toString(),
    });
    const location = result.headers.get("location");
    if (location && new URL(location, "http://backend").pathname.endsWith("/login")) {
      throw new ServiceError("Login failed. Check your username and password and account status.", 401);
    }
    if (result.status < 300 || result.status >= 400 || !location) {
      throw new ServiceError(result.status === 403 ? "The login security token was rejected. Please try signing in again." : "The timetable service could not sign you in.", result.status === 403 ? 403 : 401);
    }
    // Verify admin authorization rather than assuming a redirect grants access.
    await getCsrf(session);
    return session.finish(NextResponse.json({ success: true }));
  } catch (error) { return session.finish(failure(error)); }
}
