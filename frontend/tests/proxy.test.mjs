import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import vm from 'node:vm';
const require = createRequire(import.meta.url);
const ts = require('typescript');
const source = readFileSync(new URL('../src/services/server/timetableProxy.ts', import.meta.url), 'utf8');
const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText;
function proxy(fetch, csrfFetch = async () => Response.json(
  { header: 'X-CSRF-TOKEN', token: 'test-token' },
  { headers: { 'Set-Cookie': 'JSESSIONID=test-session; Path=/; HttpOnly' } },
), entry = 'proxySetup') {
  const exports = {};
  const backendFetch = (url, init) => url.endsWith('/csrf') ? csrfFetch(url, init) : fetch(url, init);
  vm.runInNewContext(code, { exports, require, fetch: backendFetch, process: { env: { SPRING_BOOT_API_URL: 'http://test-backend:8081/' } }, URL, URLSearchParams, Headers, Response, AbortSignal, JSON });
  return exports[entry];
}
const post = body => new Request('http://frontend/api', { method: 'POST', body: JSON.stringify(body) });
test('forwards setup fields and returned period ID', async () => {
  const payload = { startingDate: '2026-11-05', endingDate: '2026-11-10', academicYear: '2025/2026', eventType: 'EXAM', semesterIds: [1, 3] };
  const result = await proxy(async (url, init) => {
    assert.equal(url, 'http://test-backend:8081/api/admin/timetable-setup/setup');
    assert.equal(init.method, 'POST');
    assert.equal(init.cache, 'no-store');
    assert.equal(init.headers.get('X-CSRF-TOKEN'), 'test-token');
    assert.equal(init.headers.get('Cookie'), 'JSESSIONID=test-session');
    assert.deepEqual(JSON.parse(init.body), payload);
    return Response.json({ periodId: 42, eventType: 'EXAM' }, { status: 201 });
  })('setup', post(payload));
  assert.equal(result.status, 201);
  assert.deepEqual(await result.json(), { periodId: 42, eventType: 'EXAM' });
});

test('fetches a fresh CSRF token and matching cookies before each POST', async () => {
  let session = 0;
  const calls = [];
  const send = proxy(async (url, init) => {
    calls.push('POST');
    assert.equal(init.headers.get('X-CUSTOM-CSRF'), `token-${session}`);
    assert.equal(init.headers.get('Cookie'), `JSESSIONID=session-${session}; XSRF-TOKEN=cookie-${session}`);
    return Response.json({ ok: true });
  }, async (url, init) => {
    calls.push('GET');
    session++;
    assert.equal(url, 'http://test-backend:8081/api/admin/timetable-setup/csrf');
    assert.equal(init.method, 'GET');
    assert.equal(init.cache, 'no-store');
    const response = Response.json({ header: 'X-CUSTOM-CSRF', token: `token-${session}` });
    response.headers.append('Set-Cookie', `JSESSIONID=session-${session}; Path=/; HttpOnly`);
    response.headers.append('Set-Cookie', `XSRF-TOKEN=cookie-${session}; Path=/`);
    return response;
  });
  for (const path of ['filtermodule', 'setup', 'requirements']) {
    assert.equal((await send(path, post({}))).status, 200);
  }
  assert.deepEqual(calls, ['GET', 'POST', 'GET', 'POST', 'GET', 'POST']);
});

test('does not submit a POST when CSRF acquisition fails or is incomplete', async () => {
  for (const response of [
    new Response(null, { status: 403 }),

    Response.json({ header: 'Cookie', token: 'invalid-header' }, { headers: { 'Set-Cookie': 'JSESSIONID=test' } }),
  ]) {
    const result = await proxy(async () => { assert.fail('Must not POST without CSRF protection'); }, async () => response)('setup', post({}));
    assert.equal(result.status, response.status === 403 ? 403 : 502);
    assert.ok((await result.json()).message);
  }
});

