/* Barometer CRM Service Worker */

const CACHE_NAME = 'barometer-crm-v4';
const STATIC_ASSETS = [
  '/manifest.json'
];

// ── INSTALL: cache static assets ──────────────────
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(STATIC_ASSETS).catch(() => {});
    })
  );
  self.skipWaiting();
});

// ── ACTIVATE: clean old caches ────────────────────
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    )
  );
  self.clients.claim();
});

// ── FETCH: network-first for API, cache-first for static ──
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  // Never cache API or cross-origin requests
  if (url.hostname !== self.location.hostname || url.pathname.startsWith('/api/')) {
    return; // pass through
  }

  event.respondWith(
    fetch(event.request)
      .then(res => {
        if (res && res.status === 200) {
          const clone = res.clone();
          caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone));
        }
        return res;
      })
      .catch(() => caches.match(event.request))
  );
});

// ── PUSH: show notification ───────────────────────
self.addEventListener('push', event => {
  let data = { title: 'Barometer CRM', body: 'You have a new notification.' };
  try {
    data = event.data.json();
  } catch (e) {
    try { data.body = event.data.text(); } catch (_) {}
  }

  const options = {
    body: data.body || '',
    icon: data.icon || '/icons/icon-192.png',
    badge: data.badge || '/icons/badge-72.png',
    vibrate: data.vibrate || [200, 100, 200],
    timestamp: data.timestamp || Date.now(),
    requireInteraction: data.requireInteraction || false,
    tag: data.tag || 'crm-notification',
    renotify: data.renotify !== false,
    data: data.data || { url: '/' },
    actions: data.actions || []
  };

  event.waitUntil(
    self.registration.showNotification(data.title || 'Barometer CRM', options)
  );
});

// ── NOTIFICATION CLICK: open/focus CRM tab ────────
self.addEventListener('notificationclick', event => {
  event.notification.close();
  const url = event.notification.data?.url || '/';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
      for (const client of clientList) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          client.focus();
          client.navigate(self.location.origin + url);
          return;
        }
      }
      if (clients.openWindow) {
        return clients.openWindow(self.location.origin + url);
      }
    })
  );
});

// ── BACKGROUND SYNC ───────────────────────────────
self.addEventListener('sync', event => {
  if (event.tag === 'crm-sync') {
    // Placeholder for offline-to-online sync
    event.waitUntil(Promise.resolve());
  }
});
