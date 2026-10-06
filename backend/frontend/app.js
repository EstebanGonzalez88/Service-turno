const API_URL = "http://127.0.0.1:8000/api";

function toggleSection(sectionId) {
    document.getElementById('login-section').classList.add('hidden');
    document.getElementById('register-section').classList.add('hidden');
    document.getElementById('dashboard-section').classList.add('hidden');
    document.getElementById('error-msg').classList.add('hidden');

    document.getElementById(sectionId).classList.remove('hidden');
}

function showError(msg) {
    const errorDiv = document.getElementById('error-msg');
    errorDiv.innerText = msg;
    errorDiv.classList.remove('hidden');
}

async function login() {
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    const formData = new URLSearchParams();
    formData.append('username', email);
    formData.append('password', password);

    try {
        const response = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        });

        const data = await response.json();
        if (response.ok) {
            localStorage.setItem('access_token', data.access_token);
            checkAuth();
        } else {
            let errorMsg = "Error al iniciar sesión";
            if (data.detail) {
                if (Array.isArray(data.detail)) {
                    errorMsg = data.detail.map(err => `${err.loc.join('.')}: ${err.msg}`).join(', ');
                } else {
                    errorMsg = data.detail;
                }
            }
            showError(errorMsg);
        }
    } catch (error) {
        showError("Error de conexión");
    }
}

async function register() {
    const nombre = document.getElementById('reg-name').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;

    try {
        const response = await fetch(`${API_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nombre, email, password })
        });

        const data = await response.json();
        if (response.ok) {
            alert("Registro exitoso. Por favor, inicia sesión.");
            toggleSection('login-section');
        } else {
            let errorMsg = "Error en el registro";
            if (data.detail) {
                if (Array.isArray(data.detail)) {
                    errorMsg = data.detail.map(err => `${err.loc.join('.')}: ${err.msg}`).join(', ');
                } else {
                    errorMsg = data.detail;
                }
            }
            showError(errorMsg);
        }
    } catch (error) {
        showError("Error de conexión");
    }
}

function logout() {
    localStorage.removeItem('access_token');
    checkAuth();
}

function getAuthHeaders() {
    const token = localStorage.getItem('access_token');
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

async function checkAuth() {
    const token = localStorage.getItem('access_token');
    if (!token) {
        toggleSection('login-section');
        return;
    }

    // Ocultar alertas
    document.getElementById('error-msg').classList.add('hidden');
    toggleSection('dashboard-section');

    // Cargar contenido automáticamente
    fetchContents();

    // Ver si es admin para mostrar panel
    try {
        const response = await fetch(`${API_URL}/admin/users`, { headers: getAuthHeaders() });
        if (response.ok) {
            document.getElementById('admin-panel').classList.remove('hidden');
        } else {
            document.getElementById('admin-panel').classList.add('hidden');
        }
    } catch (e) {
        console.error(e);
    }
}

// ---- LÓGICA DE CONTENIDO REAL ----

async function fetchContents() {
    try {
        const response = await fetch(`${API_URL}/content/`, { headers: getAuthHeaders() });
        const data = await response.json();

        if (response.ok) {
            renderContents(data);
        } else {
            showError(`Error al cargar contenido: ${data.detail || 'Sin permiso'}`);
        }
    } catch (error) {
        console.error(error);
    }
}

async function createContent() {
    const texto = document.getElementById('new-content-text').value;
    if (!texto.trim()) return alert("El contenido no puede estar vacío");

    try {
        const response = await fetch(`${API_URL}/content/`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ texto: texto })
        });

        const data = await response.json();
        if (response.ok) {
            document.getElementById('new-content-text').value = ''; // Limpiar textarea
            fetchContents(); // Recargar lista
        } else {
            showError(`Error: ${data.detail}`);
        }
    } catch (error) {
        showError("Error de conexión al crear contenido");
    }
}

async function deleteContent(id) {
    if (!confirm("¿Seguro que quieres eliminar este contenido?")) return;

    try {
        const response = await fetch(`${API_URL}/content/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        const data = await response.json();
        if (response.ok) {
            fetchContents(); // Recargar lista
        } else {
            showError(`Error al eliminar: ${data.detail}`);
        }
    } catch (error) {
        showError("Error de conexión al eliminar");
    }
}

function renderContents(contentsArray) {
    const listDiv = document.getElementById('content-list');
    listDiv.innerHTML = '';

    if (contentsArray.length === 0) {
        listDiv.innerHTML = '<p style="color: gray;">No hay contenidos publicados aún.</p>';
        return;
    }

    contentsArray.forEach(item => {
        const card = document.createElement('div');
        card.className = 'content-card';

        const fechaStr = new Date(item.fecha_creacion).toLocaleString();

        card.innerHTML = `
            <div class="content-author">${item.autor_email} - ${fechaStr}</div>
            <p>${item.texto}</p>
            <button class="delete-btn" onclick="deleteContent(${item.id})">🗑 Eliminar (Requiere Permiso)</button>
        `;
        listDiv.appendChild(card);
    });
}

// ---- LÓGICA DE ADMIN ----

async function fetchUsers() {
    try {
        const response = await fetch(`${API_URL}/admin/users`, { headers: getAuthHeaders() });
        const data = await response.json();
        document.getElementById('admin-result').innerText = JSON.stringify(data, null, 2);
    } catch (error) {
        document.getElementById('admin-result').innerText = "Error";
    }
}

async function fetchAudit() {
    try {
        const response = await fetch(`${API_URL}/admin/audit`, { headers: getAuthHeaders() });
        const data = await response.json();
        document.getElementById('admin-result').innerText = JSON.stringify(data, null, 2);
    } catch (error) {
        document.getElementById('admin-result').innerText = "Error";
    }
}

// Al cargar la página
window.onload = checkAuth;