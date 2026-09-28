export default function Home() {
  return (
    <main className="shell">
      <section className="hero" aria-labelledby="page-title">
        <p className="eyebrow">SaaS foundation</p>
        <h1 id="page-title">SaaS-Skeleton</h1>
        <p className="summary">
          A secure, API-first starter designed to stay simple now and scale when
          the product actually needs it.
        </p>

        <dl className="stack" aria-label="Foundation stack">
          <div>
            <dt>Backend</dt>
            <dd>Java 25 + Spring Boot</dd>
          </div>
          <div>
            <dt>Frontend</dt>
            <dd>Next.js + React + TypeScript</dd>
          </div>
          <div>
            <dt>Database</dt>
            <dd>PostgreSQL</dd>
          </div>
        </dl>
      </section>
    </main>
  );
}
