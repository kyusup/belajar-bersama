import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ProgressBar } from "./ProgressBar";

describe("ProgressBar", () => {
  it("exposes numeric progress, not color alone", () => {
    render(
      <ProgressBar percent={80} completed={8} total={10} label="Progres kursus" />,
    );
    expect(screen.getByText("Progres kursus: 8 / 10 (80%)")).toBeInTheDocument();
    expect(screen.getByRole("progressbar", { name: "Progres kursus" })).toHaveAttribute(
      "aria-valuenow",
      "80",
    );
  });
});
