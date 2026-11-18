package com.quickkart.controller;

import com.quickkart.dto.CatalogDtos;
import com.quickkart.dto.OrderPaymentDtos;
import com.quickkart.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Orders & Payments", description = "Checkout, Razorpay verification, webhooks, order tracking, cancellations, and returns")
public class OrderPaymentController {

    private final OrderService orderService;

    public OrderPaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    @Operation(summary = "Place order from shopping bag (backend validates stock and prices)")
    public ResponseEntity<OrderPaymentDtos.OrderDto> placeOrder(@Valid @RequestBody OrderPaymentDtos.CheckoutRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(req));
    }

    @GetMapping("/orders/my-orders")
    @Operation(summary = "List authenticated customer's orders")
    public ResponseEntity<CatalogDtos.PageResponse<OrderPaymentDtos.OrderDto>> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getMyOrders(page, size));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get order details and tracking info")
    public ResponseEntity<OrderPaymentDtos.OrderDto> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getMyOrder(id));
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel an eligible order and restore stock")
    public ResponseEntity<OrderPaymentDtos.OrderDto> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderPaymentDtos.CancelOrderRequest req) {
        return ResponseEntity.ok(orderService.cancelMyOrder(id, req));
    }

    @PostMapping("/orders/{id}/return")
    @Operation(summary = "Submit a return request for a delivered order")
    public ResponseEntity<OrderPaymentDtos.ReturnDto> requestReturn(
            @PathVariable Long id,
            @Valid @RequestBody OrderPaymentDtos.ReturnRequestPayload req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.requestReturn(id, req));
    }

    @PostMapping("/payments/verify")
    @Operation(summary = "Verify Razorpay payment signature and confirm order")
    public ResponseEntity<OrderPaymentDtos.OrderDto> verifyPayment(@Valid @RequestBody OrderPaymentDtos.PaymentVerifyRequest req) {
        return ResponseEntity.ok(orderService.verifyPayment(req));
    }

    @PostMapping("/payments/webhook/razorpay")
    @Operation(summary = "Handle Razorpay server-to-server webhook events")
    public ResponseEntity<Map<String, String>> razorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        orderService.handleRazorpayWebhook(payload, signature);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
