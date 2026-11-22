import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, it, expect, vi } from 'vitest';
import ProductCard from '../components/ProductCard';
import { RequireAdmin, AdminLoginGuard } from '../components/ProtectedRoute';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));
vi.mock('../context/CartContext', () => ({
  useCart: () => ({
    isInWishlist: () => false,
    toggleWishlist: vi.fn(),
  }),
}));
vi.mock('../components/Toast', () => ({
  useToast: () => ({ showToast: vi.fn() }),
}));

import { useAuth } from '../context/AuthContext';

describe('Quick kart Frontend Component & Security Route Tests', () => {
  it('displays SOLD OUT badge when product inventory is zero', () => {
    useAuth.mockReturnValue({ isAuthenticated: false });
    const soldOutProduct = {
      id: 1,
      name: 'Minimalist Hoodie',
      slug: 'minimalist-hoodie',
      brandName: 'Kora Studio',
      originalPrice: 2999,
      sellingPrice: 2099,
      discountPercentage: 30,
      status: 'OUT_OF_STOCK',
      totalAvailableStock: 0,
      soldOut: true,
      ratingAvg: 0,
      reviewCount: 0,
      primaryImageUrl: 'https://example.com/hoodie.jpg',
      images: [],
    };

    render(
      <MemoryRouter>
        <ProductCard product={soldOutProduct} />
      </MemoryRouter>
    );

    expect(screen.getByText('SOLD OUT')).toBeInTheDocument();
    expect(screen.getByText('Minimalist Hoodie')).toBeInTheDocument();
    expect(screen.getByText('Kora Studio')).toBeInTheDocument();
  });

  it('redirects customer or unauthenticated users visiting /admin silently to storefront /', () => {
    useAuth.mockReturnValue({
      isAuthenticated: true,
      isAdminStaff: false,
      isOwner: false,
      loading: false,
    });

    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route path="/" element={<div>Public Storefront</div>} />
          <Route
            path="/admin"
            element={
              <RequireAdmin>
                <div>Secret Admin Dashboard</div>
              </RequireAdmin>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.queryByText('Secret Admin Dashboard')).not.toBeInTheDocument();
    expect(screen.getByText('Public Storefront')).toBeInTheDocument();
  });

  it('redirects signed-in customers away from /admin/login to storefront /', () => {
    useAuth.mockReturnValue({
      isAuthenticated: true,
      isAdminStaff: false,
      isOwner: false,
      loading: false,
    });

    render(
      <MemoryRouter initialEntries={['/admin/login']}>
        <Routes>
          <Route path="/" element={<div>Public Storefront</div>} />
          <Route
            path="/admin/login"
            element={
              <AdminLoginGuard>
                <div>Admin Login Form</div>
              </AdminLoginGuard>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.queryByText('Admin Login Form')).not.toBeInTheDocument();
    expect(screen.getByText('Public Storefront')).toBeInTheDocument();
  });

  it('renders admin workspace when authenticated as OWNER/ADMIN/STAFF', () => {
    useAuth.mockReturnValue({
      isAuthenticated: true,
      isAdminStaff: true,
      isOwner: true,
      loading: false,
    });

    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route path="/" element={<div>Public Storefront</div>} />
          <Route
            path="/admin"
            element={
              <RequireAdmin>
                <div>Owner Business Console</div>
              </RequireAdmin>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Owner Business Console')).toBeInTheDocument();
  });
});
