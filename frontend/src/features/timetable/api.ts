export class ApiError extends Error {
  constructor(readonly status: number, readonly data: unknown, readonly messages: string[]) {
    super(messages[0]);
  }
}

export function errorMessages(error: unknown) {
  return error instanceof ApiError ? error.messages
    : [error instanceof Error ? error.message : "The request could not be completed."];
}

export async function api<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(`/api/timetable/${path}`, {
    method: body === undefined ? "GET" : "POST",
    credentials: "same-origin",
    headers: body === undefined ? undefined : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body), cache: "no-store",
  });
  if (response.status === 204) return undefined as T;
  const data = await response.json().catch(() => {
    throw new Error(response.status === 401 ? "Please sign in with an administrator account." : "The timetable service returned an unexpected response.");
  });
  if (!response.ok) {
    const issues = Array.isArray(data?.issues) ? data.issues.map((issue: { description?: string }) => issue.description).filter(Boolean) : [];
    const details = Array.isArray(data?.details) ? data.details.filter((detail: unknown) => typeof detail === "string") : [];
    const fallback = response.status === 403 ? "Access denied. Please sign in with an administrator account." : "The timetable service could not complete the request.";
    throw new ApiError(response.status, data, [...new Set<string>(issues.length ? issues : [data?.message || data?.error || fallback, ...details])]);
  }
  return data as T;
}
