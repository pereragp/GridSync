import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  approveReservation,
  getManagedReservations,
  rejectReservation,
} from "../api/reservations";
import { Alert } from "../components/ui";
import PageBleedHero from "../components/PageBleedHero";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?auto=format&fit=crop&w=2000&q=80";

const STATUS_FILTERS = [
  { value: "Pending", label: "Pending", hint: "Needs review" },
  { value: "Approved", label: "Approved", hint: "QR issued" },
  { value: "Completed", label: "Completed", hint: "Transfer done" },
  { value: "Rejected", label: "Rejected", hint: "Declined" },
  { value: "Cancelled", label: "Cancelled", hint: "Prosumer cancelled" },
  { value: "Expired", label: "Expired", hint: "Window passed" },
  { value: "", label: "All", hint: "Every status" },
];

export default function OperatorReservations({
  title = "Power bookings",
  subtitle = "Review battery energy bookings. Approving issues a QR; actual kWh changes only when the transfer is completed.",
  embedded = false,
}) {
  const [reservations, setReservations] = useState([]);
  const [status, setStatus] = useState("Pending");
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState("");

  const load = useCallback(async () => {
    setError("");
    setLoading(true);
    try {
      setReservations((await getManagedReservations(status)) || []);
    } catch (err) {
      setError(err.message || "Failed to load reservations");
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getManagedReservations(status)
      .then((data) => {
        if (!cancelled) setReservations(data || []);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || "Failed to load reservations");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [status]);

  async function review(action, id, successText) {
    setError("");
    setMessage("");
    setActionId(id);
    try {
      await action();
      setMessage(successText);
      await load();
    } catch (err) {
      setError(err.message || "Reservation action failed");
    } finally {
      setActionId("");
    }
  }

  function onReject(reservation) {
    const reason = window.prompt("Rejection reason:", "");
    if (reason === null || !reason.trim()) return;

    review(
      () => rejectReservation(reservation.id, reason.trim()),
      reservation.id,
      `Rejected ${reservation.reservationCode || reservation.id}`,
    );
  }

  function setFilter(next) {
    if (next === status) return;
    setLoading(true);
    setStatus(next);
  }

  const normalizedSearch = search.trim().toLowerCase();
  const visibleReservations = reservations.filter((reservation) => {
    if (!normalizedSearch) return true;
    return [
      reservation.reservationCode,
      reservation.stationName,
      reservation.prosumerNic,
      reservation.prosumerId,
      reservation.reservationType,
    ]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(normalizedSearch));
  });

  const activeFilter =
    STATUS_FILTERS.find((f) => f.value === status) || STATUS_FILTERS[0];

  return (
    <div className={embedded ? "space-y-4" : undefined}>
      {!embedded ? (
        <PageBleedHero
          image={HERO_IMAGE}
          imageAlt="Power lines across an open sky"
          eyebrow="Grid Operator"
          title={title}
          subtitle={subtitle}
          actions={
            <Link
              to="/operator"
              className="inline-flex items-center justify-center rounded-lg border border-white/30 bg-white/10 px-5 py-2.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/18"
            >
              Back to home
            </Link>
          }
        />
      ) : (
        <div className="mb-1">
          <h2 className="text-lg font-semibold text-grid-900">{title}</h2>
          {subtitle ? (
            <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
          ) : null}
        </div>
      )}

      <div className={embedded ? "space-y-4" : "space-y-6"}>
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

        {/* Filters */}
        <section className="rounded-2xl border border-slate-200/90 bg-white p-4 shadow-sm sm:p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
                Filter queue
              </p>
              <p className="mt-0.5 text-sm text-slate-600">
                {activeFilter.hint}
              </p>
            </div>
            <button
              type="button"
              onClick={load}
              disabled={loading}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-sm font-semibold text-slate-700 transition hover:border-grid-300 hover:bg-grid-50 hover:text-grid-800 disabled:opacity-60"
            >
              <RefreshIcon spinning={loading} />
              Refresh
            </button>
          </div>

          <div className="mt-4 flex gap-2 overflow-x-auto pb-1">
            {STATUS_FILTERS.map((filter) => {
              const active = status === filter.value;
              return (
                <button
                  key={filter.value || "all"}
                  type="button"
                  onClick={() => setFilter(filter.value)}
                  className={`shrink-0 rounded-xl px-3.5 py-2 text-sm font-semibold transition ${
                    active
                      ? "bg-grid-700 text-white shadow-sm shadow-grid-900/10"
                      : "bg-slate-100 text-slate-600 hover:bg-slate-200/80 hover:text-slate-800"
                  }`}
                >
                  {filter.label}
                </button>
              );
            })}
          </div>

          <div className="relative mt-4">
            <span className="pointer-events-none absolute inset-y-0 left-3.5 flex items-center text-slate-400">
              <SearchIcon />
            </span>
            <input
              type="search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search by code, station, NIC, or type…"
              className="w-full rounded-xl border border-slate-200 bg-slate-50/80 py-2.5 pl-10 pr-3.5 text-sm outline-none transition placeholder:text-slate-400 focus:border-grid-500 focus:bg-white focus:ring-2 focus:ring-grid-500/15"
            />
          </div>
        </section>

        {/* Queue */}
        <section>
          <div className="mb-3 flex flex-wrap items-end justify-between gap-2">
            <div>
              <h2 className="font-display text-xl font-semibold text-grid-900">
                {activeFilter.label === "All"
                  ? "All bookings"
                  : `${activeFilter.label} bookings`}
              </h2>
              <p className="mt-0.5 text-sm text-slate-500">
                {loading
                  ? "Loading…"
                  : `${visibleReservations.length} result${
                      visibleReservations.length === 1 ? "" : "s"
                    }`}
                {search.trim() ? ` matching “${search.trim()}”` : ""}
              </p>
            </div>
            {status === "Pending" && !loading && visibleReservations.length > 0 ? (
              <p className="rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-900 ring-1 ring-amber-200/80">
                Approve to issue QR · Reject with a reason
              </p>
            ) : null}
          </div>

          {loading ? (
            <div className="space-y-3">
              {[0, 1, 2].map((i) => (
                <div
                  key={i}
                  className="h-28 animate-pulse rounded-2xl border border-slate-100 bg-slate-100/80"
                />
              ))}
            </div>
          ) : null}

          {!loading && visibleReservations.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-14 text-center shadow-sm">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-grid-50 text-grid-700">
                <InboxIcon />
              </div>
              <p className="mt-4 text-base font-semibold text-grid-900">
                Nothing in this queue
              </p>
              <p className="mx-auto mt-1 max-w-sm text-sm text-slate-500">
                {search.trim()
                  ? "Try a different search, or clear the box to see all results for this filter."
                  : `No ${activeFilter.label.toLowerCase()} bookings right now.`}
              </p>
              {search.trim() ? (
                <button
                  type="button"
                  onClick={() => setSearch("")}
                  className="mt-4 text-sm font-semibold text-grid-700 hover:underline"
                >
                  Clear search
                </button>
              ) : null}
            </div>
          ) : null}

          {!loading && visibleReservations.length > 0 ? (
            <ul className="space-y-3">
              {visibleReservations.map((reservation) => (
                <li key={reservation.id}>
                  <ReservationCard
                    reservation={reservation}
                    busy={actionId === reservation.id}
                    onApprove={() =>
                      review(
                        () => approveReservation(reservation.id),
                        reservation.id,
                        `Approved ${reservation.reservationCode || reservation.id} — QR is ready for the prosumer.`,
                      )
                    }
                    onReject={() => onReject(reservation)}
                  />
                </li>
              ))}
            </ul>
          ) : null}
        </section>
      </div>
    </div>
  );
}

function ReservationCard({ reservation, busy, onApprove, onReject }) {
  const pending = reservation.status === "Pending";
  const isDrop = reservation.reservationType === "DropOff";
  const typeLabel = isDrop ? "Drop-off" : "Charging";

  return (
    <article className="overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-sm transition hover:border-grid-200 hover:shadow-md hover:shadow-grid-900/5">
      <div className="flex flex-col gap-4 p-4 sm:flex-row sm:items-stretch sm:p-5">
        <div
          className={`hidden w-1 shrink-0 rounded-full sm:block ${
            pending
              ? "bg-amber-400"
              : reservation.status === "Approved"
                ? "bg-emerald-400"
                : reservation.status === "Completed"
                  ? "bg-grid-500"
                  : reservation.status === "Rejected"
                    ? "bg-red-400"
                    : "bg-slate-300"
          }`}
        />

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="font-semibold tracking-tight text-slate-900">
              {reservation.reservationCode || reservation.id}
            </h3>
            <TypePill type={reservation.reservationType} />
            <StatusBadge status={reservation.status} />
          </div>

          <div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            <Meta
              label="Station"
              value={reservation.stationName || "Unavailable"}
            />
            <Meta
              label="Prosumer NIC"
              value={reservation.prosumerNic || "Unavailable"}
            />
            <Meta label="Energy" value={`${reservation.energyKwh} kWh · ${typeLabel}`} />
          </div>

          <p className="mt-3 text-sm text-slate-600">
            <span className="font-medium text-slate-700">Slot</span>
            <span className="mx-1.5 text-slate-300">·</span>
            {formatDate(reservation.slotStart)}
            <span className="mx-1.5 text-slate-400">→</span>
            {formatDate(reservation.slotEnd)}
          </p>

          {reservation.status === "Rejected" && reservation.rejectionReason ? (
            <p className="mt-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">
              <span className="font-semibold">Reason:</span>{" "}
              {reservation.rejectionReason}
            </p>
          ) : null}

          {pending ? (
            <div className="mt-4 flex flex-wrap gap-2 border-t border-slate-100 pt-4">
              <button
                type="button"
                onClick={onApprove}
                disabled={busy}
                className="inline-flex items-center justify-center rounded-xl bg-grid-700 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:opacity-60"
              >
                {busy ? "Working…" : "Approve & issue QR"}
              </button>
              <button
                type="button"
                onClick={onReject}
                disabled={busy}
                className="inline-flex items-center justify-center rounded-xl border border-red-200 bg-white px-4 py-2.5 text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:opacity-60"
              >
                Reject
              </button>
            </div>
          ) : null}
        </div>
      </div>
    </article>
  );
}

function Meta({ label, value }) {
  return (
    <div className="rounded-xl bg-slate-50 px-3 py-2">
      <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>
      <p className="mt-0.5 truncate text-sm font-medium text-slate-900">
        {value}
      </p>
    </div>
  );
}

function TypePill({ type }) {
  const isDrop = type === "DropOff";
  return (
    <span
      className={`rounded-full px-2.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${
        isDrop ? "bg-sky-50 text-sky-800 ring-1 ring-sky-200/80" : "bg-amber-50 text-amber-900 ring-1 ring-amber-200/80"
      }`}
    >
      {isDrop ? "Drop-off" : "Charging"}
    </span>
  );
}

function StatusBadge({ status }) {
  const colors = {
    Pending: "bg-amber-50 text-amber-900 ring-amber-200/80",
    Approved: "bg-emerald-50 text-emerald-900 ring-emerald-200/80",
    Rejected: "bg-red-50 text-red-800 ring-red-200/80",
    Cancelled: "bg-slate-100 text-slate-700 ring-slate-200",
    Expired: "bg-slate-100 text-slate-600 ring-slate-200",
    Completed: "bg-grid-100 text-grid-800 ring-grid-200",
  };
  return (
    <span
      className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ${
        colors[status] || "bg-slate-100 text-slate-700 ring-slate-200"
      }`}
    >
      {status}
    </span>
  );
}

function formatDate(value) {
  if (!value) return "Unknown time";
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function SearchIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="currentColor" className="h-4 w-4" aria-hidden>
      <path
        fillRule="evenodd"
        d="M9 3.5a5.5 5.5 0 100 11 5.5 5.5 0 000-11zM2 9a7 7 0 1112.452 4.391l3.328 3.329a.75.75 0 11-1.06 1.06l-3.329-3.328A7 7 0 012 9z"
        clipRule="evenodd"
      />
    </svg>
  );
}

function RefreshIcon({ spinning }) {
  return (
    <svg
      viewBox="0 0 20 20"
      fill="currentColor"
      className={`h-4 w-4 ${spinning ? "animate-spin" : ""}`}
      aria-hidden
    >
      <path
        fillRule="evenodd"
        d="M15.312 11.424a5.5 5.5 0 01-9.201 2.466l-.312-.311h2.433a.75.75 0 000-1.5H4.39a.75.75 0 00-.75.75v3.842a.75.75 0 001.5 0v-2.26l.31.31a7 7 0 0011.712-3.138.75.75 0 00-1.449-.388zm-10.624-2.85a5.5 5.5 0 019.201-2.466l.312.311H11.77a.75.75 0 000 1.5h3.842a.75.75 0 00.75-.75V3.328a.75.75 0 00-1.5 0V5.59l-.31-.31A7 7 0 003.838 8.416a.75.75 0 001.45.388z"
        clipRule="evenodd"
      />
    </svg>
  );
}

function InboxIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="currentColor" className="h-5 w-5" aria-hidden>
      <path
        fillRule="evenodd"
        d="M1 11.27c0-.246.033-.492.099-.73l1.523-5.596A2.75 2.75 0 015.273 3h9.454a2.75 2.75 0 012.651 1.944l1.523 5.596c.066.238.099.484.099.73V15a2 2 0 01-2 2H3a2 2 0 01-2-2v-3.73zm3.068-6.728A1.25 1.25 0 015.273 4.5h9.454a1.25 1.25 0 011.205.884l1.3 4.776A3.482 3.482 0 0016.5 9.5h-3.379a1.75 1.75 0 00-1.543.923l-.376.721a.25.25 0 01-.222.136H9.02a.25.25 0 01-.222-.136l-.376-.721A1.75 1.75 0 006.879 9.5H3.5c-.284 0-.56.054-.814.154l1.3-4.776a1.25 1.25 0 01.082-.236z"
        clipRule="evenodd"
      />
    </svg>
  );
}
