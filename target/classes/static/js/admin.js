// API URL Base (since we serve the frontend from Spring Boot, this is relative)
const API_BASE = '/api';

// Cache arrays
let clients = [];
let vehicles = [];
let reservations = [];
let payments = [];

// Cache Maps for quick lookups
let clientsMap = {};
let vehiclesMap = {};

// Current editing IDs
let activeVehicleId = null;
let activeClientId = null;
let activeReservationId = null;

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    setupTheme();
    setupNavigation();
    setupModalEvents();
    setupFormSubmissions();
    setupFilters();
});

// Auth Checks
function checkAuth() {
    const token = localStorage.getItem('token');
    if (!token) {
        showLoginOverlay();
    } else {
        // Validate token with backend
        fetch(`${API_BASE}/auth/validate`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
        .then(res => {
            if (res.ok) {
                initApp();
            } else {
                localStorage.removeItem('token');
                showLoginOverlay();
            }
        })
        .catch(() => {
            // Server might be down or unreachable, but try to init anyway if offline
            initApp();
        });
    }
}

// Injects a premium dark login card if unauthenticated
function showLoginOverlay() {
    if (document.getElementById('login-overlay')) return;

    const loginHTML = `
        <div id="login-overlay" style="position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(15, 23, 42, 0.95); backdrop-filter:blur(10px); display:flex; align-items:center; justify-content:center; z-index:9999;">
            <div class="card" style="width:90%; max-width:400px; padding:32px; border-radius:16px; border:1px solid rgba(255,255,255,0.08); background:#1e293b; box-shadow:0 25px 50px -12px rgba(0,0,0,0.5); color:#f8fafc; font-family:'DM Sans', sans-serif;">
                <div style="text-align:center; margin-bottom:28px;">
                    <i class="fa-solid fa-car-side" style="font-size:3rem; color:#6366f1; margin-bottom:12px;"></i>
                    <h2 style="font-family:'Space Grotesk', sans-serif; font-size:1.75rem; font-weight:700; margin-bottom:4px;">Renta Móvil</h2>
                    <p style="color:#94a3b8; font-size:0.875rem;">Sistema de Gestión de Alquiler de Vehículos</p>
                </div>
                <form id="login-form">
                    <div style="margin-bottom:18px;">
                        <label style="display:block; font-size:0.85rem; font-weight:600; color:#94a3b8; margin-bottom:8px;">Usuario</label>
                        <input type="text" id="login-username" class="form-control" style="background:#0f172a !important; color:#ffffff !important; border:1px solid #334155 !important;" required placeholder="admin">
                    </div>
                    <div style="margin-bottom:24px;">
                        <label style="display:block; font-size:0.85rem; font-weight:600; color:#94a3b8; margin-bottom:8px;">Contraseña</label>
                        <input type="password" id="login-password" class="form-control" style="background:#0f172a !important; color:#ffffff !important; border:1px solid #334155 !important;" required placeholder="••••••••">
                    </div>
                    <button type="submit" class="btn btn-primary w-100" style="background:#4f46e5 !important; border-color:#4f46e5 !important; height:44px; font-weight:600; border-radius:10px;">Iniciar Sesión</button>
                    <a href="index.html" class="btn-back" style="display:block; text-align:center; margin-top:16px; color:#94a3b8; font-size:0.875rem; text-decoration:none; transition:color 0.2s;" onmouseover="this.style.color='#f8fafc'" onmouseout="this.style.color='#94a3b8'"><i class="fa-solid fa-arrow-left"></i> Volver a la Página Principal</a>
                    <div id="login-error" style="color:#ef4444; font-size:0.85rem; text-align:center; margin-top:12px; display:none; font-weight:500;"></div>
                </form>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', loginHTML);

    document.getElementById('login-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('login-username').value;
        const password = document.getElementById('login-password').value;
        const errorEl = document.getElementById('login-error');

        try {
            errorEl.style.display = 'none';
            const response = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            const data = await response.json();
            if (response.ok && data.token) {
                localStorage.setItem('token', data.token);
                document.getElementById('login-overlay').remove();
                showToast('¡Sesión iniciada con éxito!', 'success');
                initApp();
            } else {
                errorEl.textContent = data.mensaje || 'Credenciales inválidas';
                errorEl.style.display = 'block';
            }
        } catch (err) {
            errorEl.textContent = 'Error de conexión con el backend';
            errorEl.style.display = 'block';
        }
    });
}

// Fetch helper wrapper with token auth
async function fetchAPI(endpoint, options = {}) {
    const token = localStorage.getItem('token');
    
    // Set headers
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const config = {
        ...options,
        headers
    };

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, config);
        
        if (response.status === 401) {
            localStorage.removeItem('token');
            showLoginOverlay();
            throw new Error('No autorizado. Sesión expirada.');
        }

        if (response.status === 244 || response.status === 204) {
            return null; // No content
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        }
        return response;
    } catch (err) {
        console.error(`Error en fetchAPI para ${endpoint}:`, err);
        throw err;
    }
}

// Toast notification system
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast-custom ${type}`;

    let iconClass = 'fa-circle-info';
    if (type === 'success') iconClass = 'fa-circle-check';
    if (type === 'error') iconClass = 'fa-circle-xmark';
    if (type === 'warning') iconClass = 'fa-triangle-exclamation';

    toast.innerHTML = `
        <i class="fa-solid ${iconClass}"></i>
        <div class="toast-custom-content">${message}</div>
    `;

    container.appendChild(toast);

    // Auto remove after animation completes (3s total duration)
    setTimeout(() => {
        toast.remove();
    }, 3000);
}

// Setup Light/Dark Mode Switcher
function setupTheme() {
    const btnTheme = document.getElementById('btn-theme');
    const body = document.body;

    // Load saved theme
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark') {
        body.classList.add('dark-theme');
        btnTheme.innerHTML = '<i class="fa-regular fa-sun"></i>';
    }

    btnTheme.addEventListener('click', () => {
        body.classList.toggle('dark-theme');
        const isDark = body.classList.contains('dark-theme');
        localStorage.setItem('theme', isDark ? 'dark' : 'light');
        btnTheme.innerHTML = isDark ? '<i class="fa-regular fa-sun"></i>' : '<i class="fa-regular fa-moon"></i>';
    });
}

