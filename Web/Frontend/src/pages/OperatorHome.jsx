import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  approveReservation,
  getManagedReservations,
  getReservationDashboardStats,
  rejectReservation,
} from "../api/reservations";
import { getStations } from "../api/stations";
import { useAuth } from "../context/AuthContext";
import PageBleedHero from "../components/PageBleedHero";
import { Alert } from "../components/ui";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1466611653911-95081537e5b7?auto=format&fit=crop&w=2000&q=80";

export default function OperatorHome() {
  const { user } = useAuth();
  const firstName = user.fullName?.split(" ")[0] || "Operator";

  const [stats, setStats] = useState(null);
  const [pendingList, setPendingList] = useState([]);
  const [stationCount, setStationCount] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState("");

  const load = useCallback(async () => {
    setError("");
    setLoading(true);
    try {
      const [dashboard, pending, stations] = await Promise.all([
        getReservationDashboardStats(),
        getManagedReservations("Pending"),
        getStations().catch(() => []),
      ]);
      setStats(dashboard);
      setPendingList(pending || []);
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

  const awaiting = useMemo(() => pendingList.slice(0, 4), [pendingList]);
  const pendingCount = stats?.pendingReservations ?? pendingList.length;
  const approvedCount = stats?.approvedUpcomingReservations ?? 0;

  async function approve(reservation) {
    setMessage("");
    setError("");
    setBusyId(reservation.id);
    try {
      await approveReservation(reservation.id);
      setMessage(
        `Approved ${reservation.reservationCode || reservation.id}. QR is ready for the prosumer.`,
      );
      await load();
    } catch (err) {
      setError(err.message || "Approval failed");
    } finally {
      setBusyId("");
    }
  }

  async function reject(reservation) {
    const reason = window.prompt("Rejection reason:", "");
    if (reason === null || !reason.trim()) return;

    setMessage("");
    setError("");
    setBusyId(reservation.id);
    try {
      await rejectReservation(reservation.id, reason.trim());
      setMessage(`Rejected ${reservation.reservationCode || reservation.id}.`);
      await load();
    } catch (err) {
      setError(err.message || "Rejection failed");
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
        eyebrow="GridSync · Grid Operator"
        title={`Ready when you are, ${firstName}`}
        subtitle="Review trading bookings and keep battery availability accurate at stations. On-site QR completion is handled in the mobile app."
        actions={
          <>
            <a
              href="#bookings"
              className="inline-flex items-center gap-2 rounded-lg bg-white px-5 py-2.5 text-sm font-semibold text-grid-800 transition hover:bg-grid-50"
            >
              Review bookings
              {pendingCount > 0 ? (
                <span className="rounded-md bg-amber-500 px-1.5 py-0.5 text-[11px] font-bold text-white">
                  {pendingCount}
                </span>
              ) : null}
            </a>
            <Link
              to="/stations"
              className="inline-flex items-center rounded-lg border border-white/30 bg-white/10 px-5 py-2.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/18"
            >
              Battery stations
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

      <section
        className="gs-animate-fade-up"
        style={{ animationDelay: "100ms" }}
        aria-labelledby="operator-paths"
      >
        <div className="mb-5">
          <h2
            id="operator-paths"
            className="font-display text-2xl font-semibold tracking-tight text-grid-900"
          >
            Your console
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            Web tools for bookings and batteries. QR scan and transfer completion stay on mobile.
          </p>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <PathCard
            to="/operator/reservations"
            eyebrow="Trading"
            title="Power bookings"
            detail="Approve or reject Charging and Drop-off requests. Approving issues the prosumer QR for mobile completion."
            meta={
              loading
                ? "…"
                : `${pendingCount} pending · ${approvedCount} approved upcoming`
            }
            tone="amber"
          />
          <PathCard
            to="/stations"
            eyebrow="Stations"
            title="Battery availability"
            detail="Open a hub to close or reopen batteries and keep slot capacity accurate."
            meta={loading ? "…" : `${stationCount ?? 0} stations`}
            tone="grid"
          />
        </div>
      </section>

      <section
        id="bookings"
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
                Pending booking requests
              </h2>
              <p className="mt-1 max-w-xl text-sm text-amber-950/65">
                Approve to issue a QR, or reject with a reason. Full queue lives
                under Bookings.
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-amber-200/90 px-3 py-1 text-xs font-bold text-amber-950">
                {loading ? "…" : `${pendingCount} waiting`}
              </span>
              <Link
                to="/operator/reservations"
                className="rounded-lg border border-amber-300/80 bg-white px-3 py-1.5 text-xs font-semibold text-amber-950 transition hover:bg-amber-50"
              >
                Open full queue
              </Link>
            </div>
          </div>

          <div className="px-6 py-6 sm:px-8">
            {loading ? (
              <div className="space-y-3">
                <div className="h-16 animate-pulse rounded-xl bg-slate-100" />
                <div className="h-16 animate-pulse rounded-xl bg-slate-100" />
              </div>
            ) : pendingList.length === 0 ? (
              <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50/80 px-5 py-10 text-center">
                <p className="text-sm font-semibold text-grid-900">
                  Queue is clear
                </p>
                <p className="mt-1 text-sm text-slate-500">
                  No pending Charging or Drop-off requests right now.
                </p>
              </div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {awaiting.map((reservation) => {
                  const typeLabel =
                    reservation.reservationType === "DropOff"
                      ? "Drop-off"
                      : "Charging";
                  return (
                    <li
                      key={reservation.id}
                      className="flex flex-wrap items-center justify-between gap-4 py-4 first:pt-0 last:pb-0"
                    >
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="font-semibold text-slate-900">
                            {reservation.reservationCode || reservation.id}
                          </p>
                          <span
                            className={`rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${
                              reservation.reservationType === "DropOff"
                                ? "bg-sky-50 text-sky-800"
                                : "bg-amber-50 text-amber-900"
                            }`}
                          >
                            {typeLabel}
                          </span>
                        </div>
                        <p className="mt-1 truncate text-sm text-slate-500">
                          {reservation.stationName || "Station"} ·{" "}
                          {reservation.prosumerNic || "NIC"} ·{" "}
                          {reservation.energyKwh} kWh
                        </p>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          disabled={busyId === reservation.id}
                          onClick={() => approve(reservation)}
                          className="inline-flex rounded-xl bg-grid-700 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:opacity-60"
                        >
                          {busyId === reservation.id
                            ? "Working…"
                            : "Approve"}
                        </button>
                        <button
                          type="button"
                          disabled={busyId === reservation.id}
                          onClick={() => reject(reservation)}
                          className="inline-flex rounded-xl border border-red-200 bg-white px-4 py-2.5 text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:opacity-60"
                        >
                          Reject
                        </button>
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}

            {!loading && pendingList.length > 4 ? (
              <div className="mt-4 border-t border-slate-100 pt-4 text-center">
                <Link
                  to="/operator/reservations"
                  className="text-sm font-semibold text-grid-700 hover:underline"
                >
                  View all {pendingList.length} pending bookings →
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
        <span aria-hidden className="transition group-hover:translate-x-0.5">
          →
        </span>
      </p>
    </Link>
  );
}
