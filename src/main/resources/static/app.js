const STATUSES = ['APPLIED', 'UNDER_REVIEW', 'SHORTLISTED', 'INTERVIEW', 'OFFERED', 'REJECTED'];
const TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP'];

let session = {
    token: localStorage.getItem('token'),
    role: localStorage.getItem('role'),
    name: localStorage.getItem('name')
};

const content = () => document.getElementById('content');
const $id = (id) => document.getElementById(id);

/* ---------- helpers ---------- */
function esc(v) {
    return String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function label(s) { return String(s).replaceAll('_', ' '); }
function num(v) { return v == null ? '-' : Number(v).toLocaleString(); }
function chips(csv) {
    if (!csv) return '<span class="muted">none</span>';
    return csv.split(',').map(s => `<span class="chip">${esc(s.trim())}</span>`).join('');
}
function scoreClass(n) { return n >= 70 ? 'good' : n >= 40 ? 'mid' : 'low'; }

function toast(msg, error = false) {
    const t = $id('toast');
    t.textContent = msg;
    t.className = error ? 'show error' : 'show';
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => { t.className = ''; }, 3500);
}

async function api(path, { method = 'GET', body } = {}) {
    const headers = {};
    if (session.token) headers['Authorization'] = 'Bearer ' + session.token;
    let payload;
    if (body instanceof FormData) {
        payload = body;
    } else if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
        payload = JSON.stringify(body);
    }
    const res = await fetch(path, { method, headers, body: payload });
    const text = await res.text();
    let data = null;
    if (text) { try { data = JSON.parse(text); } catch (e) { data = text; } }

    if (!res.ok) {
        if ((res.status === 401 || res.status === 403) && session.token && !path.startsWith('/api/auth')) {
            logout();
            throw new Error('Session expired. Please log in again.');
        }
        let msg = (data && (data.message || data.error)) || ('Request failed (' + res.status + ')');
        if (data && data.fields) {
            msg += ': ' + Object.entries(data.fields).map(([k, v]) => k + ' ' + v).join(', ');
        }
        throw new Error(msg);
    }
    return data;
}

/* ---------- session ---------- */
function saveSession(r) {
    session = { token: r.token, role: r.role, name: r.fullName };
    localStorage.setItem('token', r.token);
    localStorage.setItem('role', r.role);
    localStorage.setItem('name', r.fullName);
}
function logout() {
    localStorage.clear();
    session = { token: null, role: null, name: null };
    render();
}

/* ---------- layout ---------- */
function shell(tabs) {
    $id('topbar').innerHTML =
        `<h1>Job Portal</h1><div>${esc(session.name)} (${session.role === 'CANDIDATE' ? 'Job seeker' : 'Recruiter'}) ` +
        `<button class="btn small ghost" id="logoutBtn">Logout</button></div>`;
    $id('logoutBtn').onclick = logout;

    const nav = $id('tabs');
    nav.innerHTML = '';
    tabs.forEach(([name, fn]) => {
        const b = document.createElement('button');
        b.textContent = name;
        b.className = 'tab';
        b.onclick = () => {
            [...nav.children].forEach(c => c.classList.remove('active'));
            b.classList.add('active');
            fn();
        };
        nav.appendChild(b);
    });
    nav.children[0].click();
}

function render() {
    if (!session.token) return showAuth('login');
    if (session.role === 'CANDIDATE') {
        shell([['Search jobs', searchView], ['Recommended', recommendedView],
            ['My applications', applicationsView], ['My profile', profileView]]);
    } else {
        shell([['Dashboard', dashboardView], ['My jobs', jobsView], ['Company', companyView]]);
    }
}