// Setup Sidebar routing and page toggling
function setupNavigation() {
    const navLinks = document.querySelectorAll('.nav-link');
    const pages = document.querySelectorAll('.page');
    const btnMenu = document.getElementById('btn-menu');
    const sidebar = document.getElementById('sidebar');
    const btnLogout = document.getElementById('btn-logout');

    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetPage = link.getAttribute('data-page');

            navLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');

            pages.forEach(page => {
                page.classList.remove('active');
                if (page.id === `page-${targetPage}`) {
                    page.classList.add('active');
                }
            });

            // If mobile, close sidebar on nav click
            if (window.innerWidth <= 992) {
                sidebar.classList.remove('active');
            }

            // Sync loading of corresponding page data
            loadPageData(targetPage);
        });
    });

    // Mobile Sidebar Toggle
    btnMenu.addEventListener('click', () => {
        sidebar.classList.toggle('active');
    });

    // Close sidebar clicking outside on mobile
    document.addEventListener('click', (e) => {
        if (window.innerWidth <= 992 && !sidebar.contains(e.target) && e.target !== btnMenu && !btnMenu.contains(e.target)) {
            sidebar.classList.remove('active');
        }
    });

    // Logout Button
    btnLogout.addEventListener('click', () => {
        localStorage.removeItem('token');
        showToast('Sesión cerrada correctamente', 'info');
        showLoginOverlay();
    });
}

// Setup Modal Open/Close handlers
function setupModalEvents() {
    const overlay = document.getElementById('modal-overlay');
    
    // Close modal logic
    const closeButtons = document.querySelectorAll('[data-close]');
    closeButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const modalId = btn.getAttribute('data-close');
            closeModal(modalId);
        });
    });

    // Quick Action button handlers to open modals
    const actionCards = document.querySelectorAll('.action-card');
    actionCards.forEach(card => {
        card.addEventListener('click', () => {
            const action = card.getAttribute('data-action');
            if (action === 'nueva-reserva') openModal('modal-reserva');
            if (action === 'nuevo-cliente') openModal('modal-cliente');
            if (action === 'nuevo-vehiculo') openModal('modal-vehiculo');
            if (action === 'nuevo-pago') openModal('modal-pago');
        });
    });

    // Page header primary button handlers to open modals
    document.getElementById('btn-nueva-reserva')?.addEventListener('click', () => openModal('modal-reserva'));
    document.getElementById('btn-nuevo-cliente')?.addEventListener('click', () => openModal('modal-cliente'));
    document.getElementById('btn-nuevo-vehiculo')?.addEventListener('click', () => openModal('modal-vehiculo'));
    document.getElementById('btn-nuevo-pago')?.addEventListener('click', () => openModal('modal-pago'));
}

function openModal(modalId) {
    const overlay = document.getElementById('modal-overlay');
    const modal = document.getElementById(modalId);
    
    if (overlay && modal) {
        overlay.classList.add('active');
        modal.style.display = 'flex';
        setTimeout(() => {
            modal.classList.add('active');
        }, 10);
        
        // Custom modal initialization (like loading select dropdowns)
        if (modalId === 'modal-reserva') {
            populateReservationSelects('reserva-cliente', 'reserva-vehiculo');
        }
        if (modalId === 'modal-pago') {
            populatePaymentSelects();
        }
    }
}

