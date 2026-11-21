export function formatSemesterName(semesterName: string): string {
  const name = semesterName.trim();
  return /^semester\b/i.test(name) ? name : `Semester ${name}`.trim();
}
