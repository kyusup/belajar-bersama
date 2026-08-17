import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { PlatformStatusView } from "./PlatformStatusView";

describe("PlatformStatusView", () => {
  it("shows unreachable copy when the API is down", () => {
    render(
      <PlatformStatusView
        status={null}
        error="unreachable"
        loading={false}
        onRetry={vi.fn()}
      />,
    );
    expect(screen.getByText("API tidak dapat dijangkau")).toBeInTheDocument();
  });

  it("renders component statuses from the API payload", () => {
    render(
      <PlatformStatusView
        loading={false}
        error={null}
        onRetry={vi.fn()}
        status={{
          service: "belajar-bersama-api",
          version: "0.1.0",
          status: "UP",
          components: {
            database: { status: "UP", provider: "postgresql" },
            storage: { status: "UP", provider: "memory" },
            search: { status: "UP", provider: "postgres" },
          },
        }}
      />,
    );
    expect(screen.getByText("API dapat dijangkau")).toBeInTheDocument();
    expect(screen.getByText(/postgresql/)).toBeInTheDocument();
  });
});