function closeModal(modalId) {
    const overlay = document.getElementById('modal-overlay');
    const modal = document.getElementById(modalId);
    
    if (overlay && modal) {
        modal.classList.remove('active');
        setTimeout(() => {
            modal.style.display = 'none';
            // If no other modals are active, close overlay
            const activeModals = document.querySelectorAll('.modal.active');
            if (activeModals.length === 0) {
                overlay.classList.remove('active');
            }
        }, 300);
    }
}

// Setup table filter dropdown inputs
function setupFilters() {
    document.getElementById('filter-reserva-estado')?.addEventListener('change', renderReservationsTable);
    document.getElementById('filter-cliente-estado')?.addEventListener('change', renderClientsTable);
    
    const filterVehiculoEstado = document.getElementById('filter-vehiculo-estado');
    const filterVehiculoTipo = document.getElementById('filter-vehiculo-tipo');
    
    filterVehiculoEstado?.addEventListener('change', renderVehiclesTable);
    filterVehiculoTipo?.addEventListener('change', renderVehiclesTable);
    
    document.getElementById('filter-pago-estado')?.addEventListener('change', renderPaymentsTable);
}

// Load Data based on the active tab page
function loadPageData(pageName) {
    if (pageName === 'dashboard') {
        loadDashboardStats();
    } else if (pageName === 'reservas') {
        loadReservationsData();
    } else if (pageName === 'clientes') {
        loadClientsData();
    } else if (pageName === 'vehiculos') {
        loadVehiclesData();
    } else if (pageName === 'pagos') {
        loadPaymentsData();
    } else if (pageName === 'reportes') {
        loadReportesData();
    }
}

// Initialize Application Data
async function initApp() {
    try {
        // Load basic lists for caches
        await Promise.all([
            loadClientsData(false),
            loadVehiclesData(false)
        ]);
        
        // Load default dashboard
        loadDashboardStats();
    } catch (e) {
        showToast('Error al inicializar los datos del sistema', 'error');
    }
}

// ==========================================
// 1. DASHBOARD & REPORTES SERVICE
// ==========================================

async function loadDashboardStats() {
    try {
        const stats = await fetchAPI('/dashboard');
        if (!stats) return;

        // Render dashboard view cards
        document.getElementById('stat-reservas-activas').textContent = stats.reservasActivas || 0;
        document.getElementById('stat-ingresos').textContent = `$${(stats.ingresosMes || 0).toLocaleString()}`;
        document.getElementById('stat-vehiculos-rentados').textContent = stats.vehiculosRentados || 0;
        document.getElementById('stat-clientes-activos').textContent = stats.clientesActivos || 0;

        // Render sidebar badges
        document.getElementById('badge-reservas').textContent = stats.reservasActivas || 0;
        document.getElementById('badge-clientes').textContent = stats.totalClientes || 0;
        document.getElementById('badge-vehiculos').textContent = stats.totalVehiculos - stats.vehiculosRentados || 0;

        // Populate recent activity list and vehicle status
        renderRecentActivityFeed();
        renderVehicleStatusList();
    } catch (e) {
        showToast('Error al cargar estadísticas del dashboard', 'error');
    }
}

function renderRecentActivityFeed() {
    const feed = document.getElementById('activity-feed');
    if (!feed) return;

    // Show dynamic dummy logs based on cache counts
    feed.innerHTML = `
        <div class="activity-item">
            <div class="activity-icon success">
                <i class="fa-solid fa-car"></i>
            </div>
            <div class="activity-content">
                <p>${vehicles.length} vehículos disponibles en catálogo</p>
                <span class="activity-time">Catálogo Actualizado</span>
            </div>
        </div>
        <div class="activity-item">
            <div class="activity-icon info">
                <i class="fa-solid fa-users"></i>
            </div>
            <div class="activity-content">
                <p>${clients.length} clientes registrados en la plataforma</p>
                <span class="activity-time">Base de datos al día</span>
            </div>
        </div>
    `;
}

function renderVehicleStatusList() {
    const listEl = document.getElementById('vehicle-status-list');
    if (!listEl) return;

    if (vehicles.length === 0) {
        listEl.innerHTML = '<div style="color:var(--text-secondary); text-align:center; font-size:0.85rem; padding:12px;">No hay vehículos registrados</div>';
        return;
    }

    // List top 5 vehicles with their status
    listEl.innerHTML = vehicles.slice(0, 5).map(v => `
        <div class="vehicle-status-item">
            <div class="vehicle-info">
                <i class="fa-solid ${v.tipo === 'moto' ? 'fa-motorcycle' : 'fa-car'}"></i>
                <div>
                    <span class="vehicle-name">${v.marca} ${v.modelo}</span>
                    <span class="vehicle-plate">${v.placa}</span>
                </div>
            </div>
            <div class="vehicle-status-dot ${v.estado === 'disponible' ? 'available' : v.estado === 'rentado' ? 'rented' : 'maintenance'}"></div>
        </div>
    `).join('');
}

