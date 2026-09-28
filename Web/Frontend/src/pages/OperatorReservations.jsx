import { useCallback, useEffect, useState } from "react";
import {
  approveReservation,
  getManagedReservations,
  rejectReservation,
} from "../api/reservations";
import {
  Alert,
  PageHeader,
  btnPrimary,
  btnSecondary,
  cardClass,
  inputClass,
} from "../components/ui";

const statuses = [
  "",
  "Pending",
  "Approved",
  "Rejected",
  "Cancelled",
  "Expired",
  "Completed",
];

export default function OperatorReservations({
  title = "Reservation review",
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

  const normalizedSearch = search.trim().toLowerCase();
  const visibleReservations = reservations.filter((reservation) => {
    if (!normalizedSearch) return true;
    return [
      reservation.reservationCode,
      reservation.stationName,
      reservation.prosumerNic,
      reservation.prosumerId,
    ]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(normalizedSearch));
  });

  return (
    <div className={embedded ? "space-y-4" : undefined}>
      {!embedded ? <PageHeader title={title} subtitle={subtitle} /> : null}
      {embedded ? (
        <div className="mb-1">
          <h2 className="text-lg font-semibold text-grid-900">{title}</h2>
          {subtitle ? (
            <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
          ) : null}
        </div>
      ) : null}

      {error ? <Alert>{error}</Alert> : null}
      {message ? <Alert type="success">{message}</Alert> : null}

      <section className={`${cardClass} ${embedded ? "rounded-2xl" : ""}`}>
        <div className="grid gap-3 md:grid-cols-[11rem_1fr_auto]">
          <label className="text-sm">
            <span className="mb-1 block font-medium text-slate-700">Status</span>
            <select
              className={inputClass}
              value={status}
              onChange={(event) => {
                setLoading(true);
                setStatus(event.target.value);
              }}
            >
              {statuses.map((value) => (
                <option key={value || "all"} value={value}>
                  {value || "All statuses"}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm">
            <span className="mb-1 block font-medium text-slate-700">Search</span>
            <input
              className={inputClass}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Code, station, or prosumer NIC"
            />
          </label>
          <div className="flex items-end">
            <button
              type="button"
              className={`${btnSecondary} w-full md:w-auto`}
              onClick={load}
              disabled={loading}
            >
              Refresh
            </button>
          </div>
        </div>
      </section>

      <section className={`${cardClass} ${embedded ? "rounded-2xl" : ""}`}>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-base font-semibold text-grid-900">
            Queue
            <span className="ml-2 text-sm font-medium text-slate-500">
              ({visibleReservations.length})
            </span>
          </h3>
          <div className="flex flex-wrap gap-1.5">
            {["Pending", "Approved", "Completed"].map((chip) => (
              <button
                key={chip}
                type="button"
                onClick={() => {
                  setLoading(true);
                  setStatus(chip);
                }}
                className={`rounded-full px-2.5 py-1 text-xs font-semibold transition ${
                  status === chip
                    ? "bg-grid-700 text-white"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                {chip}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <p className="py-6 text-center text-sm text-slate-500">Loading bookings…</p>
        ) : null}
        {!loading && visibleReservations.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
            No bookings for this filter.
          </div>
        ) : null}

        <div className="space-y-0 divide-y divide-slate-100">
          {visibleReservations.map((reservation) => (
            <ReservationRow
              key={reservation.id}
              reservation={reservation}
              busy={actionId === reservation.id}
              onApprove={() =>
                review(
                  () => approveReservation(reservation.id),
                  reservation.id,
                  `Approved ${reservation.reservationCode || reservation.id}`,
                )
              }
              onReject={() => onReject(reservation)}
            />
          ))}
        </div>
      </section>
    </div>
  );
}

function ReservationRow({ reservation, busy, onApprove, onReject }) {
  const pending = reservation.status === "Pending";
  const typeLabel =
    reservation.reservationType === "DropOff"
      ? "Drop-off"
      : reservation.reservationType;

  return (
    <article className="py-4 first:pt-0 last:pb-0">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="font-semibold text-slate-900">
              {reservation.reservationCode || reservation.id}
            </p>
            <TypePill type={reservation.reservationType} />
          </div>
          <p className="mt-1 text-sm text-slate-600">
            {reservation.stationName || "Station unavailable"} ·{" "}
            {reservation.prosumerNic || "NIC unavailable"}
          </p>
          <p className="mt-0.5 text-sm text-slate-600">
            {formatDate(reservation.slotStart)} – {formatDate(reservation.slotEnd)}
            <span className="mx-1.5 text-slate-300">·</span>
            <span className="font-semibold text-grid-800">
              {reservation.energyKwh} kWh
            </span>
            <span className="text-slate-400"> {typeLabel}</span>
          </p>
          {reservation.status === "Rejected" && reservation.rejectionReason ? (
            <p className="mt-1.5 text-sm text-red-700">
              Reason: {reservation.rejectionReason}
            </p>
          ) : null}
        </div>
        <StatusBadge status={reservation.status} />
      </div>

      {pending ? (
        <div className="mt-3 flex flex-wrap gap-2">
          <button
            type="button"
            className={btnPrimary}
            onClick={onApprove}
            disabled={busy}
          >
            {busy ? "Working…" : "Approve & issue QR"}
          </button>
          <button
            type="button"
            className="inline-flex items-center justify-center rounded-md border border-red-200 bg-white px-3 py-2 text-sm font-medium text-red-700 transition hover:bg-red-50 disabled:opacity-60"
            onClick={onReject}
            disabled={busy}
          >
            Reject
          </button>
        </div>
      ) : null}
    </article>
  );
}

function TypePill({ type }) {
  const isDrop = type === "DropOff";
  return (
    <span
      className={`rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${
        isDrop
          ? "bg-sky-50 text-sky-800"
          : "bg-amber-50 text-amber-900"
      }`}
    >
      {isDrop ? "Drop-off" : "Charging"}
    </span>
  );
}

function StatusBadge({ status }) {
  const colors = {
    Pending: "bg-amber-50 text-amber-900 ring-1 ring-amber-200/80",
    Approved: "bg-emerald-50 text-emerald-900 ring-1 ring-emerald-200/80",
    Rejected: "bg-red-50 text-red-800 ring-1 ring-red-200/80",
    Cancelled: "bg-slate-100 text-slate-700 ring-1 ring-slate-200",
    Expired: "bg-slate-100 text-slate-600 ring-1 ring-slate-200",
    Completed: "bg-grid-100 text-grid-800 ring-1 ring-grid-200",
  };
  return (
    <span
      className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${
        colors[status] || "bg-slate-100 text-slate-700"
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
