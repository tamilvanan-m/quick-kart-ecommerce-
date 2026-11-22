import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Plus, AlertTriangle, ArrowUpRight } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { formatCurrency, formatDate, ORDER_STATUS_LABEL } from '../utils/format';

export default function AdminDashboardPage() {
  const [stats, setStats] = useState(null);
  const [days, setDays] = useState(14);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    adminApi
      .getDashboardStats(days)
      .then((res) => setStats(res))
      .finally(() => setLoading(false));
  }, [days]);

  if (loading || !stats) {
    return <div className="py-16 text-center text-xs text-neutral-500">Loading store analytics...</div>;
  }

  const maxRevenue = Math.max(1, ...(stats.revenueChart || []).map((p) => Number(p.revenue || 0)));

  const metricCards = [
    { label: 'Total Sales', value: formatCurrency(stats.totalSales), sub: `This month: ${formatCurrency(stats.monthlySales)}` },
    { label: 'Total Orders', value: stats.totalOrders, sub: `Pending: ${stats.pendingOrders} • Delivered: ${stats.deliveredOrders}` },
    { label: 'Catalog Products', value: stats.totalProducts, sub: `Out of Stock: ${stats.outOfStockProducts}` },
    { label: 'Customers', value: stats.totalCustomers, sub: `Low-stock variants: ${stats.lowStockProducts}` },
  ];

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Business Overview</h1>
          <p className="text-xs text-neutral-500 mt-0.5">Real-time store performance calculated from live database records.</p>
        </div>
        <div className="flex items-center gap-3">
          <select
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
            className="border border-[#D4D4D4] bg-white px-3 py-2 text-xs outline-none focus:border-[#111111]"
          >
            <option value={7}>Last 7 days</option>
            <option value={14}>Last 14 days</option>
            <option value={30}>Last 30 days</option>
          </select>
          <Link
            to="/admin/products/new"
            className="bg-[#111111] text-white px-4 py-2 text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 hover:bg-neutral-800"
          >
            <Plus className="w-3.5 h-3.5" /> Add Product
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {metricCards.map((card) => (
          <div key={card.label} className="bg-white border border-[#E5E5E5] p-5">
            <div className="text-[11px] font-bold uppercase tracking-widest text-neutral-500">
              {card.label}
            </div>
            <div className="mt-2 text-2xl font-bold text-[#111111]">{card.value}</div>
            <div className="mt-1.5 text-[11px] text-neutral-500">{card.sub}</div>
          </div>
        ))}
      </div>

      {/* Low Stock / Out of Stock Alert */}
      {(stats.lowStockProducts > 0 || stats.outOfStockProducts > 0) && (
        <div className="bg-white border border-amber-300 p-4 flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2.5 text-amber-900">
            <AlertTriangle className="w-4 h-4 text-amber-700 shrink-0" />
            <span>
              Inventory Alert: <strong>{stats.lowStockProducts}</strong> variant(s) are low in stock and{' '}
              <strong>{stats.outOfStockProducts}</strong> product(s) are currently <strong>SOLD OUT</strong>.
            </span>
          </div>
          <Link to="/admin/inventory" className="font-bold underline text-[#111111]">
            Manage Stock &rarr;
          </Link>
        </div>
      )}

      {/* Sales Chart + Top Selling */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-8 bg-white border border-[#E5E5E5] p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xs font-bold uppercase tracking-widest">Revenue Analytics (₹)</h2>
            <span className="text-[11px] text-neutral-500">Daily sales trend</span>
          </div>

          <div className="h-56 flex items-end gap-1.5 sm:gap-2 border-b border-[#E5E5E5] pb-2">
            {(stats.revenueChart || []).map((point) => {
              const pct = Math.max(4, Math.round((Number(point.revenue || 0) / maxRevenue) * 100));
              return (
                <div key={point.label} className="flex-1 flex flex-col items-center justify-end h-full group relative">
                  <div className="hidden group-hover:block absolute -top-8 bg-[#111111] text-white text-[10px] px-2 py-1 whitespace-nowrap z-10">
                    {point.label}: {formatCurrency(point.revenue)} ({point.orderCount} orders)
                  </div>
                  <div
                    style={{ height: `${Number(point.revenue) > 0 ? pct : 2}%` }}
                    className={`w-full transition-all ${
                      Number(point.revenue) > 0 ? 'bg-[#111111] hover:bg-neutral-700' : 'bg-neutral-200'
                    }`}
                  />
                </div>
              );
            })}
          </div>
          <div className="flex justify-between text-[10px] text-neutral-400 mt-2">
            <span>{stats.revenueChart?.[0]?.label}</span>
            <span>{stats.revenueChart?.[Math.floor((stats.revenueChart?.length || 1) / 2)]?.label}</span>
            <span>{stats.revenueChart?.[stats.revenueChart?.length - 1]?.label}</span>
          </div>
        </div>

        <div className="lg:col-span-4 bg-white border border-[#E5E5E5] p-6">
          <h2 className="text-xs font-bold uppercase tracking-widest mb-4">Top-Selling Products</h2>
          {stats.topSellingProducts?.length === 0 ? (
            <p className="text-xs text-neutral-500">No sales recorded yet.</p>
          ) : (
            <div className="space-y-3.5 text-xs">
              {stats.topSellingProducts.map((tp) => (
                <div key={tp.productId} className="flex items-start justify-between gap-2 pb-3 border-b border-[#F5F5F5] last:border-b-0">
                  <div className="min-w-0">
                    <div className="font-semibold truncate">{tp.productName}</div>
                    <div className="text-[11px] text-neutral-500">
                      {tp.brandName} • {tp.unitsSold} units sold
                    </div>
                  </div>
                  <div className="font-bold shrink-0">{formatCurrency(tp.revenue)}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Recent Orders */}
      <div className="bg-white border border-[#E5E5E5] p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xs font-bold uppercase tracking-widest">Recent Orders</h2>
          <Link to="/admin/orders" className="text-xs font-semibold underline flex items-center gap-1">
            All Orders <ArrowUpRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#E5E5E5] text-[11px] uppercase tracking-wider text-neutral-500">
                <th className="py-2.5 pr-4">Order #</th>
                <th className="py-2.5 pr-4">Customer</th>
                <th className="py-2.5 pr-4">Date</th>
                <th className="py-2.5 pr-4">Payment</th>
                <th className="py-2.5 pr-4">Status</th>
                <th className="py-2.5 text-right">Amount</th>
              </tr>
            </thead>
            <tbody>
              {(stats.recentOrders || []).map((o) => (
                <tr key={o.id} className="border-b border-[#F5F5F5] last:border-b-0">
                  <td className="py-3 pr-4 font-bold">{o.orderNumber}</td>
                  <td className="py-3 pr-4">{o.shippingFullName}</td>
                  <td className="py-3 pr-4 text-neutral-500">{formatDate(o.createdAt)}</td>
                  <td className="py-3 pr-4">
                    {o.paymentMethod} ({o.paymentStatus})
                  </td>
                  <td className="py-3 pr-4">
                    <span className="px-2 py-0.5 border border-[#D4D4D4] text-[10px] font-semibold uppercase">
                      {ORDER_STATUS_LABEL[o.orderStatus] || o.orderStatus}
                    </span>
                  </td>
                  <td className="py-3 text-right font-bold">{formatCurrency(o.totalAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
