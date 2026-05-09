/**
 * Barometer CRM — Runtime Configuration
 *
 * DEVELOPMENT:  Leave apiBase as 'http://localhost:8080'
 * PRODUCTION:   Change apiBase to your deployed backend URL
 *               e.g. 'https://barometer-crm-api.onrender.com'
 *               or   'https://api.yourdomain.com'
 *
 * If the frontend and backend are served from the SAME domain/port
 * (e.g. Nginx proxy), set apiBase to '' (empty string).
 */
window.CRM_CONFIG = {
  apiBase: 'http://localhost:8080'
};
