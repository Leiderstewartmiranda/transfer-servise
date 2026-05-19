'use strict';

const API_BASE = '/api/v1';

// Tasa de cambio aproximada COP → USD (se actualiza al cargar)
let COP_TO_USD = 1 / 4200;
let showInUSD   = false;

let allAccounts   = [];
let allRecipients = [];

// ─── Token Storage ────────────────────────────────────────────
const Tokens = {
  get access()  { return localStorage.getItem('ts_access');  },
  get refresh() { return localStorage.getItem('ts_refresh'); },
  get user()    { return localStorage.getItem('ts_user');    },
  save(access, refresh, username) {
    if (access)   localStorage.setItem('ts_access',  access);
    if (refresh)  localStorage.setItem('ts_refresh', refresh);
    if (username) localStorage.setItem('ts_user',    username);
  },
  clear() {
    ['ts_access', 'ts_refresh', 'ts_user'].forEach(k => localStorage.removeItem(k));
  },
};

// ─── HTTP Client ──────────────────────────────────────────────
async function request(method, path, body, retry = true) {
  const headers = { 'Content-Type': 'application/json' };
  if (Tokens.access) headers['Authorization'] = `Bearer ${Tokens.access}`;

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401 && retry) {
    const refreshed = await tryRefresh();
    if (refreshed) return request(method, path, body, false);
    logout();
    return null;
  }

  if (!res.ok) {
    let err;
    try { err = await res.json(); } catch { err = { message: 'Error inesperado' }; }
    throw err;
  }

  if (res.status === 204 || res.status === 201) return null;
  return res.json();
}

const get    = (path)       => request('GET',    path);
const post   = (path, body) => request('POST',   path, body);
const put    = (path, body) => request('PUT',    path, body);

async function tryRefresh() {
  const token = Tokens.refresh;
  if (!token) return false;
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify({ refreshToken: token }),
    });
    if (!res.ok) return false;
    const data = await res.json();
    Tokens.save(data.accessToken, null, null);
    return true;
  } catch {
    return false;
  }
}

// ─── Auth ─────────────────────────────────────────────────────
async function login(username, password) {
  const data = await post('/auth/login', { username, password });
  Tokens.save(data.accessToken, data.refreshToken, data.username);
  localStorage.setItem('ts_roles',     JSON.stringify(data.roles ?? []));
  localStorage.setItem('ts_accountId', data.accountId ?? '');
  return data;
}

async function register(ownerName, username, password) {
  await post('/auth/register', { ownerName, username, password });
}

function getMyAccountId() {
  return localStorage.getItem('ts_accountId') || null;
}

function isAdmin() {
  try {
    const roles = JSON.parse(localStorage.getItem('ts_roles') ?? '[]');
    return roles.includes('ROLE_ADMIN');
  } catch { return false; }
}

function logout() {
  Tokens.clear();
  localStorage.removeItem('ts_roles');
  localStorage.removeItem('ts_accountId');
  allAccounts   = [];
  allRecipients = [];
  showInUSD     = false;
  resetDashboard();
  showPage('login');
}

// ─── API Calls ────────────────────────────────────────────────
const fetchAllAccounts  = ()          => get('/accounts');
const fetchRecipients   = ()          => get('/accounts/recipients');
const fetchAccount      = id          => get(`/accounts/${id}`);
const fetchTransfers    = id          => get(`/transfers/account/${id}`);
const createTransfer    = body        => post('/transfers', body);
const createAccount     = body        => post('/accounts', body);
const updateAccountReq  = (id, body)  => put(`/accounts/${id}`, body);
const deleteAccountById = id          => request('DELETE', `/accounts/${id}`);

// ─── Helpers ──────────────────────────────────────────────────
function accountName(id) {
  const found = allAccounts.find(a => a.id === id) ?? allRecipients.find(r => r.id === id);
  return found ? found.ownerName : id;
}

function fmtMoney(amount, currency = 'COP') {
  if (showInUSD) {
    const usd = parseFloat(amount) * COP_TO_USD;
    return new Intl.NumberFormat('en-US', {
      style: 'currency', currency: 'USD',
      minimumFractionDigits: 2,
    }).format(usd);
  }
  return new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: currency === 'MXN' ? 'COP' : currency,
    minimumFractionDigits: 2,
  }).format(amount);
}

function fmtDate(iso) {
  if (!iso) return '—';
  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  }).format(new Date(iso));
}

