import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search, User, Heart, ShoppingBag, Menu, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useStore } from '../context/StoreContext';
import { catalogApi } from '../services/catalogApi';
import { formatCurrency, resolveImageUrl } from '../utils/format';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const { cartCount, wishlistCount } = useCart();
  const { categories, settings } = useStore();
  const navigate = useNavigate();

  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const searchRef = useRef(null);

  // Top-level navigation items dynamically synced from DB categories
  const navRoots = categories.length > 0
    ? categories
    : [
        { name: 'Men', slug: 'men', genderScope: 'MEN', children: [] },
        { name: 'Women', slug: 'women', genderScope: 'WOMEN', children: [] },
        { name: 'Kids', slug: 'kids', genderScope: 'KIDS', children: [] },
        { name: 'Footwear', slug: 'footwear', genderScope: 'FOOTWEAR', children: [] },
        { name: 'Accessories', slug: 'accessories', genderScope: 'ACCESSORIES', children: [] },
        { name: 'Beauty', slug: 'beauty', genderScope: 'BEAUTY', children: [] },
      ];

  useEffect(() => {
    const trimmed = query.trim();
    if (trimmed.length < 2) {
      setSuggestions([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        const res = await catalogApi.searchSuggestions(trimmed);
        setSuggestions(res || []);
      } catch {
        setSuggestions([]);
      }
    }, 220);
    return () => clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    const onClickOutside = (e) => {
      if (searchRef.current && !searchRef.current.contains(e.target)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setShowSuggestions(false);
    setMobileMenuOpen(false);
    navigate(query.trim() ? `/shop?q=${encodeURIComponent(query.trim())}` : '/shop');
  };

  return (
    <header className="sticky top-0 z-40 bg-white border-b border-[#E5E5E5]">
      {/* Announcement Bar */}
      {settings.storeAnnouncement && (
        <div className="bg-[#111111] text-white text-[11px] tracking-widest uppercase py-2 px-4 text-center font-medium">
          {settings.storeAnnouncement}
        </div>
      )}

      <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 h-16 flex items-center justify-between gap-4">
        {/* Left: Mobile toggle + Brand */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setMobileMenuOpen(true)}
            className="lg:hidden p-1.5 text-[#111111] hover:bg-[#F5F5F5]"
            aria-label="Open menu"
          >
            <Menu className="w-5 h-5" />
          </button>

          <Link to="/" className="flex items-center gap-2 shrink-0">
            <span className="inline-flex items-center justify-center w-8 h-8 bg-[#111111] text-white text-xs font-bold tracking-tighter">
              QK
            </span>
            <span className="text-lg sm:text-xl font-bold tracking-tight uppercase text-[#111111]">
              {settings.storeName || 'Quick kart'}
            </span>
          </Link>
        </div>

        {/* Center: Primary Navigation */}
        <nav className="hidden lg:flex items-center gap-6 h-full">
          {navRoots.map((cat) => {
            const targetUrl =
              cat.genderScope && ['MEN', 'WOMEN', 'KIDS'].includes(cat.genderScope)
                ? `/shop?gender=${cat.genderScope}`
                : `/shop?category=${cat.slug}`;
            return (
              <div key={cat.slug} className="relative group h-full flex items-center">
                <Link
                  to={targetUrl}
                  className="text-xs font-semibold uppercase tracking-[0.12em] text-[#111111] hover:text-neutral-500 py-5 border-b-2 border-transparent group-hover:border-[#111111] transition-colors"
                >
                  {cat.name}
                </Link>
                {cat.children && cat.children.length > 0 && (
                  <div className="hidden group-hover:block absolute top-full left-0 w-56 bg-white border border-[#E5E5E5] shadow-md py-3 px-4 z-50">
                    <div className="text-[10px] font-semibold uppercase tracking-widest text-neutral-400 mb-2">
                      {cat.name} Categories
                    </div>
                    <ul className="space-y-1.5">
                      {cat.children.map((sub) => (
                        <li key={sub.id}>
                          <Link
                            to={`/shop?category=${sub.slug}`}
                            className="block text-xs text-[#111111] hover:underline py-0.5"
                          >
                            {sub.name}
                          </Link>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            );
          })}
        </nav>

        {/* Right: Search + Actions */}
        <div className="flex items-center gap-2 sm:gap-4 flex-1 lg:flex-none justify-end">
          {/* Desktop Search */}
          <div ref={searchRef} className="relative hidden md:block w-64 xl:w-80">
            <form onSubmit={handleSearchSubmit}>
              <div className="relative">
                <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={query}
                  onFocus={() => setShowSuggestions(true)}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    setShowSuggestions(true);
                  }}
                  placeholder="Search shirts, dresses, brands..."
                  className="w-full bg-[#F5F5F5] border border-transparent focus:border-[#111111] focus:bg-white text-xs py-2 pl-9 pr-3 outline-none transition-colors"
                />
              </div>
            </form>

            {showSuggestions && suggestions.length > 0 && (
              <div className="absolute left-0 right-0 top-full mt-1 bg-white border border-[#E5E5E5] shadow-lg z-50 max-h-96 overflow-y-auto">
                {suggestions.map((item) => (
                  <Link
                    key={item.id}
                    to={`/product/${item.slug}`}
                    onClick={() => setShowSuggestions(false)}
                    className="flex items-center gap-3 px-3 py-2.5 hover:bg-[#F7F7F7] border-b border-[#F0F0F0] last:border-b-0"
                  >
                    <img
                      src={resolveImageUrl(item.imageUrl)}
                      alt={item.name}
                      className="w-10 h-12 object-cover bg-[#F5F5F5] shrink-0"
                    />
                    <div className="min-w-0 flex-1">
                      <div className="text-[10px] uppercase tracking-wider text-neutral-500">
                        {item.brandName} • {item.categoryName}
                      </div>
                      <div className="text-xs font-medium text-[#111111] truncate">{item.name}</div>
                    </div>
                    <div className="text-xs font-semibold text-[#111111]">
                      {formatCurrency(item.sellingPrice)}
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>

          {/* Profile */}
          <div className="relative">
            <button
              type="button"
              onClick={() => {
                if (!isAuthenticated) {
                  navigate('/login');
                } else {
                  setProfileMenuOpen((v) => !v);
                }
              }}
              className="flex flex-col items-center p-1.5 text-[#111111] hover:text-neutral-600"
              aria-label="Account"
            >
              <User className="w-5 h-5" strokeWidth={1.75} />
              <span className="hidden xl:block text-[10px] font-medium mt-0.5">
                {isAuthenticated ? user?.fullName?.split(' ')[0] : 'Sign in'}
              </span>
            </button>

            {isAuthenticated && profileMenuOpen && (
              <div
                onMouseLeave={() => setProfileMenuOpen(false)}
                className="absolute right-0 top-full mt-1 w-52 bg-white border border-[#E5E5E5] shadow-lg py-2 z-50"
              >
                <div className="px-4 py-2 border-b border-[#F0F0F0]">
                  <div className="text-xs font-semibold truncate">{user?.fullName}</div>
                  <div className="text-[11px] text-neutral-500 truncate">{user?.email}</div>
                </div>
                <Link
                  to="/profile"
                  onClick={() => setProfileMenuOpen(false)}
                  className="block px-4 py-2 text-xs hover:bg-[#F7F7F7]"
                >
                  My Profile &amp; Addresses
                </Link>
                <Link
                  to="/orders"
                  onClick={() => setProfileMenuOpen(false)}
                  className="block px-4 py-2 text-xs hover:bg-[#F7F7F7]"
                >
                  My Orders &amp; Tracking
                </Link>
                <Link
                  to="/wishlist"
                  onClick={() => setProfileMenuOpen(false)}
                  className="block px-4 py-2 text-xs hover:bg-[#F7F7F7]"
                >
                  Saved Wishlist
                </Link>
                <button
                  type="button"
                  onClick={() => {
                    logout();
                    setProfileMenuOpen(false);
                    navigate('/');
                  }}
                  className="w-full text-left px-4 py-2 text-xs text-red-700 hover:bg-[#F7F7F7] border-t border-[#F0F0F0] mt-1"
                >
                  Sign Out
                </button>
              </div>
            )}
          </div>

          {/* Wishlist */}
          <Link
            to="/wishlist"
            className="relative flex flex-col items-center p-1.5 text-[#111111] hover:text-neutral-600"
            aria-label="Wishlist"
          >
            <Heart className="w-5 h-5" strokeWidth={1.75} />
            <span className="hidden xl:block text-[10px] font-medium mt-0.5">Wishlist</span>
            {wishlistCount > 0 && (
              <span className="absolute top-0.5 right-0.5 bg-[#111111] text-white text-[9px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
                {wishlistCount}
              </span>
            )}
          </Link>

          {/* Bag */}
          <Link
            to="/cart"
            className="relative flex flex-col items-center p-1.5 text-[#111111] hover:text-neutral-600"
            aria-label="Shopping Bag"
          >
            <ShoppingBag className="w-5 h-5" strokeWidth={1.75} />
            <span className="hidden xl:block text-[10px] font-medium mt-0.5">Bag</span>
            {cartCount > 0 && (
              <span className="absolute top-0.5 right-0.5 bg-[#111111] text-white text-[9px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
                {cartCount}
              </span>
            )}
          </Link>
        </div>
      </div>

      {/* Mobile Search Bar */}
      <div className="md:hidden px-4 pb-3">
        <form onSubmit={handleSearchSubmit}>
          <div className="relative">
            <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search Quick kart..."
              className="w-full bg-[#F5F5F5] text-xs py-2 pl-9 pr-3 outline-none"
            />
          </div>
        </form>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="fixed inset-0 z-50 lg:hidden flex">
          <div className="fixed inset-0 bg-black/40" onClick={() => setMobileMenuOpen(false)} />
          <div className="relative w-80 max-w-full bg-white h-full overflow-y-auto p-6 flex flex-col">
            <div className="flex items-center justify-between pb-4 border-b border-[#E5E5E5]">
              <span className="text-base font-bold uppercase tracking-tight">
                {settings.storeName || 'Quick kart'}
              </span>
              <button onClick={() => setMobileMenuOpen(false)} aria-label="Close menu">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="py-4 space-y-4 flex-1">
              {navRoots.map((cat) => (
                <div key={cat.slug} className="border-b border-[#F0F0F0] pb-3">
                  <Link
                    to={
                      cat.genderScope && ['MEN', 'WOMEN', 'KIDS'].includes(cat.genderScope)
                        ? `/shop?gender=${cat.genderScope}`
                        : `/shop?category=${cat.slug}`
                    }
                    onClick={() => setMobileMenuOpen(false)}
                    className="block text-sm font-semibold uppercase tracking-wider text-[#111111]"
                  >
                    {cat.name}
                  </Link>
                  {cat.children?.length > 0 && (
                    <div className="mt-2 pl-2 flex flex-wrap gap-x-4 gap-y-1">
                      {cat.children.map((sub) => (
                        <Link
                          key={sub.id}
                          to={`/shop?category=${sub.slug}`}
                          onClick={() => setMobileMenuOpen(false)}
                          className="text-xs text-neutral-600"
                        >
                          {sub.name}
                        </Link>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
