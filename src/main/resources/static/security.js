async function getCsrfToken() {
    const response = await fetch("/api/auth/csrf", {
        cache: "no-store"
    });

    if (!response.ok) {
        throw new Error("Could not obtain CSRF token");
    }

    return response.json();
}

async function apiFetch(url, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const headers = new Headers(options.headers);

    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
        const csrf = await getCsrfToken();
        headers.set(csrf.headerName, csrf.token);
    }

    const response = await fetch(url, {
        ...options,
        headers
    });

    if (response.status === 401) {
        window.location.href = "/login.html";
        throw new Error("Please log in");
    }

    return response;
}