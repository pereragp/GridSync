import { useCallback, useEffect, useState } from "react";
import {
  approveReservation,
  getManagedReservations,
  rejectReservation,
} from "../api/reservations";
import {
  Alert,
  PageHeader,
  btnSecondary,
  cardClass,
  inputClass,
} from "../components/ui";

const statuses = ["", "Pending", "Approved", "Rejected", "Cancelled", "Expired", "Completed"];

export default function OperatorReservations({
  title = "Reservation review",
  subtitle = "Review battery energy bookings. Approving issues a QR; actual kWh changes only when the transfer is completed.",
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
    <div>
      <PageHeader title={title} subtitle={subtitle} />
      {error ? <Alert>{error}</Alert> : null}
      {message ? <Alert type="success">{message}</Alert> : null}

      <section className={`${cardClass} mb-6`}>
        <div className="grid gap-3 md:grid-cols-[12rem_1fr]">
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
                <option key={value} value={value}>
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
              placeholder="Reservation code, station, or prosumer NIC"
            />
          </label>
        </div>
      </section>

      <section className={cardClass}>
        <h2 className="mb-4 text-lg font-medium">
          Reservations ({visibleReservations.length})
        </h2>
        {loading ? <p className="text-sm text-slate-500">Loading...</p> : null}
        {!loading && visibleReservations.length === 0 ? (
          <p className="text-sm text-slate-500">No reservations found.</p>
        ) : null}
        <div className="space-y-3">
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
  return (
    <article className="border-t border-slate-200 pt-3 first:border-t-0 first:pt-0">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-medium text-slate-900">
            {reservation.reservationCode || reservation.id}
          </p>
          <p className="text-sm text-slate-600">
            {reservation.stationName || "Station unavailable"} · {reservation.prosumerNic || "NIC unavailable"}
          </p>
          <p className="text-sm text-slate-600">
            {formatDate(reservation.slotStart)} - {formatDate(reservation.slotEnd)} ·{" "}
            {reservation.reservationType === "DropOff"
              ? "Drop-off"
              : reservation.reservationType}{" "}
            · <strong>{reservation.energyKwh} kWh</strong>
          </p>
          {reservation.status === "Rejected" && reservation.rejectionReason ? (
            <p className="mt-1 text-sm text-red-700">Reason: {reservation.rejectionReason}</p>
          ) : null}
        </div>
        <StatusBadge status={reservation.status} />
      </div>
      {pending ? (
        <div className="mt-3 flex flex-wrap gap-2">
          <button type="button" className={btnSecondary} onClick={onApprove} disabled={busy}>
            Approve
          </button>
          <button
            type="button"
            className="inline-flex items-center justify-center rounded-md border border-red-200 px-3 py-1.5 text-sm text-red-700 hover:bg-red-50 disabled:opacity-60"
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

function StatusBadge({ status }) {
  const colors = {
    Pending: "bg-amber-50 text-amber-800",
    Approved: "bg-teal-50 text-teal-800",
    Rejected: "bg-red-50 text-red-800",
    Cancelled: "bg-slate-200 text-slate-700",
    Expired: "bg-slate-100 text-slate-700",
  };
  return (
    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${colors[status] || "bg-slate-100 text-slate-700"}`}>
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