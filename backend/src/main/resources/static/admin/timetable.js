"use strict";

const api = "/api/admin/exam-timetables";
const element = id => document.getElementById(id);
const state = { period: null, setup: null, assignments: [], review: null, previous: null, csrf: null, busy: false };

async function request(path, method = "GET", body) {
    const headers = { Accept: "application/json" };
    if (method !== "GET") {
        state.csrf ??= await request("/csrf");
        headers[state.csrf.header] = state.csrf.token;
    }
    if (body !== undefined) headers["Content-Type"] = "application/json";
    const response = await fetch(api + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    if (response.redirected) {
        window.location.assign(response.url);
        throw new Error("Sign in again to continue.");
    }
    if (response.status === 204) return null;
    const data = (response.headers.get("content-type") ?? "").includes("json") ? await response.json() : null;
    if (!response.ok && !data?.review) {
        throw new Error(data?.issues?.map(issue => issue.description).join(" ") ?? "Request failed. HTTP " + response.status + ".");
    }
    return data;
}

function option(value, label) {
    const node = document.createElement("option");
    node.value = value;
    node.textContent = label;
    return node;
}

function select(id, values) {
    const field = element(id);
    const current = field.value;
    field.replaceChildren(...values.map(([value, text]) => option(value, text)));
    if (values.some(([value]) => String(value) === current)) field.value = current;
}

function cell(row, value) {
    const td = document.createElement("td");
    if (value instanceof Node) td.append(value);
    else td.textContent = value;
    row.append(td);
}

function message(text, error = false) {
    const field = element("message");
    field.textContent = text;
    field.className = error ? "error" : "";
}

async function action(work) {
    if (state.busy) return;
    state.busy = true;
    setButtons();
    try { await work(); }
    catch (error) { message(error.message, true); }
    finally { state.busy = false; setButtons(); }
}

function setButtons() {
    element("generate").disabled = state.busy || !state.period;
    element("open").disabled = state.busy || !state.setup?.drafts.length;
    element("swap").disabled = state.busy || state.assignments.length < 2;
    element("move").disabled = state.busy || !state.assignments.length || !state.setup?.slots.length;
    element("undo").disabled = state.busy || !state.previous;
    element("save").disabled = state.busy || !state.review?.valid
        || (!state.review.studentMembershipsComplete && !element("acknowledge").checked);
    element("period").disabled = state.busy;
}

async function loadSetup() {
    state.setup = await request("/" + state.period);
    select("semester", [["", "All semesters"], ...[...new Set(state.setup.exams.map(exam => exam.semester))]
        .map(semester => [semester, "Semester " + semester])]);
    select("slot", state.setup.slots.map(slot => [slot.id, slot.date + " " + slot.time]));
    select("versions", state.setup.drafts.map(draft => [draft.id, "Version " + draft.version]));
    renderSettings();
}

function renderSettings() {
    const body = element("settings");
    body.replaceChildren();
    const semester = element("semester").value;
    for (const exam of state.setup?.exams ?? []) {
        if (semester && exam.semester !== semester) continue;
        const row = document.createElement("tr");
        const enabled = document.createElement("input");
        enabled.type = "checkbox";
        enabled.checked = exam.enabled;
        enabled.setAttribute("aria-label", exam.moduleCode + " has exam");
        const duration = document.createElement("select");
        duration.append(option(120, "2 hours"), option(180, "3 hours"));
        duration.value = exam.durationMinutes;
        duration.setAttribute("aria-label", exam.moduleCode + " duration");
        const save = document.createElement("button");
        save.textContent = "Save setting";
        save.onclick = () => action(async () => {
            await request("/" + state.period + "/exams/" + exam.moduleId, "PUT",
                { enabled: enabled.checked, durationMinutes: Number(duration.value) });
            await loadSetup();
            updatePreview([], null);
            state.previous = null;
            message("Exam setting saved. Generate a fresh draft to use the updated requirements.");
        });
        for (const value of [exam.semester, exam.moduleCode + " " + exam.moduleName, exam.curriculum, enabled, duration, save]) cell(row, value);
        body.append(row);
    }
}

function updatePreview(assignments, review) {
    state.assignments = assignments;
    state.review = review;
    element("acknowledge").checked = false;
    const entries = new Map((review?.exams ?? []).map(exam => [exam.requirementId, exam]));
    const choices = assignments.map(a => {
        const exam = entries.get(a.requirementId);
        return [a.requirementId, exam ? exam.moduleCode + " " + exam.moduleName + " (" + exam.curriculum + ")"
            : "Requirement " + a.requirementId];
    });
    select("first", choices);
    select("second", choices);
    if (choices.length > 1 && element("first").value === element("second").value) element("second").selectedIndex = 1;

    renderIssues(review?.issues ?? []);
    const body = element("schedule");
    body.replaceChildren();
    for (const exam of review?.exams ?? []) {
        const row = document.createElement("tr");
        const venues = exam.venues.map(room => room.venueName + " (" + room.allocatedCount + " seats)").join(", ");
        for (const value of [exam.date, exam.startTime + "–" + exam.endTime, exam.moduleCode + " " + exam.moduleName,
            exam.curriculum, exam.studentCount, venues]) cell(row, value);
        body.append(row);
    }
    setButtons();
}

function renderIssues(entries) {
    const issues = element("issues");
    issues.replaceChildren();
    for (const issue of entries) {
        const row = document.createElement("li");
        row.className = issue.severity.toLowerCase();
        row.textContent = issue.severity + ": " + issue.description;
        issues.append(row);
    }
}

async function edit(next) {
    const review = await request("/" + state.period + "/review", "POST", { assignments: next });
    if (review.valid || !state.review?.valid) {
        state.previous = state.assignments;
        updatePreview(next, review);
        message(review.valid ? "Edit accepted in preview. Save a draft version to keep it."
            : "Working preview updated. Resolve the remaining errors before saving.", !review.valid);
    } else {
        renderIssues(review.issues);
        message("Edit rejected. The timetable preview still shows the last accepted times.", true);
    }
}

element("period").onchange = () => action(async () => {
    state.period = Number(element("period").value);
    state.previous = null;
    updatePreview([], null);
    await loadSetup();
    message("");
});
element("semester").onchange = renderSettings;
element("generate").onclick = () => action(async () => {
    const preview = await request("/" + state.period + "/preview", "POST");
    state.previous = null;
    updatePreview(preview.assignments, preview.review);
    message(preview.review.valid ? "Draft generated. Review the warnings and exam times." : "Draft needs changes before saving.");
});
element("open").onclick = () => action(async () => {
    const preview = await request("/" + state.period + "/drafts/" + element("versions").value);
    state.previous = null;
    updatePreview(preview.assignments, preview.review);
    message("Saved draft opened for review.");
});
element("swap").onclick = () => action(async () => {
    const first = state.assignments.find(a => a.requirementId === Number(element("first").value));
    const second = state.assignments.find(a => a.requirementId === Number(element("second").value));
    if (!first || !second || first === second) throw new Error("Choose two different exams.");
    await edit(state.assignments.map(a => a === first ? { ...a, startTimeslotId: second.startTimeslotId }
        : a === second ? { ...a, startTimeslotId: first.startTimeslotId } : a));
});
element("move").onclick = () => action(async () => {
    const id = Number(element("first").value);
    const slot = Number(element("slot").value);
    await edit(state.assignments.map(a => a.requirementId === id ? { ...a, startTimeslotId: slot } : a));
});
element("undo").onclick = () => action(async () => {
    const previous = state.previous;
    const review = await request("/" + state.period + "/review", "POST", { assignments: previous });
    state.previous = null;
    updatePreview(previous, review);
    message("Previous exam times restored.");
});
element("acknowledge").onchange = setButtons;
element("save").onclick = () => action(async () => {
    const result = await request("/" + state.period + "/drafts", "POST", {
        assignments: state.assignments, manualStudentReview: element("acknowledge").checked,
    });
    updatePreview(state.assignments, result.review);
    if (!result.timetableId) {
        message(result.manualReviewRequired ? "Confirm the manual student review before saving." : "Resolve the errors before saving.", true);
        return;
    }
    message("Draft version " + result.version + " saved. Timetable ID: " + result.timetableId + ".");
    await loadSetup();
});
action(async () => {
    const periods = await request("/periods");
    select("period", periods.map(period => [period.periodId, period.name]));
    if (periods.length) {
        state.period = Number(element("period").value);
        await loadSetup();
    } else message("Add an exam period before generating a draft.", true);
});
