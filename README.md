# Quick kart — Full-Stack Fashion E-Commerce Platform

**Quick kart** is a production-oriented, database-driven fashion and apparel e-commerce platform built with **React + Vite + Tailwind CSS**, **Java Spring Boot 3**, **Spring Security + JWT**, **MySQL 8**, **Flyway**, **Cloudinary**, and **Razorpay**.

It is engineered so customers get a clean, responsive fashion shopping experience while a non-technical store owner can run the entire business—products, pricing, stock, categories, banners, coupons, orders, staff, and store settings—without touching source code.

---

## System Architecture

```text
                       +---------------------------+
                       |      VERCEL / NGINX       |
                       |   React + Vite + Tailwind |
                       +-------------+-------------+
                                     |
                        HTTPS REST API (JSON + JWT)
                                     |
                       +-------------v-------------+
                       |   SPRING BOOT 3 REST API  |
                       |  Spring Security + RBAC   |
                       +------+------+------+------+
                              |      |      |
             +----------------+      |      +----------------+
             v                       v                       v
   +-------------------+   +-------------------+   +-------------------+
   |    PRODUCT &      |   |  ORDER & PAYMENT  |   |    INVENTORY &    |
   |  CATALOG SERVICE  |   |      SERVICE      |   |   STOCK LOCKING   |
   +---------+---------+   +---------+---------+   +---------+---------+
             |                       |                       |
             +-----------------------+-----------------------+
                                     |
            +------------------------+------------------------+
            v                        v                        v
  +-------------------+    +-------------------+    +-------------------+
  |     MYSQL 8.0     |    |    CLOUDINARY     |    |     RAZORPAY      |
  | (26 Flyway Tables)|    |  (Image Storage)  |    | (Payment Gateway) |
  +-------------------+    +-------------------+    +-------------------+
```

### Architectural Pattern: Modular Monolith
- **Frontend (`frontend/`):** Single-page React application separating customer storefront views (`src/customer/`) and protected management views (`src/admin/`), communicating exclusively via REST APIs with automatic JWT refresh handling.
- **Backend (`backend/`):** Layered architecture (`Controller` -> `Service` -> `Repository` -> `Entity`) with Bean Validation, DTO isolation (JPA entities are never serialized directly), centralized exception mapping (`GlobalExceptionHandler`), and `BigDecimal` precision for all monetary calculations.
- **Database (`MySQL 8 + Flyway`):** Versioned schema migrations in `V1__init_schema.sql` enforcing foreign keys, unique constraints, and indexes across 26 normalized relational tables.

---

## Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Frontend Framework** | React 18, Vite 5, React Router DOM 6 |
| **Styling & Icons** | Tailwind CSS 3 (minimal monochrome editorial UI), Lucide React |
| **HTTP Client** | Axios (with JWT request interceptor & refresh-token retry) |
| **Backend Framework** | Java 21+, Spring Boot 3.5, Spring Web, Bean Validation |
| **Security & Auth** | Spring Security, JWT (`jjwt`), BCrypt (strength 12), Login Rate Limiter |
| **Database & ORM** | MySQL 8.0, Spring Data JPA, Hibernate 6, Flyway Migrations |
| **Image Storage** | Cloudinary Signed Upload API (with local disk `/uploads` fallback in dev) |
| **Payments** | Razorpay Orders & HMAC-SHA256 Signature/Webhook Verification (with Test Mode) |
| **API Docs & Monitoring** | Springdoc OpenAPI / Swagger UI (dev), Spring Boot Actuator (`/actuator/health`) |
| **Testing** | JUnit 5, Spring Boot Test, MockMvc, H2 (MySQL mode), Vitest, React Testing Library |
| **Deployment** | Vercel (Frontend), Docker & Docker Compose + Nginx (Backend & MySQL) |

---

## Project Folder Structure

