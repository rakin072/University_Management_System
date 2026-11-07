// State variables
let currentUser = null;
let currentRole = null;
let studentsList = [];
let teachersList = [];
let adminsList = [];
let teacherPortalList = [];
let studentPortalList = [];

// Initialize on page load
window.addEventListener('DOMContentLoaded', () => {
  checkSession();
});

// =========================================================================
// 1. Session & Navigation
// =========================================================================

async function checkSession() {
  try {
    const res = await fetch('/api/auth/me', { credentials: 'include' });
    if (res.ok) {
      const data = await res.json();
      if (data.authenticated) {
        currentUser = data;
        currentRole = data.primaryRole || 'USER';
        updateNavbarUser(data.username, currentRole);

        // Show appropriate view based on role
        if (currentRole === 'ADMIN') {
          showView('admin');
        } else if (currentRole === 'TEACHER') {
          showView('teacher');
        } else if (currentRole === 'STUDENT') {
          showView('student');
        } else {
          showView('login');
        }
        return;
      }
    }
  } catch (err) {
    console.error('Session check failed:', err);
  }
  
  // Not authenticated
  currentUser = null;
  currentRole = null;
  updateNavbarUnauthenticated();
  showView('login');
}

function updateNavbarUser(username, role) {
  const statusText = document.getElementById('authStatusText');
  const authActions = document.getElementById('authActions');
  const userInfo = document.getElementById('userInfoDisplay');

  if (statusText) statusText.style.display = 'none';
  if (authActions) authActions.style.display = 'flex';
  if (userInfo) {
    const badgeClass = (role === 'ADMIN') ? 'badge-admin' : (role === 'TEACHER' ? 'badge-teacher' : 'badge-student');
    userInfo.innerHTML = `<span>User: <strong>${escapeHtml(username)}</strong></span> <span class="badge ${badgeClass}">${role}</span>`;
  }
}

function updateNavbarUnauthenticated() {
  const statusText = document.getElementById('authStatusText');
  const authActions = document.getElementById('authActions');

  if (statusText) {
    statusText.textContent = 'Not Logged In';
    statusText.style.display = 'inline';
  }
  if (authActions) {
    authActions.style.display = 'none';
  }
}

function showView(view) {
  // Strict role-based protection
  if (view === 'admin' && currentRole !== 'ADMIN') {
    alert('Access Denied: You do not have permission to access the Administrator section.');
    return;
  }
  if (view === 'teacher' && currentRole !== 'TEACHER' && currentRole !== 'ADMIN') {
    alert('Access Denied: You do not have permission to access the Teacher section.');
    return;
  }
  if (view === 'student' && currentRole !== 'STUDENT' && currentRole !== 'ADMIN') {
    alert('Access Denied: You do not have permission to access the Student section.');
    return;
  }

  const loginView = document.getElementById('loginView');
  const adminView = document.getElementById('adminView');
  const teacherView = document.getElementById('teacherView');
  const studentView = document.getElementById('studentView');

  if (loginView) loginView.style.display = (view === 'login') ? 'block' : 'none';
  if (adminView) adminView.style.display = (view === 'admin') ? 'block' : 'none';
  if (teacherView) teacherView.style.display = (view === 'teacher') ? 'block' : 'none';
  if (studentView) studentView.style.display = (view === 'student') ? 'block' : 'none';

  if (view === 'admin') {
    loadAllAdminData();
  } else if (view === 'teacher') {
    loadTeacherView();
  } else if (view === 'student') {
    loadStudentView();
  }
}

// Admin Tab Switching (Students, Teachers, Admins, or All)
function switchAdminTab(tab, btn) {
  const buttons = document.querySelectorAll('.tab-nav .tab-btn');
  buttons.forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');

  const secStudents = document.getElementById('adminSectionStudents');
  const secTeachers = document.getElementById('adminSectionTeachers');
  const secAdmins = document.getElementById('adminSectionAdmins');

  if (!secStudents || !secTeachers || !secAdmins) return;

  if (tab === 'students') {
    secStudents.style.display = 'block';
    secTeachers.style.display = 'none';
    secAdmins.style.display = 'none';
  } else if (tab === 'teachers') {
    secStudents.style.display = 'none';
    secTeachers.style.display = 'block';
    secAdmins.style.display = 'none';
  } else if (tab === 'admins') {
    secStudents.style.display = 'none';
    secTeachers.style.display = 'none';
    secAdmins.style.display = 'block';
  } else if (tab === 'all') {
    secStudents.style.display = 'block';
    secTeachers.style.display = 'block';
    secAdmins.style.display = 'block';
  }
}

