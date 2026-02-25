/**
 * AuthForge — Auth State Manager
 *
 * Manages JWT tokens in localStorage:
 * - Store / retrieve / clear access and refresh tokens
 * - Store user info
 * - Check authentication status
 */
const Auth = (() => {
    const KEYS = {
        ACCESS_TOKEN: 'authforge_access_token',
        REFRESH_TOKEN: 'authforge_refresh_token',
        USER: 'authforge_user',
        EXPIRES_IN: 'authforge_expires_in',
    };

    return {
        /**
         * Save authentication data from a successful login/register response.
         */
        save(response) {
            localStorage.setItem(KEYS.ACCESS_TOKEN, response.accessToken);
            localStorage.setItem(KEYS.REFRESH_TOKEN, response.refreshToken);
            localStorage.setItem(KEYS.EXPIRES_IN, response.expiresIn);
            localStorage.setItem(KEYS.USER, JSON.stringify(response.user));
        },

        /**
         * Clear all authentication data (logout).
         */
        clear() {
            localStorage.removeItem(KEYS.ACCESS_TOKEN);
            localStorage.removeItem(KEYS.REFRESH_TOKEN);
            localStorage.removeItem(KEYS.EXPIRES_IN);
            localStorage.removeItem(KEYS.USER);
        },

        /**
         * Check if the user is currently authenticated.
         */
        isAuthenticated() {
            return !!localStorage.getItem(KEYS.ACCESS_TOKEN);
        },

        getAccessToken() {
            return localStorage.getItem(KEYS.ACCESS_TOKEN);
        },

        getRefreshToken() {
            return localStorage.getItem(KEYS.REFRESH_TOKEN);
        },

        getUser() {
            const raw = localStorage.getItem(KEYS.USER);
            return raw ? JSON.parse(raw) : null;
        },

        getExpiresIn() {
            return parseInt(localStorage.getItem(KEYS.EXPIRES_IN) || '0', 10);
        },

        isAdmin() {
            const user = this.getUser();
            return user?.role === 'ADMIN';
        },
    };
})();
