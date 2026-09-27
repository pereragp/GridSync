import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getProsumerDashboardStats } from "../api/reservations";
import { getUser, requestDeactivation } from "../api/users";
import { useAuth } from "../context/AuthContext";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=1800&q=80";

export default function ProsumerHome() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [data, dash] = await Promise.all([
          getUser(user.userId),
          getProsumerDashboardStats().catch(() => null),
        ]);
        if (!cancelled) {
          setProfile(data);
          setStats(dash);
        }
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
            Book Charging or Drop-off energy on station batteries, track
            approvals, and present your QR at the hub.
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

      {isActive ? (
        <section className="grid gap-4 sm:grid-cols-2">
          <StatCard
            label="Pending bookings"
            value={stats?.pendingReservations}
          />
          <StatCard
            label="Active (approved) bookings"
            value={stats?.activeReservations}
          />
        </section>
      ) : null}

      {isPending ? (
        <section className="rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-amber-950">
          <h2 className="font-semibold">Account pending approval</h2>
          <p className="mt-1 text-sm text-amber-900/80">
            Your registration is waiting for Backoffice activation. Booking and
            QR features unlock after approval.
          </p>
        </section>
      ) : null}

      {isActive && deactivationRequested ? (
        <section className="rounded-2xl border border-slate-200 bg-slate-50 px-5 py-4 text-slate-800">
          <h2 className="font-semibold">Deactivation requested</h2>
          <p className="mt-1 text-sm text-slate-600">
            Your request is on file. A Backoffice officer will complete the
            process.
          </p>
        </section>
      ) : null}

      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <ActionCard
          title="Book energy"
          description="Reserve Charging or Drop-off kWh on a station battery for a visit window."
          to="/reservations"
          cta="Open reservations"
          disabled={!isActive}
        />
        <ActionCard
          title="Stations"
          description="Browse microgrid hubs and nearby nodes before you book."
          to="/stations"
          cta="View stations"
        />
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
        <div className="rounded-2xl border border-dashed border-grid-300 bg-white p-5 sm:col-span-2 lg:col-span-1">
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

      <section>
        <h2 className="text-xl font-semibold tracking-tight text-grid-900">
          How battery booking works
        </h2>
        <p className="mt-1 text-sm text-slate-600">
          Each station battery has capacity, actual stored energy, and reserved
          pools for Charging and Drop-off.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <FeaturePreview
            title="1. Reserve kWh"
            detail="Charging books free space (capacity − actual − reserved). Drop-off books stored energy and only appears after charging is completed via QR."
          />
          <FeaturePreview
            title="2. Get QR"
            detail="After Backoffice/Operator approval, your transaction QR is issued."
          />
          <FeaturePreview
            title="3. Complete on site"
            detail="When the operator scans your QR, actual energy on the battery is updated."
          />
        </div>
      </section>
    </div>
  );
}

function StatCard({ label, value }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-sm text-slate-600">{label}</p>
      <p className="mt-2 text-3xl font-semibold text-grid-800">
        {value ?? "…"}
      </p>
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

function ActionCard({ title, description, to, cta, disabled }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-grid-300 hover:shadow-md">
      <h3 className="font-semibold text-grid-900">{title}</h3>
      <p className="mt-1 text-sm text-slate-600">{description}</p>
      {disabled ? (
        <span className="mt-4 inline-flex rounded-lg bg-slate-100 px-3 py-2 text-sm font-semibold text-slate-400">
          Available after activation
        </span>
      ) : (
        <Link
          to={to}
          className="mt-4 inline-flex rounded-lg bg-grid-700 px-3 py-2 text-sm font-semibold text-white transition hover:bg-grid-800"
        >
          {cta}
        </Link>
      )}
    </div>
  );
}

function FeaturePreview({ title, detail }) {
  return (
    <div className="rounded-2xl border border-grid-100 bg-gradient-to-br from-grid-50 to-white p-5">
      <h3 className="font-semibold text-grid-900">{title}</h3>
      <p className="mt-1 text-sm leading-relaxed text-slate-600">{detail}</p>
    </div>
  );
}
