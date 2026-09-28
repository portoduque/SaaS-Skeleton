import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import RootLayout, { metadata } from "./layout";

describe("RootLayout", () => {
  it("renders children in an English document shell", () => {
    const markup = renderToStaticMarkup(
      RootLayout({ children: <main>Starter content</main> }),
    );

    expect(markup).toContain('<html lang="en">');
    expect(markup).toContain("<body>");
    expect(markup).toContain("Starter content");
  });

  it("exposes the starter metadata", () => {
    expect(metadata.title).toBe("SaaS-Skeleton");
    expect(metadata.description).toBe("Secure, API-first SaaS foundation");
  });
});
