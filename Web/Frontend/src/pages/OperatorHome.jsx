import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useEffect, useState } from "react";
import { getReservationDashboardStats } from "../api/reservations";
import { Alert } from "../components/ui";
import OperatorReservations from "./OperatorReservations";

export default function OperatorHome() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    getReservationDashboardStats()
      .then((data) => {
        if (!cancelled) setStats(data);
      })
      .catch((err) => {
        if (!cancelled)
          setError(err.message || "Could not load dashboard statistics");
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const pending = stats?.pendingReservations ?? 0;
  const approved = stats?.approvedUpcomingReservations ?? 0;

  return (
    <div className="space-y-8">
      <section className="relative overflow-hidden rounded-2xl border border-grid-200/80 bg-gradient-to-br from-grid-800 via-grid-700 to-grid-600 px-5 py-7 text-white shadow-sm sm:px-8 sm:py-8">
        <div className="pointer-events-none absolute -right-10 -top-10 h-40 w-40 rounded-full bg-emerald-400/20 blur-2xl" />
        <div className="pointer-events-none absolute -bottom-16 right-16 h-48 w-48 rounded-full bg-white/10 blur-3xl" />

        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-grid-100/70">
          Grid Operator
        </p>
        <h1 className="mt-2 font-display text-3xl font-semibold tracking-tight sm:text-4xl">
          Welcome, {user.fullName.split(" ")[0]}
        </h1>
        <p className="mt-3 max-w-2xl text-sm leading-relaxed text-grid-100/85 sm:text-base">
          Monitor power trading bookings, update battery availability at stations,
          and complete on-site energy transfers with QR.
        </p>

        <div className="mt-6 flex flex-wrap gap-3">
          <Link
            to="/stations"
            className="inline-flex items-center gap-2 rounded-lg bg-white px-4 py-2.5 text-sm font-semibold text-grid-800 transition hover:bg-grid-50"
          >
            Manage batteries
            <span aria-hidden>→</span>
          </Link>
          <Link
            to="/operator/qr-scanner"
            className="inline-flex items-center gap-2 rounded-lg border border-white/30 bg-white/10 px-4 py-2.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/15"
          >
            Verify QR
          </Link>
        </div>
      </section>

      {error ? <Alert>{error}</Alert> : null}

      <section>
        <div className="mb-4 flex flex-wrap items-end justify-between gap-2">
          <div>
            <h2 className="text-lg font-semibold text-grid-900">Booking overview</h2>
            <p className="text-sm text-slate-600">
              Live counts across the trading queue.
            </p>
          </div>
          {(pending > 0 || approved > 0) && (
            <p className="rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-900">
              {pending} pending · {approved} approved upcoming
            </p>
          )}
        </div>

        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <StatCard
            label="Pending review"
            value={stats?.pendingReservations}
            tone="amber"
            hint="Needs approve or reject"
          />
          <StatCard
            label="Approved upcoming"
            value={stats?.approvedUpcomingReservations}
            tone="teal"
            hint="Ready for QR completion"
          />
          <StatCard
            label="Completed transfers"
            value={stats?.completedTransfers}
            tone="grid"
            hint="Energy applied to batteries"
          />
          <StatCard label="Rejected" value={stats?.rejectedReservations} />
          <StatCard label="Cancelled" value={stats?.cancelledReservations} />
          <StatCard label="Expired" value={stats?.expiredReservations} />
        </div>
      </section>

      <section>
        <OperatorReservations
          embedded
          title="Power trading bookings"
          subtitle="Approve pending Charging and Drop-off requests, or filter to monitor the full queue. Completing a transfer happens under Verify QR."
        />
      </section>
    </div>
  );
}

function StatCard({ label, value, tone = "default", hint }) {
  const tones = {
    amber: "border-amber-200/80 bg-amber-50/80",
    teal: "border-emerald-200/80 bg-emerald-50/70",
    grid: "border-grid-200 bg-grid-50",
    default: "border-slate-200 bg-white",
  };
  const valueTones = {
    amber: "text-amber-950",
    teal: "text-emerald-900",
    grid: "text-grid-900",
    default: "text-grid-900",
  };

  return (
    <div className={`rounded-2xl border p-4 shadow-sm ${tones[tone]}`}>
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>
      <p className={`mt-2 text-3xl font-semibold tracking-tight ${valueTones[tone]}`}>
        {value ?? "…"}
      </p>
      {hint ? <p className="mt-1 text-xs text-slate-500">{hint}</p> : null}
    </div>
  );
}
