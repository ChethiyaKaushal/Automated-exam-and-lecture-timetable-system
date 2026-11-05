import assert from 'node:assert/strict';

const base = process.env.FRONTEND_URL || 'http://localhost:3000';
const cookieJar = new Map();
const nativeFetch = globalThis.fetch;
async function fetch(url, options = {}) {
  const headers = new Headers(options.headers);
  if (cookieJar.size) headers.set('Cookie', [...cookieJar].map(([name, value]) => `${name}=${value}`).join('; '));
  const response = await nativeFetch(url, { ...options, headers, redirect: 'manual' });
  for (const cookie of response.headers.getSetCookie()) {
    const pair = cookie.split(';', 1)[0];
    const index = pair.indexOf('=');
    cookieJar.set(pair.slice(0, index), pair.slice(index + 1));
  }
  return response;
}
for (const path of ['/', '/admin/generate', '/admin/timetable', '/admin/exam-timetable', '/admin/settings']) {
  const response = await fetch(base + path);
  assert.equal(response.status, 200, path);
  assert.match(await response.text(), /Timetable Management System/);
  console.log('PASS page', path);
}
assert.ok(process.env.TEST_ADMIN_USERNAME && process.env.TEST_ADMIN_PASSWORD,
  'Set TEST_ADMIN_USERNAME and TEST_ADMIN_PASSWORD to an existing ADMIN account to run authenticated integration checks.');
const login = await fetch(base + '/api/auth/login', {
  method: 'POST', headers: { 'Content-Type': 'application/json', Origin: new URL(base).origin },
  body: JSON.stringify({ username: process.env.TEST_ADMIN_USERNAME, password: process.env.TEST_ADMIN_PASSWORD }),
});
assert.equal(login.status, 200, 'ADMIN login must succeed');
console.log('PASS ADMIN login');
const yearsResponse = await fetch(base + '/api/timetable/academic-years');
assert.equal(yearsResponse.status, 200);
const years = await yearsResponse.json();
assert.ok(Array.isArray(years));
assert.ok(years.length, 'Seed at least one academic year to check semester filtering.');
const query = new URLSearchParams({ academicYear: years[0].academicYear });
const response = await fetch(`${base}/api/timetable/semesters?${query}`);
assert.equal(response.status, 200);
const semesters = await response.json();
assert.ok(Array.isArray(semesters));
assert.ok(semesters.length, 'Seed at least one semester to check module filtering.');
const semesterIds = semesters.slice(0, 2).map(s => s.semesterId);
const modulesResponse = await fetch(base + '/api/timetable/modules', {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ semesterIds }),
});
assert.equal(modulesResponse.status, 200);
const modules = await modulesResponse.json();
assert.ok(Array.isArray(modules));
assert.ok(modules.every(m => semesterIds.includes(m.semesterId)));
console.log(`PASS real backend reads: ${semesters.length} semesters, ${modules.length} filtered modules`);
for (const path of ['modules', 'setup', 'requirements']) {
  const invalid = await fetch(`${base}/api/timetable/${path}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}',
  });
  assert.equal(invalid.status, 400, path);
  assert.ok((await invalid.json()).message);
  console.log('PASS backend validation', path);
}
const malformed = await fetch(base + '/api/timetable/setup', {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{',
});
assert.equal(malformed.status, 400);
console.log('PASS malformed JSON rejected; no database writes performed');
