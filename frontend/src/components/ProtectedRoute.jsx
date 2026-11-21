import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function RequireAuth({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center text-sm tracking-wider uppercase text-neutral-500">
        Loading...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}

export function RequireAdmin({ children, ownerOnly = false }) {
  const { isAuthenticated, isAdminStaff, isOwner, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-sm tracking-wider uppercase text-neutral-500">
        Loading...
      </div>
    );
  }

  // Hide admin portal from non-admin/unauthenticated users by redirecting to storefront
  if (!isAuthenticated || !isAdminStaff) {
    return <Navigate to="/" replace />;
  }

  if (ownerOnly && !isOwner) {
    return <Navigate to="/admin" replace />;
  }

  return children;
}

export function AdminLoginGuard({ children }) {
  const { isAuthenticated, isAdminStaff, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-sm tracking-wider uppercase text-neutral-500">
        Loading...
      </div>
    );
  }

  // If a customer is signed in, do not expose the admin login view
  if (isAuthenticated && !isAdminStaff) {
    return <Navigate to="/" replace />;
  }

  if (isAuthenticated && isAdminStaff) {
    return <Navigate to="/admin" replace />;
  }

  return children;
}
