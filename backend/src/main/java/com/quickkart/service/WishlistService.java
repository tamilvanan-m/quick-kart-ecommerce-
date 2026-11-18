package com.quickkart.service;

import com.quickkart.dto.CartWishlistDtos;
import com.quickkart.entity.*;
import com.quickkart.exception.BadRequestException;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.ProductRepository;
import com.quickkart.repository.WishlistItemRepository;
import com.quickkart.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final AuthService authService;
    private final CartService cartService;
    private final EntityMapper mapper;

    public WishlistService(
            WishlistRepository wishlistRepository,
            WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository,
            AuthService authService,
            CartService cartService,
            EntityMapper mapper) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.authService = authService;
        this.cartService = cartService;
        this.mapper = mapper;
    }

    @Transactional
    public CartWishlistDtos.WishlistDto getMyWishlist() {
        User user = authService.getAuthenticatedUser();
        Wishlist wishlist = getOrCreate(user);
        return toDto(wishlist);
    }

    @Transactional
    public CartWishlistDtos.WishlistDto addProduct(Long productId) {
        User user = authService.getAuthenticatedUser();
        Wishlist wishlist = getOrCreate(user);
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        if (wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId).isEmpty()) {
            WishlistItem item = new WishlistItem();
            item.setWishlist(wishlist);
            item.setProduct(product);
            wishlist.getItems().add(item);
            wishlistItemRepository.save(item);
        }
        return toDto(wishlist);
    }

    @Transactional
    public CartWishlistDtos.WishlistDto removeProduct(Long productId) {
        User user = authService.getAuthenticatedUser();
        Wishlist wishlist = getOrCreate(user);
        wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId).ifPresent(item -> {
            wishlist.getItems().remove(item);
            wishlistItemRepository.delete(item);
        });
        return toDto(wishlist);
    }

    @Transactional
    public CartWishlistDtos.CartDto moveToCart(Long productId, Long variantId) {
        User user = authService.getAuthenticatedUser();
        Wishlist wishlist = getOrCreate(user);
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        Long chosenVariantId = variantId;
        if (chosenVariantId == null) {
            chosenVariantId = product.getVariants().stream()
                    .filter(v -> Boolean.TRUE.equals(v.getActive()) && v.getInventory() != null && v.getInventory().getAvailableQuantity() > 0)
                    .map(ProductVariant::getId)
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("No in-stock variant available for this product."));
        }

        CartWishlistDtos.CartDto cart = cartService.addItem(new CartWishlistDtos.AddCartItemRequest(productId, chosenVariantId, 1));
        wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId).ifPresent(item -> {
            wishlist.getItems().remove(item);
            wishlistItemRepository.delete(item);
        });
        return cart;
    }

    private Wishlist getOrCreate(User user) {
        return wishlistRepository.findByUserId(user.getId()).orElseGet(() -> {
            Wishlist w = new Wishlist();
            w.setUser(user);
            return wishlistRepository.save(w);
        });
    }

    private CartWishlistDtos.WishlistDto toDto(Wishlist wishlist) {
        List<CartWishlistDtos.WishlistItemDto> items = wishlist.getItems().stream()
                .filter(wi -> !Boolean.TRUE.equals(wi.getProduct().getDeleted()))
                .map(wi -> new CartWishlistDtos.WishlistItemDto(
                        wi.getId(),
                        wi.getProduct().getId(),
                        mapper.toProductDto(wi.getProduct())
                ))
                .toList();
        return new CartWishlistDtos.WishlistDto(wishlist.getId(), items);
    }
}
