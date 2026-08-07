const THEME_STORAGE_KEY = 'notificationOrderLive:theme';

function effectiveTheme() {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    if (stored) return stored;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function updateToggleLabel(button) {
    const dark = effectiveTheme() === 'dark';
    button.textContent = dark ? '☀️ Clair' : '\u{1F319} Sombre';
}

document.addEventListener('DOMContentLoaded', () => {
    const button = document.getElementById('theme-toggle');
    if (!button) return;

    updateToggleLabel(button);

    button.addEventListener('click', () => {
        const next = effectiveTheme() === 'dark' ? 'light' : 'dark';
        localStorage.setItem(THEME_STORAGE_KEY, next);
        document.documentElement.setAttribute('data-theme', next);
        updateToggleLabel(button);
    });
});
