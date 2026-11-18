package com.quickkart.controller;

import com.quickkart.dto.AddressDtos;
import com.quickkart.dto.CartWishlistDtos;
import com.quickkart.dto.CouponBannerReviewDtos;
import com.quickkart.service.AddressService;
import com.quickkart.service.CartService;
import com.quickkart.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Customer Account, Cart & Wishlist", description = "Authenticated customer shopping bag, saved wishlist, and shipping addresses")
public class CustomerAccountController {

    private final CartService cartService;
    private final WishlistService wishlistService;
    private final AddressService addressService;

    public CustomerAccountController(CartService cartService, WishlistService wishlistService, AddressService addressService) {
        this.cartService = cartService;
        this.wishlistService = wishlistService;
        this.addressService = addressService;
    }

    // ---- CART ----
    @GetMapping("/cart")
    @Operation(summary = "Get current customer's shopping bag")
    public ResponseEntity<CartWishlistDtos.CartDto> getCart() {
        return ResponseEntity.ok(cartService.getMyCart());
    }

    @PostMapping("/cart/items")
    @Operation(summary = "Add product variant to shopping bag")
    public ResponseEntity<CartWishlistDtos.CartDto> addToCart(@Valid @RequestBody CartWishlistDtos.AddCartItemRequest req) {
        return ResponseEntity.ok(cartService.addItem(req));
    }

    @PutMapping("/cart/items/{id}")
    @Operation(summary = "Update cart item quantity or selected size/variant")
    public ResponseEntity<CartWishlistDtos.CartDto> updateCartItem(
            @PathVariable Long id,
            @Valid @RequestBody CartWishlistDtos.UpdateCartItemRequest req) {
        return ResponseEntity.ok(cartService.updateItem(id, req));
    }

    @DeleteMapping("/cart/items/{id}")
    @Operation(summary = "Remove item from shopping bag")
    public ResponseEntity<CartWishlistDtos.CartDto> removeCartItem(@PathVariable Long id) {
        return ResponseEntity.ok(cartService.removeItem(id));
    }

    @PostMapping("/cart/coupon")
    @Operation(summary = "Apply a coupon code to the shopping bag")
    public ResponseEntity<CartWishlistDtos.CartDto> applyCoupon(@Valid @RequestBody CouponBannerReviewDtos.CouponApplyRequest req) {
        return ResponseEntity.ok(cartService.applyCoupon(req.code()));
    }

    @DeleteMapping("/cart/coupon")
    @Operation(summary = "Remove applied coupon from the shopping bag")
    public ResponseEntity<CartWishlistDtos.CartDto> removeCoupon() {
        return ResponseEntity.ok(cartService.removeCoupon());
    }

    // ---- WISHLIST ----
    @GetMapping("/wishlist")
    @Operation(summary = "Get customer's wishlist")
    public ResponseEntity<CartWishlistDtos.WishlistDto> getWishlist() {
        return ResponseEntity.ok(wishlistService.getMyWishlist());
    }

    @PostMapping("/wishlist/{productId}")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<CartWishlistDtos.WishlistDto> addToWishlist(@PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.addProduct(productId));
    }

    @DeleteMapping("/wishlist/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<CartWishlistDtos.WishlistDto> removeFromWishlist(@PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.removeProduct(productId));
    }

    @PostMapping("/wishlist/{productId}/move-to-cart")
    @Operation(summary = "Move wishlist product into shopping bag")
    public ResponseEntity<CartWishlistDtos.CartDto> moveWishlistToCart(
            @PathVariable Long productId,
            @RequestParam(required = false) Long variantId) {
        return ResponseEntity.ok(wishlistService.moveToCart(productId, variantId));
    }

    // ---- ADDRESSES ----
    @GetMapping("/addresses")
    @Operation(summary = "List saved shipping addresses")
    public ResponseEntity<List<AddressDtos.AddressDto>> getAddresses() {
        return ResponseEntity.ok(addressService.getMyAddresses());
    }

    @PostMapping("/addresses")
    @Operation(summary = "Add a new shipping address")
    public ResponseEntity<AddressDtos.AddressDto> createAddress(@Valid @RequestBody AddressDtos.AddressRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.createAddress(req));
    }

    @PutMapping("/addresses/{id}")
    @Operation(summary = "Update a shipping address")
    public ResponseEntity<AddressDtos.AddressDto> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressDtos.AddressRequest req) {
        return ResponseEntity.ok(addressService.updateAddress(id, req));
    }

    @DeleteMapping("/addresses/{id}")
    @Operation(summary = "Delete a shipping address")
    public ResponseEntity<Map<String, String>> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok(Map.of("message", "Address removed."));
    }
}
