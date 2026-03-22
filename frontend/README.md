# Mindful Finance Frontend (Milestone 6)

Minimal calm UI for viewing truth + peace outputs from the backend.

## Commands

- `npm install`
- `npm run dev`
- `npm run build`
- `npm run lint`

## API configuration

- `VITE_API_BASE_URL` controls API base URL.
- Default is `/api` so Vite can proxy to the Spring Boot backend in dev.

## API client slice (Step 2)

- Typed DTOs: `src/api/types.ts`
- Fetch wrapper + error model: `src/api/http.ts`
- Endpoint methods: `src/api/client.ts`
- Public exports: `src/api/index.ts`

Covered read endpoints:

- `GET /accounts`
- `GET /accounts/{accountId}/transactions`
- `GET /accounts/{accountId}/balance`
- `GET /net-worth`
- `GET /peace/monthly-burn?asOf=YYYY-MM-DD`
- `GET /peace/monthly-savings?asOf=YYYY-MM-DD`

Covered write endpoints:

- `POST /accounts`
- `PUT /accounts/{accountId}`
- `DELETE /accounts/{accountId}`
- `POST /accounts/{accountId}/transactions`
- `POST /imports/transactions/csv` (multipart form data: `accountId` + `file`)
