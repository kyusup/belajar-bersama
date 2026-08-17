import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AccountPanel } from "./AccountPanel";

vi.mock("@/lib/api/auth", () => ({
  fetchCurrentUser: () =>
    Promise.resolve({
      id: "11111111-1111-4111-8111-111111111111",
      displayName: "Siti",
      avatarUrl: null,
      status: "ACTIVE",
      roles: ["LEARNER"],
      storedRoles: ["LEARNER"],
      permissions: ["USER_READ_SELF"],
      identities: [
        { id: "id-1", provider: "GOOGLE", issuer: "https://accounts.google.com" },
      ],
      verifiedCompetencyIds: [],
    }),
  fetchCompetencies: () =>
    Promise.resolve([
      {
        id: "aaaaaaaa-0001-4000-8000-000000000001",
        slug: "matematika",
        name: "Mathematics",
        description: "Matematika",
      },
    ]),
}));

describe("AccountPanel", () => {
  it("renders authenticated account details", async () => {
    render(<AccountPanel />);
    expect(await screen.findByText("Siti")).toBeInTheDocument();
    expect(screen.getByText("GOOGLE")).toBeInTheDocument();
    expect(screen.getByText("LEARNER")).toBeInTheDocument();
    expect(
      screen.getByText("Belum ada kompetensi yang disetujui."),
    ).toBeInTheDocument();
  });
});
