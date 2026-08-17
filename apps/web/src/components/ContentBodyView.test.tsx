import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ContentBodyView } from "./ContentBodyView";

describe("ContentBodyView", () => {
  it("renders structured blocks as text, not raw HTML", () => {
    render(
      <ContentBodyView
        body={{
          blocks: [
            { type: "heading", level: 2, text: "Persamaan linear" },
            { type: "paragraph", text: "ax + b = 0" },
          ],
        }}
      />,
    );
    expect(
      screen.getByRole("heading", { name: "Persamaan linear" }),
    ).toBeInTheDocument();
    expect(screen.getByText("ax + b = 0")).toBeInTheDocument();
  });
});