test('GET and malformed JSON requests do not acquire CSRF sessions', async () => {
  const send = proxy(async () => Response.json([]), async () => { assert.fail('CSRF endpoint should not be called'); });
  assert.equal((await send('academic-years')).status, 200);
  assert.equal((await send('setup', new Request('http://frontend/api', { method: 'POST', body: '{' }))).status, 400);
});

test('rejects cross-site POSTs before acquiring a backend token', async () => {
  const send = proxy(async () => { assert.fail('Must not submit'); }, async () => { assert.fail('Must not acquire token'); });
  const result = await send('setup', new Request('http://frontend/api', {
    method: 'POST', headers: { Origin: 'http://other-site' }, body: '{}',
  }));
  assert.equal(result.status, 403);
});

test('accepts the browser host when Next.js normalizes the internal URL', async () => {
  const run = proxy(async () => Response.json({ ok: true }));
  const request = origin => new Request('http://localhost:3000/api', {
    method: 'POST', body: '{}', headers: { host: '127.0.0.1:3000', origin },
  });
  assert.equal((await run('setup', request('http://127.0.0.1:3000'))).status, 200);
  assert.equal((await run('setup', request('http://localhost:3000'))).status, 403);
});
test('accepts same-origin POSTs with CSRF protection', async () => {
  const result = await proxy(async () => Response.json({ ok: true }))('setup', new Request('http://frontend/api', {
    method: 'POST', headers: { Origin: 'http://frontend' }, body: '{}',
  }));
  assert.equal(result.status, 200);
});
test('forwards minute durations and handles empty 204 responses', async () => {
  const payload = { periodIds: [42], modules: [{ moduleId: 7, hasExam: true, examDurationMinutes: 90 }] };
  const result = await proxy(async (url, init) => {
    assert.ok(url.endsWith('/requirements'));
    assert.deepEqual(JSON.parse(init.body), payload);
    return new Response(null, { status: 204 });
  })('requirements', post(payload));
  assert.equal(result.status, 204);
  assert.equal(await result.text(), '');
});
test('preserves duplicate and validation errors', async () => {
  for (const status of [400, 409]) {
    const result = await proxy(async () => Response.json({ message: 'Exam requirement already exists', details: ['module 7'] }, { status }))('requirements', post({}));
    assert.equal(result.status, status);
    assert.equal((await result.json()).message, 'Exam requirement already exists');
  }
});
test('unreachable or non-JSON backend produces a usable error', async () => {
  for (const fetch of [async () => { throw new Error('offline'); }, async () => new Response('<html>error</html>')]) {
    const result = await proxy(fetch)('semesters');
    assert.equal(result.status, 502);
    assert.ok((await result.json()).message);
  }
});

