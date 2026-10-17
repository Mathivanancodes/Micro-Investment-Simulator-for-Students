# ApexTrade frontend

React 19 and Vite 8 interface for the student micro-investment simulator. All balances and prices use virtual INR. No external fonts or paid market APIs are required.

## Run locally

Use Node.js 22.12 or newer and npm. Start the Spring Boot API on port 8080 first, then:

```sh
cd frontend
npm ci
npm run dev
```

Open the URL printed by Vite, normally `http://localhost:5173`. Development requests to `/api` are proxied to `http://127.0.0.1:8080`, so the browser uses one origin. Set the shell variable `VITE_PROXY_TARGET` when the local API uses another port, for example `VITE_PROXY_TARGET=http://127.0.0.1:8081 npm run dev`. For another browser-facing API origin, set `VITE_API_URL` before starting or building, and configure the backend's allowed CORS origins. Never place credentials in a `VITE_` variable: Vite includes those values in browser code.

```sh
npm run lint
npm run build
npm run preview
```

The preview server serves the production build. In deployment, serve `dist/` with an SPA fallback to `index.html` and forward `/api` to the backend, or build with `VITE_API_URL` pointing at the API. The Vite development proxy is only active for `npm run dev`.

## Features

- Email/password registration and sign-in, with a ₹1,000 initial virtual wallet.
- Wallet balance and virtual deposits, with two-decimal amount validation.
- Searchable stocks and seven-day simulated price charts.
- Buy ₹10–₹500 per order; sell quantities with up to four decimal places.
- Portfolio cash, holdings, average cost, current value, unrealised profit/loss, history, and allocation.
- Paginated deposit, buy, and sell transactions.
- Student leaderboard and contextual learning tips.
- Automatic price and portfolio refresh every 30 seconds while the page is visible; in-flight reads and timers are cleaned up on navigation.
- Responsive layouts, labelled controls, keyboard-operable stock selection, visible focus states, and recoverable loading/error states.

## Structure

```text
src/
  components/  Navigation, stat cards, and reusable charts
  hooks/       Abortable reads, retry, and background refresh
  pages/       Authentication, dashboard, wallet, trading, history, rankings
  services/    API contracts and session handling
  styles/      Shared page primitives
  utils/       INR, quantity, date, and percentage formatting
  assets/      Educational tips and static assets
```

The API service sends the bearer token for protected requests. A protected request returning HTTP 401 ends the session; temporary network/server errors keep the session available for retry. Sign-out removes only this application's authentication keys.

## Validation

`npm run lint` checks the source and `npm run build` verifies the production bundle. The repository's API and browser regression checks exercise the integrated backend and frontend; see the root README for those commands and requirements.
