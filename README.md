# GridSync

**Smart Solar Microgrid Trading System** — SE4040 Enterprise Application Development (Assignment 1).

GridSync connects solar prosumers with microgrid hubs so they can reserve energy charging / drop-off slots, while Backoffice and Grid Operators manage users, stations, schedules, and booking fulfilment through a central FAT web service.

**Repository:** [https://github.com/pereragp/GridSync](https://github.com/pereragp/GridSync)

---

## Architecture

```
┌─────────────────────┐     REST/JSON      ┌──────────────────────┐
│  Web App (React)    │ ─────────────────► │  C# Web API          │
│  Tailwind UI layer  │                    │  FAT service         │
└─────────────────────┘                    │  JWT auth            │
                                           │         │            │
┌─────────────────────┐                    │         ▼            │
│  Android (planned)  │ ─────────────────► │      MongoDB         │
│  Pure native + SQLite│                    └──────────────────────┘
└─────────────────────┘
```

Business logic lives in the API. Clients are UI layers only and talk to the service over REST.

---

## Tech stack

| Layer | Technology |
|--------|------------|
| Web API | ASP.NET Core (`.NET 10`), JWT, Swagger |
| Database | MongoDB (NoSQL) |
| Web frontend | React 19, Vite, React Router, Tailwind CSS 4 |
| Maps / QR | Google Maps JavaScript API, QR generation & scanning |
| Mobile (assignment) | Pure native Android + SQLite (no cross-platform frameworks) |

---

## Repository structure

```
GridSync/
├── Web/
│   ├── Backend/GridSync.Api/     # C# REST API (FAT service)
│   └── Frontend/                 # React admin / operator / prosumer UI
├── docs/                         # Requirement spec, DFD, use-case diagrams
├── README.md
└── package.json
```

---

## Features

### Roles

| Role | Access |
|------|--------|
| **Backoffice** | User administration, staff creation, station CRUD, activate / deactivate / reactivate nodes |
| **Grid Operator** | Station schedules & battery slots, reservation monitoring, QR verification / transfer completion |
| **Prosumer** | Self-registration (NIC), profile, energy slot reservations (create / update / cancel within rules) |

### Domain highlights

- **Microgrid stations** — GPS location, battery slots, kWh per battery, total capacity (`slots × kWh/battery`), operating schedule
- **Booking slots & reservations** — within 7 days; updates / cancellations need ≥ 12 hours’ notice
- **Station lifecycle** — deactivate blocked while active reservations exist; reactivate restores the node

---

## Prerequisites

- [.NET SDK 10](https://dotnet.microsoft.com/download) (see `Web/Backend/GridSync.Api/global.json`)
- [Node.js](https://nodejs.org/) 18+ and npm
- A MongoDB database (Atlas or local)
- Google Maps API key (Maps JavaScript API; Places optional as configured)

---

## Setup

### 1. Clone

```bash
git clone https://github.com/pereragp/GridSync.git
cd GridSync
```

### 2. Backend API

```bash
cd Web/Backend/GridSync.Api
cp .env.example .env
```

Edit `.env` and set at least:

```env
MongoDbSettings__ConnectionString=mongodb+srv://USER:PASS@CLUSTER.mongodb.net/?retryWrites=true&w=majority
MongoDbSettings__DatabaseName=GridSync
JwtSettings__Key=REPLACE_WITH_A_LONG_RANDOM_SECRET_AT_LEAST_32_CHARS
```

Optional: enable Gmail SMTP for password-reset emails (`EmailSettings__*` in `.env`).

Run:

```bash
dotnet restore
dotnet run
```

- API: `http://localhost:5269`
- Swagger UI: `http://localhost:5269/swagger`

### 3. Web frontend

```bash
cd Web/Frontend
cp .env.example .env
```

Edit `.env`:

```env
VITE_API_BASE_URL=http://localhost:5269
VITE_GOOGLE_MAPS_API_KEY=your_google_maps_api_key_here
```

Run:

```bash
npm install
npm run dev
```

App: `http://localhost:5173`

### 4. Android mobile (prosumer)

```bash
cd Mobile
```

Add your Maps SDK key to `local.properties` (see `local.properties.example`):

```properties
MAPS_API_KEY=your_google_maps_android_api_key_here
```

Enable **Maps SDK for Android** for that key in Google Cloud Console. Then open the project in Android Studio and run on an emulator/device with Google Play services. The Home tab loads nearby stations from `GET /api/stations/nearby` onto Google Maps (Colombo fallback if location is denied).

---

## API overview

| Area | Base path |
|------|-----------|
| Auth | `/api/Auth` |
| Users | `/api/Users` |
| Stations | `/api/Stations` |
| Booking slots | `/api/BookingSlots` |
| Reservations | `/api/Reservations` |

Full request/response shapes are documented in Swagger while the API is running.

---

## Documentation

| Document | Path |
|----------|------|
| Requirement specification | [`docs/Requirement_Specification.html`](docs/Requirement_Specification.html) |
| Use case diagram | [`docs/usecase-gridsync.html`](docs/usecase-gridsync.html) |
| DFD | [`docs/dfd-gridsync.html`](docs/dfd-gridsync.html) |

---

## Demo video

> Assignment requirement: a video of **no more than 5 minutes** explaining how the application works.

**Video link:** _[Add YouTube or OneDrive link here]_

---

## Assignment notes

- Clients must not hold core business logic; enforce rules in the API.
- Deploy the Web API on **IIS** for the final demo as required by the module brief.
- Do not commit `.env` files or secrets (already covered by `.gitignore`).

---

## License

Academic coursework project for SLIIT SE4040 — not licensed for production redistribution.
