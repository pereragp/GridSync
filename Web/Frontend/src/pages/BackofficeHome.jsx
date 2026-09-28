import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { approveUser, getPendingUsers, getUsers } from "../api/users";
import { getStations } from "../api/stations";
import { useAuth } from "../context/AuthContext";
import PageBleedHero from "../components/PageBleedHero";
import { Alert } from "../components/ui";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1466611653911-95081537e5b7?auto=format&fit=crop&w=2000&q=80";

export default function BackofficeHome() {
  const { user } = useAuth();
  const firstName = user.fullName?.split(" ")[0] || "Officer";

  const [pending, setPending] = useState([]);
  const [userCount, setUserCount] = useState(null);
  const [stationCount, setStationCount] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState("");

  const load = useCallback(async () => {
    setError("");
    setLoading(true);
    try {
      const [pend, all, stations] = await Promise.all([
        getPendingUsers(),
        getUsers(),
        getStations().catch(() => []),
      ]);
      setPending(pend || []);
      setUserCount((all || []).length);
      setStationCount((stations || []).length);
    } catch (err) {
      setError(err.message || "Failed to load overview");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const awaiting = useMemo(() => pending.slice(0, 4), [pending]);

  async function approve(id, name) {
    setMessage("");
    setError("");
    setBusyId(id);
    try {
      await approveUser(id);
      setMessage(`Approved ${name}. They can sign in as an active prosumer.`);
      await load();
    } catch (err) {
      setError(err.message || "Approval failed");
    } finally {
      setBusyId("");
    }
  }

  return (
    <div>
      <PageBleedHero
        size="home"
        image={HERO_IMAGE}
        imageAlt="Wind turbines across a renewable energy landscape"
        eyebrow="GridSync · Backoffice"
        title={`Good to see you, ${firstName}`}
        subtitle="Approve new prosumers, staff the network, and keep microgrid nodes running."
        actions={
          <>
            <a
              href="#approvals"
              className="inline-flex items-center gap-2 rounded-lg bg-white px-5 py-2.5 text-sm font-semibold text-grid-800 transition hover:bg-grid-50"
            >
              Review approvals
              {pending.length > 0 ? (
                <span className="rounded-md bg-amber-500 px-1.5 py-0.5 text-[11px] font-bold text-white">
                  {pending.length}
                </span>
              ) : null}
            </a>
            <Link
              to="/stations"
              className="inline-flex items-center rounded-lg border border-white/30 bg-white/10 px-5 py-2.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/18"
            >
              Microgrid nodes
            </Link>
          </>
        }
      />

      <div className="space-y-10">
      {error ? (
        <Alert type="error" onDismiss={() => setError("")}>
          {error}
        </Alert>
      ) : null}
      {message ? (
        <Alert type="success" onDismiss={() => setMessage("")}>
          {message}
        </Alert>
      ) : null}

      {/* Major paths */}
      <section
        className="gs-animate-fade-up"
        style={{ animationDelay: "100ms" }}
        aria-labelledby="backoffice-paths"
      >
        <div className="mb-5">
          <h2
            id="backoffice-paths"
            className="font-display text-2xl font-semibold tracking-tight text-grid-900"
          >
            Your console
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            Three places that cover the Backoffice role.
          </p>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <PathCard
            to="/backoffice/users"
            eyebrow="Accounts"
            title="Users & approvals"
            detail="Activate prosumers, deactivate or reactivate accounts, and search the directory."
            meta={
              loading
                ? "…"
                : `${userCount ?? 0} users · ${pending.length} pending`
            }
            tone="amber"
          />
          <PathCard
            to="/stations"
            eyebrow="Network"
            title="Microgrid nodes"
            detail="Register hubs, set capacity and GPS, and maintain operating schedules."
            meta={loading ? "…" : `${stationCount ?? 0} stations`}
            tone="grid"
          />
          <PathCard
            to="/backoffice/staff/new"
            eyebrow="Team"
            title="Create staff"
            detail="Provision Backoffice or Grid Operator accounts with role-based access."
            meta="New staff are active immediately"
            tone="slate"
          />
        </div>
      </section>

      {/* Pending highlight */}
      <section
        id="approvals"
        className="scroll-mt-24 gs-animate-fade-up"
        style={{ animationDelay: "180ms" }}
      >
        <div className="overflow-hidden rounded-2xl border border-amber-200/90 bg-white shadow-sm shadow-amber-900/5">
          <div className="flex flex-wrap items-end justify-between gap-4 border-b border-amber-100 bg-gradient-to-br from-amber-50 to-white px-6 py-5 sm:px-8">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-amber-800/80">
                Needs attention
              </p>
              <h2 className="mt-1 font-display text-2xl font-semibold text-amber-950">
                Pending prosumer activations
              </h2>
              <p className="mt-1 max-w-xl text-sm text-amber-950/65">
                New registrations wait here until you approve them.
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-amber-200/90 px-3 py-1 text-xs font-bold text-amber-950">
                {loading ? "…" : `${pending.length} waiting`}
              </span>
              <Link
                to="/backoffice/users"
                className="rounded-lg border border-amber-300/80 bg-white px-3 py-1.5 text-xs font-semibold text-amber-950 transition hover:bg-amber-50"
              >
                Open full directory
              </Link>
            </div>
          </div>

          <div className="px-6 py-6 sm:px-8">
            {loading ? (
              <div className="space-y-3">
                <div className="h-14 animate-pulse rounded-xl bg-slate-100" />
                <div className="h-14 animate-pulse rounded-xl bg-slate-100" />
              </div>
            ) : pending.length === 0 ? (
              <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50/80 px-5 py-10 text-center">
                <p className="text-sm font-semibold text-grid-900">
                  Queue is clear
                </p>
                <p className="mt-1 text-sm text-slate-500">
                  No prosumer activations waiting right now.
                </p>
              </div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {awaiting.map((u) => (
                  <li
                    key={u.id}
                    className="flex flex-wrap items-center justify-between gap-4 py-4 first:pt-0 last:pb-0"
                  >
                    <div className="flex min-w-0 items-center gap-3">
                      <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-sm font-semibold text-amber-950">
                        {initials(u.fullName)}
                      </span>
                      <div className="min-w-0">
                        <p className="truncate font-semibold text-slate-900">
                          {u.fullName}
                        </p>
                        <p className="truncate text-sm text-slate-500">
                          {u.nic ? `NIC ${u.nic}` : "No NIC"} · {u.email}
                        </p>
                      </div>
                    </div>
                    <button
                      type="button"
                      disabled={busyId === u.id}
                      onClick={() => approve(u.id, u.fullName)}
                      className="inline-flex rounded-xl bg-grid-700 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:opacity-60"
                    >
                      {busyId === u.id ? "Approving…" : "Approve"}
                    </button>
                  </li>
                ))}
              </ul>
            )}

            {!loading && pending.length > 4 ? (
              <div className="mt-4 border-t border-slate-100 pt-4 text-center">
                <Link
                  to="/backoffice/users"
                  className="text-sm font-semibold text-grid-700 hover:underline"
                >
                  View all {pending.length} pending accounts →
                </Link>
              </div>
            ) : null}
          </div>
        </div>
      </section>
      </div>
    </div>
  );
}

function PathCard({ to, eyebrow, title, detail, meta, tone = "grid" }) {
  const tones = {
    amber: "hover:border-amber-300 hover:shadow-amber-900/5",
    grid: "hover:border-grid-400 hover:shadow-grid-900/5",
    slate: "hover:border-slate-400 hover:shadow-slate-900/5",
  };
  const accents = {
    amber: "bg-amber-500",
    grid: "bg-grid-600",
    slate: "bg-slate-600",
  };

  return (
    <Link
      to={to}
      className={`group relative flex h-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md ${tones[tone]}`}
    >
      <span
        className={`absolute left-0 top-0 h-1 w-full ${accents[tone]} opacity-80 transition group-hover:opacity-100`}
      />
      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
        {eyebrow}
      </p>
      <h3 className="mt-2 font-display text-xl font-semibold text-grid-900">
        {title}
      </h3>
      <p className="mt-2 flex-1 text-sm leading-relaxed text-slate-600">
        {detail}
      </p>
      <p className="mt-5 flex items-center justify-between gap-2 text-xs font-semibold text-grid-700">
        <span>{meta}</span>
        <span
          aria-hidden
          className="transition group-hover:translate-x-0.5"
        >
          →
        </span>
      </p>
    </Link>
  );
}

function initials(name = "") {
  return (
    name
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() || "")
      .join("") || "U"
  );
}
