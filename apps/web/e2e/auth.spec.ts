import { test, expect } from "@playwright/test";
import { copy } from "../src/lib/i18n/id";

test.describe("Authentication", () => {
  test("dev login reaches account page", async ({ page }) => {
    await page.goto("/masuk");
    await expect(page.getByRole("heading", { name: copy.loginTitle })).toBeVisible();

    const devForm = page.locator("form").filter({ hasText: copy.devLoginTitle });
    await expect(devForm).toBeVisible({ timeout: 20_000 });

    const subject = `e2e-ui-${Date.now()}`;
    await devForm.getByLabel(copy.devSubject).fill(subject);
    await devForm.getByLabel(copy.devName).fill("Pengguna E2E");
    await devForm.getByRole("button", { name: copy.submitDevLogin }).click();

    await expect(page).toHaveURL(/\/akun$/);
    await expect(page.getByRole("heading", { name: copy.accountTitle })).toBeVisible();
    await expect(page.getByText("Pengguna E2E")).toBeVisible();
  });
});