async function loadReportesData() {
    try {
        const stats = await fetchAPI('/dashboard');
        if (!stats) return;

        document.getElementById('reporte-total-reservas').textContent = reservations.length || stats.reservasActivas || 0;
        document.getElementById('reporte-total-ingresos').textContent = `$${(stats.ingresosMes || 0).toLocaleString()}`;
        document.getElementById('reporte-total-vehiculos').textContent = stats.totalVehiculos || vehicles.length || 0;
        document.getElementById('reporte-total-clientes').textContent = stats.totalClientes || clients.length || 0;
    } catch (e) {
        showToast('Error al cargar reporte general', 'error');
    }
}

// ==========================================
// 2. CLIENTES SERVICE
// ==========================================

async function loadClientsData(shouldRender = true) {
    try {
        const data = await fetchAPI('/clientes');
        clients = data || [];
        
        // Cache map
        clientsMap = {};
        clients.forEach(c => {
            clientsMap[c.id] = c;
        });

        if (shouldRender) {
            renderClientsTable();
        }
    } catch (e) {
        showToast('Error al cargar listado de clientes', 'error');
    }
}

function renderClientsTable() {
    const tbody = document.getElementById('clientes-tbody');
    if (!tbody) return;

    const filterEstado = document.getElementById('filter-cliente-estado').value;

    const filtered = clients.filter(c => {
        if (filterEstado && c.estado !== filterEstado) return false;
        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:var(--text-secondary);">No se encontraron clientes</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(c => `
        <tr>
            <td>#${c.id}</td>
            <td><strong>${c.nombres} ${c.apellidos}</strong></td>
            <td>${c.tipoDocumento} ${c.numeroDocumento}</td>
            <td>${c.telefono || '-'}</td>
            <td>${c.email}</td>
            <td><span class="status-badge ${c.estado}">${c.estado}</span></td>
            <td>
                <button class="btn-table-action" onclick="viewClientDetail(${c.id})"><i class="fa-solid fa-eye"></i></button>
            </td>
        </tr>
    `).join('');
}

// ==========================================
// 3. VEHICULOS SERVICE
// ==========================================

async function loadVehiclesData(shouldRender = true) {
    try {
        const data = await fetchAPI('/vehiculos');
        vehicles = data || [];

        // Cache map
        vehiclesMap = {};
        vehicles.forEach(v => {
            vehiclesMap[v.id] = v;
        });

        if (shouldRender) {
            renderVehiclesTable();
        }
    } catch (e) {
        showToast('Error al cargar catálogo de vehículos', 'error');
    }
}

function renderVehiclesTable() {
    const tbody = document.getElementById('vehiculos-tbody');
    if (!tbody) return;

    const filterEstado = document.getElementById('filter-vehiculo-estado').value;
    const filterTipo = document.getElementById('filter-vehiculo-tipo').value;

    const filtered = vehicles.filter(v => {
        if (filterEstado && v.estado !== filterEstado) return false;
        if (filterTipo && v.tipo !== filterTipo) return false;
        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; color:var(--text-secondary);">No se encontraron vehículos</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(v => `
        <tr>
            <td>#${v.id}</td>
            <td><i class="fa-solid ${v.tipo === 'moto' ? 'fa-motorcycle' : 'fa-car'}" style="font-size:1.15rem; color:var(--text-secondary);"></i></td>
            <td><strong>${v.marca}</strong></td>
            <td>${v.modelo}</td>
            <td>${v.anio}</td>
            <td><span style="font-family:monospace; background:rgba(0,0,0,0.05); padding:2px 6px; border-radius:4px; border:1px solid rgba(0,0,0,0.1); font-weight:600;">${v.placa}</span></td>
            <td>$${(v.precioDia || 0).toLocaleString()}</td>
            <td><span class="status-badge ${v.estado}">${v.estado}</span></td>
            <td>
                <button class="btn-table-action" onclick="viewVehicleDetail(${v.id})"><i class="fa-solid fa-eye"></i></button>
            </td>
        </tr>
    `).join('');
}

// ==========================================
// 4. RESERVAS SERVICE
// ==========================================

async function loadReservationsData() {
    try {
        // Need to load clients/vehicles lists first to make sure caches are filled
        await Promise.all([
            loadClientsData(false),
            loadVehiclesData(false)
        ]);

        const data = await fetchAPI('/reservas');
        reservations = data || [];
        renderReservationsTable();
    } catch (e) {
        showToast('Error al cargar listado de reservas', 'error');
    }
}

function renderReservationsTable() {
    const tbody = document.getElementById('reservas-tbody');
    if (!tbody) return;

    const filterEstado = document.getElementById('filter-reserva-estado').value;

    const filtered = reservations.filter(r => {
        if (filterEstado && r.estado !== filterEstado) return false;
        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:var(--text-secondary);">No se encontraron reservas</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(r => `
        <tr>
            <td>#${r.id}</td>
            <td><strong>${r.cliente ? (r.cliente.nombres + ' ' + r.cliente.apellidos) : 'Cliente Desconocido'}</strong></td>
            <td>${r.vehiculo ? (r.vehiculo.marca + ' ' + r.vehiculo.modelo) : 'Vehículo Desconocido'}</td>
            <td>${r.fechaInicio}</td>
            <td>${r.fechaFin}</td>
            <td>$${(r.total || 0).toLocaleString()}</td>
            <td><span class="status-badge ${r.estado}">${r.estado}</span></td>
            <td>
                <button class="btn-table-action" onclick="viewReservationDetail(${r.id})"><i class="fa-solid fa-eye"></i></button>
            </td>
        </tr>
    `).join('');
}

// ==========================================
// 5. PAGOS SERVICE
// ==========================================

async function loadPaymentsData() {
    try {
        const data = await fetchAPI('/pagos');
        payments = data || [];
        renderPaymentsTable();
    } catch (e) {
        showToast('Error al cargar listado de pagos', 'error');
    }
}

function renderPaymentsTable() {
    const tbody = document.getElementById('pagos-tbody');
    if (!tbody) return;

    const filterEstado = document.getElementById('filter-pago-estado').value;

    const filtered = payments.filter(p => {
        if (filterEstado && p.estado !== filterEstado) return false;
        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:var(--text-secondary);">No se encontraron pagos registrados</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(p => {
        const clientObj = clientsMap[p.clienteId];
        const clientName = clientObj ? `${clientObj.nombres} ${clientObj.apellidos}` : `Cliente #${p.clienteId}`;
        return `
            <tr>
                <td>#${p.id}</td>
                <td><strong>${clientName}</strong></td>
                <td>Reserva #${p.reservaId}</td>
                <td><span style="text-transform:capitalize;">${p.metodo}</span></td>
                <td><strong>$${(p.monto || 0).toLocaleString()}</strong></td>
                <td>${p.fecha}</td>
                <td><span class="status-badge ${p.estado}">${p.estado}</span></td>
                <td>
                    <button class="btn-table-action" style="color:var(--danger);" onclick="deletePayment(${p.id})"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `;
    }).join('');
}

