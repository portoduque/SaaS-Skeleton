import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import Home from "./page";

describe("Home", () => {
  it("renders the starter identity and core stack", () => {
    render(<Home />);

    expect(
      screen.getByRole("heading", { level: 1, name: "SaaS-Skeleton" }),
    ).toBeTruthy();
    expect(screen.getByText("Java 25 + Spring Boot")).toBeTruthy();
    expect(screen.getByText("Next.js + React + TypeScript")).toBeTruthy();
    expect(screen.getByText("PostgreSQL")).toBeTruthy();
  });
});