```text
d:/E-COMMERCE/
├── .env.example                     # Root environment variable template
├── .gitignore                       # Excludes .env, node_modules, target, dist, uploads
├── docker-compose.yml               # Production multi-container stack (MySQL internal network)
├── start.ps1                        # Local Windows full-stack launcher
├── README.md
│
├── backend/
│   ├── Dockerfile                   # Multi-stage Maven + Eclipse Temurin 21 runtime image
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/quickkart/
│       │   ├── QuickKartApplication.java
│       │   ├── config/              # DataSeeder, OpenApiConfig, WebConfig
│       │   ├── controller/          # Auth, Catalog, CustomerAccount, OrderPayment, Review, Admin*
│       │   ├── dto/                 # Validated Java Record request/response DTOs
│       │   ├── entity/              # 26 JPA entities + enums
│       │   ├── exception/           # GlobalExceptionHandler + domain/concurrency exceptions
│       │   ├── mapper/              # EntityMapper (Entity <-> DTO conversion)
│       │   ├── repository/          # JPA repositories + JpaSpecificationExecutor + PESSIMISTIC_WRITE locks
│       │   ├── security/            # SecurityConfig, JwtService, JwtAuthenticationFilter, LoginRateLimitFilter
│       │   └── service/             # Business logic, inventory concurrency, Razorpay, Cloudinary, analytics
│       ├── main/resources/
│       │   ├── application.yml      # Base configuration (env-driven)
│       │   ├── application-dev.yml  # Local development profile
│       │   ├── application-prod.yml # Cloud production profile
│       │   └── db/migration/
│       │       └── V1__init_schema.sql
│       └── test/
│           ├── java/com/quickkart/QuickKartSecurityAndBusinessTests.java
│           └── resources/application-test.yml
│
└── frontend/
    ├── Dockerfile                   # Multi-stage Vite build + Nginx server
    ├── nginx.conf                   # SPA fallback + /api reverse proxy + security headers
    ├── vercel.json                  # Vercel SPA rewrites + security headers
    ├── .env.example                 # Frontend environment variable template
    ├── package.json
    └── src/
        ├── admin/                   # Store Owner & Staff Dashboard pages
        ├── components/              # Navbar, Footer, ProductCard, Pagination, ProtectedRoute, Toast
        ├── context/                 # AuthContext, CartContext, StoreContext
        ├── customer/                # Storefront, Product Details, Cart, Wishlist, Checkout, Orders, Auth
        ├── layouts/                 # CustomerLayout, AdminLayout
        ├── routes/                  # AppRoutes
        ├── services/                # Axios instance + authApi, catalogApi, customerApi, adminApi
        ├── test/                    # Vitest component & route-guard tests
        └── utils/                   # Currency (INR), dates, image URL resolver, order status labels
```

---

## Complete Feature Breakdown

### A. Customer Storefront Features
1. **Dynamic Home Page (`/`):**
   - Promotional announcement bar, category navigation with nested subcategories, and instant search bar with product/brand/category autocomplete suggestions.
   - Database-driven hero banner carousel, promo offer banners, active coupon badges, Shop by Category grid, Trending Now, New Arrivals, Best Sellers, Top Brands, Seasonal Collections, Verified Customer Reviews, Newsletter subscription, and dynamic store Footer.
2. **Product Listing & Server-Side Filtering (`/shop`):**
   - Server-side pagination, sorting (`recommended`, `newest`, `popularity`, `rating`, `price_asc`, `price_desc`), and filters for Gender, Category, Brand, Price Range, Size, Colour, Minimum Discount %, and In-Stock Availability.
3. **Product Details (`/product/:idOrSlug`):**
   - Multi-image gallery with thumbnail switcher, discount % calculation, colour & size selector displaying real-time per-variant stock, automatic `SOLD OUT` button lock when stock is `0`, delivery estimation, return policy summary, wishlist toggle, and verified-purchaser reviews.
4. **Shopping Bag & Wishlist (`/cart`, `/wishlist`):**
   - Persistent backend-synced bag supporting quantity updates, **in-bag size/variant switching**, coupon code application/removal, free-delivery threshold calculation, and moving items from wishlist directly to the bag.
5. **Customer Authentication & Profile (`/register`, `/login`, `/forgot-password`, `/profile`):**
   - Real customer registration (`CUSTOMER` role enforced server-side), BCrypt password hashing, JWT login + automatic refresh token rotation, password reset flow, profile editing, password change, and multiple saved shipping addresses with default selection.
6. **Checkout, Payments & Order Tracking (`/checkout`, `/orders`, `/orders/:id`):**
   - Select saved address or add a new address inline, choose Razorpay Online Payment or Cash on Delivery (controlled by Store Settings), and verify payments securely on the backend (including webhook handling).
   - Full order lifecycle tracking (`PLACED` -> `CONFIRMED` -> `PACKED` -> `SHIPPED` -> `OUT_FOR_DELIVERY` -> `DELIVERED`, plus `CANCELLED`, `RETURN_REQUESTED`, `RETURNED`).
   - Customers can cancel eligible pre-shipment orders (automatically restoring inventory) and submit return requests on delivered orders.

---

### B. Store Owner & Management Capabilities
The management interface is completely isolated from the public storefront and protected by backend role-based access control (`OWNER`, `ADMIN`, `STAFF`).