// =========================================================================
// 2. Authentication
// =========================================================================

async function quickLogin(user, pass) {
  const userInput = document.getElementById('loginUser');
  const passInput = document.getElementById('loginPass');
  if (userInput) userInput.value = user;
  if (passInput) passInput.value = pass;
  await performLogin(user, pass);
}

async function submitLogin(e) {
  e.preventDefault();
  const u = document.getElementById('loginUser').value.trim();
  const p = document.getElementById('loginPass').value;
  await performLogin(u, p);
}

async function performLogin(username, password) {
  const msgEl = document.getElementById('loginMessage');
  if (msgEl) {
    msgEl.textContent = 'Authenticating...';
    msgEl.style.display = 'block';
    msgEl.className = 'alert alert-error';
  }

  try {
    const params = new URLSearchParams();
    params.append('username', username);
    params.append('password', password);

    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString(),
      credentials: 'include'
    });

    if (!res.ok) {
      if (msgEl) {
        msgEl.textContent = 'Invalid username or password';
        msgEl.style.display = 'block';
      }
      return;
    }

    if (msgEl) {
      msgEl.style.display = 'none';
      msgEl.textContent = '';
    }
    await checkSession();
  } catch (e) {
    if (msgEl) {
      msgEl.textContent = 'Error connecting to server';
      msgEl.style.display = 'block';
    }
    console.error(e);
  }
}

async function handleLogout() {
  try {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
  } catch (e) {
    console.error(e);
  }
  currentUser = null;
  currentRole = null;
  const loginUser = document.getElementById('loginUser');
  const loginPass = document.getElementById('loginPass');
  if (loginUser) loginUser.value = '';
  if (loginPass) loginPass.value = '';

  updateNavbarUnauthenticated();
  showView('login');
}

// =========================================================================
// 3. ADMIN PORTAL (CRUD on Students, Teachers, Admins)
// =========================================================================

async function loadAllAdminData() {
  resetStudentForm();
  resetTeacherForm();
  resetAdminForm();
  await Promise.all([
    loadStudents(),
    loadTeachers(),
    loadAdmins()
  ]);
}

// --- STUDENT CRUD (Admin) ---

async function loadStudents() {
  const tbody = document.getElementById('studentsTbody');
  if (!tbody) return;
  try {
    const res = await fetch('/api/students', { credentials: 'include' });
    if (!res.ok) {
      tbody.innerHTML = `<tr><td colspan="7">Access Denied (${res.status} Forbidden)</td></tr>`;
      return;
    }
    studentsList = await res.json();
    renderStudents();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7">Error loading students: ${err.message}</td></tr>`;
  }
}

function renderStudents() {
  const tbody = document.getElementById('studentsTbody');
  if (!tbody) return;
  if (studentsList.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7">No students registered yet.</td></tr>';
    return;
  }
  tbody.innerHTML = studentsList.map(s => `
    <tr>
      <td>${s.id}</td>
      <td><strong>${escapeHtml(s.username)}</strong></td>
      <td>${escapeHtml(s.fullName)}</td>
      <td>${escapeHtml(s.email)}</td>
      <td>${escapeHtml(s.program || '')}</td>
      <td>${s.batch || ''}</td>
      <td>
        <button type="button" class="btn btn-edit" onclick="editStudent(${s.id})">Edit</button>
        <button type="button" class="btn btn-danger" onclick="deleteStudent(${s.id})">Delete</button>
      </td>
    </tr>
  `).join('');
}

