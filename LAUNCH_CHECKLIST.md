# bueno-ws Launch Checklist

## Domains

- Hauptdomain: `https://bueno-ws.ch`
- `https://www.bueno-ws.ch` per Redirect auf `https://bueno-ws.ch`
- API: `https://api.bueno-ws.ch`
- Frontend `VITE_API_BASE_URL=https://api.bueno-ws.ch`

## Backend Production Environment

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://<rds-private-endpoint>:5432/<db>`
- `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `HASH_SALT`, `EMAIL_PWD` nur über Secrets/Parameter Store setzen
- `APP_CORS_ALLOWED_ORIGINS=https://bueno-ws.ch,https://www.bueno-ws.ch`
- `APP_JWT_ISSUER=https://bueno-ws.ch`
- `APP_PUBLIC_BASE_URL=https://api.bueno-ws.ch`
- `APP_SECURITY_REQUIRE_HTTPS=true`
- `SERVER_FORWARD_HEADERS_STRATEGY=framework` nur verwenden, wenn die EC2/API nur vom eigenen Proxy/Load Balancer erreichbar ist
- `SPRING_SESSION_JDBC_INITIALIZE_SCHEMA=never` in Production; benötigte Tabellen kontrolliert anlegen
- Keine Secrets loggen, kein Debug-Logging in Production aktivieren

## AWS Tasks

- RDS PostgreSQL in privatem Subnet/VPC erstellen; nicht öffentlich erreichbar
- Security Groups so setzen, dass nur Backend/EC2 auf RDS zugreifen kann
- RDS automatische Backups mit 7 Tagen Retention aktivieren
- Vor jedem grösseren Deployment manuellen RDS Snapshot erstellen
- Restore-Test regelmässig einplanen
- EC2 nur über notwendige Ports erreichbar machen
- SSL/TLS für `bueno-ws.ch`, `www.bueno-ws.ch` und `api.bueno-ws.ch` einrichten
- HTTP zu HTTPS Redirect einrichten
- `www` zu non-`www` Redirect einrichten
- CloudFront/S3 SPA-Fallback konfigurieren: 403/404 auf `/index.html`, HTTP Status 200 für SPA-Routen
- Uptime-Monitoring auf `https://api.bueno-ws.ch/actuator/health` einrichten

## Deployment Steps

- Backend testen: `.\mvnw.cmd test`
- Backend bauen: `.\mvnw.cmd package`
- Frontend `.env.production` setzen: `VITE_API_BASE_URL=https://api.bueno-ws.ch`
- Frontend testen/bauen: `npm run build`
- Frontend `dist/` nach S3 deployen und CloudFront invalidieren
- Backend Artefakt auf EC2 deployen und Service neu starten
- Datenbank-Schema vor Production-Start prüfen; Docker Compose ist nur für lokale Entwicklung

## Admin

- Kein hardcoded Admin-Seed und keine Standard-Zugangsdaten verwenden
- Admin manuell in RDS pflegen
- Benötigte Authority: `ROLE_ADMIN`

## Smoke Tests

- Startseite lädt
- About-Seite lädt
- Services-Seite lädt
- Contact-Seite lädt
- Legal-Seite lädt
- Direkter Reload funktioniert für `/`, `/about`, `/services`, `/contact`, `/legal`, `/auth/login`, `/auth/register`, `/auth/reset-password`, `/account`
- Login funktioniert
- Registrierung funktioniert
- OTP-Mail kommt an
- OTP-Mail ist Deutsch bei deutscher Website-Sprache
- OTP-Mail ist Englisch bei englischer Website-Sprache
- Passwort-Reset funktioniert
- Logout löscht `access_token` und `refresh_token` und invalidiert serverseitig den Refresh Token
- Kontaktformular funktioniert
- Admin sieht Kontaktanfragen
- Normaler User kommt nicht in Admin-Bereich oder Admin-Endpunkte
- Nicht eingeloggter User wird korrekt zu Login umgeleitet
- Mobile Ansicht prüfen
- Browser Console ohne rote Fehler
- Backend Logs ohne kritische Fehler
- CORS funktioniert mit `https://bueno-ws.ch` und `https://www.bueno-ws.ch`
- Fremde Origin wird blockiert
- Cookies haben passende Flags: `HttpOnly`, `Secure`, `SameSite=Lax`, `Path`, `Max-Age`
- Refresh Flow funktioniert
- Logout verhindert anschliessenden Refresh
