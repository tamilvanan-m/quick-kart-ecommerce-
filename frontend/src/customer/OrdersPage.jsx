import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Package, ChevronRight } from 'lucide-react';
import { customerApi } from '../services/customerApi';
import Pagination from '../components/Pagination';
import { formatCurrency, formatDate, ORDER_STATUS_LABEL, resolveImageUrl } from '../utils/format';

export default function OrdersPage() {
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  useEffect(() => {
    setLoading(true);
    customerApi
      .getMyOrders(page, 10)
      .then((res) => setData(res))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-10">
      <h1 className="text-2xl font-bold tracking-tight uppercase pb-6 border-b border-[#E5E5E5]">
        My Orders &amp; Tracking
      </h1>

      {loading ? (
        <div className="py-12 text-center text-xs text-neutral-500">Loading your orders...</div>
      ) : data.content.length === 0 ? (
        <div className="py-20 text-center">
          <Package className="w-10 h-10 mx-auto text-neutral-400" />
          <p className="mt-3 text-sm text-neutral-500">You haven't placed any orders yet.</p>
          <Link to="/shop" className="inline-block mt-5 bg-[#111111] text-white px-7 py-3 text-xs font-bold uppercase tracking-widest">
            Start Shopping
          </Link>
        </div>
      ) : (
        <div className="mt-6 space-y-4">
          {data.content.map((order) => (
            <Link
              key={order.id}
              to={`/orders/${order.id}`}
              className="block border border-[#E5E5E5] bg-white hover:border-[#111111] transition-colors p-5"
            >
              <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-[#F0F0F0] text-xs">
                <div>
                  <span className="font-bold text-sm">{order.orderNumber}</span>
                  <span className="ml-3 text-neutral-500">Placed on {formatDate(order.createdAt)}</span>
                </div>
                <div className="flex items-center gap-3">
                  <span
                    className={`px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider border ${
                      order.orderStatus === 'CANCELLED'
                        ? 'border-red-300 text-red-700 bg-red-50'
                        : order.orderStatus === 'DELIVERED'
                        ? 'border-emerald-300 text-emerald-800 bg-emerald-50'
                        : 'border-[#111111] text-[#111111]'
                    }`}
                  >
                    {ORDER_STATUS_LABEL[order.orderStatus] || order.orderStatus}
                  </span>
                  <ChevronRight className="w-4 h-4 text-neutral-400" />
                </div>
              </div>

              <div className="mt-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-3 overflow-x-auto">
                  {order.items?.slice(0, 3).map((item) => (
                    <img
                      key={item.id}
                      src={resolveImageUrl(item.imageUrl)}
                      alt={item.productName}
                      className="w-14 h-18 object-cover bg-[#F5F5F5] border border-[#E5E5E5] shrink-0"
                    />
                  ))}
                  <div className="text-xs">
                    <div className="font-medium">
                      {order.items?.[0]?.productName}
                      {order.items?.length > 1 ? ` + ${order.items.length - 1} more item(s)` : ''}
                    </div>
                    <div className="text-neutral-500 mt-0.5">
                      Payment: {order.paymentMethod} ({order.paymentStatus})
                    </div>
                  </div>
                </div>

                <div className="text-right text-sm font-bold shrink-0">
                  {formatCurrency(order.totalAmount)}
                </div>
              </div>
            </Link>
          ))}

          <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </div>
      )}
    </div>
  );
}