function editStudent(id) {
  const s = studentsList.find(x => x.id === id);
  if (!s) return;
  document.getElementById('studentId').value = s.id;
  document.getElementById('studentUsername').value = s.username;
  document.getElementById('studentUsername').disabled = true;
  document.getElementById('studentFullName').value = s.fullName || '';
  document.getElementById('studentEmail').value = s.email || '';
  document.getElementById('studentProgram').value = s.program || '';
  document.getElementById('studentBatch').value = s.batch || '';
  document.getElementById('studentPassword').value = '';
  document.getElementById('studentPassword').required = false;
  document.getElementById('studentPassReq').style.display = 'none';
  document.getElementById('studentPassNote').style.display = 'inline';

  document.getElementById('studentFormHeading').textContent = `Edit Student (ID #${s.id})`;
  document.getElementById('studentSubmitBtn').textContent = 'Update Student';
  document.getElementById('studentCancelBtn').style.display = 'inline';
}

function resetStudentForm() {
  const idEl = document.getElementById('studentId');
  if (!idEl) return;
  idEl.value = '';
  document.getElementById('studentUsername').value = '';
  document.getElementById('studentUsername').disabled = false;
  document.getElementById('studentFullName').value = '';
  document.getElementById('studentEmail').value = '';
  document.getElementById('studentProgram').value = '';
  document.getElementById('studentBatch').value = '';
  document.getElementById('studentPassword').value = '';
  document.getElementById('studentPassword').required = true;
  document.getElementById('studentPassReq').style.display = 'inline';
  document.getElementById('studentPassNote').style.display = 'none';

  document.getElementById('studentFormHeading').textContent = 'Add New Student';
  document.getElementById('studentSubmitBtn').textContent = 'Save Student';
  document.getElementById('studentCancelBtn').style.display = 'none';
}

async function saveStudent(e) {
  e.preventDefault();
  const id = document.getElementById('studentId').value;
  const isEdit = Boolean(id);

  const payload = {
    fullName: document.getElementById('studentFullName').value.trim(),
    email: document.getElementById('studentEmail').value.trim(),
    program: document.getElementById('studentProgram').value.trim(),
    batch: parseInt(document.getElementById('studentBatch').value, 10)
  };

  const pass = document.getElementById('studentPassword').value;
  if (pass) {
    payload.password = pass;
  }

  if (!isEdit) {
    payload.username = document.getElementById('studentUsername').value.trim();
  }

  try {
    const url = isEdit ? `/api/students/${id}` : '/api/students';
    const method = isEdit ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      credentials: 'include'
    });

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      alert(`Error saving student: ${errData.error || ('Status ' + res.status)}`);
      return;
    }

    alert(isEdit ? 'Student updated successfully!' : 'Student created successfully!');
    resetStudentForm();
    await loadStudents();
  } catch (err) {
    alert('Failed to save student: ' + err.message);
  }
}

async function deleteStudent(id) {
  if (!confirm(`Are you sure you want to delete student ID #${id}?`)) return;
  try {
    const res = await fetch(`/api/students/${id}`, { method: 'DELETE', credentials: 'include' });
    if (!res.ok) {
      alert(`Error deleting student (${res.status})`);
      return;
    }
    alert('Student deleted successfully!');
    await loadStudents();
  } catch (err) {
    alert('Failed to delete student: ' + err.message);
  }
}

// --- TEACHER CRUD (Admin) ---

async function loadTeachers() {
  const tbody = document.getElementById('teachersTbody');
  if (!tbody) return;
  try {
    const res = await fetch('/api/teachers', { credentials: 'include' });
    if (!res.ok) {
      tbody.innerHTML = `<tr><td colspan="7">Access Denied (${res.status} Forbidden)</td></tr>`;
      return;
    }
    teachersList = await res.json();
    renderTeachers();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7">Error loading teachers: ${err.message}</td></tr>`;
  }
}