const STATUS_LABEL = {
  ACTIVE: 'Activa', BLOCKED: 'Bloqueada', CLOSED: 'Cerrada',
  COMPLETED: 'Completada', FAILED: 'Fallida', PENDING: 'Pendiente',
};
const STATUS_CLASS = {
  ACTIVE: 'badge-active',       BLOCKED: 'badge-blocked',    CLOSED: 'badge-closed',
  COMPLETED: 'badge-completed', FAILED: 'badge-failed',      PENDING: 'badge-pending',
};

function escHtml(s) {
  return String(s)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;')
    .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

// ─── UI Helpers ───────────────────────────────────────────────
function showPage(name) {
  document.getElementById('page-login').classList.toggle('hidden', name !== 'login');
  document.getElementById('page-dashboard').classList.toggle('hidden', name !== 'dashboard');
}

function showAlert(id, message, type = 'error') {
  const el = document.getElementById(id);
  el.textContent = message;
  el.className = `alert alert-${type}`;
}

function hideAlert(id) {
  document.getElementById(id).className = 'alert hidden';
}

// ─── Populate Account Dropdowns ───────────────────────────────
function populateSourceSelect(accounts) {
  const opts = accounts.map(a =>
    `<option value="${escHtml(a.id)}">${escHtml(a.ownerName)}</option>`
  ).join('');
  document.getElementById('select-account').innerHTML =
    '<option value="">— Selecciona una cuenta —</option>' + opts;
}

function populateTargetSelect(recipients) {
  const opts = recipients.map(r =>
    `<option value="${escHtml(r.id)}">${escHtml(r.ownerName)}</option>`
  ).join('');
  document.getElementById('select-target').innerHTML =
    '<option value="">— Selecciona destino —</option>' + opts;
}

// ─── Load Accounts from API ───────────────────────────────────
async function loadAllAccounts() {
  try {
    const [accounts, recipients] = await Promise.all([
      fetchAllAccounts(),
      fetchRecipients(),
    ]);
    if (!accounts) return;
    allAccounts   = accounts;
    allRecipients = recipients ?? [];
    populateSourceSelect(accounts);
    populateTargetSelect(allRecipients);
    if (isAdmin()) renderAdminAccounts(accounts);
  } catch (err) {
    console.error('Error cargando cuentas:', err);
  }
}

// ─── Render: Account Card ─────────────────────────────────────
let currentAccountData = null;

function renderAccount(account) {
  currentAccountData = account;
  document.getElementById('info-owner').textContent    = account.ownerName;
  document.getElementById('info-id').textContent       = account.id;
  document.getElementById('info-balance').textContent  = fmtMoney(account.balance, account.currency);
  document.getElementById('info-currency').textContent = showInUSD
    ? `(${account.currency} → USD, aprox. $1 USD = ${(1 / COP_TO_USD).toFixed(0)} COP)`
    : account.currency;
  document.getElementById('btn-currency-toggle').textContent = showInUSD ? 'Ver en COP' : 'Ver en USD';

  const badge = document.getElementById('info-status');
  badge.textContent = STATUS_LABEL[account.status] ?? account.status;
  badge.className   = `badge ${STATUS_CLASS[account.status] ?? ''}`;

  document.getElementById('section-account').classList.remove('hidden');
  document.getElementById('section-actions').classList.remove('hidden');
}

// ─── Render: Transfer History ─────────────────────────────────
function renderHistory(transfers, currentId) {
  const list  = document.getElementById('history-list');
  const empty = document.getElementById('history-empty');

  if (!transfers || transfers.length === 0) {
    list.innerHTML = '';
    empty.classList.remove('hidden');
    return;
  }

  empty.classList.add('hidden');

  const sorted = [...transfers].sort(
    (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
  );

  list.innerHTML = sorted.map(t => {
    const isOut  = t.sourceAccountId === currentId;
    const dir    = isOut ? 'out' : 'in';
    const arrow  = isOut ? '↑'  : '↓';
    const sign   = isOut ? '−'  : '+';
    const peerId = isOut ? t.targetAccountId : t.sourceAccountId;
    const peer   = escHtml(accountName(peerId));
    const desc   = t.description
      ? `<div class="transfer-desc">${escHtml(t.description)}</div>`
      : '';

    return `
      <div class="transfer-item">
        <div class="transfer-direction ${dir}">${arrow}</div>
        <div class="transfer-info">
          <div class="transfer-peer">${peer}</div>
          ${desc}
          <div class="transfer-meta">
            <span class="transfer-date">${fmtDate(t.createdAt)}</span>
          </div>
        </div>
        <div class="transfer-right">
          <div class="transfer-amount ${dir}">${sign} ${fmtMoney(t.amount, t.currency)}</div>
          <div class="transfer-status">
            <span class="badge ${STATUS_CLASS[t.status] ?? ''}">${STATUS_LABEL[t.status] ?? t.status}</span>
          </div>
        </div>
      </div>`;
  }).join('');
}

// ─── Load Account ─────────────────────────────────────────────
let currentAccountId = null;

async function loadAccount(accountId) {
  currentAccountId = accountId;

  document.getElementById('section-account').classList.add('hidden');
  document.getElementById('section-actions').classList.add('hidden');
  document.getElementById('history-list').innerHTML = '';
  document.getElementById('history-empty').classList.add('hidden');
  document.getElementById('history-loading').classList.remove('hidden');

  try {
    const [account, transfers] = await Promise.all([
      fetchAccount(accountId),
      fetchTransfers(accountId),
    ]);
    if (!account) return;
    renderAccount(account);
    renderHistory(transfers ?? [], accountId);
    filterTargetOptions(accountId);
  } catch (err) {
    console.error('Error cargando cuenta:', err);
  } finally {
    document.getElementById('history-loading').classList.add('hidden');
  }
}

function filterTargetOptions(sourceId) {
  const previousTarget = document.getElementById('select-target').value;
  const filtered = allRecipients.filter(r => r.id !== sourceId);
  populateTargetSelect(filtered);
  if (previousTarget && previousTarget !== sourceId) {
    document.getElementById('select-target').value = previousTarget;
  }
}

// ─── Currency Toggle ──────────────────────────────────────────
function toggleCurrency() {
  showInUSD = !showInUSD;
  if (currentAccountData) renderAccount(currentAccountData);
  // Re-renderizar historial con nuevo formato
  if (currentAccountId) {
    fetchTransfers(currentAccountId).then(transfers => {
      renderHistory(transfers ?? [], currentAccountId);
    });
  }
}

// ─── Transfer Form ────────────────────────────────────────────
const ERROR_MESSAGES = {
  INSUFFICIENT_FUNDS: 'Saldo insuficiente para realizar la transferencia.',
  ACCOUNT_NOT_FOUND:  'Cuenta no encontrada.',
  TRANSFER_NOT_FOUND: 'Transferencia no encontrada.',
  VALIDATION_ERROR:   'Revisa los datos ingresados.',
};

function resolveError(err) {
  if (!err) return 'Error desconocido.';
  return ERROR_MESSAGES[err.error] ?? err.message ?? 'Error al procesar la solicitud.';
}

async function handleTransfer(e) {
  e.preventDefault();
  hideAlert('transfer-alert');

  const targetId = document.getElementById('select-target').value;
  const amount   = parseFloat(document.getElementById('input-amount').value);
  const desc     = document.getElementById('input-desc').value.trim();

  if (!targetId) {
    showAlert('transfer-alert', 'Selecciona una cuenta destino.');
    return;
  }
  if (!currentAccountId) {
    showAlert('transfer-alert', 'Selecciona una cuenta de origen.');
    return;
  }

  const btn = document.getElementById('btn-transfer');
  btn.disabled    = true;
  btn.textContent = 'Procesando...';

  try {
    await createTransfer({
      sourceAccountId: currentAccountId,
      targetAccountId: targetId,
      amount,
      currency:    'COP',
      description: desc || undefined,
    });

    showAlert('transfer-alert', '¡Transferencia realizada con éxito!', 'success');
    document.getElementById('form-transfer').reset();
    await loadAllAccounts();
    await loadAccount(currentAccountId);
  } catch (err) {
    showAlert('transfer-alert', resolveError(err));
  } finally {
    btn.disabled    = false;
    btn.textContent = 'Transferir';
  }
}

// ─── Admin Panel ──────────────────────────────────────────────
function renderAdminAccounts(accounts) {
  const list = document.getElementById('admin-accounts-list');
  if (!accounts.length) {
    list.innerHTML = '<p class="text-muted small">No hay cuentas registradas.</p>';
    return;
  }
  list.innerHTML = accounts.map(a => `
    <div class="admin-account-row" id="row-${escHtml(a.id)}">
      <div class="admin-account-info">
        <div class="admin-account-name">${escHtml(a.ownerName)}</div>
        <div class="admin-account-balance">${fmtMoney(a.balance, a.currency)} ·
          <span class="badge ${STATUS_CLASS[a.status] ?? ''}">${STATUS_LABEL[a.status] ?? a.status}</span>
        </div>
      </div>
      <div class="admin-row-btns">
        <button class="btn-edit"   onclick="toggleEditForm('${escHtml(a.id)}', '${escHtml(a.ownerName)}', ${a.balance})">Editar</button>
        <button class="btn-danger" onclick="handleDeleteAccount('${escHtml(a.id)}', '${escHtml(a.ownerName)}')">Eliminar</button>
      </div>
    </div>
    <div class="admin-edit-form hidden" id="edit-${escHtml(a.id)}">
      <div class="edit-fields">
        <input class="edit-input" id="edit-name-${escHtml(a.id)}" type="text" value="${escHtml(a.ownerName)}" placeholder="Nombre del titular" maxlength="100">
        <input class="edit-input" id="edit-balance-${escHtml(a.id)}" type="number" step="0.01" min="0" value="${a.balance}" placeholder="Saldo">
        <button class="btn-save" onclick="handleUpdateAccount('${escHtml(a.id)}')">Guardar</button>
        <button class="btn-cancel" onclick="toggleEditForm('${escHtml(a.id)}')">Cancelar</button>
      </div>
    </div>`
  ).join('');
}

function toggleEditForm(id, name, balance) {
  const editDiv = document.getElementById(`edit-${id}`);
  const isHidden = editDiv.classList.toggle('hidden');
  if (!isHidden && name !== undefined) {
    document.getElementById(`edit-name-${id}`).value    = name;
    document.getElementById(`edit-balance-${id}`).value = balance;
  }
}

async function handleUpdateAccount(id) {
  const ownerName = document.getElementById(`edit-name-${id}`).value.trim();
  const balance   = parseFloat(document.getElementById(`edit-balance-${id}`).value);

  if (!ownerName) { alert('El nombre no puede estar vacío.'); return; }
  if (isNaN(balance) || balance < 0) { alert('Saldo inválido.'); return; }

  try {
    await updateAccountReq(id, { ownerName, balance });
    document.getElementById(`edit-${id}`).classList.add('hidden');
    await loadAllAccounts();
    if (currentAccountId === id) await loadAccount(id);
  } catch (err) {
    alert(resolveError(err));
  }
}

async function handleDeleteAccount(id, name) {
  if (!confirm(`¿Eliminar la cuenta de "${name}"? Esta acción no se puede deshacer.`)) return;
  try {
    await deleteAccountById(id);
    if (currentAccountId === id) {
      currentAccountId = null;
      currentAccountData = null;
      document.getElementById('section-account').classList.add('hidden');
      document.getElementById('section-actions').classList.add('hidden');
    }
    await loadAllAccounts();
  } catch (err) {
    alert(resolveError(err));
  }
}

async function handleCreateAccount(e) {
  e.preventDefault();
  hideAlert('create-alert');

  const ownerName = document.getElementById('new-owner').value.trim();
  const username  = document.getElementById('new-username').value.trim();
  const password  = document.getElementById('new-password').value;
  const balance   = parseFloat(document.getElementById('new-balance').value) || 0;
  const btn       = document.getElementById('btn-create');

  btn.disabled = true;
  btn.textContent = 'Creando...';

  try {
    await createAccount({ ownerName, username, password, currency: 'COP', initialBalance: balance });
    showAlert('create-alert', `Cuenta de "${ownerName}" creada. Usuario: ${username}`, 'success');
    document.getElementById('form-create-account').reset();
    await loadAllAccounts();
  } catch (err) {
    showAlert('create-alert', resolveError(err));
  } finally {
    btn.disabled = false;
    btn.textContent = 'Crear cuenta';
  }
}

function setupAdminUI() {
  if (!isAdmin()) return;
  document.getElementById('nav-role-badge').classList.remove('hidden');
  document.getElementById('btn-admin-toggle').classList.remove('hidden');

  document.getElementById('btn-admin-toggle').addEventListener('click', () => {
    const panel    = document.getElementById('section-admin');
    const isHidden = panel.classList.toggle('hidden');
    document.getElementById('btn-admin-toggle').textContent =
      isHidden ? 'Gestión de cuentas' : 'Ocultar gestión';
  });

  document.getElementById('form-create-account').addEventListener('submit', handleCreateAccount);
}

// ─── Register ─────────────────────────────────────────────────
async function handleRegister(e) {
  e.preventDefault();
  hideAlert('register-alert');

  const ownerName = document.getElementById('reg-owner').value.trim();
  const username  = document.getElementById('reg-username').value.trim();
  const password  = document.getElementById('reg-password').value;
  const btn       = document.getElementById('btn-register');

  btn.disabled = true;
  btn.textContent = 'Creando cuenta...';

  try {
    await register(ownerName, username, password);
    showAlert('register-alert', '¡Cuenta creada! Ya puedes iniciar sesión.', 'success');
    document.getElementById('form-register').reset();
    setTimeout(() => switchLoginView('login'), 1800);
  } catch (err) {
    showAlert('register-alert', resolveError(err));
  } finally {
    btn.disabled = false;
    btn.textContent = 'Crear cuenta';
  }
}

function switchLoginView(view) {
  document.getElementById('view-login').classList.toggle('hidden',    view !== 'login');
  document.getElementById('view-register').classList.toggle('hidden', view !== 'register');
  hideAlert('login-alert');
  hideAlert('register-alert');
}

// ─── Reset Dashboard ──────────────────────────────────────────
function resetDashboard() {
  currentAccountId   = null;
  currentAccountData = null;
  document.getElementById('select-account').innerHTML = '<option value="">— Selecciona una cuenta —</option>';
  document.getElementById('select-target').innerHTML  = '<option value="">— Selecciona destino —</option>';
  document.getElementById('section-account').classList.add('hidden');
  document.getElementById('section-actions').classList.add('hidden');
  document.getElementById('history-list').innerHTML = '';
  document.getElementById('nav-username').textContent = '';
  hideAlert('transfer-alert');
}

// ─── Init ─────────────────────────────────────────────────────
function init() {

  // Toggle login ↔ register
  document.getElementById('btn-show-register').addEventListener('click', () => switchLoginView('register'));
  document.getElementById('btn-show-login').addEventListener('click',    () => switchLoginView('login'));

  // Login form
  document.getElementById('form-login').addEventListener('submit', async e => {
    e.preventDefault();
    hideAlert('login-alert');

    const username = document.getElementById('input-username').value.trim();
    const password = document.getElementById('input-password').value;
    const btn      = document.getElementById('btn-login');

    btn.disabled    = true;
    btn.textContent = 'Ingresando...';

    try {
      const data = await login(username, password);
      document.getElementById('nav-username').textContent = Tokens.user ?? username;
      showPage('dashboard');
      setupAdminUI();
      await loadAllAccounts();
      if (!isAdmin() && data.accountId) {
        document.getElementById('section-selector').classList.add('hidden');
        await loadAccount(data.accountId);
      }
    } catch {
      showAlert('login-alert', 'Usuario o contraseña incorrectos.');
    } finally {
      btn.disabled    = false;
      btn.textContent = 'Iniciar sesión';
    }
  });

  // Register form
  document.getElementById('form-register').addEventListener('submit', handleRegister);

  // Logout
  document.getElementById('btn-logout').addEventListener('click', logout);

  // Account selector
  document.getElementById('select-account').addEventListener('change', e => {
    const id = e.target.value;
    if (id) {
      loadAccount(id);
    } else {
      currentAccountId   = null;
      currentAccountData = null;
      document.getElementById('section-account').classList.add('hidden');
      document.getElementById('section-actions').classList.add('hidden');
    }
  });

  // Transfer form
  document.getElementById('form-transfer').addEventListener('submit', handleTransfer);

  // Currency toggle
  document.getElementById('btn-currency-toggle').addEventListener('click', toggleCurrency);

  // Route on load
  if (Tokens.access) {
    document.getElementById('nav-username').textContent = Tokens.user ?? '';
    showPage('dashboard');
    setupAdminUI();
    const accountId = getMyAccountId();
    if (!isAdmin() && accountId) {
      document.getElementById('section-selector').classList.add('hidden');
      loadAllAccounts().then(() => loadAccount(accountId));
    } else {
      loadAllAccounts();
    }
  } else {
    showPage('login');
  }
}

document.addEventListener('DOMContentLoaded', init);
