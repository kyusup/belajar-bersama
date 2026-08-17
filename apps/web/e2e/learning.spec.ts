import { test, expect } from "@playwright/test";
import { copy } from "../src/lib/i18n/id";
import { loadFixture } from "./fixture";

test.describe("Learning", () => {
  test("anonymous can read published material from fixture", async ({ page }) => {
    const fixture = loadFixture();
    await page.goto(`/materi/${fixture.materialSlug}`);
    await expect(
      page.getByRole("heading", { name: fixture.materialTitle }),
    ).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.getByText(`Paragraf ${fixture.marker} untuk uji E2E.`),
    ).toBeVisible();
  });

  test("signed-in learner can bookmark material and ask on Q&A board", async ({
    page,
  }) => {
    const fixture = loadFixture();
    const subject = `e2e-qa-${Date.now()}`;

    await page.goto("/masuk");
    const devForm = page.locator("form").filter({ hasText: copy.devLoginTitle });
    await expect(devForm).toBeVisible({ timeout: 20_000 });
    await devForm.getByLabel(copy.devSubject).fill(subject);
    await devForm.getByLabel(copy.devName).fill("Penanya E2E");
    await devForm.getByRole("button", { name: copy.submitDevLogin }).click();
    await expect(page).toHaveURL(/\/akun$/);

    await page.goto(`/materi/${fixture.materialSlug}`);
    await expect(
      page.getByRole("heading", { name: fixture.materialTitle }),
    ).toBeVisible();
    await page.getByRole("button", { name: copy.addBookmark }).click();
    await expect(page.getByRole("button", { name: copy.removeBookmark })).toBeVisible();

    const qaTitle = `Pertanyaan E2E ${fixture.marker}`;
    await page.goto("/tanya");
    await page.getByLabel(copy.title).fill(qaTitle);
    await page.getByLabel(copy.body).fill("Apakah materi ini sudah jelas?");
    await page.getByRole("button", { name: copy.qaAsk }).click();

    await expect(page).toHaveURL(/\/tanya\/.+/);
    await expect(page.getByRole("heading", { name: qaTitle })).toBeVisible();
  });
});
