package com.quickkart.service;

import com.quickkart.dto.CatalogDtos;
import com.quickkart.dto.CouponBannerReviewDtos;
import com.quickkart.entity.Order;
import com.quickkart.entity.Product;
import com.quickkart.entity.Review;
import com.quickkart.entity.User;
import com.quickkart.exception.BadRequestException;
import com.quickkart.exception.ConflictException;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.OrderRepository;
import com.quickkart.repository.ProductRepository;
import com.quickkart.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final AuthService authService;
    private final EntityMapper mapper;

    public ReviewService(
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            AuthService authService,
            EntityMapper mapper) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.authService = authService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PageResponse<CouponBannerReviewDtos.ReviewDto> getProductReviews(Long productId, int page, int size) {
        Page<CouponBannerReviewDtos.ReviewDto> result = reviewRepository
                .findByProductIdAndApprovedTrueOrderByCreatedAtDesc(productId, PageRequest.of(page, size))
                .map(mapper::toReviewDto);
        return mapper.toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public List<CouponBannerReviewDtos.ReviewDto> getFeaturedHomepageReviews() {
        return reviewRepository.findTop6ByApprovedTrueAndRatingGreaterThanEqualOrderByCreatedAtDesc(4).stream()
                .map(mapper::toReviewDto)
                .toList();
    }

    @Transactional
    public CouponBannerReviewDtos.ReviewDto submitReview(Long productId, CouponBannerReviewDtos.ReviewRequest req) {
        User user = authService.getAuthenticatedUser();
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        List<Order> matchingOrders = orderRepository.findUserOrdersForProduct(user.getId(), productId);
        if (matchingOrders.isEmpty()) {
            throw new BadRequestException("Only verified purchasers of this product can submit a review.");
        }

        if (reviewRepository.findByProductIdAndUserId(productId, user.getId()).isPresent()) {
            throw new ConflictException("You have already reviewed this product.");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setOrder(matchingOrders.get(0));
        review.setRating(req.rating());
        review.setTitle(req.title() != null ? req.title().trim() : null);
        review.setComment(req.comment().trim());
        review.setVerifiedPurchase(true);
        review.setApproved(true);

        Review saved = reviewRepository.save(review);
        recalculateProductRating(product);
        return mapper.toReviewDto(saved);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PageResponse<CouponBannerReviewDtos.ReviewDto> getAllReviewsAdmin(int page, int size) {
        return mapper.toPageResponse(reviewRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).map(mapper::toReviewDto));
    }

    @Transactional
    public CouponBannerReviewDtos.ReviewDto toggleReviewApproval(Long reviewId, boolean approved) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        r.setApproved(approved);
        Review saved = reviewRepository.save(r);
        recalculateProductRating(r.getProduct());
        return mapper.toReviewDto(saved);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        Product product = r.getProduct();
        reviewRepository.delete(r);
        recalculateProductRating(product);
    }

    private void recalculateProductRating(Product product) {
        List<Review> approved = reviewRepository.findByProductIdAndApprovedTrue(product.getId());
        if (approved.isEmpty()) {
            product.setRatingAvg(BigDecimal.ZERO);
            product.setReviewCount(0);
        } else {
            double avg = approved.stream().mapToInt(Review::getRating).average().orElse(0.0);
            product.setRatingAvg(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            product.setReviewCount(approved.size());
        }
        productRepository.save(product);
    }
}