1. **Business Dashboard & Reports:**
   - Live metrics computed from MySQL: Total Sales, Today's & Monthly Revenue, Total/Pending/Delivered Orders, Catalog Product count, Customer count, Low-Stock & Out-of-Stock counts, daily revenue bar chart, top-selling products, category-wise sales breakdown, and **one-click CSV order export**.
2. **No-Code Product Management:**
   - Add, edit, and safely delete/archive products (preserving historical order integrity via soft deletion when orders exist).
   - Configure original MRP, selling price (auto-calculates discount %), SKU, gender, brand, category, merchandising flags (`Featured`, `New Arrival`, `Best Seller`, `Trending`), multiple images (file upload to Cloudinary or URL), and variant rows (size, colour, swatch hex, stock, low-stock threshold).
   - Price and catalog changes reflect immediately on the customer storefront.
3. **Automatic Inventory & Concurrency Control:**
   - Stock is tracked per size/colour variant.
   - **Zero-Stock Auto Sold Out:** When variant stock reaches `0` (via purchase or admin update) and no active variant remains in stock, the parent product automatically transitions to `OUT_OF_STOCK` and displays `SOLD OUT` with purchasing disabled.
   - **Auto Restock:** Restocking a variant above `0` automatically restores the product to `ACTIVE`.
   - **Oversell Prevention:** Checkout acquires `PESSIMISTIC_WRITE` database row locks (`SELECT ... FOR UPDATE`) and optimistic `@Version` checks so two concurrent buyers cannot purchase the final unit. Online payments reserve stock during checkout and release it automatically if payment fails or is cancelled.
   - Complete `inventory_transactions` audit log for every stock movement.
4. **Category & Brand Management:**
   - Create, edit, reorder, activate/deactivate, upload images/logos, and safely delete/archive categories and brands without breaking historical product references; updates reflect immediately in storefront navigation.
5. **Order, Return, Coupon & Banner Operations:**
   - Search and filter orders by order number, customer, date range, order status, and payment status; update fulfillment statuses, tracking numbers, and delivery dates.
   - Approve, reject, or refund return requests (refunding automatically restores inventory).
   - Create percentage or fixed coupons with minimum purchase rules, max discount caps, expiry dates, and usage limits.
   - Upload and schedule dynamic homepage Hero & Promo banners.
6. **Customer, Staff & Store Settings Management:**
   - View customer spend/order history and activate/deactivate customer accounts.
   - **Owner-Only Staff Management:** Create `ADMIN` or `STAFF` accounts, manage staff status, and review the system `audit_logs` trail.
   - **Store Settings:** Update store name, support contact, GSTIN, business address, top announcement banner, delivery charges, free-delivery threshold, return window, and toggle COD / Razorpay availability live.

---

## Database Schema (26 MySQL Tables)

Managed automatically via Flyway ([V1__init_schema.sql](backend/src/main/resources/db/migration/V1__init_schema.sql)):
- **Identity & Access:** `users`, `roles`, `user_roles`, `addresses`, `audit_logs`
- **Catalog & Inventory:** `categories`, `brands`, `products`, `product_images`, `product_variants`, `inventory`, `inventory_transactions`
- **Shopping & Engagement:** `carts`, `cart_items`, `wishlists`, `wishlist_items`, `reviews`, `newsletter_subscribers`
- **Orders & Payments:** `orders`, `order_items`, `payments`, `returns`, `coupons`, `coupon_usage`
- **Storefront Config:** `banners`, `store_settings`

---

## REST API Overview

| Domain | Endpoints | Access |
| :--- | :--- | :--- |
| **Auth** | `POST /api/auth/register`, `login`, `refresh`, `forgot-password`, `reset-password`, `change-password`; `GET/PUT /api/auth/me` | Public / Authenticated |
| **Storefront** | `GET /api/products`, `GET /api/products/{idOrSlug}`, `GET /api/products/suggestions`, `GET /api/categories`, `GET /api/brands`, `GET /api/banners`, `GET /api/store/settings`, `GET /api/coupons/public`, `POST /api/newsletter/subscribe` | Public |
| **Cart & Wishlist** | `GET /api/cart`, `POST/PUT/DELETE /api/cart/items`, `POST/DELETE /api/cart/coupon`, `GET/POST/DELETE /api/wishlist`, `POST /api/wishlist/{id}/move-to-cart` | Authenticated |
| **Addresses** | `GET/POST/PUT/DELETE /api/addresses` | Authenticated |
| **Orders & Pay** | `POST /api/orders`, `GET /api/orders/my-orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/cancel`, `POST /api/orders/{id}/return`, `POST /api/payments/verify`, `POST /api/payments/webhook/razorpay` | Authenticated / Webhook |
| **Reviews** | `GET /api/reviews/product/{id}`, `GET /api/reviews/featured`, `POST /api/reviews/product/{id}` | Public / Verified Buyer |
| **Management** | `/api/admin/products/**`, `/api/admin/categories/**`, `/api/admin/brands/**`, `/api/admin/upload`, `/api/admin/inventory/**`, `/api/admin/orders/**`, `/api/admin/returns/**`, `/api/admin/coupons/**`, `/api/admin/banners/**`, `/api/admin/reviews/**`, `/api/admin/customers/**`, `/api/admin/dashboard/**`, `/api/admin/reports/orders/export`, `/api/admin/settings` | `OWNER`, `ADMIN`, `STAFF` (`staff/**` restricted to `OWNER`) |

