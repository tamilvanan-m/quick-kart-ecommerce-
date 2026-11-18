package com.quickkart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickkart.dto.CatalogDtos;
import com.quickkart.dto.OrderPaymentDtos;
import com.quickkart.entity.*;
import com.quickkart.entity.enums.*;
import com.quickkart.exception.BadRequestException;
import com.quickkart.exception.ConflictException;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final CartService cartService;
    private final AddressService addressService;
    private final CouponService couponService;
    private final StoreSettingService storeSettingService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final EntityMapper mapper;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            AddressRepository addressRepository,
            PaymentRepository paymentRepository,
            CouponRepository couponRepository,
            CouponUsageRepository couponUsageRepository,
            ReturnRequestRepository returnRequestRepository,
            CartService cartService,
            AddressService addressService,
            CouponService couponService,
            StoreSettingService storeSettingService,
            InventoryService inventoryService,
            PaymentService paymentService,
            AuthService authService,
            AuditLogService auditLogService,
            EmailService emailService,
            EntityMapper mapper,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.paymentRepository = paymentRepository;
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.cartService = cartService;
        this.addressService = addressService;
        this.couponService = couponService;
        this.storeSettingService = storeSettingService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.authService = authService;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderPaymentDtos.OrderDto placeOrder(OrderPaymentDtos.CheckoutRequest req) {
        User user = authService.getAuthenticatedUser();
        StoreSetting settings = storeSettingService.getOrCreateEntity();

        if (req.paymentMethod() == PaymentMethod.COD && !Boolean.TRUE.equals(settings.getCodEnabled())) {
            throw new BadRequestException("Cash on Delivery is currently disabled by the store.");
        }
        if (req.paymentMethod() == PaymentMethod.RAZORPAY && !Boolean.TRUE.equals(settings.getRazorpayEnabled())) {
            throw new BadRequestException("Online payment is currently disabled by the store.");
        }

        Cart cart = cartService.getOrCreateCart(user);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your shopping bag is empty.");
        }

        Address shippingAddress = resolveShippingAddress(user, req);

        // Compute prices strictly on the backend from database entities
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setShippingFullName(shippingAddress.getFullName());
        order.setShippingPhone(shippingAddress.getPhone());
        order.setShippingAddressLine1(shippingAddress.getAddressLine1());
        order.setShippingAddressLine2(shippingAddress.getAddressLine2());
        order.setShippingCity(shippingAddress.getCity());
        order.setShippingState(shippingAddress.getState());
        order.setShippingPostalCode(shippingAddress.getPostalCode());
        order.setShippingCountry(shippingAddress.getCountry());
        order.setPaymentMethod(req.paymentMethod());
        order.setNotes(req.notes());
        order.setEstimatedDeliveryDate(LocalDate.now().plusDays(5));

        for (CartItem ci : cart.getItems()) {
            Product p = ci.getProduct();
            ProductVariant v = ci.getVariant();
            BigDecimal unitPrice = v.getPriceOverride() != null ? v.getPriceOverride() : p.getSellingPrice();
            BigDecimal originalPrice = p.getOriginalPrice().max(unitPrice);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(p);
            oi.setVariant(v);
            oi.setProductName(p.getName());
            oi.setBrandName(p.getBrand().getName());
            oi.setSize(v.getSize());
            oi.setColor(v.getColor());
            oi.setSku(v.getSku());
            oi.setImageUrl(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());
            oi.setUnitPrice(unitPrice);
            oi.setOriginalPrice(originalPrice);
            oi.setQuantity(ci.getQuantity());
            oi.setTotalPrice(lineTotal);
            orderItems.add(oi);
        }
        order.setItems(orderItems);

        String couponCode = (req.couponCode() != null && !req.couponCode().isBlank())
                ? req.couponCode().trim().toUpperCase()
                : cart.getCouponCode();

        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (couponCode != null && !couponCode.isBlank()) {
            appliedCoupon = couponService.validateCouponForUserAndAmount(couponCode, user.getId(), subtotal);
            discountAmount = couponService.calculateDiscount(appliedCoupon, subtotal);
        }

        BigDecimal discountedSubtotal = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        BigDecimal deliveryCharge = discountedSubtotal.compareTo(settings.getFreeDeliveryThreshold()) < 0
                ? settings.getDeliveryCharge()
                : BigDecimal.ZERO;
        BigDecimal totalAmount = discountedSubtotal.add(deliveryCharge);

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setDeliveryCharge(deliveryCharge);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(totalAmount);
        order.setCouponCode(appliedCoupon != null ? appliedCoupon.getCode() : null);

        if (req.paymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order.setTrackingNumber("QK" + System.currentTimeMillis() % 100000000);
        } else {
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setOrderStatus(OrderStatus.PLACED);
        }

        Order savedOrder = orderRepository.save(order);

        if (req.paymentMethod() == PaymentMethod.COD) {
            inventoryService.deductStockForImmediateOrder(savedOrder, user.getId());
            cartService.clearCart(cart);
        } else {
            inventoryService.reserveStockForPendingPayment(savedOrder);
        }

        if (appliedCoupon != null) {
            appliedCoupon.setUsedCount(appliedCoupon.getUsedCount() + 1);
            couponRepository.save(appliedCoupon);

            CouponUsage usage = new CouponUsage();
            usage.setCoupon(appliedCoupon);
            usage.setUser(user);
            usage.setOrder(savedOrder);
            usage.setDiscountApplied(discountAmount);
            couponUsageRepository.save(usage);
        }

        OrderPaymentDtos.PaymentInitiateDto paymentDto = paymentService.createPaymentOrder(savedOrder);

        if (req.paymentMethod() == PaymentMethod.COD) {
            emailService.sendEmail(user.getEmail(), "Order Confirmed - " + savedOrder.getOrderNumber(),
                    "Thank you for shopping with Quick kart! Your COD order " + savedOrder.getOrderNumber() + " for ₹" + savedOrder.getTotalAmount() + " is confirmed.");
        }

        return mapper.toOrderDto(savedOrder, paymentDto, null);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public OrderPaymentDtos.OrderDto verifyPayment(OrderPaymentDtos.PaymentVerifyRequest req) {
        User user = authService.getAuthenticatedUser();
        Order order = orderRepository.findByIdAndUserId(req.orderId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        Payment payment = paymentRepository.findByGatewayOrderId(req.razorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found."));

        if (!payment.getOrder().getId().equals(order.getId())) {
            throw new BadRequestException("Payment order mismatch.");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return mapper.toOrderDto(order, null, null);
        }

        boolean verified = paymentService.verifyRazorpaySignature(
                req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature(), req.simulatedSuccess()
        );

        if (!verified) {
            payment.setStatus("FAILED");
            payment.setFailureReason("Payment verification failed or was cancelled.");
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setCancellationReason("Payment verification failed.");
            orderRepository.save(order);

            inventoryService.releaseReservedStock(order);
            throw new BadRequestException("Payment verification failed. Reserved stock has been released.");
        }

        payment.setGatewayPaymentId(req.razorpayPaymentId());
        payment.setGatewaySignature(req.razorpaySignature());
        payment.setStatus("CAPTURED");
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setTrackingNumber("QK" + System.currentTimeMillis() % 100000000);
        Order savedOrder = orderRepository.save(order);

        inventoryService.commitReservedStock(savedOrder, user.getId());
        Cart cart = cartService.getOrCreateCart(user);
        cartService.clearCart(cart);

        emailService.sendEmail(user.getEmail(), "Payment Received - " + savedOrder.getOrderNumber(),
                "Your payment of ₹" + savedOrder.getTotalAmount() + " for order " + savedOrder.getOrderNumber() + " was successful.");

        return mapper.toOrderDto(savedOrder, null, null);
    }

    @Transactional
    public void handleRazorpayWebhook(String payload, String signature) {
        if (!paymentService.verifyWebhookSignature(payload, signature)) {
            throw new BadRequestException("Invalid webhook signature.");
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText("");
            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
            String gatewayOrderId = paymentEntity.path("order_id").asText("");
            String gatewayPaymentId = paymentEntity.path("id").asText("");

            if (gatewayOrderId.isBlank()) return;
            Payment payment = paymentRepository.findByGatewayOrderId(gatewayOrderId).orElse(null);
            if (payment == null) return;
            Order order = payment.getOrder();

            if ("payment.captured".equals(event) && order.getPaymentStatus() != PaymentStatus.PAID) {
                payment.setGatewayPaymentId(gatewayPaymentId);
                payment.setStatus("CAPTURED");
                paymentRepository.save(payment);

                order.setPaymentStatus(PaymentStatus.PAID);
                order.setOrderStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
                inventoryService.commitReservedStock(order, order.getUser().getId());
            } else if ("payment.failed".equals(event) && order.getPaymentStatus() == PaymentStatus.PENDING) {
                payment.setStatus("FAILED");
                payment.setFailureReason("Gateway reported payment failed");
                paymentRepository.save(payment);

                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setOrderStatus(OrderStatus.CANCELLED);
                order.setCancellationReason("Payment failed");
                orderRepository.save(order);
                inventoryService.releaseReservedStock(order);
            }
        } catch (Exception ex) {
            throw new BadRequestException("Webhook processing error: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PageResponse<OrderPaymentDtos.OrderDto> getMyOrders(int page, int size) {
        User user = authService.getAuthenticatedUser();
        Page<OrderPaymentDtos.OrderDto> result = orderRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(Math.max(0, page), Math.max(1, size)))
                .map(this::toFullOrderDto);
        return mapper.toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public OrderPaymentDtos.OrderDto getMyOrder(Long orderId) {
        User user = authService.getAuthenticatedUser();
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        return toFullOrderDto(order);
    }

    @Transactional
    public OrderPaymentDtos.OrderDto cancelMyOrder(Long orderId, OrderPaymentDtos.CancelOrderRequest req) {
        User user = authService.getAuthenticatedUser();
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        if (!List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PACKED).contains(order.getOrderStatus())) {
            throw new BadRequestException("Order cannot be cancelled once it has been shipped or delivered.");
        }

        boolean wasReservedPendingOnline = order.getPaymentMethod() == PaymentMethod.RAZORPAY
                && order.getPaymentStatus() == PaymentStatus.PENDING;

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(req.reason().trim());
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        Order saved = orderRepository.save(order);

        if (wasReservedPendingOnline) {
            inventoryService.releaseReservedStock(saved);
        } else {
            inventoryService.restoreDeductedStock(saved, InventoryTransactionType.CANCELLATION_RESTORE,
                    "Customer cancelled order " + saved.getOrderNumber(), user.getId());
        }

        auditLogService.log("ORDER_CANCELLED", "ORDER", String.valueOf(saved.getId()),
                "Customer cancelled order " + saved.getOrderNumber());
        return toFullOrderDto(saved);
    }

    @Transactional
    public OrderPaymentDtos.ReturnDto requestReturn(Long orderId, OrderPaymentDtos.ReturnRequestPayload req) {
        User user = authService.getAuthenticatedUser();
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Returns can only be requested for delivered orders.");
        }
        if (returnRequestRepository.findByOrderId(orderId).isPresent()) {
            throw new ConflictException("A return request has already been submitted for this order.");
        }

        ReturnRequest rr = new ReturnRequest();
        rr.setOrder(order);
        rr.setUser(user);
        rr.setReason(req.reason().trim());
        rr.setComments(req.comments());
        rr.setStatus(ReturnStatus.REQUESTED);
        rr.setRefundAmount(order.getTotalAmount());
        ReturnRequest savedReturn = returnRequestRepository.save(rr);

        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
        orderRepository.save(order);

        return mapper.toReturnDto(savedReturn);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PageResponse<OrderPaymentDtos.OrderDto> getAdminOrders(
            String q, OrderStatus orderStatus, PaymentStatus paymentStatus,
            LocalDate fromDate, LocalDate toDate, int page, int size) {

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern),
                        cb.like(cb.lower(root.get("shippingFullName")), pattern),
                        cb.like(cb.lower(root.get("user").get("email")), pattern)
                ));
            }
            if (orderStatus != null) {
                predicates.add(cb.equal(root.get("orderStatus"), orderStatus));
            }
            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<OrderPaymentDtos.OrderDto> result = orderRepository
                .findAll(spec, PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toFullOrderDto);
        return mapper.toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public OrderPaymentDtos.OrderDto getAdminOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        return toFullOrderDto(order);
    }

    @Transactional
    public OrderPaymentDtos.OrderDto updateAdminOrderStatus(Long id, OrderPaymentDtos.UpdateOrderStatusRequest req, Long actorUserId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        OrderStatus prevStatus = order.getOrderStatus();
        OrderStatus nextStatus = req.status();

        order.setOrderStatus(nextStatus);
        if (req.trackingNumber() != null && !req.trackingNumber().isBlank()) {
            order.setTrackingNumber(req.trackingNumber().trim());
        }
        if (req.estimatedDeliveryDate() != null) {
            order.setEstimatedDeliveryDate(req.estimatedDeliveryDate());
        }
        if (req.notes() != null) {
            order.setNotes(req.notes());
        }

        if (nextStatus == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        if (nextStatus == OrderStatus.CANCELLED && prevStatus != OrderStatus.CANCELLED) {
            boolean wasReserved = order.getPaymentMethod() == PaymentMethod.RAZORPAY && order.getPaymentStatus() == PaymentStatus.PENDING;
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
            if (wasReserved) {
                inventoryService.releaseReservedStock(order);
            } else {
                inventoryService.restoreDeductedStock(order, InventoryTransactionType.CANCELLATION_RESTORE,
                        "Admin cancelled order " + order.getOrderNumber(), actorUserId);
            }
        }

        if (nextStatus == OrderStatus.RETURNED && prevStatus != OrderStatus.RETURNED) {
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
            inventoryService.restoreDeductedStock(order, InventoryTransactionType.RETURN_RESTORE,
                    "Order returned: " + order.getOrderNumber(), actorUserId);
        }

        Order saved = orderRepository.save(order);
        auditLogService.log("ORDER_STATUS_UPDATED", "ORDER", String.valueOf(saved.getId()),
                "Order " + saved.getOrderNumber() + " moved from " + prevStatus + " to " + nextStatus);
        return toFullOrderDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderPaymentDtos.ReturnDto> getAllReturns() {
        return returnRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toReturnDto)
                .toList();
    }

    @Transactional
    public OrderPaymentDtos.ReturnDto processReturn(Long returnId, OrderPaymentDtos.ProcessReturnRequest req, Long actorUserId) {
        ReturnRequest rr = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found."));

        ReturnStatus prev = rr.getStatus();
        rr.setStatus(req.status());
        rr.setAdminNotes(req.adminNotes());
        ReturnRequest saved = returnRequestRepository.save(rr);

        if (req.status() == ReturnStatus.REFUNDED && prev != ReturnStatus.REFUNDED) {
            Order order = rr.getOrder();
            if (order.getOrderStatus() != OrderStatus.RETURNED) {
                order.setOrderStatus(OrderStatus.RETURNED);
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                orderRepository.save(order);
                inventoryService.restoreDeductedStock(order, InventoryTransactionType.RETURN_RESTORE,
                        "Return refunded for order " + order.getOrderNumber(), actorUserId);
            }
        }

        auditLogService.log("RETURN_PROCESSED", "RETURN", String.valueOf(returnId),
                "Return status updated to " + req.status());
        return mapper.toReturnDto(saved);
    }

    private Address resolveShippingAddress(User user, OrderPaymentDtos.CheckoutRequest req) {
        if (req.addressId() != null) {
            return addressRepository.findByIdAndUserId(req.addressId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Selected shipping address not found."));
        }
        if (req.newAddress() != null) {
            return addressService.saveAddressForUser(user, req.newAddress());
        }
        throw new BadRequestException("Please select or provide a shipping address.");
    }

    private OrderPaymentDtos.OrderDto toFullOrderDto(Order order) {
        OrderPaymentDtos.ReturnDto returnDto = returnRequestRepository.findByOrderId(order.getId())
                .map(mapper::toReturnDto)
                .orElse(null);
        return mapper.toOrderDto(order, null, returnDto);
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = 1000 + new Random().nextInt(9000);
        return "QK-" + datePart + "-" + rand;
    }
}
