document.addEventListener('DOMContentLoaded', () => {

    const loadingScreen = document.getElementById('loading-screen');
    const authView = document.getElementById('auth-view');
    const dashboardView = document.getElementById('dashboard-view');

    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const forgotForm = document.getElementById('forgot-form');
    const twofaForm = document.getElementById('twofa-form');

    let twofaSetupSecret = null;

    const params = new URLSearchParams(window.location.search);
    if (params.has('token')) {
        const oauthResponse = {
            accessToken: params.get('token'),
            refreshToken: params.get('refreshToken'),
            expiresIn: params.get('expiresIn'),
            user: {
                id: params.get('userId'),
                name: params.get('userName'),
                email: params.get('userEmail'),
                role: params.get('userRole'),
            }
        };
        Auth.save(oauthResponse);
        window.history.replaceState({}, document.title, '/');
    }

    setTimeout(() => {
        loadingScreen.classList.add('hidden');

        if (Auth.isAuthenticated()) {
            showDashboard();
        }
    }, 1600);

    function hideAllAuthForms() {
        loginForm.classList.add('hidden');
        registerForm.classList.add('hidden');
        forgotForm.classList.add('hidden');
        twofaForm.classList.add('hidden');
    }

    document.getElementById('show-register').addEventListener('click', (e) => {
        e.preventDefault();
        hideAllAuthForms();
        registerForm.classList.remove('hidden');
    });

    document.getElementById('show-login').addEventListener('click', (e) => {
        e.preventDefault();
        hideAllAuthForms();
        loginForm.classList.remove('hidden');
    });

    document.getElementById('show-forgot').addEventListener('click', (e) => {
        e.preventDefault();
        hideAllAuthForms();
        forgotForm.classList.remove('hidden');
    });

    document.getElementById('show-login-from-forgot').addEventListener('click', (e) => {
        e.preventDefault();
        hideAllAuthForms();
        loginForm.classList.remove('hidden');
    });

    document.getElementById('show-login-from-twofa').addEventListener('click', (e) => {
        e.preventDefault();
        hideAllAuthForms();
        loginForm.classList.remove('hidden');
    });

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const errorEl = document.getElementById('login-error');
        errorEl.classList.remove('visible');

        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;

        try {
            const response = await API.login({ email, password });

            if (response.requiresTwoFactor) {
                hideAllAuthForms();
                twofaForm.classList.remove('hidden');
                document.getElementById('twofa-email').value = response.user.email;
                document.getElementById('twofa-code').focus();
                return;
            }

            Auth.save(response);
            toast('Logged in successfully!', 'success');
            showDashboard();
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.classList.add('visible');
        }
    });

    twofaForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const errorEl = document.getElementById('twofa-error');
        errorEl.classList.remove('visible');

        const email = document.getElementById('twofa-email').value;
        const code = document.getElementById('twofa-code').value;

        try {
            const response = await API.verify2fa({ email, code });
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

    document.getElementById('twofa-toggle-btn').addEventListener('click', async () => {
        const user = Auth.getUser();
        if (user && user.twoFactorEnabled) {
            try {
                await API.disable2fa();
                user.twoFactorEnabled = false;
                Auth.updateUser(user);
                render2faStatus(false);
                toast('Two-factor authentication disabled', 'success');
            } catch (err) {
                toast(err.message, 'error');
            }
        } else {
            try {
                const setup = await API.setup2fa();
                twofaSetupSecret = setup.secret;
                const qrContainer = document.getElementById('qr-container');
                qrContainer.innerHTML = `<img src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(setup.qrUri)}" alt="QR Code" style="border-radius:8px;">`;
                document.getElementById('twofa-qr-area').classList.remove('hidden');
            } catch (err) {
                toast(err.message, 'error');
            }
        }
    });

    document.getElementById('twofa-confirm-btn').addEventListener('click', async () => {
        const code = document.getElementById('twofa-confirm-code').value;
        if (!code || code.length !== 6) {
            toast('Enter a valid 6-digit code', 'error');
            return;
        }
        try {
            await API.enable2fa({ secret: twofaSetupSecret, code });
            const user = Auth.getUser();
            user.twoFactorEnabled = true;
            Auth.updateUser(user);
            document.getElementById('twofa-qr-area').classList.add('hidden');
            render2faStatus(true);
            toast('Two-factor authentication enabled!', 'success');
        } catch (err) {
            toast(err.message, 'error');
        }
    });

    function render2faStatus(enabled) {
        const statusEl = document.getElementById('twofa-status');
        const toggleBtn = document.getElementById('twofa-toggle-btn');
        if (enabled) {
            statusEl.textContent = 'Enabled';
            statusEl.style.color = 'var(--success)';
            toggleBtn.innerHTML = '<i class="fas fa-lock-open"></i> Disable 2FA';
        } else {
            statusEl.textContent = 'Disabled';
            statusEl.style.color = 'var(--text-muted)';
            toggleBtn.innerHTML = '<i class="fas fa-lock"></i> Enable 2FA';
        }
    }

    function showDashboard() {
        authView.classList.add('hidden');
        dashboardView.classList.remove('hidden');
        renderDashboard();
    }

    function showAuth() {
        dashboardView.classList.add('hidden');
        authView.classList.remove('hidden');
        hideAllAuthForms();
        loginForm.classList.remove('hidden');
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

        render2faStatus(user.twoFactorEnabled || false);
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
