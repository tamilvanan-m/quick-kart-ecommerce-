import React, { useState } from 'react';
import { NavLink, Link, Outlet, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Package,
  PlusSquare,
  FolderTree,
  Tag,
  Boxes,
  ShoppingCart,
  Users,
  TicketPercent,
  MessageSquare,
  Image as ImageIcon,
  BarChart3,
  UserCog,
  Settings,
  LogOut,
  Menu,
  X,
  ExternalLink,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useStore } from '../context/StoreContext';

export default function AdminLayout() {
  const { user, isOwner, logout } = useAuth();
  const { settings } = useStore();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const navItems = [
    { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
    { to: '/admin/products', label: 'Products', icon: Package },
    { to: '/admin/products/new', label: 'Add Product', icon: PlusSquare },
    { to: '/admin/categories', label: 'Categories', icon: FolderTree },
    { to: '/admin/brands', label: 'Brands', icon: Tag },
    { to: '/admin/inventory', label: 'Inventory', icon: Boxes },
    { to: '/admin/orders', label: 'Orders', icon: ShoppingCart },
    { to: '/admin/customers', label: 'Customers', icon: Users },
    { to: '/admin/coupons', label: 'Coupons', icon: TicketPercent },
    { to: '/admin/reviews', label: 'Reviews', icon: MessageSquare },
    { to: '/admin/banners', label: 'Banners', icon: ImageIcon },
    { to: '/admin/reports', label: 'Reports', icon: BarChart3 },
    ...(isOwner ? [{ to: '/admin/staff', label: 'Staff Management', icon: UserCog }] : []),
    { to: '/admin/settings', label: 'Store Settings', icon: Settings },
  ];

  const handleLogout = () => {
    logout();
    navigate('/admin/login');
  };

  const SidebarContent = () => (
    <div className="flex flex-col h-full bg-[#111111] text-white">
      <div className="px-5 py-5 border-b border-neutral-800 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center justify-center w-6 h-6 bg-white text-[#111111] text-[10px] font-bold">
              QK
            </span>
            <span className="text-sm font-bold uppercase tracking-wider">
              {settings.storeName || 'Quick kart'}
            </span>
          </div>
          <p className="text-[10px] text-neutral-400 uppercase tracking-widest mt-1">
            Store Admin Console
          </p>
        </div>
        <button
          type="button"
          onClick={() => setSidebarOpen(false)}
          className="lg:hidden text-neutral-400 hover:text-white"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 text-xs font-medium transition-colors ${
                  isActive
                    ? 'bg-white text-[#111111] font-semibold'
                    : 'text-neutral-300 hover:bg-neutral-900 hover:text-white'
                }`
              }
            >
              <Icon className="w-4 h-4 shrink-0" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <div className="p-4 border-t border-neutral-800 space-y-2">
        <div className="text-[11px] text-neutral-400 px-1 truncate">
          Signed in as <strong className="text-white">{user?.fullName}</strong> ({user?.roles?.[0]})
        </div>
        <Link
          to="/"
          className="w-full flex items-center justify-center gap-2 border border-neutral-700 py-2 text-xs text-neutral-200 hover:bg-neutral-900"
        >
          <ExternalLink className="w-3.5 h-3.5" /> View Customer Store
        </Link>
        <button
          type="button"
          onClick={handleLogout}
          className="w-full flex items-center justify-center gap-2 bg-neutral-900 hover:bg-red-900/40 text-red-300 py-2 text-xs font-semibold"
        >
          <LogOut className="w-3.5 h-3.5" /> Logout
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen flex bg-[#F7F7F7] text-[#111111]">
      {/* Desktop Sidebar */}
      <aside className="hidden lg:block w-64 shrink-0 sticky top-0 h-screen">
        <SidebarContent />
      </aside>

      {/* Mobile Sidebar */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-50 lg:hidden flex">
          <div className="fixed inset-0 bg-black/50" onClick={() => setSidebarOpen(false)} />
          <div className="relative w-64 max-w-full h-full z-10">
            <SidebarContent />
          </div>
        </div>
      )}

      {/* Main Content */}
      <div className="flex-1 min-w-0 flex flex-col">
        <header className="lg:hidden sticky top-0 z-30 bg-white border-b border-[#E5E5E5] px-4 h-14 flex items-center justify-between">
          <button
            type="button"
            onClick={() => setSidebarOpen(true)}
            className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider"
          >
            <Menu className="w-5 h-5" /> Menu
          </button>
          <span className="text-xs font-bold uppercase tracking-widest">
            {settings.storeName || 'Quick kart'} Admin
          </span>
          <Link to="/" className="text-xs underline">
            Store
          </Link>
        </header>

        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-[1400px] w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