function renderTeachers() {
  const tbody = document.getElementById('teachersTbody');
  if (!tbody) return;
  if (teachersList.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7">No teachers registered yet.</td></tr>';
    return;
  }
  tbody.innerHTML = teachersList.map(t => `
    <tr>
      <td>${t.id}</td>
      <td><strong>${escapeHtml(t.username)}</strong></td>
      <td>${escapeHtml(t.fullName)}</td>
      <td>${escapeHtml(t.email)}</td>
      <td>${escapeHtml(t.department || '')}</td>
      <td>${escapeHtml(t.designation || '')}</td>
      <td>
        <button type="button" class="btn btn-edit" onclick="editTeacher(${t.id})">Edit</button>
        <button type="button" class="btn btn-danger" onclick="deleteTeacher(${t.id})">Delete</button>
      </td>
    </tr>
  `).join('');
}

function editTeacher(id) {
  const t = teachersList.find(x => x.id === id);
  if (!t) return;
  document.getElementById('teacherId').value = t.id;
  document.getElementById('teacherUsername').value = t.username;
  document.getElementById('teacherUsername').disabled = true;
  document.getElementById('teacherFullName').value = t.fullName || '';
  document.getElementById('teacherEmail').value = t.email || '';
  document.getElementById('teacherDepartment').value = t.department || '';
  document.getElementById('teacherDesignation').value = t.designation || '';
  document.getElementById('teacherPassword').value = '';
  document.getElementById('teacherPassword').required = false;
  document.getElementById('teacherPassReq').style.display = 'none';
  document.getElementById('teacherPassNote').style.display = 'inline';

  document.getElementById('teacherFormHeading').textContent = `Edit Teacher (ID #${t.id})`;
  document.getElementById('teacherSubmitBtn').textContent = 'Update Teacher';
  document.getElementById('teacherCancelBtn').style.display = 'inline';
}

function resetTeacherForm() {
  const idEl = document.getElementById('teacherId');
  if (!idEl) return;
  idEl.value = '';
  document.getElementById('teacherUsername').value = '';
  document.getElementById('teacherUsername').disabled = false;
  document.getElementById('teacherFullName').value = '';
  document.getElementById('teacherEmail').value = '';
  document.getElementById('teacherDepartment').value = '';
  document.getElementById('teacherDesignation').value = '';
  document.getElementById('teacherPassword').value = '';
  document.getElementById('teacherPassword').required = true;
  document.getElementById('teacherPassReq').style.display = 'inline';
  document.getElementById('teacherPassNote').style.display = 'none';

  document.getElementById('teacherFormHeading').textContent = 'Add New Teacher';
  document.getElementById('teacherSubmitBtn').textContent = 'Save Teacher';
  document.getElementById('teacherCancelBtn').style.display = 'none';
}

async function saveTeacher(e) {
  e.preventDefault();
  const id = document.getElementById('teacherId').value;
  const isEdit = Boolean(id);

  const payload = {
    fullName: document.getElementById('teacherFullName').value.trim(),
    email: document.getElementById('teacherEmail').value.trim(),
    department: document.getElementById('teacherDepartment').value.trim(),
    designation: document.getElementById('teacherDesignation').value.trim()
  };

  const pass = document.getElementById('teacherPassword').value;
  if (pass) {
    payload.password = pass;
  }

  if (!isEdit) {
    payload.username = document.getElementById('teacherUsername').value.trim();
  }

  try {
    const url = isEdit ? `/api/teachers/${id}` : '/api/teachers';
    const method = isEdit ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      credentials: 'include'
    });

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      alert(`Error saving teacher: ${errData.error || ('Status ' + res.status)}`);
      return;
    }

    alert(isEdit ? 'Teacher updated successfully!' : 'Teacher created successfully!');
    resetTeacherForm();
    await loadTeachers();
  } catch (err) {
    alert('Failed to save teacher: ' + err.message);
  }
}

async function deleteTeacher(id) {
  if (!confirm(`Are you sure you want to delete teacher ID #${id}?`)) return;
  try {
    const res = await fetch(`/api/teachers/${id}`, { method: 'DELETE', credentials: 'include' });
    if (!res.ok) {
      alert(`Error deleting teacher (${res.status})`);
      return;
    }
    alert('Teacher deleted successfully!');
    await loadTeachers();
  } catch (err) {
    alert('Failed to delete teacher: ' + err.message);
  }
}

// --- ADMIN CRUD (Admin) ---