/* ---------- login / register ---------- */
function showAuth(mode) {
    const isReg = mode === 'register';
    $id('tabs').innerHTML = '';
    $id('topbar').innerHTML = '<h1>Job Portal</h1>';
    content().innerHTML = `
    <section class="card narrow">
      <h2>${isReg ? 'Create account' : 'Login'}</h2>
      <form id="authForm">
        ${isReg ? '<label>Full name<input name="fullName" required></label>' : ''}
        <label>Email<input type="email" name="email" required></label>
        <label>Password<input type="password" name="password" minlength="6" required></label>
        ${isReg ? `<label>I am a
          <select name="role">
            <option value="CANDIDATE">Job seeker</option>
            <option value="RECRUITER">Recruiter / Employer</option>
          </select></label>` : ''}
        <button class="btn" type="submit">${isReg ? 'Register' : 'Login'}</button>
      </form>
      <p class="muted">${isReg ? 'Already have an account?' : 'New here?'}
        <a href="#" id="switchAuth">${isReg ? 'Login' : 'Create an account'}</a></p>
    </section>`;

    $id('switchAuth').onclick = (e) => { e.preventDefault(); showAuth(isReg ? 'login' : 'register'); };
    $id('authForm').onsubmit = async (e) => {
        e.preventDefault();
        const body = Object.fromEntries(new FormData(e.target));
        try {
            const r = await api('/api/auth/' + (isReg ? 'register' : 'login'), { method: 'POST', body });
            saveSession(r);
            render();
        } catch (err) { toast(err.message, true); }
    };
}

/* =====================  CANDIDATE  ===================== */
function renderJobs(list, canApply) {
    const box = $id('results');
    if (!list.length) { box.innerHTML = '<p class="muted">No jobs found.</p>'; return; }
    box.innerHTML = list.map(j => `
    <article class="card">
      <div class="between">
        <h3>${esc(j.title)}</h3>
        ${j.matchScore != null ? `<span class="badge ${scoreClass(j.matchScore)}">${j.matchScore}% match</span>` : ''}
      </div>
      <p class="muted">${esc(j.companyName)} &middot; ${esc(j.location)} &middot; ${esc(label(j.employmentType))}</p>
      <p>${esc(j.description)}</p>
      <p><strong>Salary:</strong> ${num(j.minSalary)} - ${num(j.maxSalary)}</p>
      <p><strong>Skills:</strong> ${chips(j.requiredSkills)}</p>
      ${j.missingSkills ? `<p class="hint">You lack: ${esc(j.missingSkills)}</p>` : ''}
      ${canApply ? `<button class="btn small" onclick="applyTo(${j.id})">Apply</button>` : ''}
    </article>`).join('');
}

async function searchView() {
    content().innerHTML = `
    <section class="card">
      <h2>Find a job</h2>
      <form id="searchForm" class="row">
        <input name="skill" placeholder="Skill (e.g. java)">
        <input name="location" placeholder="Location">
        <input name="minSalary" type="number" placeholder="Min salary">
        <select name="type"><option value="">Any type</option>${TYPES.map(t => `<option>${t}</option>`).join('')}</select>
        <button class="btn" type="submit">Search</button>
      </form>
    </section>
    <div id="results"></div>`;

    const run = async () => {
        const f = Object.fromEntries(new FormData($id('searchForm')));
        const q = new URLSearchParams(Object.entries(f).filter(([, v]) => v !== '')).toString();
        try {
            const page = await api('/api/jobs?' + q);
            renderJobs(page.content, true);
        } catch (e) { toast(e.message, true); }
    };
    $id('searchForm').onsubmit = (e) => { e.preventDefault(); run(); };
    run();
}

async function recommendedView() {
    content().innerHTML = `
    <h2>Recommended for you</h2>
    <p class="muted">Ranked by how many required skills you already have. Add skills in "My profile" to improve this.</p>
    <div id="results"></div>`;
    try { renderJobs(await api('/api/candidate/jobs/recommended'), true); }
    catch (e) { toast(e.message, true); }
}

async function applyTo(jobId) {
    const note = prompt('Cover note (optional):');
    if (note === null) return;
    try {
        await api('/api/candidate/jobs/' + jobId + '/apply', { method: 'POST', body: { coverNote: note } });
        toast('Application submitted!');
    } catch (e) { toast(e.message, true); }
}