// ==========================================
// FORM SUBMISSIONS & DETAIL MODALS
// ==========================================

function setupFormSubmissions() {
    // 1. Save new Client
    document.getElementById('btn-guardar-cliente')?.addEventListener('click', async (e) => {
        e.preventDefault();
        const nombres = document.getElementById('cliente-nombres').value;
        const apellidos = document.getElementById('cliente-apellidos').value;
        const tipoDocumento = document.getElementById('cliente-tipo-documento').value;
        const numeroDocumento = document.getElementById('cliente-numero-documento').value;
        const telefono = document.getElementById('cliente-telefono').value;
        const email = document.getElementById('cliente-email').value;

        if (!nombres || !apellidos || !tipoDocumento || !numeroDocumento || !email) {
            showToast('Por favor diligencie todos los campos obligatorios (*)', 'warning');
            return;
        }

        try {
            await fetchAPI('/clientes', {
                method: 'POST',
                body: JSON.stringify({ nombres, apellidos, tipoDocumento, numeroDocumento, telefono, email })
            });
            showToast('Cliente guardado exitosamente', 'success');
            closeModal('modal-cliente');
            document.getElementById('form-cliente').reset();
            loadClientsData();
        } catch (err) {
            showToast('Error al registrar cliente', 'error');
        }
    });

    // 2. Save new Vehicle
    document.getElementById('btn-guardar-vehiculo')?.addEventListener('click', async (e) => {
        e.preventDefault();
        const tipo = document.getElementById('vehiculo-tipo').value;
        const marca = document.getElementById('vehiculo-marca').value;
        const modelo = document.getElementById('vehiculo-modelo').value;
        const anio = parseInt(document.getElementById('vehiculo-anio').value);
        const placa = document.getElementById('vehiculo-placa').value;
        const color = document.getElementById('vehiculo-color').value;
        const precioDia = parseFloat(document.getElementById('vehiculo-precio-dia').value);
        const kilometraje = parseInt(document.getElementById('vehiculo-kilometraje').value) || 0;

        if (!tipo || !marca || !modelo || !anio || !placa || !precioDia) {
            showToast('Por favor diligencie todos los campos obligatorios (*)', 'warning');
            return;
        }

        try {
            await fetchAPI('/vehiculos', {
                method: 'POST',
                body: JSON.stringify({ tipo, marca, modelo, anio, placa, color, precioDia, kilometraje })
            });
            showToast('Vehículo guardado exitosamente', 'success');
            closeModal('modal-vehiculo');
            document.getElementById('form-vehiculo').reset();
            loadVehiclesData();
        } catch (err) {
            showToast('Error al registrar vehículo', 'error');
        }
    });

    // 3. Save new Reservation
    document.getElementById('btn-guardar-reserva')?.addEventListener('click', async (e) => {
        e.preventDefault();
        const clienteId = document.getElementById('reserva-cliente').value;
        const vehiculoId = document.getElementById('reserva-vehiculo').value;
        const fechaInicio = document.getElementById('reserva-fecha-inicio').value;
        const fechaFin = document.getElementById('reserva-fecha-fin').value;
        const notas = document.getElementById('reserva-notas').value;

        if (!clienteId || !vehiculoId || !fechaInicio || !fechaFin) {
            showToast('Por favor complete los campos obligatorios (*)', 'warning');
            return;
        }

        // Calculate automatic total based on days and price
        const vehicle = vehiclesMap[vehiculoId];
        const days = Math.max(1, Math.round((new Date(fechaFin) - new Date(fechaInicio)) / (1000 * 60 * 60 * 24)));
        const total = vehicle ? (vehicle.precioDia * days) : 0;

        try {
            await fetchAPI('/reservas', {
                method: 'POST',
                body: JSON.stringify({ clienteId, vehiculoId, fechaInicio, fechaFin, total, notas })
            });
            showToast('Reserva registrada exitosamente', 'success');
            closeModal('modal-reserva');
            document.getElementById('form-reserva').reset();
            loadReservationsData();
        } catch (err) {
            showToast('Error al guardar la reserva', 'error');
        }
    });

    // 4. Save new Payment
    document.getElementById('btn-guardar-pago')?.addEventListener('click', async (e) => {
        e.preventDefault();
        const clienteId = document.getElementById('pago-cliente').value;
        const reservaId = document.getElementById('pago-reserva').value;
        const metodo = document.getElementById('pago-metodo').value;
        const monto = parseFloat(document.getElementById('pago-monto').value);
        const fecha = document.getElementById('pago-fecha').value;

        if (!clienteId || !reservaId || !metodo || !monto || !fecha) {
            showToast('Todos los campos son obligatorios (*)', 'warning');
            return;
        }

        try {
            await fetchAPI('/pagos', {
                method: 'POST',
                body: JSON.stringify({ clienteId, reservaId, metodo, monto, fecha, estado: 'completado' })
            });
            showToast('Pago registrado correctamente', 'success');
            closeModal('modal-pago');
            document.getElementById('form-pago').reset();
            loadPaymentsData();
        } catch (err) {
            showToast('Error al registrar pago', 'error');
        }
    });

    // 5. Update/Delete Client from View Detail Modal
    document.getElementById('btn-actualizar-cliente')?.addEventListener('click', async (e) => {
        e.preventDefault();
        const id = activeClientId;
        const nombres = document.getElementById('cliente-ver-nombres').value;
        const apellidos = document.getElementById('cliente-ver-apellidos').value;
        const tipoDocumento = document.getElementById('cliente-ver-tipo-documento').value;
        const numeroDocumento = document.getElementById('cliente-ver-numero-documento').value;
        const telefono = document.getElementById('cliente-ver-telefono').value;
        const email = document.getElementById('cliente-ver-email').value;
        const estado = document.getElementById('cliente-ver-estado').value;

        try {
            await fetchAPI(`/clientes/${id}`, {
                method: 'PUT',
                body: JSON.stringify({ nombres, apellidos, tipoDocumento, numeroDocumento, telefono, email, estado })
            });
            showToast('Cliente actualizado correctamente', 'success');
            closeModal('modal-cliente-ver');
            loadClientsData();
        } catch (err) {
            showToast('Error al actualizar datos de cliente', 'error');
        }
    });

    document.getElementById('btn-eliminar-cliente')?.addEventListener('click', async (e) => {
        e.preventDefault();
        if (!confirm('¿Está seguro de que desea eliminar este cliente?')) return;
        const id = activeClientId;
        try {
            await fetchAPI(`/clientes/${id}`, { method: 'DELETE' });
            showToast('Cliente eliminado', 'info');
            closeModal('modal-cliente-ver');
            loadClientsData();
        } catch (err) {
            showToast('No se puede eliminar el cliente. Tiene reservas asociadas.', 'error');
        }
    });

    // 6. Update/Delete Vehicle from View Detail Modal
    document.getElementById('btn-actualizar-vehiculo')?.addEventListener('click', async (e) => {
        e.preventDefault();
        const id = activeVehicleId;
        const tipo = document.getElementById('vehiculo-ver-tipo').value;
        const marca = document.getElementById('vehiculo-ver-marca').value;
        const modelo = document.getElementById('vehiculo-ver-modelo').value;
        const anio = parseInt(document.getElementById('vehiculo-ver-anio').value);
        const placa = document.getElementById('vehiculo-ver-placa').value;
        const color = document.getElementById('vehiculo-ver-color').value;
        const precioDia = parseFloat(document.getElementById('vehiculo-ver-precio-dia').value);
        const kilometraje = parseInt(document.getElementById('vehiculo-ver-kilometraje').value);
        const estado = document.getElementById('vehiculo-ver-estado').value;

        try {
            await fetchAPI(`/vehiculos/${id}`, {
                method: 'PUT',
                body: JSON.stringify({ tipo, marca, modelo, anio, placa, color, precioDia, kilometraje, estado })
            });
            showToast('Vehículo actualizado correctamente', 'success');
            closeModal('modal-vehiculo-ver');
            loadVehiclesData();
        } catch (err) {
            showToast('Error al actualizar datos de vehículo', 'error');
        }
    });

    document.getElementById('btn-eliminar-vehiculo')?.addEventListener('click', async (e) => {
        e.preventDefault();
        if (!confirm('¿Está seguro de que desea eliminar este vehículo?')) return;
        const id = activeVehicleId;
        try {
            await fetchAPI(`/vehiculos/${id}`, { method: 'DELETE' });
            showToast('Vehículo eliminado', 'info');
            closeModal('modal-vehiculo-ver');
            loadVehiclesData();
        } catch (err) {
            showToast('No se puede eliminar el vehículo. Tiene reservas asociadas.', 'error');
        }
    });

    // 7. Update/Delete Reservation
    document.getElementById('btn-actualizar-reserva')?.addEventListener('click', async (e) => {
        e.preventDefault();
        const id = activeReservationId;
        const fechaInicio = document.getElementById('reserva-ver-fecha-inicio').value;
        const fechaFin = document.getElementById('reserva-ver-fecha-fin').value;
        const estado = document.getElementById('reserva-ver-estado').value;
        const notas = document.getElementById('reserva-ver-notes')?.value || document.getElementById('reserva-ver-notas').value;

        const currentReservation = reservations.find(r => r.id === id);
        if (!currentReservation) return;

        // Recalculate price if dates changed
        const vehicle = currentReservation.vehiculo;
        const days = Math.max(1, Math.round((new Date(fechaFin) - new Date(fechaInicio)) / (1000 * 60 * 60 * 24)));
        const total = vehicle ? (vehicle.precioDia * days) : currentReservation.total;

        try {
            await fetchAPI(`/reservas/${id}`, {
                method: 'PUT',
                body: JSON.stringify({
                    clienteId: currentReservation.cliente.id,
                    vehiculoId: currentReservation.vehiculo.id,
                    fechaInicio,
                    fechaFin,
                    total,
                    estado,
                    notas
                })
            });
            showToast('Reserva actualizada', 'success');
            closeModal('modal-reserva-ver');
            loadReservationsData();
        } catch (err) {
            showToast('Error al actualizar reserva', 'error');
        }
    });

    document.getElementById('btn-eliminar-reserva')?.addEventListener('click', async (e) => {
        e.preventDefault();
        if (!confirm('¿Está seguro de que desea cancelar/eliminar esta reserva?')) return;
        const id = activeReservationId;
        try {
            await fetchAPI(`/reservas/${id}`, { method: 'DELETE' });
            showToast('Reserva eliminada', 'info');
            closeModal('modal-reserva-ver');
            loadReservationsData();
        } catch (err) {
            showToast('Error al eliminar reserva', 'error');
        }
    });
}

