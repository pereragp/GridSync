import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getProsumerDashboardStats } from "../api/reservations";
import { getUser } from "../api/users";
import { useAuth } from "../context/AuthContext";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=2000&q=80";

export default function ProsumerHome() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");

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
  const firstName = user.fullName?.split(" ")[0] || "there";

  return (
    <div>
      {/* Full-bleed hero */}
      <section className="relative left-1/2 w-screen max-w-[100vw] -translate-x-1/2 -mt-8 mb-10 overflow-hidden">
        <div className="absolute inset-0">
          <img
            src={HERO_IMAGE}
            alt="Solar panels across a green landscape"
            className="gs-animate-pan h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-br from-grid-900/95 via-grid-800/80 to-grid-700/50" />
          <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-grid-50 to-transparent" />
        </div>

        <div className="relative z-10 mx-auto flex min-h-[22rem] max-w-6xl flex-col justify-end px-4 pb-14 pt-20 sm:min-h-[26rem] sm:px-6 sm:pb-16 sm:pt-24 lg:min-h-[28rem]">
          <h1 className="gs-animate-fade-up max-w-2xl text-4xl font-semibold leading-[1.1] tracking-tight text-white sm:text-5xl lg:text-6xl">
            Welcome back, {firstName}.
          </h1>
          <p
            className="gs-animate-fade-up mt-4 max-w-lg text-sm leading-relaxed text-grid-100/85 sm:text-base"
            style={{ animationDelay: "120ms" }}
          >
            Reserve Charging or Drop-off energy on station batteries, track
            approvals, and present your QR at the hub.
          </p>
          <div
            className="gs-animate-fade-up mt-8 flex flex-wrap gap-3"
            style={{ animationDelay: "220ms" }}
          >
            {isActive ? (
              <Link
                to="/reservations"
                className="inline-flex items-center rounded-lg bg-white px-5 py-2.5 text-sm font-semibold text-grid-800 transition hover:bg-grid-50"
              >
                Book energy
              </Link>
            ) : (
              <span className="inline-flex items-center rounded-lg bg-white/15 px-5 py-2.5 text-sm font-semibold text-white/70">
                Booking unlocks after activation
              </span>
            )}
            <Link
              to="/stations"
              className="inline-flex items-center rounded-lg border border-white/30 bg-white/5 px-5 py-2.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/15"
            >
              Browse stations
            </Link>
          </div>
        </div>
      </section>

      <div className="space-y-12 pb-4">
        {error ? (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
            {error}
          </div>
        ) : null}

        {isPending ? (
          <section className="overflow-hidden rounded-2xl border border-amber-200/80 bg-gradient-to-r from-amber-50 to-orange-50/60 px-6 py-5">
            <h2 className="text-xl font-semibold text-amber-950">
              Account pending approval
            </h2>
            <p className="mt-1 max-w-2xl text-sm text-amber-900/80">
              Your registration is waiting for Backoffice activation. Booking and
              QR features unlock after approval.
            </p>
          </section>
        ) : null}

        {isActive && deactivationRequested ? (
          <section className="rounded-2xl border border-slate-200 bg-slate-50/80 px-6 py-5">
            <h2 className="text-xl font-semibold text-slate-900">
              Deactivation requested
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              Your request is on file. A Backoffice officer will complete the
              process.
            </p>
          </section>
        ) : null}

        {/* Activity snapshot */}
        {isActive ? (
          <section>
            <div className="mb-5 flex items-end justify-between gap-4">
              <div>
                <h2 className="text-2xl font-semibold tracking-tight text-grid-900">
                  Your activity
                </h2>
                <p className="mt-1 text-sm text-slate-600">
                  Live counts from your open energy reservations.
                </p>
              </div>
              <Link
                to="/reservations"
                className="hidden text-sm font-semibold text-grid-700 transition hover:text-grid-900 sm:inline"
              >
                View all →
              </Link>
            </div>
            <div className="grid gap-px overflow-hidden rounded-2xl border border-grid-200/80 bg-grid-200/80 sm:grid-cols-2">
              <StatTile
                label="Pending bookings"
                value={stats?.pendingReservations}
                hint="Awaiting operator review"
              />
              <StatTile
                label="Approved bookings"
                value={stats?.activeReservations}
                hint="Ready for your visit"
                accent
              />
            </div>
          </section>
        ) : null}

        {/* Quick actions — interactive destinations */}
        <section>
          <div className="mb-5">
            <h2 className="text-2xl font-semibold tracking-tight text-grid-900">
              Quick actions
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              Jump into the tools you use most.
            </p>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <ActionLink
              to="/reservations"
              title="Reservations"
              description="Create, update, or cancel Charging and Drop-off bookings."
              disabled={!isActive}
              primary
            />
            <ActionLink
              to="/stations"
              title="Stations"
              description="Explore microgrid hubs and nearby battery nodes."
            />
            <ActionLink
              to="/profile"
              title="Profile"
              description="Update your name, phone, and address."
            />
            <ActionLink
              to="/change-password"
              title="Security"
              description="Change your password and protect your account."
            />
          </div>
        </section>

        {/* How it works */}
        <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-grid-900 via-grid-800 to-grid-900 px-6 py-10 text-white sm:px-10 sm:py-12">
          <div className="relative">
            <h2 className="text-2xl font-semibold tracking-tight sm:text-3xl">
              How battery booking works
            </h2>
            <p className="mt-2 max-w-xl text-sm text-grid-100/70">
              Each station battery tracks capacity, stored energy, and reserved
              Charging / Drop-off pools.
            </p>
            <ol className="mt-8 grid gap-8 border-t border-white/10 pt-8 md:grid-cols-3 md:gap-6">
              <Step
                number="01"
                title="Reserve kWh"
                detail="Charging books free space. Drop-off books stored energy after charging is completed."
              />
              <Step
                number="02"
                title="Get your QR"
                detail="Once approved, your secure transaction QR is ready on the reservation."
              />
              <Step
                number="03"
                title="Complete on site"
                detail="An operator scans your QR and the battery energy balance is updated."
              />
            </ol>
          </div>
        </section>
      </div>
    </div>
  );
}