async function applicationsView() {
    content().innerHTML = '<h2>My applications</h2><div id="apps"></div>';
    try {
        const apps = await api('/api/candidate/applications');
        const box = $id('apps');
        if (!apps.length) { box.innerHTML = '<p class="muted">You have not applied to any job yet.</p>'; return; }
        box.innerHTML = apps.map(a => `
      <article class="card">
        <div class="between">
          <h3>${esc(a.jobTitle)}</h3>
          <span class="badge status-${a.status}">${label(a.status)}</span>
        </div>
        <p class="muted">${esc(a.companyName)} &middot; applied ${new Date(a.appliedAt).toLocaleDateString()} &middot; match ${a.matchScore}%</p>
        <button class="btn small ghost" onclick="toggleTimeline(${a.id})">Show tracking timeline</button>
        <div id="tl-${a.id}" class="timeline"></div>
      </article>`).join('');
    } catch (e) { toast(e.message, true); }
}

async function toggleTimeline(id) {
    const el = $id('tl-' + id);
    if (el.innerHTML) { el.innerHTML = ''; return; }
    try {
        const items = await api('/api/candidate/applications/' + id + '/timeline');
        el.innerHTML = items.map(t => `
      <div class="step"><span class="dot"></span>
        <div><strong>${label(t.status)}</strong>
          <span class="muted">${new Date(t.changedAt).toLocaleString()}</span><br>${esc(t.note || '')}</div>
      </div>`).join('');
    } catch (e) { toast(e.message, true); }
}

function profileView() {
    content().innerHTML = `
    <section class="card">
      <h2>My profile</h2>
      <form id="profileForm">
        <label>Skills (comma separated)
          <input name="skills" placeholder="Java, Spring Boot, Git" value="${esc(localStorage.getItem('skills') || '')}"></label>
        <label>Location
          <input name="location" value="${esc(localStorage.getItem('location') || '')}"></label>
        <button class="btn" type="submit">Save profile</button>
      </form>
    </section>
    <section class="card">
      <h2>Resume</h2>
      <p class="muted">PDF, DOC or DOCX, up to 5 MB. You must upload a resume before applying.</p>
      <form id="resumeForm">
        <input type="file" name="file" accept=".pdf,.doc,.docx" required>
        <p><button class="btn" type="submit">Upload resume</button></p>
      </form>
    </section>`;

    $id('profileForm').onsubmit = async (e) => {
        e.preventDefault();
        const body = Object.fromEntries(new FormData(e.target));
        try {
            const r = await api('/api/candidate/profile', { method: 'PUT', body });
            localStorage.setItem('skills', body.skills);
            localStorage.setItem('location', body.location);
            toast('Profile saved: ' + (r.skills || 'no skills'));
        } catch (err) { toast(err.message, true); }
    };
    $id('resumeForm').onsubmit = async (e) => {
        e.preventDefault();
        try {
            await api('/api/candidate/resume', { method: 'POST', body: new FormData(e.target) });
            toast('Resume uploaded');
        } catch (err) { toast(err.message, true); }
    };
}

/* =====================  RECRUITER  ===================== */
async function dashboardView() {
    content().innerHTML = '<h2>Recruiter dashboard</h2><div id="dash"></div>';
    try {
        const d = await api('/api/recruiter/dashboard');
        const max = Math.max(1, ...Object.values(d.funnel));
        $id('dash').innerHTML = `
      <div class="stats">
        <div class="stat"><b>${d.totalJobs}</b>Jobs</div>
        <div class="stat"><b>${d.openJobs}</b>Open jobs</div>
        <div class="stat"><b>${d.totalApplications}</b>Applications</div>
      </div>
      <section class="card">
        <h3>Hiring funnel</h3>
        ${Object.entries(d.funnel).map(([k, v]) => `
          <div class="bar-row"><span>${label(k)}</span>
            <div class="bar"><div style="width:${(v / max) * 100}%"></div></div><b>${v}</b></div>`).join('')}
      </section>
      <section class="card">
        <h3>Applications per job</h3>
        ${d.perJob.length ? `<table><tr><th>Job</th><th>Applications</th><th>Avg match</th></tr>
          ${d.perJob.map(p => `<tr><td>${esc(p.jobTitle)}</td><td>${p.applications}</td><td>${p.avgMatchScore}%</td></tr>`).join('')}
          </table>` : '<p class="muted">No applications yet.</p>'}
      </section>`;
    } catch (e) { toast(e.message, true); }
}