async function loadAdmins() {
  const tbody = document.getElementById('adminsTbody');
  if (!tbody) return;
  try {
    const res = await fetch('/api/admins', { credentials: 'include' });
    if (!res.ok) {
      tbody.innerHTML = `<tr><td colspan="6">Access Denied (${res.status} Forbidden)</td></tr>`;
      return;
    }
    adminsList = await res.json();
    renderAdmins();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6">Error loading admins: ${err.message}</td></tr>`;
  }
}

function renderAdmins() {
  const tbody = document.getElementById('adminsTbody');
  if (!tbody) return;
  if (adminsList.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6">No admins found.</td></tr>';
    return;
  }
  tbody.innerHTML = adminsList.map(a => `
    <tr>
      <td>${a.id}</td>
      <td><strong>${escapeHtml(a.username)}</strong></td>
      <td>${escapeHtml(a.fullName)}</td>
      <td>${escapeHtml(a.email)}</td>
      <td>${escapeHtml(a.officeNumber || '')}</td>
      <td>
        <button type="button" class="btn btn-edit" onclick="editAdmin(${a.id})">Edit</button>
        <button type="button" class="btn btn-danger" onclick="deleteAdmin(${a.id})">Delete</button>
      </td>
    </tr>
  `).join('');
}

function editAdmin(id) {
  const a = adminsList.find(x => x.id === id);
  if (!a) return;
  document.getElementById('adminId').value = a.id;
  document.getElementById('adminUsername').value = a.username;
  document.getElementById('adminUsername').disabled = true;
  document.getElementById('adminFullName').value = a.fullName || '';
  document.getElementById('adminEmail').value = a.email || '';
  document.getElementById('adminOfficeNumber').value = a.officeNumber || '';
  document.getElementById('adminPassword').value = '';
  document.getElementById('adminPassword').required = false;
  document.getElementById('adminPassReq').style.display = 'none';
  document.getElementById('adminPassNote').style.display = 'inline';

  document.getElementById('adminFormHeading').textContent = `Edit Admin (ID #${a.id})`;
  document.getElementById('adminSubmitBtn').textContent = 'Update Admin';
  document.getElementById('adminCancelBtn').style.display = 'inline';
}

function resetAdminForm() {
  const idEl = document.getElementById('adminId');
  if (!idEl) return;
  idEl.value = '';
  document.getElementById('adminUsername').value = '';
  document.getElementById('adminUsername').disabled = false;
  document.getElementById('adminFullName').value = '';
  document.getElementById('adminEmail').value = '';
  document.getElementById('adminOfficeNumber').value = '';
  document.getElementById('adminPassword').value = '';
  document.getElementById('adminPassword').required = true;
  document.getElementById('adminPassReq').style.display = 'inline';
  document.getElementById('adminPassNote').style.display = 'none';

  document.getElementById('adminFormHeading').textContent = 'Add New Admin';
  document.getElementById('adminSubmitBtn').textContent = 'Save Admin';
  document.getElementById('adminCancelBtn').style.display = 'none';
}

async function saveAdmin(e) {
  e.preventDefault();
  const id = document.getElementById('adminId').value;
  const isEdit = Boolean(id);

  const payload = {
    fullName: document.getElementById('adminFullName').value.trim(),
    email: document.getElementById('adminEmail').value.trim(),
    officeNumber: document.getElementById('adminOfficeNumber').value.trim()
  };

  const pass = document.getElementById('adminPassword').value;
  if (pass) {
    payload.password = pass;
  }

  if (!isEdit) {
    payload.username = document.getElementById('adminUsername').value.trim();
  }

  try {
    const url = isEdit ? `/api/admins/${id}` : '/api/admins';
    const method = isEdit ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      credentials: 'include'
    });

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      alert(`Error saving admin: ${errData.error || ('Status ' + res.status)}`);
      return;
    }

    alert(isEdit ? 'Admin updated successfully!' : 'Admin created successfully!');
    resetAdminForm();
    await loadAdmins();
  } catch (err) {
    alert('Failed to save admin: ' + err.message);
  }
}

