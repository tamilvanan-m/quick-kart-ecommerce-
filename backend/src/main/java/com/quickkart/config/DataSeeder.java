package com.quickkart.config;

import com.quickkart.entity.*;
import com.quickkart.entity.enums.*;
import com.quickkart.repository.*;
import com.quickkart.service.StoreSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final AddressRepository addressRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BannerRepository bannerRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final StoreSettingService storeSettingService;
    private final PasswordEncoder passwordEncoder;

    private final String ownerName;
    private final String ownerEmail;
    private final String ownerPassword;

    public DataSeeder(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CartRepository cartRepository,
            WishlistRepository wishlistRepository,
            AddressRepository addressRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            ProductRepository productRepository,
            BannerRepository bannerRepository,
            CouponRepository couponRepository,
            OrderRepository orderRepository,
            ReviewRepository reviewRepository,
            StoreSettingService storeSettingService,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.owner-name}") String ownerName,
            @Value("${app.bootstrap.owner-email}") String ownerEmail,
            @Value("${app.bootstrap.owner-password}") String ownerPassword) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.wishlistRepository = wishlistRepository;
        this.addressRepository = addressRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.bannerRepository = bannerRepository;
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.storeSettingService = storeSettingService;
        this.passwordEncoder = passwordEncoder;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.ownerPassword = ownerPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedOwnerAccount();
        storeSettingService.getOrCreateEntity();

        if (categoryRepository.count() == 0) {
            Map<String, Category> categories = seedCategories();
            Map<String, Brand> brands = seedBrands();
            List<Product> products = seedProducts(categories, brands);
            seedBanners();
            seedCoupons();
            seedSampleCustomerAndOrders(products);
            log.info("Quick kart initial catalog, banners, coupons, and bootstrap data seeded.");
        }
    }

    private void seedRoles() {
        for (RoleName rn : RoleName.values()) {
            roleRepository.findByName(rn).orElseGet(() ->
                    roleRepository.save(new Role(rn, rn.name() + " access role")));
        }
    }

    private void seedOwnerAccount() {
        if (!userRepository.existsByRoles_Name(RoleName.OWNER)) {
            if (ownerEmail == null || ownerEmail.isBlank() || ownerPassword == null || ownerPassword.isBlank()) {
                log.warn("Initial OWNER account not bootstrapped: configure APP_BOOTSTRAP_OWNER_EMAIL and APP_BOOTSTRAP_OWNER_PASSWORD environment variables.");
                return;
            }
            Role ownerRole = roleRepository.findByName(RoleName.OWNER).orElseThrow();
            User owner = new User();
            owner.setFullName(ownerName);
            owner.setEmail(ownerEmail.trim().toLowerCase());
            owner.setPasswordHash(passwordEncoder.encode(ownerPassword));
            owner.setPhone("+91 98000 00001");
            owner.setActive(true);
            owner.setEmailVerified(true);
            owner.setRoles(Set.of(ownerRole));
            userRepository.save(owner);
            log.info("Bootstrapped initial OWNER account from environment configuration.");
        }
    }

    private Map<String, Category> seedCategories() {
        Map<String, Category> map = new LinkedHashMap<>();

        Category men = createCat("Men", "men", "MEN", null, 1,
                "https://images.unsplash.com/photo-1516257984-b1b4d707412e?auto=format&fit=crop&w=900&q=80");
        Category women = createCat("Women", "women", "WOMEN", null, 2,
                "https://images.unsplash.com/photo-1483985988635-76728e1935b8?auto=format&fit=crop&w=900&q=80");
        Category kids = createCat("Kids", "kids", "KIDS", null, 3,
                "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?auto=format&fit=crop&w=900&q=80");
        Category footwear = createCat("Footwear", "footwear", "FOOTWEAR", null, 4,
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80");
        Category accessories = createCat("Accessories", "accessories", "ACCESSORIES", null, 5,
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80");
        Category beauty = createCat("Beauty", "beauty", "BEAUTY", null, 6,
                "https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=900&q=80");

        map.put("men", men);
        map.put("women", women);
        map.put("kids", kids);
        map.put("footwear", footwear);
        map.put("accessories", accessories);
        map.put("beauty", beauty);

        // MEN subcategories
        map.put("men-t-shirts", createCat("T-Shirts", "men-t-shirts", "MEN", men, 1,
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=700&q=80"));
        map.put("men-shirts", createCat("Shirts", "men-shirts", "MEN", men, 2,
                "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?auto=format&fit=crop&w=700&q=80"));
        map.put("men-jeans", createCat("Jeans", "men-jeans", "MEN", men, 3,
                "https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=700&q=80"));
        map.put("men-trousers", createCat("Trousers", "men-trousers", "MEN", men, 4,
                "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?auto=format&fit=crop&w=700&q=80"));
        map.put("men-jackets", createCat("Jackets", "men-jackets", "MEN", men, 5,
                "https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=700&q=80"));
        map.put("men-hoodies", createCat("Hoodies", "men-hoodies", "MEN", men, 6,
                "https://images.unsplash.com/photo-1556821840-3a63f95609a7?auto=format&fit=crop&w=700&q=80"));
        map.put("men-ethnic", createCat("Ethnic Wear", "men-ethnic-wear", "MEN", men, 7,
                "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=700&q=80"));

        // WOMEN subcategories
        map.put("women-tops", createCat("Tops", "women-tops", "WOMEN", women, 1,
                "https://images.unsplash.com/photo-1564257631407-4deb1f99d992?auto=format&fit=crop&w=700&q=80"));
        map.put("women-dresses", createCat("Dresses", "women-dresses", "WOMEN", women, 2,
                "https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&w=700&q=80"));
        map.put("women-kurtas", createCat("Kurtas", "women-kurtas", "WOMEN", women, 3,
                "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&w=700&q=80"));
        map.put("women-sarees", createCat("Sarees", "women-sarees", "WOMEN", women, 4,
                "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=700&q=80"));
        map.put("women-jeans", createCat("Jeans", "women-jeans", "WOMEN", women, 5,
                "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=700&q=80"));
        map.put("women-trousers", createCat("Trousers", "women-trousers", "WOMEN", women, 6,
                "https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=700&q=80"));
        map.put("women-ethnic", createCat("Ethnic Wear", "women-ethnic-wear", "WOMEN", women, 7,
                "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&w=700&q=80"));

        // KIDS subcategories
        map.put("kids-boys", createCat("Boys", "kids-boys", "KIDS", kids, 1, null));
        map.put("kids-girls", createCat("Girls", "kids-girls", "KIDS", kids, 2, null));
        map.put("kids-infants", createCat("Infants", "kids-infants", "KIDS", kids, 3, null));

        // FOOTWEAR subcategories
        map.put("fw-sneakers", createCat("Sneakers", "sneakers", "FOOTWEAR", footwear, 1, null));
        map.put("fw-casual", createCat("Casual Shoes", "casual-shoes", "FOOTWEAR", footwear, 2, null));
        map.put("fw-formal", createCat("Formal Shoes", "formal-shoes", "FOOTWEAR", footwear, 3, null));
        map.put("fw-sandals", createCat("Sandals", "sandals", "FOOTWEAR", footwear, 4, null));

        // ACCESSORIES subcategories
        map.put("acc-watches", createCat("Watches", "watches", "ACCESSORIES", accessories, 1, null));
        map.put("acc-bags", createCat("Bags", "bags", "ACCESSORIES", accessories, 2, null));
        map.put("acc-belts", createCat("Belts", "belts", "ACCESSORIES", accessories, 3, null));
        map.put("acc-sunglasses", createCat("Sunglasses", "sunglasses", "ACCESSORIES", accessories, 4, null));

        // BEAUTY subcategories
        map.put("beauty-skincare", createCat("Skincare", "skincare", "BEAUTY", beauty, 1, null));
        map.put("beauty-fragrances", createCat("Fragrances", "fragrances", "BEAUTY", beauty, 2, null));

        return map;
    }

    private Category createCat(String name, String slug, String scope, Category parent, int order, String imageUrl) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        c.setGenderScope(scope);
        c.setParent(parent);
        c.setDisplayOrder(order);
        c.setImageUrl(imageUrl);
        c.setActive(true);
        return categoryRepository.save(c);
    }

    private Map<String, Brand> seedBrands() {
        Map<String, Brand> map = new LinkedHashMap<>();
        String[][] items = {
                {"Kora Studio", "kora-studio", "Contemporary minimalist essentials crafted in breathable organic fabrics."},
                {"Northbound", "northbound", "Tailored urban outerwear, denim, and structured menswear."},
                {"Aurelia Loom", "aurelia-loom", "Modern Indian occasion wear and artisanal everyday kurtas."},
                {"Verve Athletics", "verve-athletics", "Performance-grade footwear and clean athleisure silhouettes."},
                {"Maison Blanc", "maison-blanc", "Elevated womenswear staples and refined evening dresses."},
                {"Urban Craft", "urban-craft", "Precision leather goods, timepieces, and signature fragrances."}
        };
        for (String[] b : items) {
            Brand brand = new Brand();
            brand.setName(b[0]);
            brand.setSlug(b[1]);
            brand.setDescription(b[2]);
            brand.setActive(true);
            map.put(b[1], brandRepository.save(brand));
        }
        return map;
    }

    private List<Product> seedProducts(Map<String, Category> cats, Map<String, Brand> brands) {
        List<Product> list = new ArrayList<>();

        list.add(createProduct(
                "Heavyweight Supima Cotton Oversized Tee", "heavyweight-supima-oversized-tee", "QK-TSH-101",
                brands.get("kora-studio"), cats.get("men-t-shirts"), Gender.MEN,
                "Crafted from 240 GSM combed Supima cotton with a clean drop-shoulder drape and ribbed crew neckline.",
                "100% Supima Cotton • Machine wash cold inside out",
                "1499.00", "999.00", true, true, true, true,
                List.of(
                        "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80",
                        "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("S", 8, "M", 15, "L", 12, "XL", 6), "Jet Black", "#111111"
        ));

        list.add(createProduct(
                "Tailored Oxford Button-Down Shirt", "tailored-oxford-button-down-shirt", "QK-SH-102",
                brands.get("northbound"), cats.get("men-shirts"), Gender.MEN,
                "Crisp textured Oxford weave with mother-of-pearl buttons and a clean regular-tailored silhouette.",
                "100% Combed Cotton • Warm iron",
                "2499.00", "1799.00", true, false, true, true,
                List.of(
                        "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?auto=format&fit=crop&w=900&q=80",
                        "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("S", 6, "M", 14, "L", 10, "XL", 4), "Crisp White", "#F8FAFC"
        ));

        list.add(createProduct(
                "Selvedge Straight-Fit Denim Jeans", "selvedge-straight-fit-denim-jeans", "QK-JN-103",
                brands.get("northbound"), cats.get("men-jeans"), Gender.MEN,
                "Mid-rise straight fit jeans in 12.5 oz dark indigo stretch selvedge denim for effortless daily wear.",
                "98% Cotton, 2% Elastane • Wash separately",
                "3299.00", "2299.00", false, true, false, true,
                List.of(
                        "https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("30", 7, "32", 12, "34", 9, "36", 5), "Indigo Wash", "#1E293B"
        ));

        list.add(createProduct(
                "Structured Wool-Blend Harrington Jacket", "structured-wool-harrington-jacket", "QK-JK-104",
                brands.get("northbound"), cats.get("men-jackets"), Gender.MEN,
                "Minimalist zip-front Harrington jacket with brushed twill lining and concealed welt pockets.",
                "65% Wool Blend, 35% Recycled Polyester • Dry clean only",
                "5999.00", "4199.00", true, true, false, true,
                List.of(
                        "https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("M", 5, "L", 4, "XL", 3), "Charcoal", "#27272A"
        ));

        list.add(createProduct(
                "Fleece Zip-Free Minimalist Hoodie", "fleece-minimalist-hoodie", "QK-HD-105",
                brands.get("kora-studio"), cats.get("men-hoodies"), Gender.MEN,
                "Loopback French terry hoodie designed with clean tonal seams and double-lined structured hood.",
                "100% Organic Cotton • Gentle machine wash",
                "2999.00", "2099.00", false, true, true, false,
                List.of(
                        "https://images.unsplash.com/photo-1556821840-3a63f95609a7?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("S", 0, "M", 0, "L", 0, "XL", 0), "Ash Grey", "#9CA3AF"
        ));

        list.add(createProduct(
                "Pleated Satin Midi Wrap Dress", "pleated-satin-midi-wrap-dress", "QK-DR-201",
                brands.get("maison-blanc"), cats.get("women-dresses"), Gender.WOMEN,
                "Fluid silhouette crafted in matte satin with subtle waist tie and soft pleated A-line skirt.",
                "100% Recycled Matte Satin • Hand wash cold",
                "3999.00", "2799.00", true, true, true, true,
                List.of(
                        "https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&w=900&q=80",
                        "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("XS", 5, "S", 11, "M", 12, "L", 7), "Espresso Black", "#1C1917"
        ));

        list.add(createProduct(
                "Linen Blend Relaxed Collar Blouse", "linen-blend-relaxed-blouse", "QK-TP-202",
                brands.get("maison-blanc"), cats.get("women-tops"), Gender.WOMEN,
                "Breathable European linen-cotton blend top tailored with clean notched lapels and cuffed sleeves.",
                "55% Linen, 45% Cotton • Cool iron",
                "2199.00", "1549.00", true, true, false, true,
                List.of(
                        "https://images.unsplash.com/photo-1564257631407-4deb1f99d992?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("XS", 6, "S", 10, "M", 8, "L", 5), "Ivory", "#FAF8F5"
        ));

        list.add(createProduct(
                "Handloom Chanderi Straight Kurta Set", "handloom-chanderi-kurta-set", "QK-KR-203",
                brands.get("aurelia-loom"), cats.get("women-kurtas"), Gender.WOMEN,
                "Elegant straight-cut kurta finished with subtle zari edge detailing and matched tapered trousers.",
                "Chanderi Silk Blend • Dry clean recommended",
                "4499.00", "3199.00", true, false, true, true,
                List.of(
                        "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("S", 7, "M", 9, "L", 8, "XL", 4), "Warm Sand", "#D6C5B3"
        ));

        list.add(createProduct(
                "High-Waist Wide-Leg Tailored Trousers", "high-waist-wide-leg-trousers", "QK-TR-204",
                brands.get("maison-blanc"), cats.get("women-trousers"), Gender.WOMEN,
                "Architectural pleated front trousers engineered for crisp office-to-evening drape.",
                "78% Viscose, 19% Polyamide, 3% Elastane • Machine wash cold",
                "2999.00", "2199.00", false, true, true, false,
                List.of(
                        "https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("26", 4, "28", 11, "30", 10, "32", 6), "Obsidian", "#0F172A"
        ));

        list.add(createProduct(
                "Junior Organic Cotton Chino & Tee Set", "junior-organic-chino-tee-set", "QK-KD-301",
                brands.get("kora-studio"), cats.get("kids-boys"), Gender.KIDS,
                "Soft breathable two-piece set built for active all-day comfort with adjustable waistband.",
                "100% Organic Cotton • Gentle machine wash",
                "1699.00", "1199.00", false, true, false, true,
                List.of(
                        "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("4-5Y", 8, "6-7Y", 10, "8-9Y", 9), "Olive Stone", "#4B5563"
        ));

        list.add(createProduct(
                "Monochrome Leather Court Sneakers", "monochrome-leather-court-sneakers", "QK-FW-401",
                brands.get("verve-athletics"), cats.get("fw-sneakers"), Gender.UNISEX,
                "Full-grain pebble leather upper resting on a cushioned vulcanized cupsole for sleek everyday styling.",
                "Full-Grain Leather Upper, Rubber Outsole • Wipe with damp cloth",
                "4999.00", "3499.00", true, true, true, true,
                List.of(
                        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80",
                        "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("UK 7", 6, "UK 8", 12, "UK 9", 10, "UK 10", 5), "White / Onyx", "#E5E7EB"
        ));

        list.add(createProduct(
                "Minimalist Chrono Steel Wristwatch", "minimalist-chrono-steel-wristwatch", "QK-AC-501",
                brands.get("urban-craft"), cats.get("acc-watches"), Gender.UNISEX,
                "40mm brushed stainless steel case with sapphire-coated mineral glass and matte dial.",
                "316L Stainless Steel • 5 ATM Water Resistant",
                "6499.00", "4899.00", true, false, true, true,
                List.of(
                        "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("ONE SIZE", 14), "Brushed Silver", "#CBD5E1"
        ));

        list.add(createProduct(
                "Structured Italian Grain Tote Bag", "structured-grain-tote-bag", "QK-AC-502",
                brands.get("urban-craft"), cats.get("acc-bags"), Gender.WOMEN,
                "Roomy everyday work-to-weekend tote with padded 14-inch laptop sleeve and magnetic closure.",
                "100% Vegan Pebble Leather • Spot clean only",
                "3499.00", "2599.00", false, true, true, false,
                List.of(
                        "https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("ONE SIZE", 11), "Noir Black", "#111111"
        ));

        list.add(createProduct(
                "Botanical Barrier Hydrating Serum 50ml", "botanical-barrier-hydrating-serum", "QK-BT-601",
                brands.get("maison-blanc"), cats.get("beauty-skincare"), Gender.UNISEX,
                "Lightweight hyaluronic and squalane daily serum formulated for balanced, radiant skin.",
                "Dermatologically Tested • Paraben & Fragrance Free",
                "1899.00", "1499.00", false, true, true, true,
                List.of(
                        "https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of("50ML", 18), "Clear Glass", "#F3F4F6"
        ));

        return list;
    }

    private Product createProduct(
            String name, String slug, String sku,
            Brand brand, Category category, Gender gender,
            String desc, String materialCare,
            String originalPriceStr, String sellingPriceStr,
            boolean featured, boolean newArrival, boolean bestSeller, boolean trending,
            List<String> imageUrls, Map<String, Integer> sizeStocks, String color, String colorHex) {

        BigDecimal orig = new BigDecimal(originalPriceStr);
        BigDecimal sell = new BigDecimal(sellingPriceStr);
        int discountPct = orig.subtract(sell).multiply(BigDecimal.valueOf(100))
                .divide(orig, 0, RoundingMode.HALF_UP).intValue();

        int totalStock = sizeStocks.values().stream().mapToInt(Integer::intValue).sum();

        Product p = new Product();
        p.setName(name);
        p.setSlug(slug);
        p.setSku(sku);
        p.setBrand(brand);
        p.setCategory(category);
        p.setGender(gender);
        p.setDescription(desc);
        p.setMaterialCare(materialCare);
        p.setOriginalPrice(orig);
        p.setSellingPrice(sell);
        p.setDiscountPercentage(discountPct);
        p.setStatus(totalStock > 0 ? ProductStatus.ACTIVE : ProductStatus.OUT_OF_STOCK);
        p.setFeatured(featured);
        p.setNewArrival(newArrival);
        p.setBestSeller(bestSeller);
        p.setTrending(trending);
        p.setTotalSales((long) (15 + new Random(sku.hashCode()).nextInt(90)));

        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage img = new ProductImage();
            img.setProduct(p);
            img.setImageUrl(imageUrls.get(i));
            img.setIsPrimary(i == 0);
            img.setDisplayOrder(i);
            img.setAltText(name);
            p.getImages().add(img);
        }

        int idx = 1;
        for (Map.Entry<String, Integer> entry : sizeStocks.entrySet()) {
            ProductVariant v = new ProductVariant();
            v.setProduct(p);
            v.setSku((sku + "-" + entry.getKey() + "-" + idx++).replaceAll("[^A-Z0-9-]", ""));
            v.setSize(entry.getKey());
            v.setColor(color);
            v.setColorHex(colorHex);
            v.setActive(true);

            Inventory inv = new Inventory();
            inv.setVariant(v);
            inv.setQuantity(entry.getValue());
            inv.setReservedQuantity(0);
            inv.setLowStockThreshold(5);
            v.setInventory(inv);
            p.getVariants().add(v);
        }

        return productRepository.save(p);
    }

    private void seedBanners() {
        createBanner(
                "THE AUTUMN TAILORED EDIT",
                "SEASONAL COLLECTION 2026",
                "Sharp monochrome tailoring, heavyweight Supima cotton essentials, and effortless silhouettes crafted for the modern wardrobe.",
                "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=1800&q=85",
                "/shop",
                "EXPLORE COLLECTION",
                BannerPlacement.HERO,
                1
        );
        createBanner(
                "ARCHITECTURAL MENSWEAR",
                "NEW ARRIVALS",
                "Crisp Oxford shirting, raw selvedge denim, and structured wool outerwear.",
                "https://images.unsplash.com/photo-1441984904996-e0b6ba687e04?auto=format&fit=crop&w=1800&q=85",
                "/shop?gender=MEN",
                "SHOP MEN",
                BannerPlacement.HERO,
                2
        );
        createBanner(
                "FLAT 10% OFF FIRST ORDER",
                "LIMITED OFFER • USE CODE WELCOME10",
                "Complimentary express shipping on all orders above ₹999.",
                "https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=1600&q=80",
                "/shop?sort=newest",
                "CLAIM OFFER",
                BannerPlacement.PROMO,
                1
        );
    }

    private void createBanner(String title, String subtitle, String desc, String imgUrl, String linkUrl, String cta, BannerPlacement placement, int order) {
        Banner b = new Banner();
        b.setTitle(title);
        b.setSubtitle(subtitle);
        b.setDescription(desc);
        b.setImageUrl(imgUrl);
        b.setLinkUrl(linkUrl);
        b.setCtaText(cta);
        b.setPlacement(placement);
        b.setDisplayOrder(order);
        b.setActive(true);
        bannerRepository.save(b);
    }

    private void seedCoupons() {
        Coupon c1 = new Coupon();
        c1.setCode("WELCOME10");
        c1.setDescription("10% off on orders above ₹799 (max discount ₹500)");
        c1.setDiscountType(DiscountType.PERCENTAGE);
        c1.setDiscountValue(new BigDecimal("10.00"));
        c1.setMinPurchaseAmount(new BigDecimal("799.00"));
        c1.setMaxDiscountAmount(new BigDecimal("500.00"));
        c1.setExpiryDate(LocalDateTime.now().plusYears(1));
        c1.setUsageLimit(1000);
        c1.setActive(true);
        couponRepository.save(c1);

        Coupon c2 = new Coupon();
        c2.setCode("QUICK200");
        c2.setDescription("Flat ₹200 off on orders above ₹1,999");
        c2.setDiscountType(DiscountType.FIXED);
        c2.setDiscountValue(new BigDecimal("200.00"));
        c2.setMinPurchaseAmount(new BigDecimal("1999.00"));
        c2.setExpiryDate(LocalDateTime.now().plusYears(1));
        c2.setUsageLimit(500);
        c2.setActive(true);
        couponRepository.save(c2);
    }

    private void seedSampleCustomerAndOrders(List<Product> products) {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER).orElseThrow();
        User customer = new User();
        customer.setFullName("Ananya Sharma");
        customer.setEmail("reviewer-sample@quickkart.local");
        customer.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        customer.setPhone("+91 98450 11223");
        customer.setActive(true);
        customer.setEmailVerified(true);
        customer.setRoles(Set.of(customerRole));
        User savedCustomer = userRepository.save(customer);

        Cart cart = new Cart();
        cart.setUser(savedCustomer);
        cartRepository.save(cart);

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(savedCustomer);
        wishlistRepository.save(wishlist);

        Address addr = new Address();
        addr.setUser(savedCustomer);
        addr.setFullName("Ananya Sharma");
        addr.setPhone("+91 98450 11223");
        addr.setAddressLine1("18 Indiranagar 100 Feet Road");
        addr.setAddressLine2("Near CMH Junction");
        addr.setCity("Bengaluru");
        addr.setState("Karnataka");
        addr.setPostalCode("560038");
        addr.setCountry("India");
        addr.setAddressType("HOME");
        addr.setIsDefault(true);
        addressRepository.save(addr);

        if (products.size() >= 3) {
            Order order = createSampleOrder(savedCustomer, products.get(0), OrderStatus.DELIVERED, PaymentMethod.RAZORPAY, PaymentStatus.PAID, "QK-20261101-8421", 3);
            createSampleOrder(savedCustomer, products.get(5), OrderStatus.SHIPPED, PaymentMethod.COD, PaymentStatus.PENDING, "QK-20261108-9104", 1);

            Review r1 = new Review();
            r1.setProduct(products.get(0));
            r1.setUser(savedCustomer);
            r1.setOrder(order);
            r1.setRating(5);
            r1.setTitle("Substantial fabric weight and clean drape");
            r1.setComment("The 240 GSM Supima cotton feels genuinely structured without being stiff. Holds shape after washing.");
            r1.setVerifiedPurchase(true);
            r1.setApproved(true);
            reviewRepository.save(r1);

            Product p0 = products.get(0);
            p0.setRatingAvg(new BigDecimal("5.00"));
            p0.setReviewCount(1);
            productRepository.save(p0);
        }
    }

    private Order createSampleOrder(User user, Product product, OrderStatus orderStatus, PaymentMethod method, PaymentStatus paymentStatus, String orderNumber, int daysAgo) {
        ProductVariant variant = product.getVariants().get(0);
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setShippingFullName(user.getFullName());
        order.setShippingPhone(user.getPhone());
        order.setShippingAddressLine1("18 Indiranagar 100 Feet Road");
        order.setShippingCity("Bengaluru");
        order.setShippingState("Karnataka");
        order.setShippingPostalCode("560038");
        order.setShippingCountry("India");
        order.setSubtotal(product.getSellingPrice());
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setDeliveryCharge(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(product.getSellingPrice());
        order.setPaymentMethod(method);
        order.setPaymentStatus(paymentStatus);
        order.setOrderStatus(orderStatus);
        order.setEstimatedDeliveryDate(LocalDate.now().plusDays(3));
        order.setTrackingNumber("QKTRACK" + orderNumber.substring(orderNumber.length() - 4));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setVariant(variant);
        item.setProductName(product.getName());
        item.setBrandName(product.getBrand().getName());
        item.setSize(variant.getSize());
        item.setColor(variant.getColor());
        item.setSku(variant.getSku());
        item.setImageUrl(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());
        item.setUnitPrice(product.getSellingPrice());
        item.setOriginalPrice(product.getOriginalPrice());
        item.setQuantity(1);
        item.setTotalPrice(product.getSellingPrice());
        order.getItems().add(item);

        Order saved = orderRepository.save(order);
        saved.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        return orderRepository.save(saved);
    }
}
