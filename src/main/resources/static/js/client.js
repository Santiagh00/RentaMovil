// API Base URL
const API_BASE = '/api';

// State Management
let state = {
    vehicles: [],
    filteredVehicles: [],
    selectedVehicle: null,
    currentStep: 1,
    clientExists: false,
    clientId: null
};

// DOM Elements
const elements = {
    themeBtn: document.getElementById('btn-theme'),
    catalogGrid: document.getElementById('catalog-grid'),
    searchInput: document.getElementById('search-input'),
    tabBtns: document.querySelectorAll('.tab-btn'),
    rentModal: document.getElementById('modal-rent'),
    modalTitle: document.getElementById('modal-title'),
    modalCloseBtn: document.querySelector('.modal-close'),
    prevStepBtn: document.getElementById('btn-prev-step'),
    nextStepBtn: document.getElementById('btn-next-step'),
    stepDots: document.querySelectorAll('.step-dot'),
    stepPanels: document.querySelectorAll('.step-panel'),
    toastContainer: document.getElementById('toast-container'),
    
    // Step 1: Fechas
    fechaInicio: document.getElementById('rent-fecha-inicio'),
    fechaFin: document.getElementById('rent-fecha-fin'),
    daysCount: document.getElementById('days-count'),
    pricePerDay: document.getElementById('price-per-day'),
    totalPrice: document.getElementById('total-price'),
    
    // Step 2: Cliente
    docTipo: document.getElementById('client-doc-tipo'),
    docNum: document.getElementById('client-doc-num'),
    clientNombres: document.getElementById('client-nombres'),
    clientApellidos: document.getElementById('client-apellidos'),
    clientTelefono: document.getElementById('client-telefono'),
    clientEmail: document.getElementById('client-email'),
    verifiedBanner: document.getElementById('client-verified-banner'),
    verifiedName: document.getElementById('verified-client-name'),
    
    // Step 3: Pago Manual
    wompiTotal: document.getElementById('wompi-total'),
    btnSubmitComprobante: document.getElementById('btn-submit-comprobante'),
    comprobanteFile: document.getElementById('comprobante-file')
};

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    loadVehicles();
    setupEventListeners();
});

// Theme Management
function initTheme() {
    const isDark = localStorage.getItem('theme') === 'dark';
    if (isDark) {
        document.body.classList.add('dark-theme');
        elements.themeBtn.innerHTML = '<i class="fa-regular fa-sun"></i>';
    } else {
        document.body.classList.remove('dark-theme');
        elements.themeBtn.innerHTML = '<i class="fa-regular fa-moon"></i>';
    }
}

function toggleTheme() {
    const isDark = document.body.classList.toggle('dark-theme');
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
    elements.themeBtn.innerHTML = isDark ? '<i class="fa-regular fa-sun"></i>' : '<i class="fa-regular fa-moon"></i>';
}

// Load Vehicles from Backend
async function loadVehicles() {
    try {
        const response = await fetch(`${API_BASE}/vehiculos/disponibles`);
        if (!response.ok) throw new Error('Error al cargar vehículos');
        const data = await response.ok ? await response.json() : [];
        state.vehicles = data;
        state.filteredVehicles = [...state.vehicles];
        renderCatalog();
    } catch (error) {
        showToast('Error al conectar con el servidor', 'danger');
        console.error(error);
    }
}

