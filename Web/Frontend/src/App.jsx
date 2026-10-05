import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { FeedbackProvider } from "./context/FeedbackContext";
import { ProtectedRoute, PublicOnlyRoute } from "./components/ProtectedRoute";
import Layout from "./components/Layout";
import BackofficeHome from "./pages/BackofficeHome";
import BackofficeUsers from "./pages/BackofficeUsers";
import ChangePassword from "./pages/ChangePassword";
import CreateStaff from "./pages/CreateStaff";
import CreateStation from "./pages/CreateStation";
import ForgotPassword from "./pages/ForgotPassword";
import Login from "./pages/Login";
import OperatorHome from "./pages/OperatorHome";
import OperatorReservations from "./pages/OperatorReservations";
import Profile from "./pages/Profile";
import ResetPassword from "./pages/ResetPassword";
import StationDetail from "./pages/StationDetail";
import Stations from "./pages/Stations";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <FeedbackProvider>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path='/login' element={<Login />} />
            <Route path='/register' element={<Navigate to='/login' replace />} />
            <Route path='/forgot-password' element={<ForgotPassword />} />
            <Route path='/reset-password' element={<ResetPassword />} />
          </Route>
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route element={<ProtectedRoute roles={["Backoffice"]} />}>
                <Route path="/stations/new" element={<CreateStation />} />
              </Route>
              <Route
                element={
                  <ProtectedRoute roles={["Backoffice", "GridOperator"]} />
                }
              >
                <Route path="/stations" element={<Stations />} />
                <Route path="/stations/:id" element={<StationDetail />} />
              </Route>
              <Route path='/profile' element={<Profile />} />
              <Route path='/change-password' element={<ChangePassword />} />
              <Route
                path="/backoffice"
                element={<ProtectedRoute roles={["Backoffice"]} />}
              >
                <Route index element={<BackofficeHome />} />
                <Route path="users" element={<BackofficeUsers />} />
                <Route path="staff/new" element={<CreateStaff />} />
                <Route
                  path="reservations"
                  element={<Navigate to="/backoffice" replace />}
                />
              </Route>
              <Route
                path='/operator'
                element={<ProtectedRoute roles={['GridOperator']} />}
              >
                <Route index element={<OperatorHome />} />
                <Route path='reservations' element={<OperatorReservations />} />
              </Route>
            </Route>
          </Route>
          <Route path='/prosumer' element={<Navigate to='/login' replace />} />
          <Route path='/reservations' element={<Navigate to='/login' replace />} />
          <Route path='*' element={<Navigate to='/login' replace />} />
        </Routes>
        </FeedbackProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
