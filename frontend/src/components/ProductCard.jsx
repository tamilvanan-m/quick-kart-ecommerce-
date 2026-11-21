import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Heart, Star } from 'lucide-react';
import { formatCurrency, resolveImageUrl } from '../utils/format';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useToast } from './Toast';

export default function ProductCard({ product }) {
  const { isAuthenticated } = useAuth();
  const { isInWishlist, toggleWishlist } = useCart();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const wishlisted = isInWishlist(product.id);
  const primaryImg = resolveImageUrl(product.primaryImageUrl || product.images?.[0]?.imageUrl);
  const secondaryImg = product.images?.[1]?.imageUrl ? resolveImageUrl(product.images[1].imageUrl) : null;
  const soldOut = Boolean(product.soldOut) || product.status === 'OUT_OF_STOCK' || product.totalAvailableStock <= 0;

  const handleWishlist = async (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!isAuthenticated) {
      showToast('Please sign in to save items to your wishlist.', 'error');
      navigate('/login');
      return;
    }
    try {
      const added = await toggleWishlist(product.id);
      showToast(added ? 'Saved to wishlist' : 'Removed from wishlist');
    } catch {
      showToast('Unable to update wishlist', 'error');
    }
  };

  return (
    <Link
      to={`/product/${product.slug}`}
      className="group block bg-white border border-transparent hover:border-[#E5E5E5] transition-colors"
    >
      <div className="relative aspect-[3/4] w-full overflow-hidden bg-[#F5F5F5]">
        <img
          src={primaryImg}
          alt={product.name}
          loading="lazy"
          className={`h-full w-full object-cover object-center transition-all duration-500 ${
            secondaryImg ? 'group-hover:opacity-0' : 'group-hover:scale-[1.03]'
          } ${soldOut ? 'opacity-70 grayscale-[35%]' : ''}`}
        />
        {secondaryImg && (
          <img
            src={secondaryImg}
            alt={`${product.name} alternate view`}
            loading="lazy"
            className="absolute inset-0 h-full w-full object-cover object-center opacity-0 group-hover:opacity-100 transition-opacity duration-500"
          />
        )}

        {/* Top badges */}
        <div className="absolute top-2.5 left-2.5 flex flex-col gap-1">
          {soldOut ? (
            <span className="bg-[#111111] text-white text-[10px] font-semibold tracking-widest uppercase px-2.5 py-1">
              SOLD OUT
            </span>
          ) : product.newArrival ? (
            <span className="bg-white/95 text-[#111111] border border-[#E5E5E5] text-[10px] font-medium tracking-widest uppercase px-2 py-0.5">
              NEW
            </span>
          ) : null}
        </div>

        {/* Wishlist Button */}
        <button
          type="button"
          onClick={handleWishlist}
          aria-label={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
          className="absolute top-2.5 right-2.5 w-8 h-8 rounded-full bg-white/95 border border-[#E5E5E5] flex items-center justify-center text-[#111111] hover:bg-[#111111] hover:text-white transition-colors"
        >
          <Heart className={`w-4 h-4 ${wishlisted ? 'fill-current' : ''}`} />
        </button>

        {/* Rating badge */}
        {Number(product.ratingAvg) > 0 && (
          <div className="absolute bottom-2.5 left-2.5 bg-white/95 px-2 py-0.5 text-[11px] font-medium flex items-center gap-1 border border-[#E5E5E5]">
            <span>{Number(product.ratingAvg).toFixed(1)}</span>
            <Star className="w-3 h-3 fill-[#111111] text-[#111111]" />
            <span className="text-neutral-400">|</span>
            <span className="text-neutral-600">{product.reviewCount}</span>
          </div>
        )}
      </div>

      <div className="p-3">
        <div className="text-[11px] font-semibold tracking-wider uppercase text-neutral-500 truncate">
          {product.brandName}
        </div>
        <h3 className="mt-0.5 text-sm font-medium text-[#111111] truncate">{product.name}</h3>
        <div className="mt-1.5 flex items-baseline gap-2 flex-wrap">
          <span className="text-sm font-semibold text-[#111111]">
            {formatCurrency(product.sellingPrice)}
          </span>
          {Number(product.originalPrice) > Number(product.sellingPrice) && (
            <>
              <span className="text-xs text-neutral-400 line-through">
                {formatCurrency(product.originalPrice)}
              </span>
              <span className="text-xs font-medium text-red-700">
                ({product.discountPercentage}% OFF)
              </span>
            </>
          )}
        </div>
      </div>
    </Link>
  );
}