const semesterRouteSource = readFileSync(new URL('../src/app/api/timetable/semesters/route.ts', import.meta.url), 'utf8');
const semesterRouteCode = ts.transpileModule(semesterRouteSource, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText;
function semesterRoute(fetch) {
  const exports = {};
  vm.runInNewContext(semesterRouteCode, {
    exports, URL, URLSearchParams, Response,
    require: name => {
      assert.equal(name, '@/services/server/timetableProxy');
      return { proxySetup: proxy(fetch) };
    },
  });
  return exports.GET;
}
test('semester route encodes the selected academic year and uses GET', async () => {
  const academicYear = '2025/2026 & next';
  const result = await semesterRoute(async (url, init) => {
    const upstream = new URL(url);
    assert.equal(upstream.pathname, '/api/admin/timetable-setup/semesters');
    assert.equal(upstream.searchParams.get('academicYear'), academicYear);
    assert.equal([...upstream.searchParams].length, 1);
    assert.equal(init.method, 'GET');
    return Response.json([{ semesterId: 5, semesterName: '1' }]);
  })(new Request('http://frontend/api/timetable/semesters?' + new URLSearchParams({ academicYear })));
  assert.equal(result.status, 200);
  assert.deepEqual(await result.json(), [{ semesterId: 5, semesterName: '1' }]);
});
test('semester route rejects a missing or blank year without querying the backend', async () => {
  const get = semesterRoute(async () => { assert.fail('Backend should not be called without a year'); });
  for (const query of ['', '?academicYear=', '?academicYear=%20%20']) {
    const result = await get(new Request('http://frontend/api/timetable/semesters' + query));
    assert.equal(result.status, 400);
    assert.match((await result.json()).message, /academic year/);
  }
});

test('authenticated reads forward the browser session and preserve GET', async () => {
  for (const path of ['academic-years', 'semesters?academicYear=2025%2F2026']) {
    const result = await proxy(async (url, init) => {
      assert.equal(init.method, 'GET');
      assert.equal(init.headers.get('cookie'), 'JSESSIONID=admin');
      assert.equal(init.redirect, 'manual');
      return Response.json([]);
    })(path, new Request('http://frontend/api', { headers: { Cookie: 'JSESSIONID=admin; unrelated=secret' } }));
    assert.equal(result.status, 200);
  }
});

test('CSRF reuses an authenticated session even without a new Set-Cookie', async () => {
  const send = proxy(async (url, init) => {
    assert.equal(init.headers.get('cookie'), 'JSESSIONID=admin');
    assert.equal(init.headers.get('X-CSRF-TOKEN'), 'admin-token');
    return Response.json([{ moduleId: 7 }]);
  }, async (url, init) => {
    assert.equal(init.headers.get('cookie'), 'JSESSIONID=admin');
    return Response.json({ header: 'X-CSRF-TOKEN', token: 'admin-token' });
  });
  const result = await send('filtermodule', new Request('http://frontend/api', {
    method: 'POST', headers: { Cookie: 'JSESSIONID=admin' }, body: '{"semesterIds":[1]}',
  }));
  assert.equal(result.status, 200);
});

test('rotated cookies reach the POST and browser with frontend scope', async () => {
  const send = proxy(async (url, init) => {
    assert.equal(init.headers.get('cookie'), 'JSESSIONID=rotated');
    return new Response(null, { status: 204, headers: { 'Set-Cookie': 'XSRF-TOKEN=; Max-Age=0; Path=/backend' } });
  }, async () => Response.json({ header: 'X-CSRF-TOKEN', token: 'token' }, {
    headers: { 'Set-Cookie': 'JSESSIONID=rotated; Domain=backend; Path=/backend; Secure; HttpOnly; SameSite=Strict' },
  }));
  const response = await send('requirements', post({}));
  assert.equal(response.status, 204);
  const cookies = response.headers.getSetCookie();
  assert.equal(cookies.length, 2);
  assert.match(cookies[0], /Path=\/; Secure; HttpOnly; SameSite=Strict/);
  assert.doesNotMatch(cookies[0], /Domain=|\/backend/);
  assert.match(cookies[1], /Max-Age=0/);
});

test('login redirects and HTML are authentication errors, not network errors', async () => {
  for (const backend of [
    new Response(null, { status: 302, headers: { Location: 'http://backend/login' } }),
    new Response('<form><input name="username"></form>', { headers: { 'Content-Type': 'text/html' } }),
  ]) {
    const response = await proxy(async () => backend)('academic-years');
    assert.equal(response.status, 401);
    assert.match((await response.json()).message, /sign|log in/i);
  }
});

test('CSRF login redirect stops writes and a forbidden POST is never retried', async () => {
  const response = await proxy(async () => { assert.fail('Must not write'); }, async () =>
    new Response(null, { status: 302, headers: { Location: '/login' } }))('setup', post({}));
  assert.equal(response.status, 401);
  let writes = 0;
  const forbidden = await proxy(async () => { writes++; return new Response('Forbidden', { status: 403 }); })('setup', post({}));
  assert.equal(forbidden.status, 403);
  assert.equal(writes, 1);
});

function loginHandler(fetch) {
  const exports = {};
  vm.runInNewContext(code, { exports, require, fetch, process: { env: { SPRING_BOOT_API_URL: 'http://test-backend:8081' } }, URL, URLSearchParams, Headers, Response, AbortSignal, JSON });
  return exports.login;
}

test('Spring form login preserves pre-auth CSRF session, rotates session and verifies ADMIN', async () => {
  const calls = [];
  const signIn = loginHandler(async (url, init) => {
    calls.push([url, init.method]);
    if (calls.length === 1) return new Response('<input value="login-token" name="_csrf" type="hidden">', {
      headers: { 'Set-Cookie': 'JSESSIONID=preauth; Path=/; HttpOnly' },
    });
    if (calls.length === 2) {
      assert.equal(init.headers.get('cookie'), 'JSESSIONID=preauth');
      const form = new URLSearchParams(init.body);
      assert.equal(form.get('_csrf'), 'login-token');
      assert.equal(form.get('username'), 'admin');
      assert.equal(form.get('password'), 'test&password');
      return new Response(null, { status: 302, headers: { Location: '/admin/timetable.html', 'Set-Cookie': 'JSESSIONID=authenticated; Path=/; HttpOnly' } });
    }
    assert.ok(url.endsWith('/api/admin/timetable-setup/csrf'));
    assert.equal(init.headers.get('cookie'), 'JSESSIONID=authenticated');
    return Response.json({ header: 'X-CSRF-TOKEN', token: 'authenticated-token' });
  });
  const response = await signIn(post({ username: 'admin', password: 'test&password' }));
  assert.equal(response.status, 200);
  assert.equal(calls.length, 3);
  assert.match(response.headers.get('set-cookie'), /JSESSIONID=authenticated/);
  assert.doesNotMatch(await response.text(), /test&password|authenticated-token/);
});

test('wrong credentials and non-admin users cannot enter the workspace', async () => {
  for (const admin of [false, true]) {
    let calls = 0;
    const response = await loginHandler(async () => {
      calls++;
      if (calls === 1) return new Response('<input name="_csrf" value="token">');
      if (calls === 2) return new Response(null, { status: 302, headers: { Location: admin ? '/admin/timetable.html' : '/login?error' } });
      return new Response('Forbidden', { status: 403 });
    })(post({ username: 'user', password: 'password' }));
    assert.equal(response.status, admin ? 403 : 401);
    assert.equal(calls, admin ? 3 : 2);
  }
});

test('exam preview uses the existing authenticated proxy and passes the chosen engine', async () => {
  const send = proxy(async (url, init) => {
    assert.equal(url, 'http://test-backend:8081/api/admin/exam-timetables/42/preview?engine=gemini');
    assert.equal(init.method, 'POST');
    assert.equal(init.headers.get('Cookie'), 'JSESSIONID=test-session');
    assert.equal(init.headers.get('X-CSRF-TOKEN'), 'test-token');
    assert.deepEqual(JSON.parse(init.body), {});
    return Response.json({ assignments: [], review: { valid: true, issues: [] } });
  }, undefined, 'proxyExams');

  const result = await send('42/preview?engine=gemini', post({}), true);
  assert.equal(result.status, 200);
  assert.equal((await result.json()).review.valid, true);
});

test('exam save keeps the backend rejection and readable issues', async () => {
  const issue = { severity: 'ERROR', code: 'VENUES_UNAVAILABLE', description: 'Choose another time. No halls are free.' };
  const send = proxy(async (url, init) => {
    assert.ok(url.endsWith('/exam-timetables/42/drafts'));
    assert.equal(init.headers.get('X-CSRF-TOKEN'), 'test-token');
    return Response.json({ timetableId: null, review: { valid: false, issues: [issue] } }, { status: 422 });
  }, undefined, 'proxyExams');

  const result = await send('42/drafts', post({ assignments: [], manualStudentReview: false }));
  assert.equal(result.status, 422);
  assert.deepEqual((await result.json()).review.issues, [issue]);
});
