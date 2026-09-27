import { Link } from "react-router-dom";
import { PageHeader, cardClass } from "../components/ui";
import { useAuth } from "../context/AuthContext";

export default function OperatorHome() {
  const { user } = useAuth();

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Welcome, ${user.fullName}`}
        subtitle="Grid Operator console — manage station schedules and battery slots."
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <Link
          to="/stations"
          className={`${cardClass} block transition hover:border-grid-300 hover:shadow-md`}
        >
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
            Operations
          </p>
          <h2 className="mt-2 text-lg font-semibold text-grid-900">Solar stations</h2>
          <p className="mt-2 text-sm text-slate-600">
            View hubs, update open/close hours, working days, and available battery
            slots.
          </p>
          <span className="mt-4 inline-flex text-sm font-semibold text-grid-700">
            Open stations →
          </span>
        </Link>

        <div className={cardClass}>
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
            Account
          </p>
          <h2 className="mt-2 text-lg font-semibold text-grid-900">Signed in</h2>
          <p className="mt-2 text-sm text-slate-600">
            You are signed in as <strong>{user.role}</strong>. Use Profile or Password
            in the top nav for account settings.
          </p>
        </div>
      </div>
    </div>
  );
}
