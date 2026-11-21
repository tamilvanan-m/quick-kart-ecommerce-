import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { SlidersHorizontal, X } from 'lucide-react';
import { catalogApi } from '../services/catalogApi';
import { useStore } from '../context/StoreContext';
import ProductCard from '../components/ProductCard';
import Pagination from '../components/Pagination';

const SIZES = ['XS', 'S', 'M', 'L', 'XL', '30', '32', '34', 'UK 7', 'UK 8', 'UK 9', 'ONE SIZE'];
const COLORS = ['Jet Black', 'Crisp White', 'Indigo Wash', 'Charcoal', 'Ivory', 'Obsidian'];

export default function ShopPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { categories, brands } = useStore();

  const [data, setData] = useState({ content: [], page: 0, totalElements: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);

  const flatCategories = [];
  categories.forEach((root) => {
    flatCategories.push(root);
    (root.children || []).forEach((c) => flatCategories.push(c));
  });

  const q = searchParams.get('q') || '';
  const gender = searchParams.get('gender') || '';
  const category = searchParams.get('category') || '';
  const brand = searchParams.get('brand') || '';
  const size = searchParams.get('size') || '';
  const color = searchParams.get('color') || '';
  const minPrice = searchParams.get('minPrice') || '';
  const maxPrice = searchParams.get('maxPrice') || '';
  const minDiscount = searchParams.get('minDiscount') || '';
  const inStockOnly = searchParams.get('inStockOnly') === 'true';
  const trending = searchParams.get('trending') === 'true';
  const sort = searchParams.get('sort') || 'recommended';
  const page = Number(searchParams.get('page') || 0);

  useEffect(() => {
    let active = true;
    const fetchProducts = async () => {
      setLoading(true);
      try {
        const params = {
          page,
          pageSize: 12,
          sort,
        };
        if (q) params.q = q;
        if (gender) params.gender = gender;
        if (category) params.category = category;
        if (brand) params.brand = brand;
        if (size) params.size = size;
        if (color) params.color = color;
        if (minPrice) params.minPrice = minPrice;
        if (maxPrice) params.maxPrice = maxPrice;
        if (minDiscount) params.minDiscount = minDiscount;
        if (inStockOnly) params.inStockOnly = true;
        if (trending) params.trending = true;

        const res = await catalogApi.getProducts(params);
        if (active) setData(res);
      } finally {
        if (active) setLoading(false);
      }
    };
    fetchProducts();
    return () => {
      active = false;
    };
  }, [q, gender, category, brand, size, color, minPrice, maxPrice, minDiscount, inStockOnly, trending, sort, page]);

  const updateFilter = (key, value) => {
    const next = new URLSearchParams(searchParams);
    if (value === '' || value === false || value === null || value === undefined) {
      next.delete(key);
    } else {
      next.set(key, String(value));
    }
    if (key !== 'page') next.delete('page');
    setSearchParams(next);
  };

  const clearAllFilters = () => {
    setSearchParams({});
  };

  const FilterPanel = () => (
    <div className="space-y-6 text-xs">
      <div className="flex items-center justify-between pb-3 border-b border-[#E5E5E5]">
        <span className="font-bold uppercase tracking-widest">Filters</span>
        <button onClick={clearAllFilters} className="text-[11px] underline text-neutral-500 hover:text-[#111111]">
          Clear All
        </button>
      </div>

      {/* Gender */}
      <div>
        <h4 className="font-bold uppercase tracking-wider mb-2.5 text-[11px]">Gender</h4>
        <div className="space-y-1.5">
          {['MEN', 'WOMEN', 'KIDS', 'UNISEX'].map((g) => (
            <label key={g} className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                name="genderFilter"
                checked={gender === g}
                onChange={() => updateFilter('gender', gender === g ? '' : g)}
                className="accent-[#111111]"
              />
              <span>{g}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Category */}
      <div>
        <h4 className="font-bold uppercase tracking-wider mb-2.5 text-[11px]">Category</h4>
        <select
          value={category}
          onChange={(e) => updateFilter('category', e.target.value)}
          className="w-full border border-[#D4D4D4] px-2.5 py-2 bg-white text-xs outline-none focus:border-[#111111]"
        >
          <option value="">All Categories</option>
          {flatCategories.map((c) => (
            <option key={c.id} value={c.slug}>
              {c.parentName ? `${c.parentName} — ${c.name}` : c.name}
            </option>
          ))}
        </select>
      </div>

      {/* Brand */}
      <div>
        <h4 className="font-bold uppercase tracking-wider mb-2.5 text-[11px]">Brand</h4>
        <div className="space-y-1.5 max-h-44 overflow-y-auto">
          {brands.map((b) => (
            <label key={b.id} className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={brand === b.slug}
                onChange={() => updateFilter('brand', brand === b.slug ? '' : b.slug)}
                className="accent-[#111111]"
              />
              <span>{b.name}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Price Range */}
      <div>
        <h4 className="font-bold uppercase tracking-wider mb-2.5 text-[11px]">Price Range (₹)</h4>
        <div className="flex items-center gap-2">
          <input
            type="number"
            placeholder="Min"
            value={minPrice}
            onChange={(e) => updateFilter('minPrice', e.target.value)}
            className="w-1/2 border border-[#D4D4D4] px-2 py-1.5 outline-none focus:border-[#111111]"
          />
          <input
            type="number"
            placeholder="Max"
            value={maxPrice}
            onChange={(e) => updateFilter('maxPrice', e.target.value)}
            className="w-1/2 border border-[#D4D4D4] px-2 py-1.5 outline-none focus:border-[#111111]"
          />
        </div>
      </div>

      {/* Size */}
      <div>
        <h4 className="font-bold uppercase tracking-wider mb-2.5 text-[11px]">Size</h4>
        <div className="flex flex-wrap gap-1.5">
          {SIZES.map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => updateFilter('size', size === s ? '' : s)}
              className={`px-2.5 py-1 border text-[11px] font-medium ${
                size === s
                  ? 'bg-[#111111] text-white border-[#111111]'
                  : 'bg-white text-[#111111] border-[#E5E5E5] hover:border-[#111111]'
              }`}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      {/* Color */}
      <div>
        <h4 className="font-bold uppercase tracking-wider mb-2.5 text-[11px]">Colour</h4>
        <select
          value={color}
          onChange={(e) => updateFilter('color', e.target.value)}
          className="w-full border border-[#D4D4D4] px-2.5 py-2 bg-white text-xs outline-none focus:border-[#111111]"
        >
          <option value="">Any Colour</option>
          {COLORS.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      {/* Discount */}
      <div>
        <h4 className="font-bold uppercase tracking-wider mb-2.5 text-[11px]">Discount</h4>
        <select
          value={minDiscount}
          onChange={(e) => updateFilter('minDiscount', e.target.value)}
          className="w-full border border-[#D4D4D4] px-2.5 py-2 bg-white text-xs outline-none focus:border-[#111111]"
        >
          <option value="">All Discounts</option>
          <option value="10">10% &amp; above</option>
          <option value="20">20% &amp; above</option>
          <option value="30">30% &amp; above</option>
          <option value="50">50% &amp; above</option>
        </select>
      </div>

      {/* Availability */}
      <div>
        <label className="flex items-center gap-2 cursor-pointer font-medium">
          <input
            type="checkbox"
            checked={inStockOnly}
            onChange={(e) => updateFilter('inStockOnly', e.target.checked)}
            className="accent-[#111111]"
          />
          <span>In Stock Only (Exclude Sold Out)</span>
        </label>
      </div>
    </div>
  );

  return (
    <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-8">
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-[#E5E5E5]">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight uppercase">
            {q ? `Results for "${q}"` : gender ? `${gender} Fashion` : category ? category.replace(/-/g, ' ') : 'All Fashion'}
          </h1>
          <p className="text-xs text-neutral-500 mt-1">
            {data.totalElements} {data.totalElements === 1 ? 'item' : 'items'} found
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setMobileFiltersOpen(true)}
            className="lg:hidden flex items-center gap-2 border border-[#111111] px-4 py-2 text-xs font-semibold uppercase tracking-wider"
          >
            <SlidersHorizontal className="w-3.5 h-3.5" /> Filters
          </button>

          <div className="flex items-center gap-2">
            <span className="text-xs text-neutral-500 hidden sm:inline">Sort by:</span>
            <select
              value={sort}
              onChange={(e) => updateFilter('sort', e.target.value)}
              className="border border-[#D4D4D4] px-3 py-2 text-xs bg-white outline-none focus:border-[#111111]"
            >
              <option value="recommended">Recommended</option>
              <option value="newest">Newest Arrivals</option>
              <option value="popularity">Popularity</option>
              <option value="rating">Highest Rated</option>
              <option value="price_asc">Price: Low to High</option>
              <option value="price_desc">Price: High to Low</option>
            </select>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="mt-8 grid grid-cols-1 lg:grid-cols-12 gap-8">
        <aside className="hidden lg:block lg:col-span-3 xl:col-span-2">
          <FilterPanel />
        </aside>

        <div className="lg:col-span-9 xl:col-span-10">
          {loading ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-5">
              {[...Array(8)].map((_, i) => (
                <div key={i} className="aspect-[3/4] bg-[#F5F5F5] animate-pulse" />
              ))}
            </div>
          ) : data.content.length === 0 ? (
            <div className="border border-[#E5E5E5] py-20 px-6 text-center">
              <h3 className="text-lg font-bold">No matching products found</h3>
              <p className="mt-2 text-xs text-neutral-500 max-w-md mx-auto">
                Try clearing some filters or searching for a different keyword, brand, or category.
              </p>
              <button
                type="button"
                onClick={clearAllFilters}
                className="mt-6 bg-[#111111] text-white px-6 py-3 text-xs font-bold uppercase tracking-widest hover:bg-neutral-800"
              >
                Reset All Filters
              </button>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-5">
                {data.content.map((p) => (
                  <ProductCard key={p.id} product={p} />
                ))}
              </div>
              <Pagination
                page={data.page}
                totalPages={data.totalPages}
                onPageChange={(nextPage) => updateFilter('page', nextPage)}
              />
            </>
          )}
        </div>
      </div>

      {/* Mobile Filters Modal */}
      {mobileFiltersOpen && (
        <div className="fixed inset-0 z-50 lg:hidden flex">
          <div className="fixed inset-0 bg-black/40" onClick={() => setMobileFiltersOpen(false)} />
          <div className="relative w-80 max-w-full bg-white h-full overflow-y-auto p-6">
            <div className="flex items-center justify-between pb-4 mb-4 border-b border-[#E5E5E5]">
              <span className="font-bold uppercase text-sm tracking-wider">Filter Products</span>
              <button onClick={() => setMobileFiltersOpen(false)}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <FilterPanel />
          </div>
        </div>
      )}
    </div>
  );
}