let jobsCache = [];

async function jobsView() {
    content().innerHTML = `
    <section class="card">
      <h2 id="jobFormTitle">Post a new job</h2>
      <form id="jobForm">
        <input type="hidden" name="id">
        <label>Title<input name="title" required></label>
        <label>Description<textarea name="description" rows="3" required></textarea></label>
        <div class="row">
          <label>Location<input name="location" required></label>
          <label>Type<select name="employmentType">${TYPES.map(t => `<option>${t}</option>`).join('')}</select></label>
        </div>
        <div class="row">
          <label>Min salary<input name="minSalary" type="number"></label>
          <label>Max salary<input name="maxSalary" type="number"></label>
          <label>Expires on<input name="expiresAt" type="date"></label>
        </div>
        <label>Required skills (comma separated)
          <input name="requiredSkills" placeholder="java, spring boot, postgresql" required></label>
        <button class="btn" type="submit">Save job</button>
        <button class="btn ghost" type="button" id="resetJob">Clear</button>
      </form>
    </section>
    <h2>My jobs</h2>
    <div id="myJobs"></div>
    <div id="applicants"></div>`;

    const form = $id('jobForm');
    $id('resetJob').onclick = () => { form.reset(); $id('jobFormTitle').textContent = 'Post a new job'; };
    form.onsubmit = async (e) => {
        e.preventDefault();
        const f = Object.fromEntries(new FormData(form));
        const id = f.id;
        delete f.id;
        f.minSalary = f.minSalary ? Number(f.minSalary) : null;
        f.maxSalary = f.maxSalary ? Number(f.maxSalary) : null;
        if (!f.expiresAt) f.expiresAt = null;
        try {
            await api(id ? '/api/recruiter/jobs/' + id : '/api/recruiter/jobs', { method: id ? 'PUT' : 'POST', body: f });
            toast('Job saved');
            jobsView();
        } catch (err) { toast(err.message, true); }
    };
    loadMyJobs();
}

async function loadMyJobs() {
    try {
        jobsCache = await api('/api/recruiter/jobs');
        const box = $id('myJobs');
        if (!jobsCache.length) { box.innerHTML = '<p class="muted">You have not posted any job yet. Create your company profile first, then post a job.</p>'; return; }
        box.innerHTML = jobsCache.map(j => `
      <article class="card">
        <div class="between">
          <h3>${esc(j.title)}</h3>
          <span class="badge ${j.status === 'OPEN' ? 'good' : 'low'}">${j.status}</span>
        </div>
        <p class="muted">${esc(j.location)} &middot; ${esc(label(j.employmentType))} &middot; expires ${j.expiresAt || 'never'}</p>
        <p>${chips(j.requiredSkills)}</p>
        <button class="btn small" onclick="viewApplicants(${j.id})">Applicants</button>
        <button class="btn small ghost" onclick="editJob(${j.id})">Edit</button>
        ${j.status === 'OPEN' ? `<button class="btn small ghost" onclick="closeJob(${j.id})">Close</button>` : ''}
      </article>`).join('');
    } catch (e) { toast(e.message, true); }
}

