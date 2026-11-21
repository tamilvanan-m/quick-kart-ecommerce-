import React from 'react';
import { Link } from 'react-router-dom';
import { useStore } from '../context/StoreContext';

export default function Footer() {
  const { settings, categories } = useStore();

  return (
    <footer className="bg-[#111111] text-white mt-20">
      <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-14 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-10">
        <div>
          <div className="flex items-center gap-2 mb-4">
            <span className="inline-flex items-center justify-center w-7 h-7 bg-white text-[#111111] text-xs font-bold">
              QK
            </span>
            <span className="text-lg font-bold tracking-tight uppercase">
              {settings.storeName || 'Quick kart'}
            </span>
          </div>
          <p className="text-xs text-neutral-400 leading-relaxed max-w-xs">
            Contemporary fashion essentials, curated menswear, womenswear, footwear, and accessories crafted for everyday refinement.
          </p>
          {settings.gstNumber && (
            <p className="mt-4 text-[11px] text-neutral-500">GSTIN: {settings.gstNumber}</p>
          )}
        </div>

        <div>
          <h4 className="text-xs font-semibold uppercase tracking-widest mb-4">Shop</h4>
          <ul className="space-y-2.5 text-xs text-neutral-400">
            {categories.slice(0, 6).map((cat) => (
              <li key={cat.id || cat.slug}>
                <Link
                  to={
                    cat.genderScope && ['MEN', 'WOMEN', 'KIDS'].includes(cat.genderScope)
                      ? `/shop?gender=${cat.genderScope}`
                      : `/shop?category=${cat.slug}`
                  }
                  className="hover:text-white transition-colors"
                >
                  {cat.name}
                </Link>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <h4 className="text-xs font-semibold uppercase tracking-widest mb-4">Customer Care</h4>
          <ul className="space-y-2.5 text-xs text-neutral-400">
            <li>
              <Link to="/orders" className="hover:text-white transition-colors">
                Track Orders
              </Link>
            </li>
            <li>
              <Link to="/profile" className="hover:text-white transition-colors">
                My Account &amp; Addresses
              </Link>
            </li>
            <li>
              <Link to="/wishlist" className="hover:text-white transition-colors">
                Saved Wishlist
              </Link>
            </li>
            <li className="text-neutral-400">
              {settings.returnPolicyDays || 14}-Day Easy Returns
            </li>
          </ul>
        </div>

        <div>
          <h4 className="text-xs font-semibold uppercase tracking-widest mb-4">Contact</h4>
          <ul className="space-y-2.5 text-xs text-neutral-400">
            <li>{settings.contactEmail || 'support@quickkart.com'}</li>
            {settings.contactPhone && <li>{settings.contactPhone}</li>}
            {settings.businessAddress && (
              <li className="leading-relaxed">{settings.businessAddress}</li>
            )}
          </ul>
        </div>
      </div>

      <div className="border-t border-neutral-800">
        <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-5 flex flex-col sm:flex-row items-center justify-between gap-3 text-[11px] text-neutral-500">
          <span>
            &copy; {new Date().getFullYear()} {settings.storeName || 'Quick kart'}. All rights reserved.
          </span>
          <span>100% Authentic Apparel • Secure Payments • Fast Pan-India Delivery</span>
        </div>
      </div>
    </footer>
  );
}
