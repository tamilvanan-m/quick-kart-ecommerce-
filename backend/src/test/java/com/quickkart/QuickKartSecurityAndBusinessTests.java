package com.quickkart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickkart.dto.*;
import com.quickkart.entity.*;
import com.quickkart.entity.enums.*;
import com.quickkart.repository.*;
import com.quickkart.service.InventoryService;
import com.quickkart.service.StoreSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuickKartSecurityAndBusinessTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponUsageRepository couponUsageRepository;
    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private StoreSettingService storeSettingService;

    private String ownerToken;
    private String staffToken;
    private String customerToken;
    private String secondCustomerToken;
    private Brand testBrand;
    private Category testCategory;

    @BeforeEach
    void setup() throws Exception {
        reviewRepository.deleteAll();
        couponUsageRepository.deleteAll();
        paymentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        wishlistRepository.deleteAll();
        addressRepository.deleteAll();
        productRepository.deleteAll();
        couponRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role ownerRole = roleRepository.save(new Role(RoleName.OWNER, "Owner"));
        roleRepository.save(new Role(RoleName.ADMIN, "Admin"));
        Role staffRole = roleRepository.save(new Role(RoleName.STAFF, "Staff"));
        Role customerRole = roleRepository.save(new Role(RoleName.CUSTOMER, "Customer"));
        storeSettingService.getOrCreateEntity();

        createUser("Store Owner", "owner@quickkart.com", "Owner@123", ownerRole);
        createUser("Store Staff", "staff@quickkart.com", "Staff@123", staffRole);
        createUser("Customer One", "cust1@quickkart.com", "Cust@123", customerRole);
        createUser("Customer Two", "cust2@quickkart.com", "Cust@123", customerRole);

        ownerToken = loginAndGetToken("owner@quickkart.com", "Owner@123");
        staffToken = loginAndGetToken("staff@quickkart.com", "Staff@123");
        customerToken = loginAndGetToken("cust1@quickkart.com", "Cust@123");
        secondCustomerToken = loginAndGetToken("cust2@quickkart.com", "Cust@123");

        Brand b = new Brand();
        b.setName("Kora Studio");
        b.setSlug("kora-studio");
        b.setActive(true);
        testBrand = brandRepository.save(b);

        Category c = new Category();
        c.setName("Shirts");
        c.setSlug("shirts");
        c.setGenderScope("MEN");
        c.setActive(true);
        testCategory = categoryRepository.save(c);
    }

    private User createUser(String name, String email, String rawPassword, Role role) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setActive(true);
        u.setEmailVerified(true);
        u.setRoles(Set.of(role));
        return userRepository.save(u);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthDtos.LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private JsonNode createProductAsOwner(String name, String price, int initialStock) throws Exception {
        BigDecimal selling = new BigDecimal(price);
        BigDecimal original = selling.add(new BigDecimal("500.00"));
        CatalogDtos.ProductRequest req = new CatalogDtos.ProductRequest(
                name, null, testBrand.getId(), testCategory.getId(), Gender.MEN,
                "Premium combed cotton shirt", "100% Cotton",
                original, selling,
                ProductStatus.ACTIVE, true, true, false, true,
                List.of(new CatalogDtos.ProductImageRequest("https://example.com/shirt.jpg", null, true, 0, name)),
                List.of(new CatalogDtos.ProductVariantRequest(null, null, "M", "Black", "#111111", null, true, initialStock, 2))
        );
        MvcResult res = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    @Test
    void customerCannotAccessAdminDashboardOrAddProducts() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        CatalogDtos.ProductRequest req = new CatalogDtos.ProductRequest(
                "Unauthorized Shirt", null, testBrand.getId(), testCategory.getId(), Gender.MEN,
                "Desc", null, new BigDecimal("999.00"), new BigDecimal("799.00"),
                ProductStatus.ACTIVE, false, false, false, false,
                List.of(),
                List.of(new CatalogDtos.ProductVariantRequest(null, null, "M", "White", "#FFF", null, true, 5, 2))
        );

        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotPerformOwnerOnlyOperations() throws Exception {
        AdminDtos.StaffCreateRequest staffReq = new AdminDtos.StaffCreateRequest(
                "New Staff", "newstaff@quickkart.com", "Password@123", "9999999999", RoleName.STAFF
        );
        mockMvc.perform(post("/api/admin/staff")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerPriceUpdateReflectsImmediatelyOnCustomerWebsite() throws Exception {
        JsonNode created = createProductAsOwner("Oxford Classic Shirt", "999.00", 10);
        long productId = created.get("id").asLong();
        long variantId = created.get("variants").get(0).get("id").asLong();

        CatalogDtos.ProductRequest updateReq = new CatalogDtos.ProductRequest(
                "Oxford Classic Shirt", created.get("sku").asText(), testBrand.getId(), testCategory.getId(), Gender.MEN,
                "Premium combed cotton shirt", "100% Cotton",
                new BigDecimal("1499.00"), new BigDecimal("799.00"),
                ProductStatus.ACTIVE, true, true, false, true,
                List.of(new CatalogDtos.ProductImageRequest("https://example.com/shirt.jpg", null, true, 0, "Shirt")),
                List.of(new CatalogDtos.ProductVariantRequest(variantId, null, "M", "Black", "#111111", null, true, 10, 2))
        );

        mockMvc.perform(put("/api/admin/products/" + productId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellingPrice").value(799.0));

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellingPrice").value(799.0));
    }

    @Test
    void zeroStockMarksSoldOutAndDisablesPurchaseUntilRestocked() throws Exception {
        JsonNode created = createProductAsOwner("Limited Linen Shirt", "1299.00", 1);
        long productId = created.get("id").asLong();
        long variantId = created.get("variants").get(0).get("id").asLong();

        // Set stock to 0 via admin inventory endpoint
        mockMvc.perform(put("/api/admin/inventory/" + variantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminDtos.UpdateInventoryRequest(0, 5, "Zero out"))))
                .andExpect(status().isOk());

        // Customer storefront must show OUT_OF_STOCK and soldOut = true
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.soldOut").value(true));

        // Customer cannot add sold out item to cart
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartWishlistDtos.AddCartItemRequest(productId, variantId, 1))))
                .andExpect(status().isConflict());

        // Owner restocks to 5 units -> automatically ACTIVE again
        mockMvc.perform(put("/api/admin/inventory/" + variantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminDtos.UpdateInventoryRequest(5, 5, "Restocked"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.soldOut").value(false));

        // Customer can now add to cart
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartWishlistDtos.AddCartItemRequest(productId, variantId, 1))))
                .andExpect(status().isOk());
    }

    @Test
    void twoCustomersCannotPurchaseFinalUnitSimultaneously() throws Exception {
        JsonNode created = createProductAsOwner("Final Drop Jacket", "2999.00", 1);
        long productId = created.get("id").asLong();
        long variantId = created.get("variants").get(0).get("id").asLong();

        // Both customers add the 1 remaining unit to their own carts
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartWishlistDtos.AddCartItemRequest(productId, variantId, 1))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + secondCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartWishlistDtos.AddCartItemRequest(productId, variantId, 1))))
                .andExpect(status().isOk());

        OrderPaymentDtos.CheckoutRequest checkoutReq = new OrderPaymentDtos.CheckoutRequest(
                null,
                new AddressDtos.AddressRequest("Buyer", "9876543210", "12 MG Road", null, "Bengaluru", "Karnataka", "560001", "India", "HOME", true),
                PaymentMethod.COD, null, null
        );
        String body = objectMapper.writeValueAsString(checkoutReq);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        Callable<Integer> c1 = () -> {
            latch.await();
            return mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();
        };
        Callable<Integer> c2 = () -> {
            latch.await();
            return mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer " + secondCustomerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();
        };

        Future<Integer> f1 = pool.submit(c1);
        Future<Integer> f2 = pool.submit(c2);
        latch.countDown();
        int s1 = f1.get();
        int s2 = f2.get();
        pool.shutdown();

        // Exactly one succeeds (201) and the other fails with stock conflict (409)
        assertThat(List.of(s1, s2)).containsExactlyInAnyOrder(201, 409);

        // Product should now be SOLD OUT automatically
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.soldOut").value(true));
    }

    @Test
    void invalidPaymentReleasesReservedStockAndBackendEnforcesRealPriceAndCoupons() throws Exception {
        JsonNode created = createProductAsOwner("Merino Knit Polo", "1500.00", 2);
        long productId = created.get("id").asLong();
        long variantId = created.get("variants").get(0).get("id").asLong();

        Coupon coupon = new Coupon();
        coupon.setCode("FLAT200");
        coupon.setDiscountType(DiscountType.FIXED);
        coupon.setDiscountValue(new BigDecimal("200.00"));
        coupon.setMinPurchaseAmount(new BigDecimal("1000.00"));
        coupon.setExpiryDate(LocalDateTime.now().plusDays(10));
        coupon.setActive(true);
        couponRepository.save(coupon);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartWishlistDtos.AddCartItemRequest(productId, variantId, 1))))
                .andExpect(status().isOk());

        // Checkout with Razorpay (server computes 1500 - 200 coupon = 1300 total, ignoring any client manipulation)
        OrderPaymentDtos.CheckoutRequest checkoutReq = new OrderPaymentDtos.CheckoutRequest(
                null,
                new AddressDtos.AddressRequest("Buyer", "9876543210", "12 MG Road", null, "Bengaluru", "Karnataka", "560001", "India", "HOME", true),
                PaymentMethod.RAZORPAY, "FLAT200", null
        );
        MvcResult orderRes = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(1500.0))
                .andExpect(jsonPath("$.discountAmount").value(200.0))
                .andExpect(jsonPath("$.totalAmount").value(1300.0))
                .andReturn();

        JsonNode orderNode = objectMapper.readTree(orderRes.getResponse().getContentAsString());
        long orderId = orderNode.get("id").asLong();
        String gwOrderId = orderNode.get("paymentDetails").get("gatewayOrderId").asText();

        // Simulate failed payment verification
        OrderPaymentDtos.PaymentVerifyRequest failReq = new OrderPaymentDtos.PaymentVerifyRequest(
                orderId, gwOrderId, "pay_invalid", "bad_sig", false
        );
        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isBadRequest());

        // Verify order is CANCELLED/FAILED and all 2 stock units are still available
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvailableStock").value(2));
    }

    @Test
    void publicRegistrationNeverAssignsAdminOrOwnerRoles() throws Exception {
        AuthDtos.RegisterRequest reg = new AuthDtos.RegisterRequest("Hacker", "hacker@quickkart.com", "Pass@1234", "9999999999");
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode userNode = objectMapper.readTree(res.getResponse().getContentAsString()).get("user");
        assertThat(userNode.get("roles").toString()).contains("CUSTOMER");
        assertThat(userNode.get("roles").toString()).doesNotContain("OWNER", "ADMIN", "STAFF");
    }
}
