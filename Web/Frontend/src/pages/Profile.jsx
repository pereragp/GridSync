import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { changePassword } from "../api/auth";
import { getUser, updateUser } from "../api/users";
import { useAuth } from "../context/AuthContext";
import PageBleedHero from "../components/PageBleedHero";
import { Alert } from "../components/ui";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=2000&q=80";

const ROLE_COPY = {
  Backoffice: {
    eyebrow: "Backoffice",
    title: "Your account",
    blurb:
      "Keep your contact details current and update your password. Administration tools stay on Home and Stations.",
  },
  GridOperator: {
    eyebrow: "Grid Operator",
    title: "Your account",
    blurb:
      "Update how the team reaches you and refresh your password. Booking and station tools stay on your operator home; QR completion is on mobile.",
  },
  Prosumer: {
    eyebrow: "Prosumer",
    title: "Your profile",
    blurb:
      "Manage the contact details on your account and change your password when you need to.",
  },
};

export default function Profile() {
  const { user, homePathFor } = useAuth();
  const roleKey = user.role in ROLE_COPY ? user.role : "Prosumer";
  const copy = ROLE_COPY[roleKey];
  const roleLabel =
    user.role === "GridOperator" ? "Grid Operator" : user.role;

  const [form, setForm] = useState({ fullName: "", phone: "", address: "" });
  const [pageLoading, setPageLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [pwError, setPwError] = useState("");
  const [pwSuccess, setPwSuccess] = useState("");
  const [pwLoading, setPwLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setPageLoading(true);
      try {
        const data = await getUser(user.userId);
        if (!cancelled) {
          setForm({
            fullName: data.fullName || "",
            phone: data.phone || "",
            address: data.address || "",
          });
        }
      } catch (err) {
        if (!cancelled) setError(err.message || "Failed to load profile");
      } finally {
        if (!cancelled) setPageLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user.userId]);

  function setField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      await updateUser(user.userId, form);
      setSuccess("Profile saved. Your contact details are up to date.");
    } catch (err) {
      setError(err.message || "Update failed");
    } finally {
      setLoading(false);
    }
  }

  async function onPasswordSubmit(e) {
    e.preventDefault();
    setPwError("");
    setPwSuccess("");

    if (newPassword !== confirmPassword) {
      setPwError("New password and confirmation do not match.");
      return;
    }

    setPwLoading(true);
    try {
      await changePassword(currentPassword, newPassword);
      setPwSuccess("Password updated. Use the new password next time you sign in.");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      setPwError(err.message || "Change failed");
    } finally {
      setPwLoading(false);
    }
  }

  const displayName = form.fullName || user.fullName || "Member";

  return (
    <div>
      <PageBleedHero
        image={HERO_IMAGE}
        imageAlt="Bright workspace for account management"
        actions={
          <Link
            to={homePathFor(user.role)}
            className="inline-flex items-center justify-center rounded-lg border border-white/30 bg-white/10 px-5 py-2.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/18"
          >
            Back to home
          </Link>
        }
      >
        <div className="flex min-w-0 flex-col gap-5 sm:flex-row sm:items-center">
          <div className="gs-animate-fade-up flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-white/15 text-xl font-semibold text-white ring-2 ring-white/25 backdrop-blur sm:h-20 sm:w-20 sm:text-2xl">
            {initials(displayName)}
          </div>
          <div className="min-w-0">
            <p className="gs-animate-fade-up text-xs font-semibold uppercase tracking-[0.2em] text-grid-100/70">
              {copy.eyebrow}
            </p>
            <h1 className="gs-animate-fade-up mt-2 font-display text-4xl font-semibold leading-[1.1] tracking-tight text-white sm:text-5xl">
              {copy.title}
            </h1>
            <p
              className="gs-animate-fade-up mt-3 max-w-xl text-sm leading-relaxed text-grid-100/85 sm:text-base"
              style={{ animationDelay: "120ms" }}
            >
              {copy.blurb}
            </p>
            <div
              className="gs-animate-fade-up mt-4 flex flex-wrap items-center gap-2"
              style={{ animationDelay: "180ms" }}
            >
              <span className="rounded-full bg-white/15 px-3 py-1 text-xs font-semibold text-white backdrop-blur">
                {roleLabel}
              </span>
              <StatusPill status={user.status} />
              <span className="truncate rounded-full bg-black/20 px-3 py-1 text-xs text-grid-100/90">
                {user.email}
              </span>
            </div>
          </div>
        </div>
      </PageBleedHero>

      <div className="grid gap-6 lg:grid-cols-12">
        <aside className="space-y-4 lg:col-span-4 gs-animate-fade-up" style={{ animationDelay: "80ms" }}>
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
              Snapshot
            </p>
            <h2 className="mt-2 font-display text-xl font-semibold text-grid-900">
              {displayName}
            </h2>
            <dl className="mt-5 space-y-3 text-sm">
              <InfoRow label="Email" value={user.email} />
              <InfoRow label="Role" value={roleLabel} />
              <InfoRow label="Status" value={user.status} />
              {user.nic ? <InfoRow label="NIC" value={user.nic} /> : null}
            </dl>
          </div>

          <div className="rounded-2xl border border-grid-100 bg-gradient-to-br from-grid-50 to-white p-5">
            <p className="text-sm font-semibold text-grid-900">Password tips</p>
            <ul className="mt-2 space-y-1.5 text-xs leading-relaxed text-slate-600">
              <li>At least 8 characters</li>
              <li>Include at least one letter and one number</li>
              <li>Avoid reusing passwords from other systems</li>
            </ul>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-600 shadow-sm">
            <p className="font-semibold text-grid-900">Need help?</p>
            <p className="mt-1.5 leading-relaxed">
              Email cannot be changed here. Contact a Backoffice officer if your
              sign-in email needs updating.
            </p>
          </div>
        </aside>

        <div className="space-y-6 lg:col-span-8">
          {pageLoading ? (
            <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
              <div className="h-4 w-40 animate-pulse rounded bg-slate-200" />
              <div className="mt-6 space-y-3">
                <div className="h-10 animate-pulse rounded-lg bg-slate-100" />
                <div className="h-10 animate-pulse rounded-lg bg-slate-100" />
                <div className="h-10 animate-pulse rounded-lg bg-slate-100" />
              </div>
            </div>
          ) : (
            <>
              <section
                className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8 gs-animate-fade-up"
                style={{ animationDelay: "120ms" }}
              >
                <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h2 className="text-xl font-semibold tracking-tight text-grid-900">
                      Contact details
                    </h2>
                    <p className="mt-1 text-sm text-slate-600">
                      Name, phone, and address used across GridSync.
                    </p>
                  </div>
                </div>

                {error ? (
                  <Alert type="error" onDismiss={() => setError("")}>
                    {error}
                  </Alert>
                ) : null}
                {success ? (
                  <Alert type="success" onDismiss={() => setSuccess("")}>
                    {success}
                  </Alert>
                ) : null}

                <form onSubmit={onSubmit} className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <label className="block text-sm sm:col-span-2">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        Full name
                      </span>
                      <input
                        className={inputClass}
                        value={form.fullName}
                        onChange={(e) => setField("fullName", e.target.value)}
                        placeholder="Your full name"
                        required
                        autoComplete="name"
                      />
                    </label>

                    <label className="block text-sm">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        Phone
                      </span>
                      <input
                        className={inputClass}
                        value={form.phone}
                        onChange={(e) => setField("phone", e.target.value)}
                        placeholder="07xxxxxxxx"
                        required
                        autoComplete="tel"
                      />
                    </label>

                    <label className="block text-sm">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        Email
                      </span>
                      <input
                        className={`${inputClass} cursor-not-allowed bg-slate-50 text-slate-500`}
                        value={user.email}
                        disabled
                        readOnly
                        title="Email cannot be changed from this page"
                      />
                    </label>

                    <label className="block text-sm sm:col-span-2">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        Address
                        <span className="ml-1 font-normal text-slate-400">
                          (optional)
                        </span>
                      </span>
                      <input
                        className={inputClass}
                        value={form.address}
                        onChange={(e) => setField("address", e.target.value)}
                        placeholder="Street, city"
                        autoComplete="street-address"
                      />
                    </label>
                  </div>

                  <div className="flex flex-wrap items-center gap-3 border-t border-slate-100 pt-5">
                    <button
                      type="submit"
                      disabled={loading}
                      className="inline-flex rounded-xl bg-grid-700 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {loading ? "Saving…" : "Save changes"}
                    </button>
                    <p className="text-xs text-slate-500">
                      Changes apply immediately after save.
                    </p>
                  </div>
                </form>
              </section>

              <section
                className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8 gs-animate-fade-up"
                style={{ animationDelay: "180ms" }}
              >
                <div className="mb-6">
                  <h2 className="text-xl font-semibold tracking-tight text-grid-900">
                    Security
                  </h2>
                  <p className="mt-1 text-sm text-slate-600">
                    Change your password using your current credentials.
                  </p>
                </div>

                {pwError ? (
                  <Alert type="error" onDismiss={() => setPwError("")}>
                    {pwError}
                  </Alert>
                ) : null}
                {pwSuccess ? (
                  <Alert type="success" onDismiss={() => setPwSuccess("")}>
                    {pwSuccess}
                  </Alert>
                ) : null}

                <form onSubmit={onPasswordSubmit} className="space-y-4">
                  <label className="block text-sm">
                    <span className="mb-1.5 block font-medium text-slate-700">
                      Current password
                    </span>
                    <div className="relative">
                      <input
                        className={`${inputClass} pr-16`}
                        type={showCurrent ? "text" : "password"}
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        required
                        autoComplete="current-password"
                      />
                      <ToggleVisibility
                        show={showCurrent}
                        onClick={() => setShowCurrent((v) => !v)}
                      />
                    </div>
                  </label>

                  <div className="grid gap-4 sm:grid-cols-2">
                    <label className="block text-sm">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        New password
                      </span>
                      <div className="relative">
                        <input
                          className={`${inputClass} pr-16`}
                          type={showNew ? "text" : "password"}
                          value={newPassword}
                          onChange={(e) => setNewPassword(e.target.value)}
                          required
                          autoComplete="new-password"
                          placeholder="Min 8 chars, letter + number"
                        />
                        <ToggleVisibility
                          show={showNew}
                          onClick={() => setShowNew((v) => !v)}
                        />
                      </div>
                    </label>

                    <label className="block text-sm">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        Confirm new password
                      </span>
                      <input
                        className={inputClass}
                        type={showNew ? "text" : "password"}
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        required
                        autoComplete="new-password"
                        placeholder="Repeat new password"
                      />
                    </label>
                  </div>

                  <div className="flex flex-wrap items-center gap-3 border-t border-slate-100 pt-5">
                    <button
                      type="submit"
                      disabled={pwLoading}
                      className="inline-flex rounded-xl bg-grid-700 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {pwLoading ? "Updating…" : "Update password"}
                    </button>
                    {user.role === "Prosumer" ? (
                      <Link
                        to="/change-password"
                        className="text-sm font-medium text-grid-700 hover:underline"
                      >
                        Open dedicated password page
                      </Link>
                    ) : null}
                  </div>
                </form>
              </section>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function InfoRow({ label, value }) {
  return (
    <div className="flex items-start justify-between gap-3 border-b border-slate-100 pb-3 last:border-0 last:pb-0">
      <dt className="text-slate-500">{label}</dt>
      <dd className="max-w-[60%] truncate text-right font-medium text-grid-900">
        {value || "—"}
      </dd>
    </div>
  );
}

function StatusPill({ status }) {
  const active = status === "Active";
  return (
    <span
      className={`rounded-full px-3 py-1 text-xs font-semibold ${
        active
          ? "bg-emerald-400/25 text-emerald-50"
          : "bg-amber-400/25 text-amber-50"
      }`}
    >
      {status}
    </span>
  );
}

function ToggleVisibility({ show, onClick }) {
  return (
    <button
      type="button"
      className="absolute inset-y-0 right-0 px-3 text-xs font-medium text-grid-700 hover:text-grid-900"
      onClick={onClick}
    >
      {show ? "Hide" : "Show"}
    </button>
  );
}

function initials(name = "") {
  return (
    name
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() || "")
      .join("") || "GS"
  );
}

const inputClass =
  "w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition focus:border-grid-600 focus:ring-2 focus:ring-grid-500/20";
