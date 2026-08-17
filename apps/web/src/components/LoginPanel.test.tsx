import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LoginPanel } from "./LoginPanel";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
}));

vi.mock("@/lib/api/auth", () => ({
  fetchAuthConfig: () =>
    Promise.resolve({ google: false, apple: false, devLogin: true }),
  startProviderLogin: (provider: string) => `/start/${provider}`,
  devLogin: vi.fn(),
}));

describe("LoginPanel", () => {
  it("shows development login when enabled", async () => {
    render(<LoginPanel />);
    expect(await screen.findByText("Masuk pengembangan")).toBeInTheDocument();
    expect(screen.getByText(/Masuk dengan Google/)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Masuk (pengembangan)" }),
    ).toBeInTheDocument();
  });
});
