document.addEventListener('DOMContentLoaded', () => {

    const loadingScreen = document.getElementById('loading-screen');
    const authView = document.getElementById('auth-view');
    const dashboardView = document.getElementById('dashboard-view');

    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const forgotForm = document.getElementById('forgot-form');

    setTimeout(() => {
        loadingScreen.classList.add('hidden');

        if (Auth.isAuthenticated()) {
            showDashboard();
        }
    }, 1600);

    document.getElementById('show-register').addEventListener('click', (e) => {
        e.preventDefault();
        loginForm.classList.add('hidden');
        registerForm.classList.remove('hidden');
        forgotForm.classList.add('hidden');
    });

    document.getElementById('show-login').addEventListener('click', (e) => {
        e.preventDefault();
        loginForm.classList.remove('hidden');
        registerForm.classList.add('hidden');
        forgotForm.classList.add('hidden');
    });

    document.getElementById('show-forgot').addEventListener('click', (e) => {
        e.preventDefault();
        loginForm.classList.add('hidden');
        registerForm.classList.add('hidden');
        forgotForm.classList.remove('hidden');
    });

    document.getElementById('show-login-from-forgot').addEventListener('click', (e) => {
        e.preventDefault();
        loginForm.classList.remove('hidden');
        forgotForm.classList.add('hidden');
    });

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const errorEl = document.getElementById('login-error');
        errorEl.classList.remove('visible');

        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;

        try {
            const response = await API.login({ email, password });
            Auth.save(response);
            toast('Logged in successfully!', 'success');
            showDashboard();
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.classList.add('visible');
        }
    });

    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const errorEl = document.getElementById('register-error');
        errorEl.classList.remove('visible');

        const name = document.getElementById('register-name').value;
        const email = document.getElementById('register-email').value;
        const password = document.getElementById('register-password').value;

        try {
            const response = await API.register({ name, email, password });
            Auth.save(response);
            toast('Account created successfully!', 'success');
            showDashboard();
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.classList.add('visible');
        }
    });

    forgotForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const errorEl = document.getElementById('forgot-error');
        const successEl = document.getElementById('forgot-success');
        errorEl.classList.remove('visible');
        successEl.classList.add('hidden');

        const email = document.getElementById('forgot-email').value;

        try {
            const response = await API.forgotPassword(email);
            successEl.textContent = `Reset token generated! Check the server console logs. Token: ${response.token}`;
            successEl.classList.remove('hidden');
            toast('Reset token generated!', 'success');
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.classList.add('visible');
        }
    });

    document.getElementById('logout-btn').addEventListener('click', async () => {
        try {
            await API.logout();
        } catch (err) {
        }
        Auth.clear();
        toast('Logged out', 'success');
        showAuth();
    });

    document.getElementById('nav-dashboard').addEventListener('click', () => {
        setActiveNav('dashboard');
        document.getElementById('dashboard-content').classList.remove('hidden');
        document.getElementById('admin-content').classList.add('hidden');
    });

    document.getElementById('nav-admin').addEventListener('click', async () => {
        setActiveNav('admin');
        document.getElementById('dashboard-content').classList.add('hidden');
        document.getElementById('admin-content').classList.remove('hidden');
        await loadAdminUsers();
    });

    function setActiveNav(view) {
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.view === view);
        });
    }

    document.getElementById('copy-token-btn').addEventListener('click', () => {
        const token = Auth.getAccessToken();
        if (token) {
            navigator.clipboard.writeText(token);
            toast('Token copied to clipboard!', 'success');
        }
    });

    function showDashboard() {
        authView.classList.add('hidden');
        dashboardView.classList.remove('hidden');
        renderDashboard();
    }

    function showAuth() {
        dashboardView.classList.add('hidden');
        authView.classList.remove('hidden');
        loginForm.classList.remove('hidden');
        registerForm.classList.add('hidden');
        forgotForm.classList.add('hidden');
    }

    async function renderDashboard() {
        const user = Auth.getUser();
        if (!user) return showAuth();

        document.getElementById('user-name').textContent = user.name;
        document.getElementById('user-role').textContent = user.role;

        const adminNav = document.getElementById('nav-admin');
        if (user.role === 'ADMIN') {
            adminNav.classList.remove('hidden');
        } else {
            adminNav.classList.add('hidden');
        }

        document.getElementById('profile-name').textContent = user.name;
        document.getElementById('profile-email').textContent = user.email;
        document.getElementById('profile-role').textContent = user.role;

        document.getElementById('token-display').textContent = Auth.getAccessToken();
        const expiresIn = Auth.getExpiresIn();
        document.getElementById('token-expiry').textContent =
            expiresIn ? `${Math.round(expiresIn / 60000)} minutes` : '—';
    }

    async function loadAdminUsers() {
        const tbody = document.getElementById('users-table-body');
        tbody.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';

        try {
            const users = await API.getUsers();
            tbody.innerHTML = users.map(user => `
                <tr>
                    <td>${user.id}</td>
                    <td>${user.name}</td>
                    <td>${user.email}</td>
                    <td><span class="role-badge ${user.role.toLowerCase()}">${user.role}</span></td>
                    <td>
                        <button class="btn-role-toggle" onclick="toggleRole(${user.id}, '${user.role}')">
                            ${user.role === 'ADMIN' ? 'Demote to USER' : 'Promote to ADMIN'}
                        </button>
                    </td>
                </tr>
            `).join('');
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="5" style="color:var(--error)">${err.message}</td></tr>`;
        }
    }

    window.toggleRole = async (userId, currentRole) => {
        const newRole = currentRole === 'ADMIN' ? 'USER' : 'ADMIN';
        try {
            await API.changeRole(userId, newRole);
            toast(`Role changed to ${newRole}`, 'success');
            await loadAdminUsers();
        } catch (err) {
            toast(err.message, 'error');
        }
    };

    function toast(message, type = 'success') {
        const container = document.getElementById('toast-container');
        const icon = type === 'success' ? 'check-circle' : 'exclamation-circle';

        const el = document.createElement('div');
        el.className = `toast ${type}`;
        el.innerHTML = `<i class="fas fa-${icon}"></i><span>${message}</span>`;
        container.appendChild(el);

        setTimeout(() => {
            el.classList.add('removing');
            setTimeout(() => el.remove(), 300);
        }, 3000);
    }
});
