import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getUser, requestDeactivation } from "../api/users";
import { useAuth } from "../context/AuthContext";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=1800&q=80";

export default function ProsumerHome() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await getUser(user.userId);
        if (!cancelled) setProfile(data);
      } catch (err) {
        if (!cancelled) setError(err.message || "Could not load account details");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user.userId]);

  const status = profile?.status || user.status;
  const isPending = status === "Pending";
  const isActive = status === "Active";
  const deactivationRequested = Boolean(profile?.deactivationRequestedAt);

  async function onRequestDeactivation() {
    setError("");
    setMessage("");
    setBusy(true);
    try {
      const updated = await requestDeactivation(user.userId);
      setProfile(updated);
      setMessage("Deactivation request sent. A Backoffice officer will process it.");
    } catch (err) {
      setError(err.message || "Request failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-8">
      {/* Hero */}
      <section className="relative overflow-hidden rounded-2xl border border-grid-800/10 shadow-lg shadow-grid-900/10">
        <div className="absolute inset-0">
          <img
            src={HERO_IMAGE}
            alt="Solar panels in a green field"
            className="h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-grid-900/90 via-grid-800/75 to-grid-700/40" />
        </div>

        <div className="relative z-10 px-6 py-10 sm:px-10 sm:py-14">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-grid-100/70">
            Prosumer portal
          </p>
          <h1 className="mt-3 max-w-2xl font-display text-3xl font-semibold leading-tight text-white sm:text-4xl">
            Welcome back, {user.fullName.split(" ")[0]}.
          </h1>
          <p className="mt-3 max-w-xl text-sm leading-relaxed text-grid-100/85 sm:text-base">
            Manage your solar trading account, prepare energy reservations, and
            stay connected to nearby microgrid nodes.
          </p>

          <div className="mt-6 flex flex-wrap items-center gap-3">
            <StatusChip status={status} />
            {user.nic ? (
              <span className="rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs text-white/90 backdrop-blur">
                NIC {user.nic}
              </span>
            ) : null}
          </div>
        </div>
      </section>

      {error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
          {error}
        </div>
      ) : null}
      {message ? (
        <div className="rounded-lg border border-grid-200 bg-grid-50 px-4 py-3 text-sm text-grid-800">
          {message}
        </div>
      ) : null}

      {/* Status callout */}
      {isPending ? (
        <section className="rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-amber-950">
          <h2 className="font-semibold">Account pending approval</h2>
          <p className="mt-1 text-sm text-amber-900/80">
            Your registration is waiting for Backoffice activation. You can update
            your profile now; booking and QR features unlock after approval.
          </p>
        </section>
      ) : null}

      {isActive && deactivationRequested ? (
        <section className="rounded-2xl border border-slate-200 bg-slate-50 px-5 py-4 text-slate-800">
          <h2 className="font-semibold">Deactivation requested</h2>
          <p className="mt-1 text-sm text-slate-600">
            Your request is on file. A Backoffice officer will complete the process.
          </p>
        </section>
      ) : null}

      {/* Quick actions */}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <ActionCard
          title="Profile"
          description="Update your name, phone, and address details."
          to="/profile"
          cta="Edit profile"
        />
        <ActionCard
          title="Security"
          description="Change your password and keep your account secure."
          to="/change-password"
          cta="Change password"
        />
        <div className="rounded-2xl border border-dashed border-grid-300 bg-white p-5">
          <h3 className="font-semibold text-grid-900">Account status</h3>
          <p className="mt-1 text-sm text-slate-600">
            {isActive
              ? "Your prosumer account is active and ready for energy trading."
              : "Complete activation to unlock full trading features."}
          </p>
          {isActive && !deactivationRequested ? (
            <button
              type="button"
              disabled={busy}
              onClick={onRequestDeactivation}
              className="mt-4 rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
            >
              {busy ? "Sending…" : "Request deactivation"}
            </button>
          ) : null}
        </div>
      </section>

      {/* Upcoming features */}
      <section>
        <div className="mb-4 flex items-end justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold tracking-tight text-grid-900">
              Coming next on your portal
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              These flows will connect to the reservation, map, and QR APIs.
            </p>
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <FeaturePreview
            title="Energy bookings"
            detail="Reserve, update, and cancel charging or drop-off slots within the 7-day window."
          />
          <FeaturePreview
            title="Nearby grid nodes"
            detail="View microgrid stations on the map using live GPS locations from the API."
          />
          <FeaturePreview
            title="Transaction QR"
            detail="Generate a secure QR after approval for operator verification at the hub."
          />
        </div>
      </section>
    </div>
  );
}

function StatusChip({ status }) {
  const styles = {
    Active: "bg-grid-500/20 text-grid-50 border-grid-400/40",
    Pending: "bg-amber-400/20 text-amber-50 border-amber-200/40",
    Deactivated: "bg-white/10 text-white/80 border-white/20",
  };

  return (
    <span
      className={`rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-wide ${
        styles[status] || styles.Pending
      }`}
    >
      {status}
    </span>
  );
}

function ActionCard({ title, description, to, cta }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-grid-300 hover:shadow-md">
      <h3 className="font-semibold text-grid-900">{title}</h3>
      <p className="mt-1 text-sm text-slate-600">{description}</p>
      <Link
        to={to}
        className="mt-4 inline-flex rounded-lg bg-grid-700 px-3 py-2 text-sm font-semibold text-white transition hover:bg-grid-800"
      >
        {cta}
      </Link>
    </div>
  );
}

function FeaturePreview({ title, detail }) {
  return (
    <div className="rounded-2xl border border-grid-100 bg-gradient-to-br from-grid-50 to-white p-5">
      <div className="mb-3 inline-flex rounded-full bg-grid-100 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-grid-700">
        Soon
      </div>
      <h3 className="font-semibold text-grid-900">{title}</h3>
      <p className="mt-1 text-sm leading-relaxed text-slate-600">{detail}</p>
    </div>
  );
}
