package com.quickkart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickkart.dto.OrderPaymentDtos;
import com.quickkart.entity.Order;
import com.quickkart.entity.Payment;
import com.quickkart.exception.BadRequestException;
import com.quickkart.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String razorpayWebhookSecret;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public PaymentService(
            PaymentRepository paymentRepository,
            ObjectMapper objectMapper,
            @Value("${app.razorpay.key-id:}") String razorpayKeyId,
            @Value("${app.razorpay.key-secret:}") String razorpayKeySecret,
            @Value("${app.razorpay.webhook-secret:}") String razorpayWebhookSecret) {
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
        this.razorpayWebhookSecret = razorpayWebhookSecret;
    }

    public boolean isLiveRazorpayConfigured() {
        return razorpayKeyId != null && !razorpayKeyId.isBlank()
                && razorpayKeySecret != null && !razorpayKeySecret.isBlank();
    }

    @Transactional
    public OrderPaymentDtos.PaymentInitiateDto createPaymentOrder(Order order) {
        if (order.getPaymentMethod().name().equals("COD")) {
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentGateway("COD");
            payment.setAmount(order.getTotalAmount());
            payment.setCurrency("INR");
            payment.setStatus("PENDING");
            paymentRepository.save(payment);
            return new OrderPaymentDtos.PaymentInitiateDto(
                    "COD", null, null, order.getTotalAmount(), "INR", false, "PENDING"
            );
        }

        String gatewayOrderId;
        boolean testMode = !isLiveRazorpayConfigured();
        String keyToReturn = isLiveRazorpayConfigured() ? razorpayKeyId : "rzp_test_quickkart_demo";
        String rawResponse = null;

        if (isLiveRazorpayConfigured()) {
            try {
                long amountInPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
                String payload = objectMapper.writeValueAsString(Map.of(
                        "amount", amountInPaise,
                        "currency", "INR",
                        "receipt", order.getOrderNumber()
                ));
                String auth = Base64.getEncoder().encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.razorpay.com/v1/orders"))
                        .header("Authorization", "Basic " + auth)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new BadRequestException("Unable to initiate Razorpay payment: " + response.body());
                }
                rawResponse = response.body();
                JsonNode node = objectMapper.readTree(rawResponse);
                gatewayOrderId = node.get("id").asText();
            } catch (BadRequestException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BadRequestException("Razorpay initialization error: " + ex.getMessage());
            }
        } else {
            gatewayOrderId = "order_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            rawResponse = "{\"mode\":\"simulated_test\",\"order\":\"" + gatewayOrderId + "\"}";
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentGateway("RAZORPAY");
        payment.setGatewayOrderId(gatewayOrderId);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency("INR");
        payment.setStatus("CREATED");
        payment.setRawResponse(rawResponse);
        paymentRepository.save(payment);

        return new OrderPaymentDtos.PaymentInitiateDto(
                "RAZORPAY",
                gatewayOrderId,
                keyToReturn,
                order.getTotalAmount(),
                "INR",
                testMode,
                "CREATED"
        );
    }

    public boolean verifyRazorpaySignature(String gatewayOrderId, String paymentId, String signature, Boolean simulatedSuccess) {
        if (!isLiveRazorpayConfigured()) {
            // In simulated test mode (when no live Razorpay credentials are configured),
            // verify the expected test order format and simulatedSuccess flag.
            return Boolean.TRUE.equals(simulatedSuccess)
                    && gatewayOrderId != null && gatewayOrderId.startsWith("order_test_")
                    && paymentId != null && !paymentId.isBlank();
        }
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String expected = hmacSha256Hex(gatewayOrderId + "|" + paymentId, razorpayKeySecret);
        return constantTimeEquals(expected, signature);
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank()) {
            return false;
        }
        String expected = hmacSha256Hex(payload, razorpayWebhookSecret);
        return constantTimeEquals(expected, signature);
    }

    private String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC calculation failed", ex);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
