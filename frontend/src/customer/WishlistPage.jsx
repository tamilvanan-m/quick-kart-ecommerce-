import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Heart, ShoppingBag, Trash2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useToast } from '../components/Toast';
import { getApiErrorMessage } from '../services/api';
import { formatCurrency, resolveImageUrl } from '../utils/format';

export default function WishlistPage() {
  const { isAuthenticated } = useAuth();
  const { wishlist, toggleWishlist, moveWishlistToCart } = useCart();
  const { showToast } = useToast();
  const navigate = useNavigate();

  if (!isAuthenticated) {
    return (
      <div className="max-w-xl mx-auto px-4 py-20 text-center">
        <Heart className="w-10 h-10 mx-auto text-neutral-400" />
        <h1 className="mt-4 text-2xl font-bold">Your Saved Wishlist</h1>
        <p className="mt-2 text-xs text-neutral-500">Sign in to save your favorite styles and move them to bag anytime.</p>
        <Link to="/login" className="inline-block mt-6 bg-[#111111] text-white px-7 py-3.5 text-xs font-bold uppercase tracking-widest">
          Sign In
        </Link>
      </div>
    );
  }

  const items = wishlist.items || [];

  const handleMove = async (product) => {
    try {
      await moveWishlistToCart(product.id);
      showToast(`Moved ${product.name} to your bag`);
      navigate('/cart');
    } catch (err) {
      showToast(getApiErrorMessage(err), 'error');
    }
  };

  return (
    <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-10 py-10">
      <h1 className="text-2xl font-bold tracking-tight uppercase pb-6 border-b border-[#E5E5E5]">
        My Wishlist ({items.length})
      </h1>

      {items.length === 0 ? (
        <div className="py-20 text-center">
          <p className="text-sm text-neutral-500">You have not saved any items yet.</p>
          <Link to="/shop" className="inline-block mt-6 bg-[#111111] text-white px-7 py-3.5 text-xs font-bold uppercase tracking-widest">
            Explore Collection
          </Link>
        </div>
      ) : (
        <div className="mt-8 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-5">
          {items.map(({ id, product }) => {
            const soldOut = Boolean(product.soldOut) || product.status === 'OUT_OF_STOCK';
            return (
              <div key={id} className="border border-[#E5E5E5] bg-white flex flex-col">
                <Link to={`/product/${product.slug}`} className="relative aspect-[3/4] bg-[#F5F5F5] block overflow-hidden">
                  <img
                    src={resolveImageUrl(product.primaryImageUrl)}
                    alt={product.name}
                    className={`w-full h-full object-cover ${soldOut ? 'opacity-70 grayscale-[35%]' : ''}`}
                  />
                  {soldOut && (
                    <span className="absolute top-2.5 left-2.5 bg-[#111111] text-white text-[10px] font-bold uppercase tracking-widest px-2.5 py-1">
                      SOLD OUT
                    </span>
                  )}
                  <button
                    type="button"
                    onClick={(e) => {
                      e.preventDefault();
                      toggleWishlist(product.id);
                    }}
                    className="absolute top-2.5 right-2.5 w-8 h-8 rounded-full bg-white border border-[#E5E5E5] flex items-center justify-center hover:text-red-700"
                    aria-label="Remove from wishlist"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </Link>

                <div className="p-3.5 flex-1 flex flex-col justify-between">
                  <div>
                    <div className="text-[11px] font-bold uppercase tracking-wider text-neutral-500">
                      {product.brandName}
                    </div>
                    <Link to={`/product/${product.slug}`} className="text-sm font-medium block truncate mt-0.5">
                      {product.name}
                    </Link>
                    <div className="mt-1.5 text-sm font-bold">{formatCurrency(product.sellingPrice)}</div>
                  </div>

                  <button
                    type="button"
                    disabled={soldOut}
                    onClick={() => handleMove(product)}
                    className="mt-4 w-full py-2.5 border border-[#111111] text-xs font-bold uppercase tracking-wider hover:bg-[#111111] hover:text-white transition-colors disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-1.5"
                  >
                    <ShoppingBag className="w-3.5 h-3.5" />
                    {soldOut ? 'Sold Out' : 'Move to Bag'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
