# Appium tests — Belajar Bersama

Mobile-web smoke tests for the Dockerized local stack using [Appium 2](https://appium.io/) and WebdriverIO.

## Prerequisites

1. Full stack running:

```bash
docker compose up -d --build
```

Wait until `http://localhost:3000/status` shows **API dapat dijangkau**.

2. Appium service (included in Compose):

```bash
curl -s http://localhost:4723/status
```

## Install (first time)

```bash
cd testing/appium
npm install
cp .env.example .env
```

## Run

```bash
# Appium health only (no browser)
npm run test:ping

# Mobile-emulated Chrome via Appium (default, no Android emulator required)
npm test

# Explicit smoke spec
npm run test:smoke
```

Environment variables (see `.env.example`):

| Variable | Default | Purpose |
|---|---|---|
| `APPIUM_HOST` | `localhost` | Appium server from Compose |
| `APPIUM_PORT` | `4723` | Appium port |
| `WEB_BASE_URL` | `http://localhost:3000` | Web URL (browser on host) |
| `RUN_MOBILE_TESTS` | `false` | `true` = real Android emulator via UiAutomator2 |

## Real Android emulator (optional)

Heavy profile — requires KVM and ~8 GB RAM:

```bash
docker compose --profile mobile up -d android
# noVNC: http://localhost:6080
export RUN_MOBILE_TESTS=true
export APPIUM_PORT=4724
npm run test:smoke
```

From the emulator, the web app is reached via `http://host.docker.internal:3000` (configure in specs if needed).

## Reports

WebdriverIO writes logs to the terminal. Add `reporters` in `wdio.conf.ts` for HTML/Allure if required later.
