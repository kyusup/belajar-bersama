import { writeFileSync } from "node:fs";
import path from "node:path";
import {
  ensurePublishedMaterialFixture,
  waitForHealth,
  type E2EFixture,
} from "./helpers/api";

const FIXTURE_PATH = path.join(__dirname, ".fixture.json");

async function globalSetup() {
  const webURL = process.env.E2E_BASE_URL ?? "http://localhost:3000";
  const apiURL = process.env.E2E_API_URL ?? "http://localhost:8080";

  await waitForHealth();

  const webStarted = Date.now();
  let webReady = false;
  while (Date.now() - webStarted < 120_000) {
    try {
      const response = await fetch(webURL);
      if (response.ok) {
        webReady = true;
        break;
      }
    } catch {
      // retry
    }
    await new Promise((resolve) => setTimeout(resolve, 1_000));
  }
  if (!webReady) {
    throw new Error(
      `Web app not reachable at ${webURL}. Start it with AUTH_DEV_LOGIN=true on the API and pnpm --filter web dev.`,
    );
  }

  const marker = Date.now().toString(36);
  const fixture: E2EFixture = await ensurePublishedMaterialFixture(marker);
  writeFileSync(FIXTURE_PATH, JSON.stringify({ ...fixture, apiURL, webURL }, null, 2));
}

export default globalSetup;