async function deleteAdmin(id) {
  if (!confirm(`Are you sure you want to delete admin ID #${id}?`)) return;
  try {
    const res = await fetch(`/api/admins/${id}`, { method: 'DELETE', credentials: 'include' });
    if (!res.ok) {
      alert(`Error deleting admin (${res.status})`);
      return;
    }
    alert('Admin deleted successfully!');
    await loadAdmins();
  } catch (err) {
    alert('Failed to delete admin: ' + err.message);
  }
}

// =========================================================================
// 4. TEACHER PORTAL (CRUD on Teachers ONLY)
// =========================================================================

async function loadTeacherView() {
  const prof = (currentUser && currentUser.profile) ? currentUser.profile : {};
  const tProfU = document.getElementById('tProfUsername');
  const tProfFN = document.getElementById('tProfFullName');
  const tProfE = document.getElementById('tProfEmail');
  const tProfD = document.getElementById('tProfDepartment');
  const tProfDes = document.getElementById('tProfDesignation');

  if (tProfU) tProfU.textContent = prof.username || (currentUser ? currentUser.username : '-');
  if (tProfFN) tProfFN.textContent = prof.fullName || '-';
  if (tProfE) tProfE.textContent = prof.email || '-';
  if (tProfD) tProfD.textContent = prof.department || '-';
  if (tProfDes) tProfDes.textContent = prof.designation || '-';

  resetTeacherPortalForm();
  await loadTeacherPortalData();
}

async function loadTeacherPortalData() {
  const tbody = document.getElementById('teacherPortalTbody');
  if (!tbody) return;
  try {
    const res = await fetch('/api/teachers', { credentials: 'include' });
    if (!res.ok) {
      tbody.innerHTML = `<tr><td colspan="6">Access Denied (${res.status} Forbidden)</td></tr>`;
      return;
    }
    teacherPortalList = await res.json();
    renderTeacherPortalTable();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6">Error loading teachers: ${err.message}</td></tr>`;
  }
}

function renderTeacherPortalTable() {
  const tbody = document.getElementById('teacherPortalTbody');
  if (!tbody) return;
  if (teacherPortalList.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6">No teachers found.</td></tr>';
    return;
  }
  tbody.innerHTML = teacherPortalList.map(t => `
    <tr>
      <td>${t.id}</td>
      <td><strong>${escapeHtml(t.username)}</strong></td>
      <td>${escapeHtml(t.fullName)}</td>
      <td>${escapeHtml(t.email)}</td>
      <td>${escapeHtml(t.department || '')}</td>
      <td>${escapeHtml(t.designation || '')}</td>
    </tr>
  `).join('');
}

function editTeacherPortal(id) {
  const t = teacherPortalList.find(x => x.id === id);
  if (!t) return;
  document.getElementById('teacherPortalId').value = t.id;
  document.getElementById('teacherPortalUsername').value = t.username;
  document.getElementById('teacherPortalUsername').disabled = true;
  document.getElementById('teacherPortalFullName').value = t.fullName || '';
  document.getElementById('teacherPortalEmail').value = t.email || '';
  document.getElementById('teacherPortalDepartment').value = t.department || '';
  document.getElementById('teacherPortalDesignation').value = t.designation || '';
  document.getElementById('teacherPortalPassword').value = '';
  document.getElementById('teacherPortalPassword').required = false;
  document.getElementById('teacherPortalPassReq').style.display = 'none';
  document.getElementById('teacherPortalPassNote').style.display = 'inline';

  document.getElementById('teacherPortalHeading').textContent = `Edit Teacher (ID #${t.id})`;
  document.getElementById('teacherPortalSubmitBtn').textContent = 'Update Teacher';
  document.getElementById('teacherPortalCancelBtn').style.display = 'inline';
}