function editJob(id) {
    const j = jobsCache.find(x => x.id === id);
    const form = $id('jobForm');
    form.id.value = j.id;
    form.title.value = j.title;
    form.description.value = j.description;
    form.location.value = j.location;
    form.employmentType.value = j.employmentType;
    form.minSalary.value = j.minSalary ?? '';
    form.maxSalary.value = j.maxSalary ?? '';
    form.expiresAt.value = j.expiresAt || '';
    form.requiredSkills.value = j.requiredSkills;
    $id('jobFormTitle').textContent = 'Edit job #' + j.id;
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function closeJob(id) {
    if (!confirm('Close this job? It will no longer appear in search.')) return;
    try { await api('/api/recruiter/jobs/' + id + '/close', { method: 'PUT' }); toast('Job closed'); loadMyJobs(); }
    catch (e) { toast(e.message, true); }
}

async function viewApplicants(jobId) {
    try {
        const apps = await api('/api/recruiter/jobs/' + jobId + '/applications');
        const box = $id('applicants');
        if (!apps.length) { box.innerHTML = '<section class="card"><p class="muted">No applications for this job yet.</p></section>'; return; }
        box.innerHTML = `
      <section class="card">
        <h3>Applicants (best match first)</h3>
        <table>
          <tr><th>Candidate</th><th>Skills</th><th>Match</th><th>Status</th><th>Update</th></tr>
          ${apps.map(a => `
            <tr>
              <td>${esc(a.candidateName)}<br><span class="muted">${esc(a.candidateEmail)}</span></td>
              <td>${chips(a.candidateSkills)}</td>
              <td><span class="badge ${scoreClass(a.matchScore)}">${a.matchScore}%</span></td>
              <td><span class="badge status-${a.status}">${label(a.status)}</span></td>
              <td class="actions">
                <select id="st-${a.id}">${STATUSES.map(s => `<option ${s === a.status ? 'selected' : ''}>${s}</option>`).join('')}</select>
                <input id="nt-${a.id}" placeholder="Note">
                <button class="btn small" onclick="setStatus(${a.id}, ${jobId})">Update</button>
                <button class="btn small ghost" onclick="downloadResume(${a.id})">Resume</button>
              </td>
            </tr>`).join('')}
        </table>
      </section>`;
        box.scrollIntoView({ behavior: 'smooth' });
    } catch (e) { toast(e.message, true); }
}

async function setStatus(appId, jobId) {
    const status = $id('st-' + appId).value;
    const note = $id('nt-' + appId).value;
    try {
        await api('/api/recruiter/applications/' + appId + '/status', { method: 'PUT', body: { status, note } });
        toast('Status updated');
        viewApplicants(jobId);
    } catch (e) { toast(e.message, true); }
}

async function downloadResume(appId) {
    try {
        const res = await fetch('/api/recruiter/applications/' + appId + '/resume',
            { headers: { Authorization: 'Bearer ' + session.token } });
        if (!res.ok) { toast('Could not download resume', true); return; }
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'resume_application_' + appId;
        a.click();
        URL.revokeObjectURL(url);
    } catch (e) { toast(e.message, true); }
}

async function companyView() {
    content().innerHTML = `
    <section class="card">
      <h2>Company profile</h2>
      <form id="companyForm">
        <label>Company name<input name="name" required></label>
        <label>Description<textarea name="description" rows="3"></textarea></label>
        <div class="row">
          <label>Website<input name="website"></label>
          <label>Location<input name="location"></label>
        </div>
        <button class="btn" type="submit">Save company</button>
      </form>
    </section>`;
    const form = $id('companyForm');
    try {
        const c = await api('/api/recruiter/company');
        form.name.value = c.name || '';
        form.description.value = c.description || '';
        form.website.value = c.website || '';
        form.location.value = c.location || '';
    } catch (e) { /* no company yet: show empty form */ }

    form.onsubmit = async (e) => {
        e.preventDefault();
        const body = Object.fromEntries(new FormData(form));
        try { await api('/api/recruiter/company', { method: 'PUT', body }); toast('Company saved'); }
        catch (err) { toast(err.message, true); }
    };
}

/* ---------- start ---------- */
render();