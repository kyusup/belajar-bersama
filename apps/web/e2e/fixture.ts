import { readFileSync } from "node:fs";
import path from "node:path";
import type { E2EFixture } from "./helpers/api";

type StoredFixture = E2EFixture & { apiURL: string; webURL: string };

export function loadFixture(): StoredFixture {
  const raw = readFileSync(path.join(__dirname, ".fixture.json"), "utf8");
  return JSON.parse(raw) as StoredFixture;
}
