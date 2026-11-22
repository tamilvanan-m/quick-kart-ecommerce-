import React, { useState, useEffect } from 'react';
import { Star, Trash2 } from 'lucide-react';
import { adminApi } from '../services/adminApi';
import { useToast } from '../components/Toast';
import Pagination from '../components/Pagination';
import { formatDate } from '../utils/format';

export default function AdminReviewsPage() {
  const { showToast } = useToast();
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0 });
  const [page, setPage] = useState(0);

  const load = async () => {
    const res = await adminApi.getReviews(page, 15);
    setData(res);
  };

  useEffect(() => {
    load();
  }, [page]);

  const handleToggle = async (rev) => {
    await adminApi.toggleReviewApproval(rev.id, !rev.approved);
    showToast(rev.approved ? 'Review hidden from storefront' : 'Review approved');
    load();
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this review?')) return;
    await adminApi.deleteReview(id);
    showToast('Review deleted');
    load();
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Customer Reviews</h1>

      <div className="bg-white border border-[#E5E5E5] overflow-x-auto">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-[#E5E5E5] bg-[#FAFAFA] text-[11px] uppercase text-neutral-500">
              <th className="py-3 px-4">Product &amp; Customer</th>
              <th className="py-3 px-3">Rating</th>
              <th className="py-3 px-3">Feedback</th>
              <th className="py-3 px-3">Status</th>
              <th className="py-3 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {data.content.map((r) => (
              <tr key={r.id} className="border-b border-[#F0F0F0]">
                <td className="py-3 px-4">
                  <div className="font-semibold">{r.productName}</div>
                  <div className="text-[11px] text-neutral-500">
                    {r.userName} • {formatDate(r.createdAt)}
                  </div>
                </td>
                <td className="py-3 px-3">
                  <span className="inline-flex items-center gap-1 font-bold">
                    {r.rating} <Star className="w-3.5 h-3.5 fill-[#111111]" />
                  </span>
                </td>
                <td className="py-3 px-3 max-w-md">
                  {r.title && <div className="font-semibold">{r.title}</div>}
                  <div className="text-neutral-600">{r.comment}</div>
                </td>
                <td className="py-3 px-3">
                  <span className={`px-2 py-0.5 text-[10px] font-bold uppercase border ${r.approved ? 'border-emerald-300 bg-emerald-50 text-emerald-800' : 'border-neutral-300 text-neutral-500'}`}>
                    {r.approved ? 'Visible' : 'Hidden'}
                  </span>
                </td>
                <td className="py-3 px-4 text-right space-x-2">
                  <button
                    onClick={() => handleToggle(r)}
                    className="border border-[#D4D4D4] px-2.5 py-1 text-[11px] font-semibold hover:border-[#111111]"
                  >
                    {r.approved ? 'Hide' : 'Approve'}
                  </button>
                  <button onClick={() => handleDelete(r.id)} className="p-1.5 border border-[#D4D4D4] hover:text-red-700">
                    <Trash2 className="w-3.5 h-3.5" />
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
