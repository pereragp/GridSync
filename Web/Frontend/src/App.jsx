import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import { ProtectedRoute, PublicOnlyRoute } from "./components/ProtectedRoute";
import { AuthProvider } from "./context/AuthContext";
import BackofficeHome from "./pages/BackofficeHome";
import ChangePassword from "./pages/ChangePassword";
import CreateStaff from "./pages/CreateStaff";
import ForgotPassword from "./pages/ForgotPassword";
import Login from "./pages/Login";
import OperatorHome from "./pages/OperatorHome";
import Profile from "./pages/Profile";
import ProsumerHome from "./pages/ProsumerHome";
import RegisterProsumer from "./pages/RegisterProsumer";
import ResetPassword from "./pages/ResetPassword";

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<RegisterProsumer />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
          </Route>

          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route element={<ProtectedRoute roles={["Backoffice"]} />}>
                <Route path="/backoffice" element={<BackofficeHome />} />
                <Route path="/backoffice/staff/new" element={<CreateStaff />} />
              </Route>
              <Route element={<ProtectedRoute roles={["GridOperator"]} />}>
                <Route path="/operator" element={<OperatorHome />} />
              </Route>
              <Route element={<ProtectedRoute roles={["Prosumer"]} />}>
                <Route path="/prosumer" element={<ProsumerHome />} />
              </Route>
              <Route path="/profile" element={<Profile />} />
              <Route path="/change-password" element={<ChangePassword />} />
            </Route>
          </Route>

          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