---

## Security Design

- **Zero Public Discovery of Management UI:** No management links or credential hints appear anywhere in customer navigation, footers, or login pages. Unauthorized or customer access to management routes redirects silently to `/`.
- **Backend RBAC Enforcement:** Every `/api/admin/**` route requires `OWNER`, `ADMIN`, or `STAFF` JWT roles, and staff creation/modification requires `OWNER`. Public `/api/auth/register` always assigns `CUSTOMER`.
- **Zero Client-Trust Pricing or Stock:** Order totals, coupon discounts, delivery fees, and inventory availability are strictly computed and re-validated on the backend inside database transactions.
- **Brute-Force Protection:** `LoginRateLimitFilter` throttles rapid requests to `/api/auth/login` and `/api/auth/forgot-password`.
- **Secrets Management:** Credentials, JWT secrets, database passwords, Cloudinary keys, and Razorpay keys are read exclusively from environment variables and never committed to Git.

---

## Running Locally

### Prerequisites
- Java 21+
- MySQL 8.0 running on `localhost:3306`
- Node.js 20+ & npm

### Setup & Launch
1. Copy `.env.example` to `.env` (`.env` is git-ignored) and set `DB_PASSWORD`, `JWT_SECRET`, and your private initial owner bootstrap variables (`APP_BOOTSTRAP_OWNER_EMAIL`, `APP_BOOTSTRAP_OWNER_PASSWORD`).
2. Start MySQL and run:
   - **Windows PowerShell:** `.\start.ps1`
   - **Manual:**
     - Backend: `cd backend` -> `mvn spring-boot:run`
     - Frontend: `cd frontend` -> `npm install` -> `npm run dev`
3. Open `http://localhost:5173`.

---

## Automated Tests

- **Backend (`cd backend && mvn test`):**
  - Verifies customer `403` isolation from management/product APIs, staff `403` isolation from owner-only APIs, live price reflection, zero-stock `OUT_OF_STOCK` + restocking reactivation, multi-threaded final-unit concurrency protection (`[201, 409]`), failed payment stock release, and public registration role safety.
- **Frontend (`cd frontend && npm test`):**
  - Verifies `SOLD OUT` product badge rendering and protected route redirection for unauthorized/customer users.

---

## Deployment Guide (GitHub + Vercel + Cloud Backend)

### 1. Push to GitHub
Ensure `.env` is ignored (`git status` must not show `.env`, `uploads/`, `target/`, or `node_modules/`), then commit and push to GitHub.

### 2. Deploy Frontend to Vercel
1. Import the GitHub repository in **Vercel**.
2. Set **Root Directory** to `frontend`.
3. Vercel detects Vite automatically (`npm run build`, output `dist`, SPA routing configured in [frontend/vercel.json](frontend/vercel.json)).
4. Add Environment Variable in Vercel:
   - `VITE_API_BASE_URL` = `https://<your-backend-api-domain>/api`
   - `VITE_RAZORPAY_KEY_ID` = *(optional Razorpay key ID)*

### 3. Deploy Backend & MySQL on Cloud Server
1. Run `backend/` via [backend/Dockerfile](backend/Dockerfile) or [docker-compose.yml](docker-compose.yml) on Railway, Render, AWS EC2, DigitalOcean, or any Docker host with a managed MySQL 8 database.
2. Configure production environment variables:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
   - `JWT_SECRET` (64+ random characters)
   - `CORS_ALLOWED_ORIGINS=https://<your-vercel-app>.vercel.app`
   - `APP_BOOTSTRAP_OWNER_NAME`, `APP_BOOTSTRAP_OWNER_EMAIL`, `APP_BOOTSTRAP_OWNER_PASSWORD` (used only on initial boot when no `OWNER` exists)
   - `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
   - `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`
   - `SWAGGER_ENABLED=false`
3. Terminate TLS/HTTPS via reverse proxy/load balancer, keep MySQL port `3306` private (internal network only), and schedule automated encrypted `mysqldump` backups.
