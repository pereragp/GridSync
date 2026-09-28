import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { changePassword } from "../api/auth";
import { getUser, updateUser } from "../api/users";
import { useAuth } from "../context/AuthContext";
import {
  Alert,
  Field,
  btnPrimary,
  cardClass,
  inputClass,
} from "../components/ui";

export default function Profile() {
  const { user } = useAuth();
  const isOperator = user.role === "GridOperator";
  const [form, setForm] = useState({ fullName: "", phone: "", address: "" });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [pwError, setPwError] = useState("");
  const [pwSuccess, setPwSuccess] = useState("");
  const [pwLoading, setPwLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
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
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user.userId]);

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      await updateUser(user.userId, form);
      setSuccess("Profile updated.");
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
    setPwLoading(true);
    try {
      await changePassword(currentPassword, newPassword);
      setPwSuccess("Password changed successfully.");
      setCurrentPassword("");
      setNewPassword("");
    } catch (err) {
      setPwError(err.message || "Change failed");
    } finally {
      setPwLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <section className="rounded-2xl border border-grid-200/80 bg-white px-5 py-6 shadow-sm sm:px-7">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-grid-600">
          Account
        </p>
        <h1 className="mt-2 font-display text-3xl font-semibold tracking-tight text-grid-900">
          Profile
        </h1>
        <p className="mt-2 text-sm text-slate-600">
          {user.email}
          <span className="mx-1.5 text-slate-300">·</span>
          {isOperator ? "Grid Operator" : user.role}
          <span className="mx-1.5 text-slate-300">·</span>
          {user.status}
        </p>
      </section>

      {error ? <Alert>{error}</Alert> : null}
      {success ? <Alert type="success">{success}</Alert> : null}

      <form onSubmit={onSubmit} className={`${cardClass} rounded-2xl`}>
        <h2 className="mb-4 text-base font-semibold text-grid-900">
          Contact details
        </h2>
        <Field label="Full name">
          <input
            className={inputClass}
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            required
          />
        </Field>
        <Field label="Phone">
          <input
            className={inputClass}
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
            required
          />
        </Field>
        <Field label="Address">
          <input
            className={inputClass}
            value={form.address}
            onChange={(e) => setForm({ ...form, address: e.target.value })}
          />
        </Field>
        <button className={btnPrimary} disabled={loading}>
          {loading ? "Saving…" : "Save changes"}
        </button>
      </form>

      <form onSubmit={onPasswordSubmit} className={`${cardClass} rounded-2xl`}>
        <h2 className="mb-1 text-base font-semibold text-grid-900">
          Change password
        </h2>
        <p className="mb-4 text-sm text-slate-500">
          Requires your current password.
        </p>
        {pwError ? <Alert>{pwError}</Alert> : null}
        {pwSuccess ? <Alert type="success">{pwSuccess}</Alert> : null}
        <Field label="Current password">
          <input
            className={inputClass}
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
            autoComplete="current-password"
          />
        </Field>
        <Field label="New password">
          <input
            className={inputClass}
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            autoComplete="new-password"
          />
        </Field>
        <button className={btnPrimary} disabled={pwLoading}>
          {pwLoading ? "Updating…" : "Update password"}
        </button>
        {!isOperator ? (
          <p className="mt-3 text-xs text-slate-500">
            You can also open the dedicated{" "}
            <Link to="/change-password" className="font-semibold text-grid-700 hover:underline">
              change password
            </Link>{" "}
            page.
          </p>
        ) : null}
      </form>
    </div>
  );
}