// Populate dropdown selection boxes
function populateReservationSelects(clientSelectId, vehicleSelectId) {
    const clientSelect = document.getElementById(clientSelectId);
    const vehicleSelect = document.getElementById(vehicleSelectId);

    if (clientSelect) {
        clientSelect.innerHTML = '<option value="">Seleccione un cliente...</option>' + 
            clients.map(c => `<option value="${c.id}">${c.nombres} ${c.apellidos} (${c.numeroDocumento})</option>`).join('');
    }

    if (vehicleSelect) {
        // Only allow available vehicles for new reservation
        const available = vehicles.filter(v => v.estado === 'disponible');
        vehicleSelect.innerHTML = '<option value="">Seleccione un vehículo...</option>' + 
            available.map(v => `<option value="${v.id}">${v.marca} ${v.modelo} - $${v.precioDia.toLocaleString()}/día</option>`).join('');
    }
}

function populatePaymentSelects() {
    const clientSelect = document.getElementById('pago-cliente');
    const reservaSelect = document.getElementById('pago-reserva');

    if (clientSelect) {
        clientSelect.innerHTML = '<option value="">Seleccione un cliente...</option>' + 
            clients.map(c => `<option value="${c.id}">${c.nombres} ${c.apellidos}</option>`).join('');
            
        // Sync reservations select when client is changed
        clientSelect.addEventListener('change', () => {
            const clientId = parseInt(clientSelect.value);
            if (!clientId) {
                reservaSelect.innerHTML = '<option value="">Primero seleccione un cliente...</option>';
                return;
            }
            
            const clientReservations = reservations.filter(r => r.cliente && r.cliente.id === clientId);
            reservaSelect.innerHTML = '<option value="">Seleccione la reserva...</option>' + 
                clientReservations.map(r => `<option value="${r.id}">Reserva #${r.id} - ${r.vehiculo.marca} (${r.fechaInicio} a ${r.fechaFin})</option>`).join('');
        });
    }

    reservaSelect.innerHTML = '<option value="">Primero seleccione un cliente...</option>';

    // Auto set today's date in date field
    const dateField = document.getElementById('pago-fecha');
    if (dateField) {
        dateField.value = new Date().toISOString().split('T')[0];
    }
}

