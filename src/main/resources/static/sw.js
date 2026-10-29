self.addEventListener("install", event => {
    event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", event => {
    event.waitUntil(self.clients.claim());
});

self.addEventListener("fetch", event => {
    if (event.request.mode !== "navigate") {
        return;
    }

    event.respondWith(
        fetch(event.request, { cache: "no-store" }).catch(() => {
            return new Response(
                `<!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1">
                    <title>Job Tracker — Offline</title>
                </head>
                <body style="font-family:system-ui;padding:32px;
                             max-width:500px;margin:auto">
                    <h1>Unable to connect</h1>
                    <p>Job Tracker needs a connection to its server.
                       Check your internet connection and try again.</p>
                    <a href="/">Try again</a>
                </body>
                </html>`,
                {
                    status: 503,
                    headers: {
                        "Content-Type": "text/html; charset=utf-8",
                        "Cache-Control": "no-store"
                    }
                }
            );
        })
    );
});