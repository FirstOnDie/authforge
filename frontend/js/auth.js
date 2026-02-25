const Auth = (() => {
    const KEYS = {
        ACCESS_TOKEN: 'authforge_access_token',
        REFRESH_TOKEN: 'authforge_refresh_token',
        USER: 'authforge_user',
        EXPIRES_IN: 'authforge_expires_in',
    };

    return {
        save(response) {
            localStorage.setItem(KEYS.ACCESS_TOKEN, response.accessToken);
            localStorage.setItem(KEYS.REFRESH_TOKEN, response.refreshToken);
            localStorage.setItem(KEYS.EXPIRES_IN, response.expiresIn);
            localStorage.setItem(KEYS.USER, JSON.stringify(response.user));
        },

        clear() {
            localStorage.removeItem(KEYS.ACCESS_TOKEN);
            localStorage.removeItem(KEYS.REFRESH_TOKEN);
            localStorage.removeItem(KEYS.EXPIRES_IN);
            localStorage.removeItem(KEYS.USER);
        },

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

        updateUser(user) {
            localStorage.setItem(KEYS.USER, JSON.stringify(user));
        },
    };
})();