// ==========================================
// ROW CLICK DETAILS DISPLAY
// ==========================================

window.viewClientDetail = function(id) {
    const c = clientsMap[id];
    if (!c) return;

    activeClientId = id;
    document.getElementById('cliente-ver-id').value = c.id;
    document.getElementById('cliente-ver-nombres').value = c.nombres;
    document.getElementById('cliente-ver-apellidos').value = c.apellidos;
    document.getElementById('cliente-ver-tipo-documento').value = c.tipoDocumento;
    document.getElementById('cliente-ver-numero-documento').value = c.numeroDocumento;
    document.getElementById('cliente-ver-telefono').value = c.telefono || '';
    document.getElementById('cliente-ver-email').value = c.email;
    document.getElementById('cliente-ver-estado').value = c.estado;

    openModal('modal-cliente-ver');
};

window.viewVehicleDetail = function(id) {
    const v = vehiclesMap[id];
    if (!v) return;

    activeVehicleId = id;
    document.getElementById('vehiculo-ver-id').value = v.id;
    document.getElementById('vehiculo-ver-tipo').value = v.tipo;
    document.getElementById('vehiculo-ver-marca').value = v.marca;
    document.getElementById('vehiculo-ver-modelo').value = v.modelo;
    document.getElementById('vehiculo-ver-anio').value = v.anio;
    document.getElementById('vehiculo-ver-placa').value = v.placa;
    document.getElementById('vehiculo-ver-color').value = v.color || '';
    document.getElementById('vehiculo-ver-precio-dia').value = v.precioDia;
    document.getElementById('vehiculo-ver-kilometraje').value = v.kilometraje || 0;
    document.getElementById('vehiculo-ver-estado').value = v.estado;

    openModal('modal-vehiculo-ver');
};

