package com.quickkart.dto;

import com.quickkart.entity.enums.OrderStatus;
import com.quickkart.entity.enums.PaymentMethod;
import com.quickkart.entity.enums.PaymentStatus;
import com.quickkart.entity.enums.ReturnStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OrderPaymentDtos {

    public record CheckoutRequest(
            Long addressId,
            AddressDtos.AddressRequest newAddress,

            @NotNull(message = "Payment method is required")
            PaymentMethod paymentMethod,

            String couponCode,
            String notes
    ) {}

    public record OrderItemDto(
            Long id,
            Long productId,
            Long variantId,
            String productName,
            String brandName,
            String size,
            String color,
            String sku,
            String imageUrl,
            BigDecimal unitPrice,
            BigDecimal originalPrice,
            Integer quantity,
            BigDecimal totalPrice
    ) {}

    public record OrderDto(
            Long id,
            String orderNumber,
            Long userId,
            String customerName,
            String customerEmail,
            String shippingFullName,
            String shippingPhone,
            String shippingAddressLine1,
            String shippingAddressLine2,
            String shippingCity,
            String shippingState,
            String shippingPostalCode,
            String shippingCountry,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal deliveryCharge,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String couponCode,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            OrderStatus orderStatus,
            LocalDate estimatedDeliveryDate,
            String trackingNumber,
            String notes,
            String cancellationReason,
            List<OrderItemDto> items,
            PaymentInitiateDto paymentDetails,
            ReturnDto returnRequest,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record PaymentInitiateDto(
            String gateway,
            String gatewayOrderId,
            String keyId,
            BigDecimal amount,
            String currency,
            Boolean testMode,
            String status
    ) {}

    public record PaymentVerifyRequest(
            @NotNull Long orderId,
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            String razorpaySignature,
            Boolean simulatedSuccess
    ) {}

    public record UpdateOrderStatusRequest(
            @NotNull OrderStatus status,
            String trackingNumber,
            LocalDate estimatedDeliveryDate,
            String notes
    ) {}

    public record CancelOrderRequest(
            @NotBlank(message = "Cancellation reason is required")
            String reason
    ) {}

    public record ReturnRequestPayload(
            @NotBlank(message = "Return reason is required")
            String reason,
            String comments
    ) {}

    public record ProcessReturnRequest(
            @NotNull ReturnStatus status,
            String adminNotes
    ) {}

    public record ReturnDto(
            Long id,
            Long orderId,
            String orderNumber,
            Long userId,
            String customerName,
            String reason,
            String comments,
            ReturnStatus status,
            BigDecimal refundAmount,
            String adminNotes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
