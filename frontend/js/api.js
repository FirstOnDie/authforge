const API = (() => {
    const BASE_URL = '/api';

    async function request(endpoint, options = {}) {
        const url = `${BASE_URL}${endpoint}`;

        const headers = {
            'Content-Type': 'application/json',
            ...options.headers,
        };

        const token = Auth.getAccessToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(url, {
            method: options.method || 'GET',
            headers,
            body: options.body ? JSON.stringify(options.body) : undefined,
        });

        const data = await response.json().catch(() => null);

        if (!response.ok) {
            const errorMessage = data?.error
                || data?.details
                || data?.message
                || `Request failed (${response.status})`;
            throw new Error(typeof errorMessage === 'object'
                ? Object.values(errorMessage).join(', ')
                : errorMessage);
        }

        return data;
    }

    return {
        register: (body) => request('/auth/register', { method: 'POST', body }),
        login: (body) => request('/auth/login', { method: 'POST', body }),
        refresh: (body) => request('/auth/refresh', { method: 'POST', body }),
        logout: () => request('/auth/logout', { method: 'POST' }),
        forgotPassword: (email) => request('/auth/forgot-password', { method: 'POST', body: { email } }),
        resetPassword: (body) => request('/auth/reset-password', { method: 'POST', body }),
        getMe: () => request('/users/me'),
        getUsers: () => request('/admin/users'),
        changeRole: (id, role) => request(`/admin/users/${id}/role`, { method: 'PUT', body: { role } }),
    };
})();
