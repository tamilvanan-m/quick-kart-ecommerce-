import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import CustomerLayout from '../layouts/CustomerLayout';
import AdminLayout from '../layouts/AdminLayout';
import { RequireAuth, RequireAdmin, AdminLoginGuard } from '../components/ProtectedRoute';

import HomePage from '../customer/HomePage';
import ShopPage from '../customer/ShopPage';
import ProductDetailPage from '../customer/ProductDetailPage';
import CartPage from '../customer/CartPage';
import WishlistPage from '../customer/WishlistPage';
import CheckoutPage from '../customer/CheckoutPage';
import OrdersPage from '../customer/OrdersPage';
import OrderDetailPage from '../customer/OrderDetailPage';
import ProfilePage from '../customer/ProfilePage';
import LoginPage from '../customer/LoginPage';
import RegisterPage from '../customer/RegisterPage';
import ForgotPasswordPage from '../customer/ForgotPasswordPage';

import AdminLoginPage from '../admin/AdminLoginPage';
import AdminDashboardPage from '../admin/AdminDashboardPage';
import AdminProductsPage from '../admin/AdminProductsPage';
import AdminProductFormPage from '../admin/AdminProductFormPage';
import AdminCategoriesPage from '../admin/AdminCategoriesPage';
import AdminBrandsPage from '../admin/AdminBrandsPage';
import AdminInventoryPage from '../admin/AdminInventoryPage';
import AdminOrdersPage from '../admin/AdminOrdersPage';
import AdminCustomersPage from '../admin/AdminCustomersPage';
import AdminCouponsPage from '../admin/AdminCouponsPage';
import AdminBannersPage from '../admin/AdminBannersPage';
import AdminReviewsPage from '../admin/AdminReviewsPage';
import AdminReportsPage from '../admin/AdminReportsPage';
import AdminStaffPage from '../admin/AdminStaffPage';
import AdminSettingsPage from '../admin/AdminSettingsPage';

export default function AppRoutes() {
  return (
    <Routes>
      {/* Customer Storefront Routes */}
      <Route element={<CustomerLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/shop" element={<ShopPage />} />
        <Route path="/product/:idOrSlug" element={<ProductDetailPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/wishlist" element={<WishlistPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />

        <Route
          path="/checkout"
          element={
            <RequireAuth>
              <CheckoutPage />
            </RequireAuth>
          }
        />
        <Route
          path="/orders"
          element={
            <RequireAuth>
              <OrdersPage />
            </RequireAuth>
          }
        />
        <Route
          path="/orders/:id"
          element={
            <RequireAuth>
              <OrderDetailPage />
            </RequireAuth>
          }
        />
        <Route
          path="/profile"
          element={
            <RequireAuth>
              <ProfilePage />
            </RequireAuth>
          }
        />
      </Route>

      {/* Admin Portal Routes */}
      <Route
        path="/admin/login"
        element={
          <AdminLoginGuard>
            <AdminLoginPage />
          </AdminLoginGuard>
        }
      />
      <Route
        path="/admin"
        element={
          <RequireAdmin>
            <AdminLayout />
          </RequireAdmin>
        }
      >
        <Route index element={<AdminDashboardPage />} />
        <Route path="products" element={<AdminProductsPage />} />
        <Route path="products/new" element={<AdminProductFormPage />} />
        <Route path="products/:id/edit" element={<AdminProductFormPage />} />
        <Route path="categories" element={<AdminCategoriesPage />} />
        <Route path="brands" element={<AdminBrandsPage />} />
        <Route path="inventory" element={<AdminInventoryPage />} />
        <Route path="orders" element={<AdminOrdersPage />} />
        <Route path="customers" element={<AdminCustomersPage />} />
        <Route path="coupons" element={<AdminCouponsPage />} />
        <Route path="reviews" element={<AdminReviewsPage />} />
        <Route path="banners" element={<AdminBannersPage />} />
        <Route path="reports" element={<AdminReportsPage />} />
        <Route
          path="staff"
          element={
            <RequireAdmin ownerOnly>
              <AdminStaffPage />
            </RequireAdmin>
          }
        />
        <Route path="settings" element={<AdminSettingsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