function StatTile({ label, value, hint, accent = false }) {
  return (
    <div
      className={`bg-white px-6 py-6 sm:px-8 sm:py-7 ${
        accent ? "bg-gradient-to-br from-white to-grid-50" : ""
      }`}
    >
      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
        {label}
      </p>
      <p className="mt-3 text-4xl font-semibold tracking-tight text-grid-900 sm:text-5xl">
        {value ?? "…"}
      </p>
      {hint ? <p className="mt-2 text-sm text-slate-500">{hint}</p> : null}
    </div>
  );
}

function ActionLink({ to, title, description, disabled, primary }) {
  const base =
    "group flex flex-col rounded-2xl border px-5 py-5 transition focus:outline-none focus-visible:ring-2 focus-visible:ring-grid-500/40";

  if (disabled) {
    return (
      <div
        className={`${base} cursor-not-allowed border-slate-200 bg-slate-50 opacity-70`}
      >
        <h3 className="text-lg font-semibold text-slate-500">{title}</h3>
        <p className="mt-1 text-sm text-slate-500">{description}</p>
        <span className="mt-4 text-sm font-semibold text-slate-400">
          Available after activation
        </span>
      </div>
    );
  }

  return (
    <Link
      to={to}
      className={`${base} ${
        primary
          ? "border-grid-600/30 bg-grid-700 text-white hover:bg-grid-800"
          : "border-slate-200 bg-white hover:border-grid-300 hover:bg-grid-50/60"
      }`}
    >
      <h3
        className={`text-lg font-semibold ${
          primary ? "text-white" : "text-grid-900"
        }`}
      >
        {title}
      </h3>
      <p
        className={`mt-1 text-sm ${
          primary ? "text-grid-100/80" : "text-slate-600"
        }`}
      >
        {description}
      </p>
      <span
        className={`mt-4 text-sm font-semibold transition group-hover:translate-x-0.5 ${
          primary ? "text-white" : "text-grid-700"
        }`}
      >
        Open →
      </span>
    </Link>
  );
}

function Step({ number, title, detail }) {
  return (
    <li className="relative">
      <p className="text-3xl font-semibold text-grid-500/80">
        {number}
      </p>
      <h3 className="mt-2 text-lg font-semibold text-white">{title}</h3>
      <p className="mt-1.5 text-sm leading-relaxed text-grid-100/65">{detail}</p>
    </li>
  );
}
