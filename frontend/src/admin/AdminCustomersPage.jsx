import React, { useState, useEffect } from 'react';
import { Search } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { getApiErrorMessage } from '../services/api';
import { useToast } from '../components/Toast';
import Pagination from '../components/Pagination';
import { formatCurrency, formatDate } from '../utils/format';

export default function AdminCustomersPage() {
  const { showToast } = useToast();
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0 });
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);

  const load = async () => {
    const res = await adminApi.getCustomers({ q: q.trim() || undefined, page, size: 15 });
    setData(res);
  };

  useEffect(() => {
    load();
  }, [page]);

  const handleToggle = async (customer) => {
    try {
      await adminApi.toggleCustomerStatus(customer.id, !customer.active);
      showToast(`Account ${customer.active ? 'deactivated' : 'activated'}`);
      load();
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Customers</h1>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          setPage(0);
          load();
        }}
        className="bg-white border border-[#E5E5E5] p-4 flex gap-2 max-w-md"
      >
        <div className="relative flex-1">
          <Search className="w-3.5 h-3.5 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search customer name or email..."
            className="w-full border border-[#D4D4D4] pl-8 pr-3 py-2 text-xs outline-none focus:border-[#111111]"
          />
        </div>
        <button type="submit" className="bg-[#111111] text-white px-4 py-2 text-xs font-bold uppercase">
          Search
        </button>
      </form>

      <div className="bg-white border border-[#E5E5E5] overflow-x-auto">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-[#E5E5E5] bg-[#FAFAFA] text-[11px] uppercase text-neutral-500">
              <th className="py-3 px-4">Customer</th>
              <th className="py-3 px-3">Phone</th>
              <th className="py-3 px-3">Joined</th>
              <th className="py-3 px-3">Orders</th>
              <th className="py-3 px-3">Total Spent</th>
              <th className="py-3 px-3">Status</th>
              <th className="py-3 px-4 text-right">Action</th>
            </tr>
          </thead>
          <tbody>
            {data.content.map((c) => (
              <tr key={c.id} className="border-b border-[#F0F0F0]">
                <td className="py-3 px-4">
                  <div className="font-semibold">{c.fullName}</div>
                  <div className="text-[11px] text-neutral-500">{c.email}</div>
                </td>
                <td className="py-3 px-3">{c.phone || '—'}</td>
                <td className="py-3 px-3 text-neutral-500">{formatDate(c.createdAt)}</td>
                <td className="py-3 px-3 font-semibold">{c.totalOrders}</td>
                <td className="py-3 px-3 font-bold">{formatCurrency(c.totalSpent)}</td>
                <td className="py-3 px-3">
                  <span className={`px-2 py-0.5 text-[10px] font-bold uppercase border ${c.active ? 'border-emerald-300 bg-emerald-50 text-emerald-800' : 'border-red-300 bg-red-50 text-red-800'}`}>
                    {c.active ? 'Active' : 'Disabled'}
                  </span>
                </td>
                <td className="py-3 px-4 text-right">
                  <button
                    type="button"
                    onClick={() => handleToggle(c)}
                    className="border border-[#D4D4D4] px-3 py-1 text-[11px] font-semibold hover:border-[#111111]"
                  >
                    {c.active ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
    </div>
  );
}
