# ColdChain Monitor (frontend, demo mode)

React + TypeScript + Vite + Tailwind CSS + React Router + Lucide.

    npm install
    npm run dev     # http://localhost:5173
    npm run build   # type-check + production build

Demo only: users admin, warehouse, transport, driver, hospital, supplier, inspector; password `Admin@123`; MFA code `123456`.

Backend hookup: replace the functions in `src/services/authService.ts` and `dashboardService.ts` with fetch calls to `/api/auth/*` and `/api/dashboard/*`. Demo data lives in `src/data/`.
