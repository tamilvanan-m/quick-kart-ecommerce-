import React, { useState, useEffect } from 'react';
import { Download } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { formatCurrency } from '../utils/format';

export default function AdminReportsPage() {
  const [stats, setStats] = useState(null);
  const [lowStock, setLowStock] = useState([]);

  useEffect(() => {
    Promise.all([adminApi.getDashboardStats(30), adminApi.getLowStock()]).then(([st, ls]) => {
      setStats(st);
      setLowStock(ls || []);
    });
  }, []);

  const handleExportCsv = async () => {
    const blob = await adminApi.exportOrdersCsv();
    const url = window.URL.createObjectURL(new Blob([blob]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'quickkart-orders-report.csv');
    document.body.appendChild(link);
    link.click();
    link.remove();
  };

  if (!stats) return <div className="py-12 text-center text-xs text-neutral-500">Loading reports...</div>;

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Reports &amp; Business Analytics</h1>
          <p className="text-xs text-neutral-500 mt-0.5">
            Sales performance, category revenue breakdown, and stock reports.
          </p>
        </div>
        <button
          type="button"
          onClick={handleExportCsv}
          className="bg-[#111111] text-white px-5 py-2.5 text-xs font-bold uppercase tracking-wider flex items-center gap-2 hover:bg-neutral-800"
        >
          <Download className="w-4 h-4" /> Export Orders CSV
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white border border-[#E5E5E5] p-5">
          <div className="text-[11px] font-bold uppercase tracking-widest text-neutral-500">Today's Revenue</div>
          <div className="mt-2 text-2xl font-bold">{formatCurrency(stats.todaySales)}</div>
        </div>
        <div className="bg-white border border-[#E5E5E5] p-5">
          <div className="text-[11px] font-bold uppercase tracking-widest text-neutral-500">Monthly Revenue</div>
          <div className="mt-2 text-2xl font-bold">{formatCurrency(stats.monthlySales)}</div>
        </div>
        <div className="bg-white border border-[#E5E5E5] p-5">
          <div className="text-[11px] font-bold uppercase tracking-widest text-neutral-500">All-Time Total Sales</div>
          <div className="mt-2 text-2xl font-bold">{formatCurrency(stats.totalSales)}</div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Category Breakdown */}
        <div className="bg-white border border-[#E5E5E5] p-6">
          <h2 className="text-xs font-bold uppercase tracking-widest mb-4">Category-Wise Sales</h2>
          {stats.categorySales?.length === 0 ? (
            <p className="text-xs text-neutral-500">No category sales yet.</p>
          ) : (
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-[#E5E5E5] text-[11px] uppercase text-neutral-500">
                  <th className="py-2">Category</th>
                  <th className="py-2">Units</th>
                  <th className="py-2 text-right">Revenue</th>
                </tr>
              </thead>
              <tbody>
                {stats.categorySales.map((cs) => (
                  <tr key={cs.categoryName} className="border-b border-[#F5F5F5]">
                    <td className="py-2.5 font-semibold">{cs.categoryName}</td>
                    <td className="py-2.5">{cs.unitsSold}</td>
                    <td className="py-2.5 text-right font-bold">{formatCurrency(cs.revenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Low Stock Report */}
        <div className="bg-white border border-[#E5E5E5] p-6">
          <h2 className="text-xs font-bold uppercase tracking-widest mb-4">
            Low &amp; Zero-Stock Report ({lowStock.length})
          </h2>
          {lowStock.length === 0 ? (
            <p className="text-xs text-neutral-500">All variants are sufficiently stocked.</p>
          ) : (
            <div className="max-h-72 overflow-y-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-[#E5E5E5] text-[11px] uppercase text-neutral-500">
                    <th className="py-2">Product</th>
                    <th className="py-2">Variant</th>
                    <th className="py-2 text-right">Available</th>
                  </tr>
                </thead>
                <tbody>
                  {lowStock.map((it) => (
                    <tr key={it.variantId} className="border-b border-[#F5F5F5]">
                      <td className="py-2 font-medium">{it.productName}</td>
                      <td className="py-2 text-neutral-500">{it.size} / {it.color}</td>
                      <td className={`py-2 text-right font-bold ${it.availableQuantity <= 0 ? 'text-red-700' : 'text-amber-700'}`}>
                        {it.availableQuantity}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
