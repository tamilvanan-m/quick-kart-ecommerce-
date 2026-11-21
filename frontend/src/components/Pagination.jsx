import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export default function Pagination({ page, totalPages, onPageChange }) {
  if (!totalPages || totalPages <= 1) return null;

  const pages = [];
  const start = Math.max(0, page - 2);
  const end = Math.min(totalPages - 1, page + 2);
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  return (
    <div className="flex items-center justify-center gap-1.5 mt-10">
      <button
        type="button"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
        className="px-3 py-2 border border-[#E5E5E5] text-xs font-medium disabled:opacity-40 hover:border-[#111111] flex items-center gap-1"
      >
        <ChevronLeft className="w-3.5 h-3.5" /> Prev
      </button>

      {pages.map((p) => (
        <button
          key={p}
          type="button"
          onClick={() => onPageChange(p)}
          className={`w-9 h-9 text-xs font-medium border transition-colors ${
            p === page
              ? 'bg-[#111111] text-white border-[#111111]'
              : 'bg-white text-[#111111] border-[#E5E5E5] hover:border-[#111111]'
          }`}
        >
          {p + 1}
        </button>
      ))}

      <button
        type="button"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className="px-3 py-2 border border-[#E5E5E5] text-xs font-medium disabled:opacity-40 hover:border-[#111111] flex items-center gap-1"
      >
        Next <ChevronRight className="w-3.5 h-3.5" />
      </button>
    </div>
  );
}
