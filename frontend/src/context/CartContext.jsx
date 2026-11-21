import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { customerApi } from '../services/customerApi';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

const EMPTY_CART = {
  items: [],
  totalItems: 0,
  mrpTotal: 0,
  subtotal: 0,
  productSavings: 0,
  couponCode: null,
  couponDiscount: 0,
  deliveryCharge: 0,
  freeDeliveryThreshold: 999,
  finalAmount: 0,
};

export function CartProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [cart, setCart] = useState(EMPTY_CART);
  const [wishlist, setWishlist] = useState({ items: [] });

  const refreshCartAndWishlist = useCallback(async () => {
    if (!isAuthenticated) {
      setCart(EMPTY_CART);
      setWishlist({ items: [] });
      return;
    }
    try {
      const [cartData, wishlistData] = await Promise.all([
        customerApi.getCart(),
        customerApi.getWishlist(),
      ]);
      setCart(cartData || EMPTY_CART);
      setWishlist(wishlistData || { items: [] });
    } catch {
      // Ignore silent failures on token transitions
    }
  }, [isAuthenticated]);

  useEffect(() => {
    refreshCartAndWishlist();
  }, [refreshCartAndWishlist]);

  const addToCart = async (productId, variantId, quantity = 1) => {
    const updated = await customerApi.addToCart(productId, variantId, quantity);
    setCart(updated);
    return updated;
  };

  const updateCartItem = async (itemId, payload) => {
    const updated = await customerApi.updateCartItem(itemId, payload);
    setCart(updated);
    return updated;
  };

  const removeCartItem = async (itemId) => {
    const updated = await customerApi.removeCartItem(itemId);
    setCart(updated);
    return updated;
  };

  const applyCoupon = async (code) => {
    const updated = await customerApi.applyCoupon(code);
    setCart(updated);
    return updated;
  };

  const removeCoupon = async () => {
    const updated = await customerApi.removeCoupon();
    setCart(updated);
    return updated;
  };

  const toggleWishlist = async (productId) => {
    const exists = wishlist.items?.some((item) => item.productId === productId);
    const updated = exists
      ? await customerApi.removeFromWishlist(productId)
      : await customerApi.addToWishlist(productId);
    setWishlist(updated);
    return !exists;
  };

  const moveWishlistToCart = async (productId, variantId) => {
    const updatedCart = await customerApi.moveWishlistToCart(productId, variantId);
    setCart(updatedCart);
    const updatedWishlist = await customerApi.getWishlist();
    setWishlist(updatedWishlist);
    return updatedCart;
  };

  const isInWishlist = (productId) =>
    Boolean(wishlist.items?.some((item) => item.productId === productId));

  return (
    <CartContext.Provider
      value={{
        cart,
        wishlist,
        cartCount: cart.totalItems || 0,
        wishlistCount: wishlist.items?.length || 0,
        refreshCartAndWishlist,
        addToCart,
        updateCartItem,
        removeCartItem,
        applyCoupon,
        removeCoupon,
        toggleWishlist,
        moveWishlistToCart,
        isInWishlist,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export const useCart = () => useContext(CartContext);
