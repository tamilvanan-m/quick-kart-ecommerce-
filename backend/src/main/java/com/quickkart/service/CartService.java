package com.quickkart.service;

import com.quickkart.dto.CartWishlistDtos;
import com.quickkart.entity.*;
import com.quickkart.entity.enums.ProductStatus;
import com.quickkart.exception.BadRequestException;
import com.quickkart.exception.InsufficientStockException;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CouponService couponService;
    private final StoreSettingService storeSettingService;
    private final AuthService authService;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            CouponService couponService,
            StoreSettingService storeSettingService,
            AuthService authService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.couponService = couponService;
        this.storeSettingService = storeSettingService;
        this.authService = authService;
    }

    @Transactional
    public CartWishlistDtos.CartDto getMyCart() {
        User user = authService.getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);
        return buildCartDto(cart, user.getId());
    }

    @Transactional
    public CartWishlistDtos.CartDto addItem(CartWishlistDtos.AddCartItemRequest req) {
        User user = authService.getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findByIdAndDeletedFalse(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        ProductVariant variant = variantRepository.findById(req.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Selected size/color option not found."));

        if (!variant.getProduct().getId().equals(product.getId())) {
            throw new BadRequestException("Variant does not belong to the specified product.");
        }
        if (product.getStatus() != ProductStatus.ACTIVE || !Boolean.TRUE.equals(variant.getActive())) {
            throw new InsufficientStockException("This item is currently unavailable or sold out.");
        }

        int available = variant.getInventory() != null ? variant.getInventory().getAvailableQuantity() : 0;
        Optional<CartItem> existingOpt = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId());
        int targetQty = req.quantity() + existingOpt.map(CartItem::getQuantity).orElse(0);

        if (available <= 0) {
            throw new InsufficientStockException("This item is SOLD OUT.");
        }
        if (targetQty > available) {
            throw new InsufficientStockException("Only " + available + " unit(s) available for this size/color.");
        }

        if (existingOpt.isPresent()) {
            CartItem item = existingOpt.get();
            item.setQuantity(targetQty);
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setVariant(variant);
            item.setQuantity(targetQty);
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }

        return buildCartDto(cartRepository.findById(cart.getId()).orElse(cart), user.getId());
    }

    @Transactional
    public CartWishlistDtos.CartDto updateItem(Long itemId, CartWishlistDtos.UpdateCartItemRequest req) {
        User user = authService.getAuthenticatedUser();
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found."));

        ProductVariant targetVariant = item.getVariant();
        if (req.newVariantId() != null && !req.newVariantId().equals(targetVariant.getId())) {
            targetVariant = variantRepository.findById(req.newVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Selected variant not found."));
            if (!targetVariant.getProduct().getId().equals(item.getProduct().getId())) {
                throw new BadRequestException("Selected variant belongs to a different product.");
            }
            if (!Boolean.TRUE.equals(targetVariant.getActive())) {
                throw new InsufficientStockException("Selected size/color is unavailable.");
            }
        }

        int targetQty = req.quantity() != null ? req.quantity() : item.getQuantity();
        int available = targetVariant.getInventory() != null ? targetVariant.getInventory().getAvailableQuantity() : 0;

        if (item.getProduct().getStatus() != ProductStatus.ACTIVE || available <= 0) {
            throw new InsufficientStockException("Selected item is SOLD OUT.");
        }
        if (targetQty > available) {
            throw new InsufficientStockException("Only " + available + " unit(s) available in stock.");
        }

        // If user switched to a variant that is already in the cart, merge quantities
        if (!targetVariant.getId().equals(item.getVariant().getId())) {
            Optional<CartItem> duplicateOpt = cartItemRepository.findByCartIdAndVariantId(item.getCart().getId(), targetVariant.getId());
            if (duplicateOpt.isPresent()) {
                CartItem duplicate = duplicateOpt.get();
                int merged = duplicate.getQuantity() + targetQty;
                if (merged > available) {
                    throw new InsufficientStockException("Only " + available + " unit(s) available in stock.");
                }
                duplicate.setQuantity(merged);
                cartItemRepository.save(duplicate);
                item.getCart().getItems().remove(item);
                cartItemRepository.delete(item);
                return buildCartDto(item.getCart(), user.getId());
            }
        }

        item.setVariant(targetVariant);
        item.setQuantity(targetQty);
        cartItemRepository.save(item);
        return buildCartDto(item.getCart(), user.getId());
    }

    @Transactional
    public CartWishlistDtos.CartDto removeItem(Long itemId) {
        User user = authService.getAuthenticatedUser();
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found."));
        Cart cart = item.getCart();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return buildCartDto(cart, user.getId());
    }

    @Transactional
    public CartWishlistDtos.CartDto applyCoupon(String code) {
        User user = authService.getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);
        BigDecimal subtotal = computeSubtotal(cart);
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Add items to your bag before applying a coupon.");
        }
        Coupon coupon = couponService.validateCouponForUserAndAmount(code, user.getId(), subtotal);
        cart.setCouponCode(coupon.getCode());
        cartRepository.save(cart);
        return buildCartDto(cart, user.getId());
    }

    @Transactional
    public CartWishlistDtos.CartDto removeCoupon() {
        User user = authService.getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);
        cart.setCouponCode(null);
        cartRepository.save(cart);
        return buildCartDto(cart, user.getId());
    }

    @Transactional
    public void clearCart(Cart cart) {
        cart.getItems().clear();
        cart.setCouponCode(null);
        cartRepository.save(cart);
    }

    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(user);
            return cartRepository.save(c);
        });
    }

    private BigDecimal computeSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(i -> {
                    BigDecimal unit = i.getVariant().getPriceOverride() != null
                            ? i.getVariant().getPriceOverride()
                            : i.getProduct().getSellingPrice();
                    return unit.multiply(BigDecimal.valueOf(i.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public CartWishlistDtos.CartDto buildCartDto(Cart cart, Long userId) {
        StoreSetting settings = storeSettingService.getOrCreateEntity();

        List<CartWishlistDtos.CartItemDto> itemDtos = cart.getItems().stream().map(item -> {
            Product p = item.getProduct();
            ProductVariant v = item.getVariant();
            BigDecimal unitPrice = v.getPriceOverride() != null ? v.getPriceOverride() : p.getSellingPrice();
            BigDecimal originalPrice = p.getOriginalPrice().max(unitPrice);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            int available = v.getInventory() != null ? v.getInventory().getAvailableQuantity() : 0;
            boolean soldOut = Boolean.TRUE.equals(p.getDeleted())
                    || p.getStatus() != ProductStatus.ACTIVE
                    || !Boolean.TRUE.equals(v.getActive())
                    || available <= 0;
            String imageUrl = p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl();

            return new CartWishlistDtos.CartItemDto(
                    item.getId(),
                    p.getId(),
                    p.getName(),
                    p.getSlug(),
                    p.getBrand().getName(),
                    imageUrl,
                    v.getId(),
                    v.getSize(),
                    v.getColor(),
                    v.getColorHex(),
                    unitPrice,
                    originalPrice,
                    item.getQuantity(),
                    lineTotal,
                    available,
                    soldOut
            );
        }).toList();

        int totalItems = itemDtos.stream().mapToInt(CartWishlistDtos.CartItemDto::quantity).sum();
        BigDecimal mrpTotal = itemDtos.stream()
                .map(i -> i.originalPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal subtotal = itemDtos.stream()
                .map(CartWishlistDtos.CartItemDto::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal productSavings = mrpTotal.subtract(subtotal).max(BigDecimal.ZERO);

        String appliedCouponCode = cart.getCouponCode();
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (appliedCouponCode != null && !appliedCouponCode.isBlank() && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            try {
                Coupon coupon = couponService.validateCouponForUserAndAmount(appliedCouponCode, userId, subtotal);
                couponDiscount = couponService.calculateDiscount(coupon, subtotal);
            } catch (Exception ex) {
                appliedCouponCode = null;
                cart.setCouponCode(null);
                cartRepository.save(cart);
            }
        }

        BigDecimal discountedSubtotal = subtotal.subtract(couponDiscount).max(BigDecimal.ZERO);
        BigDecimal deliveryCharge = BigDecimal.ZERO;
        if (subtotal.compareTo(BigDecimal.ZERO) > 0 && discountedSubtotal.compareTo(settings.getFreeDeliveryThreshold()) < 0) {
            deliveryCharge = settings.getDeliveryCharge();
        }
        BigDecimal finalAmount = discountedSubtotal.add(deliveryCharge);

        return new CartWishlistDtos.CartDto(
                cart.getId(),
                itemDtos,
                totalItems,
                mrpTotal,
                subtotal,
                productSavings,
                appliedCouponCode,
                couponDiscount,
                deliveryCharge,
                settings.getFreeDeliveryThreshold(),
                finalAmount
        );
    }
}