function resetTeacherPortalForm() {
  const idEl = document.getElementById('teacherPortalId');
  if (!idEl) return;
  idEl.value = '';
  document.getElementById('teacherPortalUsername').value = '';
  document.getElementById('teacherPortalUsername').disabled = false;
  document.getElementById('teacherPortalFullName').value = '';
  document.getElementById('teacherPortalEmail').value = '';
  document.getElementById('teacherPortalDepartment').value = '';
  document.getElementById('teacherPortalDesignation').value = '';
  document.getElementById('teacherPortalPassword').value = '';
  document.getElementById('teacherPortalPassword').required = true;
  document.getElementById('teacherPortalPassReq').style.display = 'inline';
  document.getElementById('teacherPortalPassNote').style.display = 'none';

  document.getElementById('teacherPortalHeading').textContent = 'Add New Teacher';
  document.getElementById('teacherPortalSubmitBtn').textContent = 'Save Teacher';
  document.getElementById('teacherPortalCancelBtn').style.display = 'none';
}

async function saveTeacherPortal(e) {
  e.preventDefault();
  const id = document.getElementById('teacherPortalId').value;
  const isEdit = Boolean(id);

  const payload = {
    fullName: document.getElementById('teacherPortalFullName').value.trim(),
    email: document.getElementById('teacherPortalEmail').value.trim(),
    department: document.getElementById('teacherPortalDepartment').value.trim(),
    designation: document.getElementById('teacherPortalDesignation').value.trim()
  };

  const pass = document.getElementById('teacherPortalPassword').value;
  if (pass) {
    payload.password = pass;
  }

  if (!isEdit) {
    payload.username = document.getElementById('teacherPortalUsername').value.trim();
  }

  try {
    const url = isEdit ? `/api/teachers/${id}` : '/api/teachers';
    const method = isEdit ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      credentials: 'include'
    });

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      alert(`Error saving teacher: ${errData.error || ('Status ' + res.status)}`);
      return;
    }

    alert(isEdit ? 'Teacher updated successfully!' : 'Teacher created successfully!');
    resetTeacherPortalForm();
    await loadTeacherPortalData();
  } catch (err) {
    alert('Failed to save teacher: ' + err.message);
  }
}

async function deleteTeacherPortal(id) {
  if (!confirm(`Are you sure you want to delete teacher ID #${id}?`)) return;
  try {
    const res = await fetch(`/api/teachers/${id}`, { method: 'DELETE', credentials: 'include' });
    if (!res.ok) {
      alert(`Error deleting teacher (${res.status})`);
      return;
    }
    alert('Teacher deleted successfully!');
    await loadTeacherPortalData();
  } catch (err) {
    alert('Failed to delete teacher: ' + err.message);
  }
}

// =========================================================================
// 5. STUDENT PORTAL (CRUD on Students ONLY)
// =========================================================================

async function loadStudentView() {
  const prof = (currentUser && currentUser.profile) ? currentUser.profile : {};
  const sProfU = document.getElementById('sProfUsername');
  const sProfFN = document.getElementById('sProfFullName');
  const sProfE = document.getElementById('sProfEmail');
  const sProfP = document.getElementById('sProfProgram');
  const sProfB = document.getElementById('sProfBatch');

  if (sProfU) sProfU.textContent = prof.username || (currentUser ? currentUser.username : '-');
  if (sProfFN) sProfFN.textContent = prof.fullName || '-';
  if (sProfE) sProfE.textContent = prof.email || '-';
  if (sProfP) sProfP.textContent = prof.program || '-';
  if (sProfB) sProfB.textContent = prof.batch || '-';

  resetStudentPortalForm();
  await loadStudentPortalData();
}

async function loadStudentPortalData() {
  const tbody = document.getElementById('studentPortalTbody');
  if (!tbody) return;
  try {
    const res = await fetch('/api/students', { credentials: 'include' });
    if (!res.ok) {
      tbody.innerHTML = `<tr><td colspan="6">Access Denied (${res.status} Forbidden)</td></tr>`;
      return;
    }
    studentPortalList = await res.json();
    renderStudentPortalTable();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6">Error loading students: ${err.message}</td></tr>`;
  }
}

