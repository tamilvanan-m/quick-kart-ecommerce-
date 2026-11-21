import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, ChevronLeft, ChevronRight, Star, ShieldCheck, Truck, RefreshCw } from 'lucide-react';
import { catalogApi } from '../services/catalogApi';
import { useStore } from '../context/StoreContext';
import { useToast } from '../components/Toast';
import ProductCard from '../components/ProductCard';
import { resolveImageUrl } from '../utils/format';

export default function HomePage() {
  const { categories, brands } = useStore();
  const { showToast } = useToast();

  const [heroBanners, setHeroBanners] = useState([]);
  const [promoBanners, setPromoBanners] = useState([]);
  const [activeSlide, setActiveSlide] = useState(0);
  const [trending, setTrending] = useState([]);
  const [newArrivals, setNewArrivals] = useState([]);
  const [bestSellers, setBestSellers] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [coupons, setCoupons] = useState([]);
  const [newsletterEmail, setNewsletterEmail] = useState('');
  const [subscribing, setSubscribing] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const [heroRes, promoRes, trendRes, newRes, bestRes, revRes, coupRes] = await Promise.all([
          catalogApi.getBanners('HERO'),
          catalogApi.getBanners('PROMO'),
          catalogApi.getProducts({ trending: true, pageSize: 8 }),
          catalogApi.getProducts({ newArrival: true, pageSize: 8 }),
          catalogApi.getProducts({ bestSeller: true, pageSize: 4 }),
          catalogApi.getFeaturedReviews(),
          catalogApi.getPublicCoupons(),
        ]);
        if (!active) return;
        setHeroBanners(heroRes || []);
        setPromoBanners(promoRes || []);
        setTrending(trendRes?.content || []);
        setNewArrivals(newRes?.content || []);
        setBestSellers(bestRes?.content || []);
        setReviews(revRes || []);
        setCoupons(coupRes || []);
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (heroBanners.length <= 1) return;
    const timer = setInterval(() => {
      setActiveSlide((prev) => (prev + 1) % heroBanners.length);
    }, 6000);
    return () => clearInterval(timer);
  }, [heroBanners.length]);

  const handleNewsletter = async (e) => {
    e.preventDefault();
    if (!newsletterEmail.trim()) return;
    setSubscribing(true);
    try {
      const res = await catalogApi.subscribeNewsletter(newsletterEmail.trim());
      showToast(res.message || 'Subscribed successfully');
      setNewsletterEmail('');
    } catch {
      showToast('Please enter a valid email address', 'error');
    } finally {
      setSubscribing(false);
    }
  };

  const currentHero = heroBanners[activeSlide];

  return (
    <div>
      {/* 1. Large Fashion Hero Banner (Dynamic from DB) */}
      <section className="relative bg-[#F5F5F5] border-b border-[#E5E5E5]">
        {currentHero ? (
          <div className="relative h-[440px] sm:h-[540px] lg:h-[620px] w-full overflow-hidden">
            <img
              src={resolveImageUrl(currentHero.imageUrl)}
              alt={currentHero.title}
              className="w-full h-full object-cover object-center"
            />
            <div className="absolute inset-0 bg-gradient-to-r from-black/70 via-black/35 to-transparent" />
            <div className="absolute inset-0 max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 flex items-center">
              <div className="max-w-xl text-white">
                {currentHero.subtitle && (
                  <p className="text-[11px] sm:text-xs font-semibold tracking-[0.22em] uppercase mb-3 text-neutral-200">
                    {currentHero.subtitle}
                  </p>
                )}
                <h1 className="text-3xl sm:text-5xl lg:text-6xl font-bold tracking-tight leading-[1.08]">
                  {currentHero.title}
                </h1>
                {currentHero.description && (
                  <p className="mt-4 text-sm sm:text-base text-neutral-200 leading-relaxed max-w-lg">
                    {currentHero.description}
                  </p>
                )}
                <div className="mt-7 flex flex-wrap gap-3">
                  <Link
                    to={currentHero.linkUrl || '/shop'}
                    className="inline-flex items-center gap-2 bg-white text-[#111111] px-7 py-3.5 text-xs font-bold uppercase tracking-widest hover:bg-neutral-200 transition-colors"
                  >
                    {currentHero.ctaText || 'SHOP NOW'}
                    <ArrowRight className="w-4 h-4" />
                  </Link>
                  <Link
                    to="/shop?sort=newest"
                    className="inline-flex items-center gap-2 border border-white text-white px-7 py-3.5 text-xs font-bold uppercase tracking-widest hover:bg-white hover:text-[#111111] transition-colors"
                  >
                    New Arrivals
                  </Link>
                </div>
              </div>
            </div>

            {heroBanners.length > 1 && (
              <>
                <button
                  type="button"
                  onClick={() => setActiveSlide((activeSlide - 1 + heroBanners.length) % heroBanners.length)}
                  className="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 bg-white/90 text-[#111111] flex items-center justify-center hover:bg-white"
                  aria-label="Previous slide"
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>
                <button
                  type="button"
                  onClick={() => setActiveSlide((activeSlide + 1) % heroBanners.length)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 bg-white/90 text-[#111111] flex items-center justify-center hover:bg-white"
                  aria-label="Next slide"
                >
                  <ChevronRight className="w-5 h-5" />
                </button>
                <div className="absolute bottom-5 left-1/2 -translate-x-1/2 flex gap-2">
                  {heroBanners.map((b, i) => (
                    <button
                      key={b.id}
                      type="button"
                      onClick={() => setActiveSlide(i)}
                      className={`h-1.5 transition-all ${
                        i === activeSlide ? 'w-8 bg-white' : 'w-3 bg-white/50'
                      }`}
                      aria-label={`Banner ${i + 1}`}
                    />
                  ))}
                </div>
              </>
            )}
          </div>
        ) : (
          <div className="h-[420px] flex items-center justify-center text-sm text-neutral-500">
            Loading seasonal lookbook...
          </div>
        )}
      </section>

      {/* Value Pillars */}
      <section className="border-b border-[#E5E5E5] bg-[#FAFAFA]">
        <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-5 grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
          <div className="flex items-center justify-center sm:justify-start gap-2.5 text-[#111111]">
            <Truck className="w-4 h-4 shrink-0" />
            <span>Complimentary Shipping above ₹999</span>
          </div>
          <div className="flex items-center justify-center gap-2.5 text-[#111111]">
            <RefreshCw className="w-4 h-4 shrink-0" />
            <span>14-Day Hassle-Free Returns &amp; Exchanges</span>
          </div>
          <div className="flex items-center justify-center sm:justify-end gap-2.5 text-[#111111]">
            <ShieldCheck className="w-4 h-4 shrink-0" />
            <span>100% Authentic Curated Apparel</span>
          </div>
        </div>
      </section>

      {/* 2. Shop by Category */}
      <section className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-14">
        <div className="flex items-end justify-between mb-8">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-neutral-500">
              Collections
            </p>
            <h2 className="mt-1 text-2xl sm:text-3xl font-bold tracking-tight">Shop by Category</h2>
          </div>
          <Link to="/shop" className="text-xs font-semibold uppercase tracking-widest hover:underline flex items-center gap-1">
            View All <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
          {categories.map((cat) => (
            <Link
              key={cat.id}
              to={
                cat.genderScope && ['MEN', 'WOMEN', 'KIDS'].includes(cat.genderScope)
                  ? `/shop?gender=${cat.genderScope}`
                  : `/shop?category=${cat.slug}`
              }
              className="group block border border-[#E5E5E5] bg-[#F7F7F7] overflow-hidden"
            >
              <div className="aspect-[4/5] overflow-hidden bg-[#EDEDED]">
                <img
                  src={resolveImageUrl(cat.imageUrl)}
                  alt={cat.name}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                />
              </div>
              <div className="p-3 bg-white flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider">{cat.name}</span>
                <ArrowRight className="w-3.5 h-3.5 text-neutral-400 group-hover:text-[#111111] transition-colors" />
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* 3. Trending Products */}
      <section className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-10">
        <div className="flex items-end justify-between mb-8">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-neutral-500">
              In Demand
            </p>
            <h2 className="mt-1 text-2xl sm:text-3xl font-bold tracking-tight">Trending Now</h2>
          </div>
          <Link to="/shop?trending=true" className="text-xs font-semibold uppercase tracking-widest hover:underline flex items-center gap-1">
            Shop Trending <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {loading ? (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-5">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="aspect-[3/4] bg-[#F5F5F5] animate-pulse" />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-5">
            {trending.slice(0, 8).map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        )}
      </section>

      {/* 4. Promotional Offers & Active Coupons */}
      {promoBanners.length > 0 && (
        <section className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-8">
          <div className="relative h-72 sm:h-80 overflow-hidden bg-[#111111]">
            <img
              src={resolveImageUrl(promoBanners[0].imageUrl)}
              alt={promoBanners[0].title}
              className="w-full h-full object-cover opacity-55"
            />
            <div className="absolute inset-0 flex flex-col justify-center px-8 sm:px-14 text-white max-w-2xl">
              {promoBanners[0].subtitle && (
                <span className="text-[11px] font-semibold uppercase tracking-[0.22em] text-neutral-200">
                  {promoBanners[0].subtitle}
                </span>
              )}
              <h3 className="mt-2 text-2xl sm:text-4xl font-bold tracking-tight">
                {promoBanners[0].title}
              </h3>
              {promoBanners[0].description && (
                <p className="mt-2 text-sm text-neutral-200">{promoBanners[0].description}</p>
              )}
              <div className="mt-5 flex flex-wrap items-center gap-3">
                <Link
                  to={promoBanners[0].linkUrl || '/shop'}
                  className="bg-white text-[#111111] px-6 py-3 text-xs font-bold uppercase tracking-widest hover:bg-neutral-200"
                >
                  {promoBanners[0].ctaText || 'SHOP NOW'}
                </Link>
                {coupons.slice(0, 2).map((c) => (
                  <span
                    key={c.id}
                    className="border border-white/60 px-3 py-2 text-[11px] tracking-widest uppercase"
                  >
                    Code: <strong>{c.code}</strong>
                  </span>
                ))}
              </div>
            </div>
          </div>
        </section>
      )}

      {/* 5. New Arrivals */}
      <section className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-12">
        <div className="flex items-end justify-between mb-8">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-neutral-500">
              Just Dropped
            </p>
            <h2 className="mt-1 text-2xl sm:text-3xl font-bold tracking-tight">New Arrivals</h2>
          </div>
          <Link to="/shop?sort=newest" className="text-xs font-semibold uppercase tracking-widest hover:underline flex items-center gap-1">
            Explore New <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-5">
          {newArrivals.slice(0, 8).map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      </section>

      {/* 6. Seasonal Collections Split */}
      <section className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-8">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Link to="/shop?gender=WOMEN" className="group relative h-80 overflow-hidden bg-[#F5F5F5]">
            <img
              src="https://images.unsplash.com/photo-1483985988635-76728e1935b8?auto=format&fit=crop&w=1200&q=80"
              alt="Women Seasonal Edit"
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/75 via-black/20 to-transparent" />
            <div className="absolute bottom-6 left-6 text-white">
              <p className="text-[11px] uppercase tracking-[0.2em] text-neutral-300">Womenswear</p>
              <h3 className="text-2xl font-bold mt-1">Modern Tailoring &amp; Silhouettes</h3>
              <span className="inline-flex items-center gap-1 mt-3 text-xs font-semibold uppercase tracking-widest border-b border-white pb-0.5">
                Shop Women <ArrowRight className="w-3.5 h-3.5" />
              </span>
            </div>
          </Link>
          <Link to="/shop?gender=MEN" className="group relative h-80 overflow-hidden bg-[#F5F5F5]">
            <img
              src="https://images.unsplash.com/photo-1516257984-b1b4d707412e?auto=format&fit=crop&w=1200&q=80"
              alt="Men Seasonal Edit"
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/75 via-black/20 to-transparent" />
            <div className="absolute bottom-6 left-6 text-white">
              <p className="text-[11px] uppercase tracking-[0.2em] text-neutral-300">Menswear</p>
              <h3 className="text-2xl font-bold mt-1">Elevated Everyday Essentials</h3>
              <span className="inline-flex items-center gap-1 mt-3 text-xs font-semibold uppercase tracking-widest border-b border-white pb-0.5">
                Shop Men <ArrowRight className="w-3.5 h-3.5" />
              </span>
            </div>
          </Link>
        </div>
      </section>

      {/* 7. Best Sellers */}
      <section className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-12">
        <div className="flex items-end justify-between mb-8">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-neutral-500">
              Customer Favorites
            </p>
            <h2 className="mt-1 text-2xl sm:text-3xl font-bold tracking-tight">Best Sellers</h2>
          </div>
          <Link to="/shop?sort=popularity" className="text-xs font-semibold uppercase tracking-widest hover:underline flex items-center gap-1">
            View Best Sellers <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-5">
          {bestSellers.slice(0, 4).map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      </section>

      {/* 8. Top Brands */}
      {brands.length > 0 && (
        <section className="bg-[#FAFAFA] border-y border-[#E5E5E5] py-14">
          <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10">
            <div className="text-center mb-8">
              <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-neutral-500">
                Curated Labels
              </p>
              <h2 className="mt-1 text-2xl font-bold tracking-tight">Top Brands on Quick kart</h2>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
              {brands.slice(0, 6).map((b) => (
                <Link
                  key={b.id}
                  to={`/shop?brand=${b.slug}`}
                  className="bg-white border border-[#E5E5E5] p-5 text-center hover:border-[#111111] transition-colors flex flex-col justify-center items-center min-h-[100px]"
                >
                  <span className="text-sm font-bold tracking-widest uppercase text-[#111111]">
                    {b.name}
                  </span>
                  <span className="mt-1 text-[11px] text-neutral-500 line-clamp-1">Explore Label</span>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* 9. Customer Reviews */}
      {reviews.length > 0 && (
        <section className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-14">
          <div className="mb-8">
            <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-neutral-500">
              Verified Feedback
            </p>
            <h2 className="mt-1 text-2xl font-bold tracking-tight">What Customers Say</h2>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {reviews.slice(0, 3).map((r) => (
              <div key={r.id} className="border border-[#E5E5E5] p-6 bg-white">
                <div className="flex items-center gap-1 mb-3">
                  {[...Array(5)].map((_, idx) => (
                    <Star
                      key={idx}
                      className={`w-3.5 h-3.5 ${
                        idx < r.rating ? 'fill-[#111111] text-[#111111]' : 'text-neutral-300'
                      }`}
                    />
                  ))}
                </div>
                {r.title && <h4 className="text-sm font-semibold text-[#111111]">{r.title}</h4>}
                <p className="mt-2 text-xs text-neutral-600 leading-relaxed">{r.comment}</p>
                <div className="mt-4 pt-3 border-t border-[#F0F0F0] flex items-center justify-between text-[11px] text-neutral-500">
                  <span className="font-medium text-[#111111]">{r.userName}</span>
                  <span>Verified Buyer • {r.productName}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* 10. Newsletter Subscription */}
      <section className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 pt-4">
        <div className="border border-[#E5E5E5] bg-[#FAFAFA] py-12 px-6 text-center">
          <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-neutral-500">
            Stay Updated
          </p>
          <h2 className="mt-2 text-2xl font-bold tracking-tight">Join the Quick kart Edit</h2>
          <p className="mt-2 text-xs text-neutral-600 max-w-md mx-auto">
            Receive early access to seasonal lookbooks, private restocks, and subscriber-only offers.
          </p>
          <form onSubmit={handleNewsletter} className="mt-6 max-w-md mx-auto flex gap-2">
            <input
              type="email"
              required
              value={newsletterEmail}
              onChange={(e) => setNewsletterEmail(e.target.value)}
              placeholder="Enter your email address"
              className="flex-1 bg-white border border-[#D4D4D4] focus:border-[#111111] px-4 py-3 text-xs outline-none"
            />
            <button
              type="submit"
              disabled={subscribing}
              className="bg-[#111111] text-white px-6 py-3 text-xs font-bold uppercase tracking-widest hover:bg-neutral-800 disabled:opacity-50"
            >
              {subscribing ? 'Joining...' : 'Subscribe'}
            </button>
          </form>
        </div>
      </section>
    </div>
  );
}
