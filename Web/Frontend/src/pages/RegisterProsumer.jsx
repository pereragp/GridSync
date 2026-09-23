import { useState } from "react";
import { Link } from "react-router-dom";
import { registerProsumer } from "../api/users";
import { Alert, Field, PageHeader, btnPrimary, cardClass, inputClass } from "../components/ui";
import { AuthShell } from "./Login";

export default function RegisterProsumer() {
  const [form, setForm] = useState({
    nic: "",
    fullName: "",
    email: "",
    phone: "",
    password: "",
    address: "",
  });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  function set(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      await registerProsumer(form);
      setSuccess("Registered. Status is Pending until Backoffice approval.");
      setForm({ nic: "", fullName: "", email: "", phone: "", password: "", address: "" });
    } catch (err) {
      setError(err.message || "Registration failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell>
      <PageHeader title="Prosumer registration" subtitle="Creates a Pending account (NIC is required)." />
      {error ? <Alert>{error}</Alert> : null}
      {success ? <Alert type="success">{success}</Alert> : null}
      <form onSubmit={onSubmit} className={cardClass}>
        <Field label="NIC">
          <input className={inputClass} value={form.nic} onChange={(e) => set("nic", e.target.value)} required />
        </Field>
        <Field label="Full name">
          <input className={inputClass} value={form.fullName} onChange={(e) => set("fullName", e.target.value)} required />
        </Field>
        <Field label="Email">
          <input className={inputClass} type="email" value={form.email} onChange={(e) => set("email", e.target.value)} required />
        </Field>
        <Field label="Phone">
          <input className={inputClass} value={form.phone} onChange={(e) => set("phone", e.target.value)} required />
        </Field>
        <Field label="Address">
          <input className={inputClass} value={form.address} onChange={(e) => set("address", e.target.value)} />
        </Field>
        <Field label="Password (min 8, letter + number)">
          <input className={inputClass} type="password" value={form.password} onChange={(e) => set("password", e.target.value)} required />
        </Field>
        <button className={`${btnPrimary} w-full`} disabled={loading}>
          {loading ? "Registering…" : "Register"}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        Already have an account?{" "}
        <Link className="text-teal-700 hover:underline" to="/login">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
