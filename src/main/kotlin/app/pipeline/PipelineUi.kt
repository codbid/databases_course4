package com.example.app.pipeline

object PipelineUi {
    fun html(): String = """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <title>Library Event Pipeline</title>
          <style>
            :root {
              --bg: #f4efe7;
              --ink: #182126;
              --muted: #5b676f;
              --panel: rgba(255,255,255,0.72);
              --line: rgba(24,33,38,0.12);
              --accent: #d66a2a;
              --accent-2: #1c6f64;
              --good: #1f8f5f;
              --bad: #b84a33;
              --shadow: 0 20px 60px rgba(29, 36, 42, 0.12);
              --mono: "JetBrains Mono", "Fira Code", monospace;
              --sans: "Manrope", "Segoe UI", sans-serif;
            }
            * { box-sizing: border-box; }
            body {
              margin: 0;
              font-family: var(--sans);
              color: var(--ink);
              background:
                radial-gradient(circle at top left, rgba(214,106,42,0.18), transparent 28%),
                radial-gradient(circle at top right, rgba(28,111,100,0.18), transparent 26%),
                linear-gradient(180deg, #faf6f0 0%, var(--bg) 100%);
              min-height: 100vh;
            }
            header {
              padding: 48px 24px 18px;
            }
            .hero {
              max-width: 1240px;
              margin: 0 auto;
              display: grid;
              grid-template-columns: 1.4fr 0.9fr;
              gap: 28px;
              align-items: end;
            }
            .hero h1 {
              font-size: clamp(2.4rem, 5vw, 5.4rem);
              line-height: 0.92;
              margin: 0 0 12px;
              letter-spacing: -0.06em;
            }
            .hero p {
              max-width: 42rem;
              color: var(--muted);
              margin: 0;
              font-size: 1.05rem;
            }
            .hero-note {
              padding: 18px 20px;
              background: rgba(24,33,38,0.92);
              color: #f3ede3;
              border-radius: 24px;
              box-shadow: var(--shadow);
            }
            .hero-note strong {
              display: block;
              margin-bottom: 8px;
              font-size: 0.92rem;
              letter-spacing: 0.08em;
              text-transform: uppercase;
              color: #f0b287;
            }
            main {
              max-width: 1240px;
              margin: 0 auto;
              padding: 0 24px 40px;
              display: grid;
              gap: 18px;
            }
            .grid {
              display: grid;
              gap: 18px;
              grid-template-columns: repeat(12, minmax(0, 1fr));
            }
            .panel {
              background: var(--panel);
              backdrop-filter: blur(18px);
              border: 1px solid var(--line);
              border-radius: 28px;
              box-shadow: var(--shadow);
              overflow: hidden;
            }
            .panel .inner { padding: 22px; }
            .span-8 { grid-column: span 8; }
            .span-4 { grid-column: span 4; }
            .span-6 { grid-column: span 6; }
            .span-12 { grid-column: span 12; }
            h2 {
              margin: 0 0 6px;
              font-size: 1.3rem;
              letter-spacing: -0.03em;
            }
            .sub {
              color: var(--muted);
              font-size: 0.95rem;
              margin-bottom: 18px;
            }
            .stats {
              display: grid;
              grid-template-columns: repeat(4, minmax(0, 1fr));
              gap: 12px;
            }
            .stat {
              padding: 18px;
              border-radius: 20px;
              background: rgba(255,255,255,0.7);
              border: 1px solid rgba(24,33,38,0.08);
            }
            .stat label {
              display: block;
              color: var(--muted);
              font-size: 0.82rem;
              margin-bottom: 6px;
              text-transform: uppercase;
              letter-spacing: 0.06em;
            }
            .stat strong {
              font-size: 1.6rem;
              letter-spacing: -0.04em;
            }
            table {
              width: 100%;
              border-collapse: collapse;
              font-size: 0.94rem;
            }
            th, td {
              padding: 10px 12px;
              text-align: left;
              border-bottom: 1px solid rgba(24,33,38,0.08);
              vertical-align: top;
            }
            th {
              color: var(--muted);
              font-size: 0.78rem;
              text-transform: uppercase;
              letter-spacing: 0.08em;
            }
            code, pre {
              font-family: var(--mono);
              font-size: 0.86rem;
            }
            pre {
              white-space: pre-wrap;
              word-break: break-word;
              background: rgba(24,33,38,0.92);
              color: #eef1f2;
              padding: 16px;
              border-radius: 18px;
              max-height: 340px;
              overflow: auto;
            }
            .row {
              display: grid;
              grid-template-columns: repeat(2, minmax(0, 1fr));
              gap: 12px;
            }
            .field {
              display: grid;
              gap: 6px;
              margin-bottom: 12px;
            }
            .field label {
              color: var(--muted);
              font-size: 0.82rem;
              text-transform: uppercase;
              letter-spacing: 0.06em;
            }
            input, select, textarea, button {
              font: inherit;
            }
            input, select, textarea {
              width: 100%;
              padding: 12px 14px;
              border-radius: 16px;
              border: 1px solid rgba(24,33,38,0.12);
              background: rgba(255,255,255,0.84);
              color: var(--ink);
            }
            textarea { min-height: 120px; resize: vertical; }
            .actions {
              display: flex;
              flex-wrap: wrap;
              gap: 10px;
              margin-top: 8px;
            }
            button {
              border: 0;
              border-radius: 999px;
              padding: 12px 18px;
              cursor: pointer;
              transition: transform 180ms ease, opacity 180ms ease;
            }
            button:hover { transform: translateY(-1px); }
            .primary { background: var(--ink); color: white; }
            .accent { background: var(--accent); color: white; }
            .secondary { background: rgba(24,33,38,0.08); color: var(--ink); }
            .badge {
              display: inline-flex;
              align-items: center;
              gap: 8px;
              padding: 8px 12px;
              border-radius: 999px;
              font-size: 0.82rem;
              background: rgba(31,143,95,0.12);
              color: var(--good);
            }
            .badge.bad { background: rgba(184,74,51,0.12); color: var(--bad); }
            .topic-tabs {
              display: flex;
              flex-wrap: wrap;
              gap: 8px;
              margin-bottom: 12px;
            }
            .topic-tabs button {
              padding: 10px 14px;
              border-radius: 999px;
              background: rgba(24,33,38,0.06);
            }
            .topic-tabs button.active {
              background: var(--accent-2);
              color: white;
            }
            .muted { color: var(--muted); }
            @media (max-width: 980px) {
              .hero, .stats, .row, .grid { grid-template-columns: 1fr; }
              .span-8, .span-4, .span-6, .span-12 { grid-column: auto; }
            }
          </style>
        </head>
        <body>
          <header>
            <div class="hero">
              <div>
                <h1>Library Event Pipeline</h1>
                <p>Ручная проверка всего контура в одном месте: событие backend, Kafka topic, Kafka Streams, Kafka Connect, PostgreSQL sink и ClickHouse analytics.</p>
              </div>
              <div class="hero-note">
                <strong>What This UI Is For</strong>
                Проверить, что события реально проходят через пайплайн, не лезя каждый раз в `docker exec`, `clickhouse-client` и REST Kafka Connect вручную.
              </div>
            </div>
          </header>
          <main>
            <section class="panel">
              <div class="inner">
                <h2>Pipeline Overview</h2>
                <div class="sub">Сводка по топикам, ClickHouse и sink-таблице в PostgreSQL.</div>
                <div id="overview-stats" class="stats"></div>
              </div>
            </section>

            <section class="grid">
              <article class="panel span-8">
                <div class="inner">
                  <h2>Manual Event Publisher</h2>
                  <div class="sub">Отправьте тестовое событие в `library.book-events` и сразу проверьте, как оно проходит по всему потоку.</div>
                  <form id="event-form">
                    <div class="row">
                      <div class="field">
                        <label>Event Type</label>
                        <select name="eventType">
                          <option>BookLoaned</option>
                          <option>ReservationCreated</option>
                          <option>BookIssued</option>
                        </select>
                      </div>
                      <div class="field">
                        <label>Status</label>
                        <input name="status" value="ACTIVE" />
                      </div>
                    </div>
                    <div class="row">
                      <div class="field">
                        <label>Office ID</label>
                        <input name="officeId" type="number" value="3" />
                      </div>
                      <div class="field">
                        <label>Client ID</label>
                        <input name="clientId" type="number" value="101" />
                      </div>
                    </div>
                    <div class="row">
                      <div class="field">
                        <label>Book ID</label>
                        <input name="bookId" type="number" value="2001" />
                      </div>
                      <div class="field">
                        <label>Book Copy ID</label>
                        <input name="bookCopyId" type="number" value="7001" />
                      </div>
                    </div>
                    <div class="actions">
                      <button class="primary" type="submit">Send Event</button>
                      <button class="secondary" type="button" id="seed-topics">Ensure Topics</button>
                      <button class="accent" type="button" id="register-connector">Register Connector</button>
                    </div>
                  </form>
                  <pre id="event-result">Ready.</pre>
                </div>
              </article>

              <article class="panel span-4">
                <div class="inner">
                  <h2>Connector Status</h2>
                  <div class="sub">Kafka Connect sink для результатов Kafka Streams.</div>
                  <div id="connector-status"></div>
                </div>
              </article>
            </section>

            <section class="grid">
              <article class="panel span-7">
                <div class="inner">
                  <h2>Topic Inspector</h2>
                  <div class="sub">Последние сообщения в raw topic, stream topic и DLQ.</div>
                  <div class="topic-tabs">
                    <button type="button" data-topic="library.book-events" class="active">book-events</button>
                    <button type="button" data-topic="library.book-stats-hourly">stream-output</button>
                    <button type="button" data-topic="library.book-events.dlq">dlq</button>
                  </div>
                  <pre id="topic-messages">Loading…</pre>
                </div>
              </article>
              <article class="panel span-5">
                <div class="inner">
                  <h2>PostgreSQL Sink Sample</h2>
                  <div class="sub">Данные, которые Kafka Connect записал из stream topic в таблицу `book_stats_hourly`.</div>
                  <pre id="postgres-sink">Loading…</pre>
                </div>
              </article>
            </section>

            <section class="grid">
              <article class="panel span-12">
                <div class="inner">
                  <h2>ClickHouse Query Console</h2>
                  <div class="sub">Ручная проверка raw таблицы и витрины без выхода из UI.</div>
                  <div class="field">
                    <label>SQL</label>
                    <textarea id="query-input">SELECT event_type, count() AS events FROM library_analytics.book_events GROUP BY event_type ORDER BY events DESC</textarea>
                  </div>
                  <div class="actions">
                    <button class="primary" type="button" id="run-query">Run Query</button>
                    <button class="secondary" type="button" id="load-overview">Refresh Overview</button>
                  </div>
                  <pre id="query-result">Waiting for query.</pre>
                </div>
              </article>
            </section>
          </main>

          <script>
            const state = { topic: "library.book-events" };

            async function api(url, options = {}) {
              const response = await fetch(url, {
                headers: { "Content-Type": "application/json" },
                ...options
              });
              if (!response.ok) {
                throw new Error(await response.text());
              }
              return response.json();
            }

            function pretty(value) {
              return JSON.stringify(value, null, 2);
            }

            function renderOverview(data) {
              const stats = document.getElementById("overview-stats");
              const topics = data.kafka || [];
              const click = data.clickHouse || {};
              const sink = data.postgresSink || {};
              stats.innerHTML = `
                <div class="stat"><label>Raw Topic</label><strong>${'$'}{topics.find(t => t.topic === "library.book-events")?.totalMessages ?? 0}</strong></div>
                <div class="stat"><label>Stream Topic</label><strong>${'$'}{topics.find(t => t.topic === "library.book-stats-hourly")?.totalMessages ?? 0}</strong></div>
                <div class="stat"><label>ClickHouse Rows</label><strong>${'$'}{click.book_events ?? "n/a"}</strong></div>
                <div class="stat"><label>Postgres Sink Rows</label><strong>${'$'}{sink.rowCount ?? "n/a"}</strong></div>
              `;
              const connector = document.getElementById("connector-status");
              const statusCode = data.connectorStatus?.statusCode;
              const body = data.connectorStatus?.body || data.connectorStatus;
              const ok = statusCode && statusCode >= 200 && statusCode < 300;
              connector.innerHTML = `
                <div class="badge ${'$'}{ok ? "" : "bad"}">${'$'}{ok ? "Connector reachable" : "Connector needs attention"}</div>
                <pre>${'$'}{typeof body === "string" ? body : pretty(body)}</pre>
              `;
              document.getElementById("postgres-sink").textContent = pretty(sink);
            }

            async function loadOverview() {
              renderOverview(await api("/api/pipeline/overview"));
            }

            async function loadMessages() {
              const data = await api(`/api/pipeline/messages?topic=${'$'}{encodeURIComponent(state.topic)}`);
              document.getElementById("topic-messages").textContent = pretty(data);
            }

            async function runQuery() {
              const sql = document.getElementById("query-input").value;
              const data = await api("/api/pipeline/clickhouse/query", {
                method: "POST",
                body: JSON.stringify({ sql })
              });
              document.getElementById("query-result").textContent = pretty(data);
            }

            document.getElementById("event-form").addEventListener("submit", async (event) => {
              event.preventDefault();
              const form = new FormData(event.target);
              const payload = Object.fromEntries(form.entries());
              ["officeId", "clientId", "bookId", "bookCopyId"].forEach((key) => {
                if (payload[key] !== undefined && payload[key] !== "") payload[key] = Number(payload[key]);
              });
              const data = await api("/api/pipeline/events/manual", {
                method: "POST",
                body: JSON.stringify(payload)
              });
              document.getElementById("event-result").textContent = pretty(data);
              setTimeout(() => {
                loadOverview();
                loadMessages();
              }, 900);
            });

            document.getElementById("seed-topics").addEventListener("click", async () => {
              document.getElementById("event-result").textContent = pretty(await api("/api/pipeline/topics/ensure", { method: "POST" }));
              await loadOverview();
            });

            document.getElementById("register-connector").addEventListener("click", async () => {
              document.getElementById("event-result").textContent = pretty(await api("/api/pipeline/connect/register", { method: "POST" }));
              await loadOverview();
            });

            document.getElementById("run-query").addEventListener("click", runQuery);
            document.getElementById("load-overview").addEventListener("click", loadOverview);

            document.querySelectorAll("[data-topic]").forEach((button) => {
              button.addEventListener("click", async () => {
                state.topic = button.dataset.topic;
                document.querySelectorAll("[data-topic]").forEach((item) => item.classList.remove("active"));
                button.classList.add("active");
                await loadMessages();
              });
            });

            loadOverview();
            loadMessages();
          </script>
        </body>
        </html>
    """.trimIndent()
}
