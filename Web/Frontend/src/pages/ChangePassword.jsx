import { useState } from "react";
import { changePassword } from "../api/auth";
import { Alert, Field, PageHeader, btnPrimary, cardClass, inputClass } from "../components/ui";

export default function ChangePassword() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      await changePassword(currentPassword, newPassword);
      setSuccess("Password changed successfully.");
      setCurrentPassword("");
      setNewPassword("");
    } catch (err) {
      setError(err.message || "Change failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-lg">
      <PageHeader title="Change password" subtitle="Requires your current password." />
      {error ? <Alert>{error}</Alert> : null}
      {success ? <Alert type="success">{success}</Alert> : null}
      <form onSubmit={onSubmit} className={cardClass}>
        <Field label="Current password">
          <input className={inputClass} type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required />
        </Field>
        <Field label="New password">
          <input className={inputClass} type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
        </Field>
        <button className={btnPrimary} disabled={loading}>
          {loading ? "Saving…" : "Update password"}
        </button>
      </form>
    </div>
  );
}