function renderStudentPortalTable() {
  const tbody = document.getElementById('studentPortalTbody');
  if (!tbody) return;
  if (studentPortalList.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6">No students found.</td></tr>';
    return;
  }
  tbody.innerHTML = studentPortalList.map(s => `
    <tr>
      <td>${s.id}</td>
      <td><strong>${escapeHtml(s.username)}</strong></td>
      <td>${escapeHtml(s.fullName)}</td>
      <td>${escapeHtml(s.email)}</td>
      <td>${escapeHtml(s.program || '')}</td>
      <td>${s.batch || ''}</td>
    </tr>
  `).join('');
}

function editStudentPortal(id) {
  const s = studentPortalList.find(x => x.id === id);
  if (!s) return;
  document.getElementById('studentPortalId').value = s.id;
  document.getElementById('studentPortalUsername').value = s.username;
  document.getElementById('studentPortalUsername').disabled = true;
  document.getElementById('studentPortalFullName').value = s.fullName || '';
  document.getElementById('studentPortalEmail').value = s.email || '';
  document.getElementById('studentPortalProgram').value = s.program || '';
  document.getElementById('studentPortalBatch').value = s.batch || '';
  document.getElementById('studentPortalPassword').value = '';
  document.getElementById('studentPortalPassword').required = false;
  document.getElementById('studentPortalPassReq').style.display = 'none';
  document.getElementById('studentPortalPassNote').style.display = 'inline';

  document.getElementById('studentPortalHeading').textContent = `Edit Student (ID #${s.id})`;
  document.getElementById('studentPortalSubmitBtn').textContent = 'Update Student';
  document.getElementById('studentPortalCancelBtn').style.display = 'inline';
}

function resetStudentPortalForm() {
  const idEl = document.getElementById('studentPortalId');
  if (!idEl) return;
  idEl.value = '';
  document.getElementById('studentPortalUsername').value = '';
  document.getElementById('studentPortalUsername').disabled = false;
  document.getElementById('studentPortalFullName').value = '';
  document.getElementById('studentPortalEmail').value = '';
  document.getElementById('studentPortalProgram').value = '';
  document.getElementById('studentPortalBatch').value = '';
  document.getElementById('studentPortalPassword').value = '';
  document.getElementById('studentPortalPassword').required = true;
  document.getElementById('studentPortalPassReq').style.display = 'inline';
  document.getElementById('studentPortalPassNote').style.display = 'none';

  document.getElementById('studentPortalHeading').textContent = 'Add New Student';
  document.getElementById('studentPortalSubmitBtn').textContent = 'Save Student';
  document.getElementById('studentPortalCancelBtn').style.display = 'none';
}

async function saveStudentPortal(e) {
  e.preventDefault();
  const id = document.getElementById('studentPortalId').value;
  const isEdit = Boolean(id);

  const payload = {
    fullName: document.getElementById('studentPortalFullName').value.trim(),
    email: document.getElementById('studentPortalEmail').value.trim(),
    program: document.getElementById('studentPortalProgram').value.trim(),
    batch: parseInt(document.getElementById('studentPortalBatch').value, 10)
  };

  const pass = document.getElementById('studentPortalPassword').value;
  if (pass) {
    payload.password = pass;
  }

  if (!isEdit) {
    payload.username = document.getElementById('studentPortalUsername').value.trim();
  }

  try {
    const url = isEdit ? `/api/students/${id}` : '/api/students';
    const method = isEdit ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      credentials: 'include'
    });

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      alert(`Error saving student: ${errData.error || ('Status ' + res.status)}`);
      return;
    }

    alert(isEdit ? 'Student updated successfully!' : 'Student created successfully!');
    resetStudentPortalForm();
    await loadStudentPortalData();
  } catch (err) {
    alert('Failed to save student: ' + err.message);
  }
}

async function deleteStudentPortal(id) {
  if (!confirm(`Are you sure you want to delete student ID #${id}?`)) return;
  try {
    const res = await fetch(`/api/students/${id}`, { method: 'DELETE', credentials: 'include' });
    if (!res.ok) {
      alert(`Error deleting student (${res.status})`);
      return;
    }
    alert('Student deleted successfully!');
    await loadStudentPortalData();
  } catch (err) {
    alert('Failed to delete student: ' + err.message);
  }
}

// =========================================================================
// Utility
// =========================================================================

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