// Render Catalog Grid
function renderCatalog() {
    elements.catalogGrid.innerHTML = '';
    
    if (state.filteredVehicles.length === 0) {
        elements.catalogGrid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-secondary);">
                <i class="fa-solid fa-car-burst" style="font-size: 3rem; margin-bottom: 16px; opacity: 0.5;"></i>
                <p>No se encontraron vehículos disponibles en este momento.</p>
            </div>
        `;
        return;
    }
    
    state.filteredVehicles.forEach(vehicle => {
        const card = document.createElement('div');
        card.className = 'vehicle-card';
        
        const isCar = vehicle.tipo.toLowerCase() === 'carro';
        const typeIcon = isCar ? 'fa-car' : 'fa-motorcycle';
        
        card.innerHTML = `
            <div class="vehicle-image-wrapper">
                <div class="vehicle-type-badge">
                    <i class="fa-solid ${typeIcon}"></i> ${vehicle.tipo}
                </div>
                <i class="fa-solid ${isCar ? 'fa-car-side' : 'fa-motorcycle'}" style="font-size: 4.5rem;"></i>
                <div class="vehicle-price-badge">
                    $${parseFloat(vehicle.precioDia).toLocaleString('es-CO')}/día
                </div>
            </div>
            <div class="vehicle-info-body">
                <h3 class="vehicle-title">${vehicle.marca} ${vehicle.modelo}</h3>
                <div class="vehicle-specs">
                    <div class="spec-item">
                        <i class="fa-solid fa-calendar"></i>
                        <span>Año: ${vehicle.anio}</span>
                    </div>
                    <div class="spec-item">
                        <i class="fa-solid fa-palette"></i>
                        <span>Color: ${vehicle.color}</span>
                    </div>
                    <div class="spec-item">
                        <i class="fa-solid fa-gauge-high"></i>
                        <span>${parseFloat(vehicle.kilometraje).toLocaleString('es-CO')} km</span>
                    </div>
                    <div class="spec-item">
                        <i class="fa-solid fa-hashtag"></i>
                        <span>Placa: ${vehicle.placa}</span>
                    </div>
                </div>
                <button class="btn-rent" data-id="${vehicle.id}">
                    <i class="fa-solid fa-key"></i> Alquilar Ahora
                </button>
            </div>
        `;
        
        elements.catalogGrid.appendChild(card);
    });

    // Add rent button listeners
    document.querySelectorAll('.btn-rent').forEach(btn => {
        btn.addEventListener('click', () => {
            const vehicleId = btn.getAttribute('data-id');
            const vehicle = state.vehicles.find(v => v.id == vehicleId);
            if (vehicle) openRentModal(vehicle);
        });
    });
}

// Setup Event Listeners
function setupEventListeners() {
    elements.themeBtn.addEventListener('click', toggleTheme);
    
    // Search filter
    elements.searchInput.addEventListener('input', filterVehicles);
    
    // Tab filters
    elements.tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            elements.tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            filterVehicles();
        });
    });
    
    // Close Modal
    elements.modalCloseBtn.addEventListener('click', closeRentModal);
    
    // Stepper navigation
    elements.prevStepBtn.addEventListener('click', prevStep);
    elements.nextStepBtn.addEventListener('click', nextStep);
    
    // Date calculation
    elements.fechaInicio.addEventListener('change', calculatePrice);
    elements.fechaFin.addEventListener('change', calculatePrice);
    
    // Client auto-lookup
    elements.docNum.addEventListener('blur', lookupClient);
    
    // Manual payment validation
    elements.btnSubmitComprobante.addEventListener('click', processBookingWithReceipt);
}

// Filter Vehicles logic
function filterVehicles() {
    const searchVal = elements.searchInput.value.toLowerCase();
    const activeTab = document.querySelector('.tab-btn.active').getAttribute('data-type');
    
    state.filteredVehicles = state.vehicles.filter(v => {
        const matchesSearch = v.marca.toLowerCase().includes(searchVal) || v.modelo.toLowerCase().includes(searchVal);
        const matchesTab = activeTab === 'todos' || v.tipo.toLowerCase() === activeTab;
        return matchesSearch && matchesTab;
    });
    
    renderCatalog();
}

// Open Rent Modal
function openRentModal(vehicle) {
    state.selectedVehicle = vehicle;
    elements.modalTitle.textContent = `Alquilar: ${vehicle.marca} ${vehicle.modelo}`;
    
    // Setup Default Dates
    const today = new Date().toISOString().split('T')[0];
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().split('T')[0];
    
    elements.fechaInicio.value = today;
    elements.fechaInicio.min = today;
    elements.fechaFin.value = tomorrowStr;
    elements.fechaFin.min = tomorrowStr;
    
    // Reset Stepper
    setStep(1);
    
    // Reset Client Form
    elements.docNum.value = '';
    elements.clientNombres.value = '';
    elements.clientApellidos.value = '';
    elements.clientTelefono.value = '';
    elements.clientEmail.value = '';
    elements.verifiedBanner.style.display = 'none';
    elements.verifiedName.textContent = '';
    state.clientExists = false;
    state.clientId = null;
    
    calculatePrice();
    elements.rentModal.classList.add('active');
}

function closeRentModal() {
    elements.rentModal.classList.remove('active');
    state.selectedVehicle = null;
}

// Stepper control
function setStep(stepNum) {
    state.currentStep = stepNum;
    
    // Update Dots
    elements.stepDots.forEach((dot, index) => {
        dot.className = 'step-dot';
        if (index + 1 < stepNum) dot.classList.add('completed');
        if (index + 1 === stepNum) dot.classList.add('active');
    });
    
    // Update Panels
    elements.stepPanels.forEach((panel, index) => {
        panel.className = 'step-panel';
        if (index + 1 === stepNum) panel.classList.add('active');
    });
    
    // Update Footer Buttons
    if (stepNum === 1) {
        elements.prevStepBtn.style.display = 'none';
        elements.nextStepBtn.style.display = 'block';
        elements.nextStepBtn.textContent = 'Siguiente';
    } else if (stepNum === 2) {
        elements.prevStepBtn.style.display = 'block';
        elements.nextStepBtn.style.display = 'block';
        elements.nextStepBtn.textContent = 'Proceder al Pago';
    } else {
        elements.prevStepBtn.style.display = 'block';
        elements.nextStepBtn.style.display = 'none';
    }
}

function prevStep() {
    if (state.currentStep > 1) {
        setStep(state.currentStep - 1);
    }
}

function nextStep() {
    if (state.currentStep === 1) {
        // Validate Dates
        const inicio = new Date(elements.fechaInicio.value);
        const fin = new Date(elements.fechaFin.value);
        
        if (isNaN(inicio.getTime()) || isNaN(fin.getTime())) {
            showToast('Por favor ingrese fechas válidas', 'danger');
            return;
        }
        if (inicio >= fin) {
            showToast('La fecha de fin debe ser posterior a la fecha de inicio', 'danger');
            return;
        }
        setStep(2);
    } else if (state.currentStep === 2) {
        // Validate Client Form
        if (!elements.docNum.value.trim()) {
            showToast('El número de documento es requerido', 'danger');
            return;
        }
        if (!elements.clientNombres.value.trim() || !elements.clientApellidos.value.trim() || !elements.clientEmail.value.trim()) {
            showToast('Por favor, rellene todos los campos obligatorios (*)', 'danger');
            return;
        }
        
        // Setup Step 3 Price Total text
        const total = parseFloat(elements.totalPrice.getAttribute('data-total'));
        elements.wompiTotal.textContent = `$${total.toLocaleString('es-CO')}`;
        
        setStep(3);
    }
}

// Calculate Price Dynamically
function calculatePrice() {
    if (!state.selectedVehicle) return;
    
    const startVal = elements.fechaInicio.value;
    const endVal = elements.fechaFin.value;
    
    if (!startVal || !endVal) return;
    
    const start = new Date(startVal);
    const end = new Date(endVal);
    
    if (start >= end) {
        elements.daysCount.textContent = '0';
        elements.pricePerDay.textContent = `$0`;
        elements.totalPrice.textContent = `$0`;
        elements.totalPrice.setAttribute('data-total', '0');
        return;
    }
    
    const diffTime = Math.abs(end - start);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    const price = parseFloat(state.selectedVehicle.precioDia);
    const total = price * diffDays;
    
    elements.daysCount.textContent = diffDays;
    elements.pricePerDay.textContent = `$${price.toLocaleString('es-CO')}`;
    elements.totalPrice.textContent = `$${total.toLocaleString('es-CO')}`;
    elements.totalPrice.setAttribute('data-total', total.toString());
}

// Client Lookup by Document
async function lookupClient() {
    const docNumber = elements.docNum.value.trim();
    if (!docNumber) return;
    
    try {
        const response = await fetch(`${API_BASE}/clientes/documento/${docNumber}`);
        if (response.ok) {
            const client = await response.json();
            state.clientExists = true;
            state.clientId = client.id;
            
            // Auto-fill and notify
            elements.clientNombres.value = client.nombres;
            elements.clientApellidos.value = client.apellidos;
            elements.clientTelefono.value = client.telefono || '';
            elements.clientEmail.value = client.email;
            
            elements.verifiedName.textContent = `${client.nombres} ${client.apellidos}`;
            elements.verifiedBanner.style.display = 'flex';
            
            showToast('Cliente existente encontrado y vinculado', 'success');
        } else {
            // Not found - reset lookup state
            state.clientExists = false;
            state.clientId = null;
            elements.verifiedBanner.style.display = 'none';
        }
    } catch (error) {
        console.error('Error al consultar cliente:', error);
    }
}

// Process Booking (connected to backend)
async function processBookingWithReceipt() {
    if (!state.selectedVehicle) return;
    
    const file = elements.comprobanteFile.files[0];
    if (!file) {
        showToast('Por favor adjunte un comprobante de pago', 'danger');
        return;
    }
    
    // Check file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
        showToast('El archivo es demasiado grande. Máximo 5MB.', 'danger');
        return;
    }

    const total = parseFloat(elements.totalPrice.getAttribute('data-total'));
    const fechaIni = elements.fechaInicio.value;
    const fechaFi = elements.fechaFin.value;
    
    // Disable button to prevent double submission
    elements.btnSubmitComprobante.disabled = true;
    elements.btnSubmitComprobante.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Procesando...';
    
    try {
        // Step 1: Create client if they do not exist
        if (!state.clientExists) {
            const clientPayload = {
                nombres: elements.clientNombres.value.trim(),
                apellidos: elements.clientApellidos.value.trim(),
                tipoDocumento: elements.docTipo.value,
                numeroDocumento: elements.docNum.value.trim(),
                telefono: elements.clientTelefono.value.trim() || null,
                email: elements.clientEmail.value.trim(),
                estado: 'activo'
            };
            
            const clientRes = await fetch(`${API_BASE}/clientes`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(clientPayload)
            });
            
            if (!clientRes.ok) {
                const err = await clientRes.text();
                throw new Error(err || 'Error al registrar información del cliente');
            }
            
            const newClient = await clientRes.json();
            state.clientId = newClient.id;
            state.clientExists = true;
        }
        
        // Step 2: Create reservation
        const reservationPayload = {
            clienteId: state.clientId,
            vehiculoId: state.selectedVehicle.id,
            fechaInicio: fechaIni,
            fechaFin: fechaFi,
            total: total,
            estado: 'pendiente',
            notas: 'Reserva creada. Pendiente de subir comprobante.'
        };
        
        const reservationRes = await fetch(`${API_BASE}/reservas`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(reservationPayload)
        });
        
        if (!reservationRes.ok) {
            const err = await reservationRes.text();
            throw new Error(err || 'Error al procesar reserva en el sistema');
        }
        
        const reservation = await reservationRes.json();
        
        // Step 3: Upload receipt
        const formData = new FormData();
        formData.append('file', file);

        const uploadRes = await fetch(`${API_BASE}/reservas/${reservation.id}/comprobante`, {
            method: 'POST',
            body: formData
        });

        if (!uploadRes.ok) {
            const err = await uploadRes.text();
            throw new Error(err || 'Error al subir el comprobante.');
        }

        showToast('¡Comprobante enviado! Su reserva será validada por un administrador.', 'success');
        
        // Close modal, refresh catalogue and state
        closeRentModal();
        loadVehicles();
        
    } catch (error) {
        showToast(error.message || 'Error durante la reserva', 'danger');
        console.error(error);
    } finally {
        elements.btnSubmitComprobante.disabled = false;
        elements.btnSubmitComprobante.innerHTML = '<i class="fa-solid fa-cloud-arrow-up"></i> Enviar Comprobante';
    }
}

// Toast Messages Alert Utilities
function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let iconClass = 'fa-circle-check';
    if (type === 'danger') iconClass = 'fa-circle-xmark';
    if (type === 'warning') iconClass = 'fa-circle-exclamation';
    
    toast.innerHTML = `
        <i class="fa-solid ${iconClass}"></i>
        <div class="toast-message">${message}</div>
    `;
    
    elements.toastContainer.appendChild(toast);
    
    // Auto-remove after 4 seconds
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}
