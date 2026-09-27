import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useEffect, useState } from "react";
import { getReservationDashboardStats } from "../api/reservations";
import { Alert, PageHeader, cardClass } from "../components/ui";

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

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Welcome, ${user.fullName}`}
        subtitle="Grid Operator console — review battery bookings, complete QR transfers, and manage station batteries."
      />

      {error ? <Alert>{error}</Alert> : null}

      <section className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard
          label="Pending reservations"
          value={stats?.pendingReservations}
        />
        <StatCard
          label="Approved upcoming"
          value={stats?.approvedUpcomingReservations}
        />
        <StatCard
          label="Completed transfers"
          value={stats?.completedTransfers}
        />
        <StatCard label="Rejected" value={stats?.rejectedReservations} />
        <StatCard label="Cancelled" value={stats?.cancelledReservations} />
        <StatCard label="Expired" value={stats?.expiredReservations} />
      </section>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Link
          to="/stations"
          className={`${cardClass} block transition hover:border-grid-300 hover:shadow-md`}
        >
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
            Operations
          </p>
          <h2 className="mt-2 text-lg font-semibold text-grid-900">
            Stations & batteries
          </h2>
          <p className="mt-2 text-sm text-slate-600">
            Open a hub to view each battery&apos;s capacity, actual energy, and
            reserved Charging / Drop-off pools. Close batteries when needed.
          </p>
          <span className="mt-4 inline-flex text-sm font-semibold text-grid-700">
            Open stations →
          </span>
        </Link>

        <Link
          to="/operator/reservations"
          className={`${cardClass} block transition hover:border-grid-300 hover:shadow-md`}
        >
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
            Bookings
          </p>
          <h2 className="mt-2 text-lg font-semibold text-grid-900">
            Review reservations
          </h2>
          <p className="mt-2 text-sm text-slate-600">
            Approve or reject pending Charging and Drop-off requests. Approval
            issues a QR without changing actual battery energy yet.
          </p>
          <span className="mt-4 inline-flex text-sm font-semibold text-grid-700">
            Review queue →
          </span>
        </Link>

        <Link
          to="/operator/qr-scanner"
          className={`${cardClass} block transition hover:border-grid-300 hover:shadow-md`}
        >
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
            On-site
          </p>
          <h2 className="mt-2 text-lg font-semibold text-grid-900">
            Scan QR & complete
          </h2>
          <p className="mt-2 text-sm text-slate-600">
            Verify the prosumer QR and complete the transfer to update actual
            kWh on the battery.
          </p>
          <span className="mt-4 inline-flex text-sm font-semibold text-grid-700">
            Open scanner →
          </span>
        </Link>
      </div>
    </div>
  );
}

function StatCard({ label, value }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-sm text-slate-600">{label}</p>
      <p className="mt-2 text-3xl font-semibold text-teal-800">
        {value ?? "..."}
      </p>
    </div>
  );
}