window.viewReservationDetail = function(id) {
    const r = reservations.find(res => res.id === id);
    if (!r) return;

    activeReservationId = id;
    document.getElementById('reserva-ver-id').value = r.id;
    document.getElementById('reserva-ver-cliente').value = r.cliente ? `${r.cliente.nombres} ${r.cliente.apellidos}` : 'Cliente Desconocido';
    document.getElementById('reserva-ver-vehiculo').value = r.vehiculo ? `${r.vehiculo.marca} ${r.vehiculo.modelo}` : 'Vehículo Desconocido';
    document.getElementById('reserva-ver-fecha-inicio').value = r.fechaInicio;
    document.getElementById('reserva-ver-fecha-fin').value = r.fechaFin;
    document.getElementById('reserva-ver-total').value = `$${r.total.toLocaleString()}`;
    document.getElementById('reserva-ver-estado').value = r.estado;
    document.getElementById('reserva-ver-notas').value = r.notas || '';

    openModal('modal-reserva-ver');
};

window.deletePayment = async function(id) {
    if (!confirm('¿Seguro que desea eliminar/anular este pago?')) return;
    try {
        await fetchAPI(`/pagos/${id}`, { method: 'DELETE' });
        showToast('Pago eliminado correctamente', 'info');
        loadPaymentsData();
    } catch (e) {
        showToast('Error al eliminar pago', 'error');
    }
};
