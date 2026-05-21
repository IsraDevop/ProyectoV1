# Claude Code — Yala Backend (CS2031 DBP UTEC 2026-1)

## Mission
Build the **complete, production-ready backend** of **Yala** — a collectibles auction marketplace (Pokémon TCG, Funko Pops, comics) for Latin America. This is a graded university project; every rubric item must be fully implemented. **Maximum score is the goal.**

---

## Non-negotiable Rules
- **All code in English**: class names, fields, methods, REST paths, variables. Zero Spanish in code.
- **Root package**: `com.yala`
- **All REST paths** start with `/api/v1/`
- **All test method names** follow BDD: `shouldXxxWhenYyy()`
- **No logic in controllers** — delegate everything to services
- `passwordHash`, `dni`, and `cci` must **never** appear in any public response DTO
- Inject dependencies **by constructor only** — never `@Autowired` on fields, never `new` for Spring beans
- `@Transactional` on service methods only, never on controllers or repositories
- Paginate **every** list endpoint with `Pageable` + `Page<XxxResponse>`

---

## Tech Stack (mandatory)
- Java 17 + Spring Boot 3.x + Maven
- PostgreSQL + JPA/Hibernate (all relations `FetchType.LAZY`)
- Spring Security + JWT (stateless) with refresh tokens
- Spring Events (`@EventListener` + `@Async`) + `@Scheduled`
- WebSockets with STOMP (`spring-boot-starter-websocket`)
- Email: Resend API + Thymeleaf HTML templates
- Image storage: Supabase Storage (REST client)
- Payments: Stripe (PaymentIntent, sandbox mode)
- Tests: JUnit 5 + Mockito + `@DataJpaTest` + `@WebMvcTest` + Testcontainers (PostgreSQL)
- API Docs: SpringDoc OpenAPI (`@Tag` on every controller, `@Operation` on every endpoint)
- Lombok (`@Builder` + `@Getter` + `@Setter` on entities; records for DTOs)
- `@Version` on `Auction` entity for optimistic locking (concurrent bid protection)

---

## Domain Model — English names everywhere

| Spanish      | Class prefix | DB table      |
|--------------|--------------|---------------|
| usuario      | User         | users         |
| publicación  | Listing      | listings      |
| subasta      | Auction      | auctions      |
| puja         | Bid          | bids          |
| orden        | Order        | orders        |
| pago         | Payment      | payments      |
| reseña       | Review       | reviews       |
| notificación | Notification | notifications |
| categoría    | Category     | categories    |
| imagen       | Image        | images        |
| etiqueta     | Tag          | tags          |

---

## Entities (11 total — full JPA annotations required on all)

Apply to **every** entity: `@NotNull` on required fields, `@Email` on emails, `@Size` on strings, `@Min`/`@Max` on numbers, `@Column(unique=true)` on unique fields.

```
User          → id, name(@Size(min=2,max=100)), email(@Email @Column(unique=true)), passwordHash,
                avatarUrl, dni(@Column(unique=true), nullable), dniVerified(Boolean, default=false),
                cci(String, encrypted, nullable), reputation(Float, default=0.0),
                isVerifiedSeller(Boolean, default=false), role(enum USER/SELLER/ADMIN),
                failedPayments(Integer, default=0), createdAt
               @OneToMany → listings, bids, ordersAsBuyer, ordersAsSeller,
                            notifications, reviewsWritten, reviewsReceived
               @Version Long version   ← NOT on User; see Auction below

Category      → id, name(@Column(unique=true) @Size(max=80)), description
               @OneToMany → listings

Listing       → id, title(@Size(min=10,max=200)), description(@Size(max=2000)),
                mode(enum FIXED/AUCTION), fixedPrice(@Min(0)), condition(String),
                status(enum ACTIVE/SOLD/CANCELLED/SUSPENDED), maxActiveListings=20,
                createdAt
               @ManyToOne → seller(User), category(Category)
               @OneToMany → images(cascade=ALL), orders
               @ManyToMany → tags (@JoinTable listing_tags)
               @OneToOne  → auction(cascade=ALL)

Image         → id, url, sortOrder
               @ManyToOne → listing

Tag           → id, name(@Column(unique=true))
               @ManyToMany(mappedBy="tags") → listings

Auction       → id, startingPrice(@Min(0)), currentPrice(@Min(0)), startedAt,
                endsAt(@NotNull), status(enum SCHEDULED/ACTIVE/CLOSED/CANCELLED/PAID),
                duration(Integer: 1/3/5/7 days), scheduledStartAt(nullable)
               @Version Long version   ← REQUIRED for optimistic locking
               @OneToOne  → listing
               @ManyToOne → winner(User, nullable)
               @OneToMany → bids

Bid           → id, amount(@Min(0)), placedAt
               @ManyToOne → auction, bidder(User)

Order         → id, amount(@Min(0)), status(enum PENDING/CONFIRMED/CANCELLED/IN_TRANSIT/COMPLETED/DISPUTED),
                commissionAmount(Float), netSellerAmount(Float), trackingNumber(String, nullable),
                paymentDeadline(LocalDateTime), createdAt
               @ManyToOne → listing, buyer(User), seller(User)
               @OneToMany → payments(cascade=ALL), reviews

Payment       → id, gateway(String), externalReference(String), amount(@Min(0)),
                status(enum PENDING/SUCCESS/FAILED/REFUNDED), attemptedAt
               @ManyToOne → order

Review        → id, rating(@Min(1) @Max(5)), comment(@Size(max=1000)), createdAt
               @ManyToOne → order, author(User), recipient(User)

Notification  → id, type(enum BID_OUTBID/AUCTION_WON/SALE_CONFIRMED/NEW_BID/
                          AUCTION_NO_BIDS/PAYMENT_RECEIVED/SECOND_BIDDER_OFFER/
                          STORE_APPROVED/SELLER_VERIFIED),
                message(@NotNull), isRead(Boolean, default=false), createdAt
               @ManyToOne → user
```

---

## Package Structure

```
com.yala/
  YalaApplication.java          ← @EnableAsync @EnableScheduling
  auth/                         ← AuthController, AuthService, JwtService, JwtAuthFilter, dto/
  user/                         ← User, Role(enum), UserRepository, UserService, UserController, dto/
  category/                     ← Category, CategoryRepository, CategoryService, CategoryController, dto/
  listing/                      ← Listing, ListingMode(enum), ListingStatus(enum),
                                   ListingRepository, ListingService, ListingController, dto/
  image/                        ← Image, ImageRepository, ImageService, ImageController,
                                   SupabaseStorageClient
  tag/                          ← Tag, TagRepository, TagService
  auction/                      ← Auction, AuctionStatus(enum), AuctionRepository,
                                   AuctionService, AuctionController, AuctionScheduler, dto/
  bid/                          ← Bid, BidRepository, BidService, BidController, dto/
  order/                        ← Order, OrderStatus(enum), OrderRepository,
                                   OrderService, OrderController, OrderScheduler, dto/
  payment/                      ← Payment, PaymentStatus(enum), PaymentRepository,
                                   PaymentService, PaymentController, dto/
  review/                       ← Review, ReviewRepository, ReviewService, ReviewController, dto/
  notification/                 ← Notification, NotificationType(enum), NotificationRepository,
                                   NotificationService, NotificationController, dto/
  event/                        ← UserRegisteredEvent, NewBidEvent, AuctionFinishedEvent,
                                   AuctionNoBidsEvent, OrderConfirmedEvent, PaymentExpiredEvent,
                                   StoreApprovedEvent, SellerVerifiedEvent,
                                   EventListeners (all @Async @EventListener/@TransactionalEventListener)
  config/                       ← SecurityConfig, WebSocketConfig, AsyncConfig, StripeConfig
  exception/                    ← GlobalExceptionHandler, ErrorResponse, + 10 custom exceptions
  admin/                        ← AdminController, AdminService (approve stores, verify sellers)
```

---

## DTOs (Java records — 12+ for full score)

Use static factory `XxxResponse.from(Entity e)` for all mapping. **Never expose entities directly.**

```java
// Auth
RegisterRequest(name, email, password, role)          ← @Valid with Bean Validation
LoginRequest(email, password)
RefreshTokenRequest(refreshToken)
AuthResponse(accessToken, refreshToken, userId, email, name, role)

// User
UserResponse(id, name, email, avatarUrl, reputation, isVerifiedSeller, role, dniVerified)
                                                       ← NO passwordHash, dni, cci ever
UpdateUserRequest(name, avatarUrl)
VerifyIdentityRequest(dni, firstName, lastName)        ← @Pattern(regexp="\\d{8}") on dni
RegisterStoreRequest(storeName, address, email, password, cci, phoneNumber)

// Category
CategoryResponse(id, name, description)
CreateCategoryRequest(name, description)

// Listing
CreateListingRequest(title, description, mode, fixedPrice, condition, categoryId, tags)
ListingResponse(id, title, description, mode, fixedPrice, condition, status, createdAt,
                seller, category, imageUrls, auction)
ListingSummaryResponse(id, title, condition, status, fixedPrice, imageUrl, seller)

// Auction
CreateAuctionRequest(listingId, startingPrice, durationDays, scheduledStartAt)
                                                       ← durationDays: 1, 3, 5, or 7
AuctionResponse(id, startingPrice, currentPrice, startedAt, endsAt, status, winner, totalBids)
AuctionSummaryResponse(id, currentPrice, endsAt, status)

// Bid
CreateBidRequest(auctionId, amount)
BidResponse(id, amount, placedAt, bidder)

// Order
CreateOrderRequest(listingId)
OrderResponse(id, amount, status, createdAt, listing, buyer, seller,
              commissionAmount, netSellerAmount, paymentDeadline)

// Payment
CreatePaymentIntentRequest(orderId)
PaymentIntentResponse(clientSecret, paymentIntentId)

// Review
CreateReviewRequest(orderId, rating, comment)
ReviewResponse(id, rating, comment, createdAt, author)

// Notification
NotificationResponse(id, type, message, isRead, createdAt)

// Admin
SellerVerificationRequest(userId)       ← approve seller verification
StoreApprovalRequest(storeId)           ← approve store registration

// Error (not a record — class)
ErrorResponse(timestamp, status, error, message, path)
```

---

## REST Endpoints

```
# Auth
POST   /api/v1/auth/register              → 201; sends UserRegisteredEvent (welcome email)
POST   /api/v1/auth/login                 → 200 AuthResponse (JWT 24h)
POST   /api/v1/auth/refresh-token         → 200 new accessToken
POST   /api/v1/auth/register-store        → 201; store starts PENDING_VERIFICATION

# Identity verification (required before bidding/buying)
POST   /api/v1/users/me/verify-identity   → 200; sets dniVerified=true (auth required)

# Users
GET    /api/v1/users/me                   → 200 (auth)
PUT    /api/v1/users/me                   → 200 (auth)
GET    /api/v1/users/{id}                 → 200 public profile
GET    /api/v1/users/{id}/listings        → 200 paginated
POST   /api/v1/users/me/request-seller    → 200; request seller verification (needs 5 participated + 3 won)

# Categories
GET    /api/v1/categories                 → 200
POST   /api/v1/categories                 → 201 @PreAuthorize("hasRole('ADMIN')")

# Listings
POST   /api/v1/listings                   → 201 @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
                                              max 20 active listings per SELLER
GET    /api/v1/listings                   → 200 paginated; filters: ?category=&mode=&condition=
                                              &minPrice=&maxPrice=&q=&page=&size=&sort=
GET    /api/v1/listings/{id}              → 200
PUT    /api/v1/listings/{id}              → 200 owner only; forbidden if has pending/confirmed orders
DELETE /api/v1/listings/{id}             → 204 owner only (CANCELLED status)

# Auctions
POST   /api/v1/auctions                   → 201 @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
                                              durationDays in [1,3,5,7]; starts SCHEDULED or ACTIVE
GET    /api/v1/auctions                   → 200 paginated active auctions
GET    /api/v1/auctions/{id}              → 200 with bid history
GET    /api/v1/auctions/{id}/bids         → 200 paginated bid history
DELETE /api/v1/auctions/{id}             → 204 owner only; only if zero bids (else 400)

# Bids
POST   /api/v1/bids                       → 201; validates:
                                              - user must be dniVerified (else 403 IdentityNotVerifiedException)
                                              - amount > currentPrice (else 400 InvalidBidException)
                                              - auction status=ACTIVE (else 409 AuctionNotActiveException)
                                              - bidder != auction seller (else 403)
                                              - no consecutive bids by same user (else 400)
                                              triggers NewBidEvent, updates currentPrice

# Orders
POST   /api/v1/orders                     → 201 (direct buy; buyer must be dniVerified)
GET    /api/v1/orders/my-orders           → 200 paginated (auth)
GET    /api/v1/orders/{id}               → 200
PUT    /api/v1/orders/{id}/confirm        → 200 @PreAuthorize("hasRole('SELLER')")
PUT    /api/v1/orders/{id}/cancel         → 200 (only before payment)
PUT    /api/v1/orders/{id}/ship           → 200 seller marks as IN_TRANSIT + trackingNumber (optional)
PUT    /api/v1/orders/{id}/complete       → 200 buyer confirms receipt → COMPLETED → enables reviews

# Payments
POST   /api/v1/payments/intent            → 201 Stripe PaymentIntent (48h deadline enforced)
POST   /api/v1/payments/webhook           → 200 Stripe webhook — PUBLIC; no auth

# Reviews (only when order is COMPLETED — not just CONFIRMED)
POST   /api/v1/reviews                    → 201; both buyer→seller and seller→buyer allowed; 1 per user per order
GET    /api/v1/reviews/user/{id}          → 200 paginated reviews received

# Notifications
GET    /api/v1/notifications              → 200 paginated, newest first (auth)
PUT    /api/v1/notifications/{id}/read    → 200
PUT    /api/v1/notifications/read-all     → 200

# Images
POST   /api/v1/images/upload              → 201 multipart/form-data → Supabase; max 5/listing; max 5MB each; JPG/PNG/WEBP

# Admin
POST   /api/v1/admin/stores/{id}/approve         → 200 @PreAuthorize("hasRole('ADMIN')"); triggers StoreApprovedEvent
POST   /api/v1/admin/verifications/{id}/approve  → 200 @PreAuthorize("hasRole('ADMIN')"); triggers SellerVerifiedEvent
```

**HTTP status codes** — use consistently:
`200` GET/PUT ok · `201` created · `204` delete · `400` validation/business rule · `401` unauthenticated · `403` forbidden/wrong role · `404` not found · `409` conflict/state error · `500` server error · `502` external API error

---

## Business Rules (implement ALL)

### Users & Roles
- **USER**: can browse, bid, buy. Must complete DNI verification before bidding/buying.
- **SELLER**: can create listings and auctions, confirm/cancel orders, mark orders as shipped. Must have `isVerifiedSeller=true`. Two paths: (a) store registered and approved by ADMIN, (b) user with ≥5 auctions participated AND ≥3 won, approved by ADMIN. Must provide CCI before receiving payments.
- **ADMIN**: manages categories, approves stores, verifies sellers, suspends listings.
- DNI must be exactly 8 numeric digits (`@Pattern(regexp="\\d{8}")`). One account per DNI.
- Seller cannot have more than **20 active listings** simultaneously → `ListingLimitExceededException` (400).
- Cannot edit a listing that has pending or confirmed orders associated.
- Cannot edit `startingPrice` once auction has at least one bid.
- Images: max 5 per listing, max 5MB each, formats JPG/PNG/WEBP only.

### Auctions & Bids
- Auction `status` transitions: `SCHEDULED → ACTIVE → CLOSED → PAID` or `CANCELLED`.
- Scheduler runs every 60s: activates SCHEDULED auctions whose `scheduledStartAt <= now()`.
- Scheduler runs every 60s: closes ACTIVE auctions where `endsAt <= now()`.
- When closing with bids → set winner (highest bidder), create `Order(PENDING)`, `paymentDeadline = now() + 48h`, calculate `commissionAmount = amount * 0.08`, `netSellerAmount = amount * 0.92`.
- When closing without bids → `CANCELLED`, listing back to `ACTIVE`, trigger `AuctionNoBidsEvent`.
- Seller **cannot** bid on their own auction → 403.
- Same user **cannot** bid twice consecutively (someone else must bid between) → 400.
- Concurrent bids: `@Version` on Auction → `OptimisticLockException` → handled as 409 "price changed, retry".
- Recommended minimum increment: 1% of currentPrice (informational, not enforced server-side).

### Payments & Orders
- Payment deadline: **48 hours** from order creation. Enforced by daily scheduler.
- If winner doesn't pay in 48h: order → `CANCELLED(PAYMENT_EXPIRED)`, increment `user.failedPayments`, find second highest bidder, create new `Order(PENDING)` for them (48h deadline), trigger `PaymentExpiredEvent`.
- If second bidder also doesn't pay: listing returns to `ACTIVE` for seller to republish.
- Stripe webhook endpoint is PUBLIC — no JWT required.
- On successful payment: `Order → CONFIRMED`, `Auction → PAID`, `Listing → SOLD`, commission tracked.
- Order flow: `PENDING → CONFIRMED → IN_TRANSIT → COMPLETED` or `CANCELLED` or `DISPUTED`.
- Auto-complete: if buyer doesn't confirm receipt within **15 days** of `IN_TRANSIT` → order auto-completes (daily scheduler).

### Reviews
- Reviews are **mutual**: buyer reviews seller AND seller reviews buyer — both after same order.
- Only allowed when `order.status = COMPLETED` (not CONFIRMED) → else `ReviewNotAllowedException` (403).
- Max 1 review per user per order. Not editable or deletable once published.
- Seller reputation = `avg(rating)` of all reviews received as seller.
- User with reputation < 2.0 → ADMIN review (flag in system).

---

## Security

```java
// Public routes (permitAll):
POST /api/v1/auth/**
GET  /api/v1/listings/**
GET  /api/v1/auctions/**
GET  /api/v1/categories/**
GET  /api/v1/reviews/user/**
GET  /api/v1/users/{id}
GET  /api/v1/users/{id}/listings
POST /api/v1/payments/webhook
/swagger-ui/** · /v3/api-docs/** · /ws/**
// All others → authenticated
```

- `JwtService`: generate `accessToken` (24h) + `refreshToken` (7d). Claims: `sub`(email), `userId`, `role`. Secret from env `JWT_SECRET`.
- `JwtAuthFilter extends OncePerRequestFilter`: extract Bearer → validate → set SecurityContext.
- `@EnableMethodSecurity` in SecurityConfig for `@PreAuthorize`.
- Get authenticated user in services via `SecurityContextHolder` — **never** trust userId from request params.
- CCI stored encrypted in DB. Never returned in any response.
- `.env` and secrets in `.gitignore` — document required env vars in README with example values.

---

## Spring Events (ALL listeners `@Async`)

Use `@TransactionalEventListener(phase = AFTER_COMMIT)` for events that must run after DB commit.

**`UserRegisteredEvent`** → async welcome email via Resend (HTML template)

**`NewBidEvent`** (auctionId, newAmount, previousBidderId, currentBidderId):
1. Notify previous highest bidder in-app → `Notification(BID_OUTBID)` (NOT email)
2. Broadcast to WebSocket `/topic/auction/{id}`: `{ auctionId, currentPrice, totalBids, latestBid: { user, amount, placedAt } }`

**`AuctionFinishedEvent`** (auctionId) — from scheduler, use `@TransactionalEventListener`:
1. Set `status=CLOSED`, assign winner (highest bidder)
2. Auto-create `Order(PENDING)` with 48h deadline, calculate commission
3. Email winner → "You won! Pay S/. {amount} within 48 hours" (HTML template)
4. Notify seller in-app → "Auction closed. You'll receive S/. {netAmount} (92%)"

**`AuctionNoBidsEvent`** (auctionId):
1. Set `status=CANCELLED`, listing back to `ACTIVE`
2. Notify seller in-app → "Auction ended with no bids"

**`OrderConfirmedEvent`** (orderId, buyerId, sellerId):
1. Email buyer → "Payment confirmed!" (HTML template)
2. Notify seller in-app → "Payment received. Process shipping."
3. Recalculate seller reputation: `avg(rating)` from all received reviews

**`PaymentExpiredEvent`** (orderId, originalBuyerId, secondBidderId):
1. Email original buyer → "Payment deadline passed" (HTML template)
2. Notify second bidder in-app → "Winner didn't pay. You have 48h to accept at your bid price"
3. Notify seller in-app at each stage

**`StoreApprovedEvent`** (storeId) → welcome email to store (HTML template)

**`SellerVerifiedEvent`** (userId) → in-app notification → "You can now create auctions"

**`AsyncConfig`**: `ThreadPoolTaskExecutor` — coreSize=4, maxSize=10, queueCapacity=100, prefix `"yala-async-"`.

**Scheduler classes:**
- `AuctionScheduler`: `@Scheduled(fixedRate=60000)` — activate SCHEDULED + close ACTIVE expired
- `OrderScheduler`: `@Scheduled(cron="0 0 * * * *")` — check 48h payment deadlines + 15-day auto-complete

---

## WebSockets (STOMP)

`WebSocketConfig`: endpoint `/ws` (SockJS fallback), broker prefix `/topic`, app destination prefix `/app`.

Topics:
- `/topic/auction/{id}` — real-time bid updates and auction close (all viewers of that page)
- `/topic/notifications/{userId}` — real-time in-app notification badge counter

Use `SimpMessagingTemplate` inside event listeners.

---

## Business Logic — Complete Reference

### Actors & Roles
| Actor | Role | Description |
|-------|------|-------------|
| Buyer | USER | Can browse freely. Must verify DNI to bid or buy. |
| Verified bidder | SELLER | USER who participated in ≥5 auctions AND won ≥3; approved by ADMIN. |
| Store | SELLER | Registered business; can sell from day one after ADMIN approval. |
| Admin | ADMIN | Full system access; approves stores/verifications; moderates listings. |

### Registration Flows

**User registration** (`POST /api/v1/auth/register`):
- Fields: name, email, password (min 8 chars, 1 uppercase, 1 number), dateOfBirth. Must be 18+.
- DNI is optional at registration — collected later via `/users/me/verify-identity`.
- Account created with `dniVerified=false`. JWT returned with role USER immediately.
- Password hashed with BCrypt strength 12. Never stored in plain text.
- Fires `UserRegisteredEvent` async → welcome email.
- Duplicate email → 409 `DuplicateResourceException`.

**DNI Identity Verification** (`POST /api/v1/users/me/verify-identity`):
- Required before bidding or buying. Triggered by the frontend when user first attempts either.
- Fields: dni (exactly 8 numeric digits, `@Pattern(regexp="\d{8}")`), firstName, lastName (letters and spaces only, `@NotBlank + @Pattern`).
- On success: sets `dniVerified=true`. User can bid immediately.
- Duplicate DNI → 409 with message "Este DNI ya está asociado a otra cuenta".
- DNI never shown in public profile — only a "Verified Identity" icon is displayed.
- Without dniVerified: user CAN browse, view listings, view auction history, view profiles. CANNOT bid or create orders.

**Store registration** (`POST /api/v1/auth/register-store`):
- Fields: storeName, address, email, password, cci (CCI = bank account code for payouts), phoneNumber.
- CCI is mandatory (needed for Yala to transfer net amount after 8% commission).
- Store created with status `PENDING_VERIFICATION`. ADMIN activates it.
- On ADMIN approval → role SELLER + `StoreApprovedEvent` → welcome email.
- Confirmation email sent both at registration request AND at approval.

**User seller verification** (`POST /api/v1/users/me/request-seller`):
- Requirement: participated in ≥5 auctions AND won ≥3. Both conditions simultaneously.
- System automatically counts. If not met → 400 with message showing how many are missing.
- If met → request goes to PENDING for ADMIN review.
- ADMIN approves → user gets role SELLER + `SellerVerifiedEvent` → in-app notification.
- User must provide CCI before receiving payments as seller.

### Listing Rules
- Only SELLER (verified user or approved store) can create listings.
- Title: min 10 chars. Description: required. Min 1 image, max 5. Images: max 5MB, JPG/PNG/WEBP only.
- Categories: "Pokémon TCG", "Funko Pop", "Comics", "Other".
- Condition/PSA grade: "PSA 10 (Gem Mint)", "PSA 9 (Mint)", "PSA 8 (Near Mint)", "PSA 7 or lower", "Ungraded (Excellent/Good/Fair)".
- Optional tags: "First Edition", "Holographic", "Limited", etc.
- Max **20 active listings per SELLER** simultaneously → `ListingLimitExceededException` (400).
- Cannot edit a listing that has PENDING or CONFIRMED orders → `InvalidOperationException` (400).
- Listing status: `DRAFT → ACTIVE → SOLD / CANCELLED / SUSPENDED`.
  - `DRAFT`: created but not published; only visible to seller.
  - `ACTIVE`: visible to all; accepting bids.
  - `SOLD`: order confirmed and payment processed.
  - `CANCELLED`: seller withdrew it, or auction ended with no bids.
  - `SUSPENDED`: ADMIN suspended for policy violation.

### Auction Rules
- Only SELLER can create auctions.
- Fields: listingId, startingPrice (= minimum opening bid), durationDays (1, 3, 5, or 7), optional scheduledStartAt.
- System auto-calculates `endsAt = scheduledStartAt + durationDays` (or `now() + durationDays` if immediate).
- Starts as `SCHEDULED` if future date, or `ACTIVE` if immediate.
- **AuctionScheduler** (`@Scheduled(fixedRate=60000)`) does two jobs:
  1. Activates SCHEDULED auctions whose `scheduledStartAt <= now()`.
  2. Closes ACTIVE auctions whose `endsAt <= now()`.
- Auction statuses: `SCHEDULED → ACTIVE → CLOSED → PAID` or `CANCELLED`.
- Seller can cancel ONLY if zero bids; once any bid exists → `InvalidOperationException` (400).
- If cancelled by seller before any bid → listing returns to `DRAFT`.

### Bid Rules
- Must be dniVerified → else 403 `IdentityNotVerifiedException`.
- Amount must be **strictly greater** than `currentPrice` (equal rejected) → `InvalidBidException` (400).
- Auction must be in `ACTIVE` state → else `AuctionClosedException` (400).
- Bidder cannot be the auction's seller → `InvalidBidException` (400).
- Same user cannot bid twice consecutively without another user bidding in between → `InvalidBidException` (400).
- Recommended minimum increment: 1% of currentPrice (informational only; not enforced server-side).
- On successful bid: update `auction.currentPrice`, save Bid, publish `NewBidEvent`.
- **Optimistic locking**: `@Version` on Auction entity. If two bids arrive simultaneously, second throws `OptimisticLockException` → GlobalExceptionHandler returns 409 "Price changed, please retry".

### Auction Close Flow (via AuctionScheduler)
**With bids:**
1. Set auction `status = CLOSED`, assign `winner` (highest bidder).
2. Calculate `commissionAmount = amount * 0.08`, `netSellerAmount = amount * 0.92`.
3. Create `Order(PENDING)` with `paymentDeadline = now() + 48h`.
4. Publish `AuctionFinishedEvent` → email winner + in-app notification to seller.

**Without bids:**
1. Set auction `status = CANCELLED`.
2. Set listing `status = ACTIVE` (seller can republish).
3. Publish `AuctionNoBidsEvent` → in-app notification to seller immediately.

### Payment Flow
- Stripe sandbox mode. Card data never passes through Yala servers — handled by Stripe SDK directly.
- On Stripe success: `Order → CONFIRMED`, `Auction → PAID`, `Listing → SOLD`. Publish `OrderConfirmedEvent`.
- `OrderConfirmedEvent` → email buyer "payment confirmed" + in-app notification to seller "payment received; transfer in 1-3 business days".
- On Stripe error: `Payment.status = FAILED`, email buyer "payment rejected, please retry". Order stays PENDING.
- All payment attempts saved for audit.

### Payment Expiry Flow (OrderScheduler, runs daily)
1. Find Orders in PENDING where `paymentDeadline < now()`.
2. Cancel original order (`CANCELLED`, reason `PAYMENT_EXPIRED`). Increment `buyer.failedPayments`.
3. Find second-highest bidder for that auction.
4. If exists: create new `Order(PENDING)` for them (new 48h deadline). Publish `PaymentExpiredEvent` → in-app notification to second bidder + in-app to seller.
5. If no second bidder or they also don't pay: listing returns to `ACTIVE`. In-app notification to seller at each stage.

### Order Status Flow
`PENDING → CONFIRMED → IN_TRANSIT → COMPLETED` or `CANCELLED` or `DISPUTED`
- Seller calls `PUT /api/v1/orders/{id}/ship` → `IN_TRANSIT` (optional trackingNumber).
- Buyer calls `PUT /api/v1/orders/{id}/complete` → `COMPLETED` → enables reviews for both parties.
- **Auto-complete**: if buyer doesn't confirm within **15 days** of `IN_TRANSIT` → OrderScheduler auto-completes.
- Buyer can cancel only before payment (while `PENDING`).

### Review Rules
- Only allowed when `order.status = COMPLETED` (not CONFIRMED) → else `ReviewNotAllowedException` (403).
- **Mutual**: buyer reviews seller AND seller reviews buyer — both on same order. 1 review per user per order.
- Not editable or deletable once published.
- Rating: 1–5 stars. Comment: optional, max 1000 chars.
- Seller reputation = `avg(rating)` of all reviews received **as seller**. Recalculated on `OrderConfirmedEvent`.
- User with reputation < 2.0 → flagged for ADMIN review.
- Public profile shows: total transactions, % positive ratings (4–5 stars), recent comments.
- Buyer reputation (as buyer) is visible to sellers.

### Notification Strategy (exact from business doc)
**Email (only 4 cases):**
1. User/store registration → welcome email
2. User wins auction → "You won! Pay S/. {amount} within 48 hours"
3. Payment processed successfully → "Your payment was confirmed"
4. Payment rejected or expired → "Your payment deadline passed"

**Real-time WebSocket** (inside auction page — all viewers):
- Every new bid → all users currently viewing that auction page see updated price and history instantly

**In-app notifications (everything else):**
- Previous highest bidder outbid (`BID_OUTBID`)
- Seller notified when auction closes with bids (with net amount)
- Seller notified when auction closes without bids (`AUCTION_NO_BIDS`)
- Seller notified when buyer pays (`PAYMENT_RECEIVED`)
- Second bidder offered the item (`SECOND_BIDDER_OFFER`)
- Seller notified at each stage of payment expiry flow
- Shipping and receipt status updates
- Seller verification approved (`SELLER_VERIFIED`)
- Store approved (`STORE_APPROVED`)

Notification badge counter updated in real-time via WebSocket `/topic/notifications/{userId}`.

### Commission Model
| Concept | Example |
|---------|---------|
| Final auction price | S/. 1,000 |
| Yala commission (8%) | S/. 80 |
| Net to seller | S/. 920 |
| Who pays | Seller (deducted from payout) |

CCI stored encrypted in DB. Never returned in any response DTO. Visible only to the account owner and ADMIN.

### Security Details
- BCrypt strength 12 for passwords.
- JWT: 24h access token + 7d refresh token. Claims: `userId`, `email`, `role`, `exp`.
- Get authenticated user in services via `SecurityContextHolder` — never trust userId from request params.
- `.env` and secrets in `.gitignore`. Document required vars in README with example values only.

---

## Exception Handling — 10+ custom exceptions

Use these **exact class names** (from the project's business logic document):

```
ResourceNotFoundException         → 404   (auction, listing, user, or order not found by id)
DuplicateResourceException        → 409   (email already registered; duplicate review on same order)
InvalidBidException               → 400   (amount ≤ currentPrice; bidding on own auction; consecutive bid by same user)
AuctionClosedException            → 400   (bidding on auction that is CLOSED or CANCELLED)
InvalidOperationException         → 400   (editing listing with bids; cancelling auction with bids)
UnauthorizedException             → 401   (JWT missing, invalid, or expired)
ForbiddenException                → 403   (operating on another user's resource or wrong role)
PaymentException                  → 500   (unexpected Stripe API error)
VerificationRequiredException     → 403   (user without isVerifiedSeller tries to create auction/listing)
IdentityNotVerifiedException      → 403   (user tries to bid without having verified their DNI)
DniAlreadyExistsException         → 409   (DNI already linked to another account)
ListingLimitExceededException     → 400   (>20 active listings per seller)
ImageLimitExceededException       → 400   (>5 images per listing)
ReviewNotAllowedException         → 403   (order not COMPLETED yet; or already reviewed)
```

`@RestControllerAdvice GlobalExceptionHandler` handles ALL above + Spring exceptions:
- `MethodArgumentNotValidException` → 400 with field-level error details
- `HttpMessageNotReadableException` → 400
- `AccessDeniedException` → 403
- `OptimisticLockException` → 409 "Price changed, please retry"
- `Exception` → 500 fallback

Always return:
```java
record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {}
```
Inject `HttpServletRequest` into each handler method to get the exact path.

---

## Tests — BDD naming is MANDATORY on every single test method

### Repository tests (`@DataJpaTest` + Testcontainers — use on ALL repo tests)
```java
// Testcontainers pattern for ALL @DataJpaTest:
@Testcontainers @DataJpaTest
class XxxRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }
}

// AuctionRepositoryTest
shouldFindActiveAuctionsWhenStatusIsActive()
shouldReturnExpiredAuctionsWhenEndsAtIsBeforeNow()
shouldFindAuctionsByListingId()
shouldFindScheduledAuctionsWhenStartAtHasPassed()

// ListingRepositoryTest
shouldFilterListingsByCategory()
shouldFilterListingsByMode()
shouldSearchListingsByTitleKeyword()
shouldReturnPaginatedResults()
shouldCountActiveListingsBySeller()

// BidRepositoryTest
shouldFindHighestBidByAuctionId()
shouldReturnBidsOrderedByAmountDesc()
shouldCountBidsByAuction()
shouldFindSecondHighestBidWhenWinnerDidNotPay()
```

### Service tests (Mockito — mock ALL dependencies)
```java
// AuctionServiceTest
shouldCreateAuctionWhenListingExistsAndUserIsSeller()
shouldThrowResourceNotFoundExceptionWhenAuctionDoesNotExist()
shouldAssignWinnerAndCreateOrderWhenAuctionFinishesWithBids()
shouldCancelAuctionAndRestoreListingWhenNoBids()
shouldThrowAuctionCancellationExceptionWhenCancellingWithBids()

// BidServiceTest
shouldPlaceBidWhenAmountIsHigherThanCurrentPrice()
shouldThrowInvalidBidExceptionWhenAmountIsLowerThanCurrentPrice()
shouldThrowAuctionNotActiveExceptionWhenAuctionIsFinished()
shouldThrowInvalidBidExceptionWhenBidderIsSeller()
shouldThrowIdentityNotVerifiedExceptionWhenBidderHasNoDni()
shouldUpdateCurrentPriceAfterSuccessfulBid()

// AuthServiceTest
shouldRegisterUserWhenEmailIsUnique()
shouldThrowEmailAlreadyExistsExceptionWhenEmailIsDuplicated()
shouldReturnTokenWhenCredentialsAreValid()
shouldThrowUnauthorizedExceptionWhenPasswordIsWrong()

// OrderServiceTest
shouldExpireOrderAndOfferToSecondBidderWhenPaymentDeadlinePassed()
shouldAutoCompleteOrderWhenBuyerDidNotConfirmIn15Days()
```

### Controller tests (`@WebMvcTest` + MockMvc — minimum 6 controllers)
```java
// AuthControllerTest
shouldReturn201WhenRegisterIsSuccessful()
shouldReturn409WhenEmailAlreadyExists()
shouldReturn200WithTokenWhenLoginIsSuccessful()
shouldReturn401WhenCredentialsAreInvalid()

// ListingControllerTest
shouldReturn201WhenListingIsCreatedByASeller()
shouldReturn403WhenListingIsCreatedByAUser()
shouldReturn200WithPagedListingsWhenFiltersAreApplied()
shouldReturn404WhenListingDoesNotExist()
shouldReturn400WhenSellerExceedsActiveListingLimit()

// AuctionControllerTest
shouldReturn201WhenAuctionIsCreated()
shouldReturn200WithActiveAuctions()
shouldReturn403WhenUserTriesToCreateAuction()
shouldReturn400WhenCancellingAuctionWithBids()

// BidControllerTest
shouldReturn201WhenBidIsValid()
shouldReturn400WhenBidAmountIsLowerThanCurrent()
shouldReturn409WhenAuctionIsFinished()
shouldReturn401WhenUserIsNotAuthenticated()
shouldReturn403WhenBidderHasNotVerifiedIdentity()

// OrderControllerTest
shouldReturn201WhenOrderIsCreated()
shouldReturn200WithUserOrders()
shouldReturn409WhenOrderIsAlreadyConfirmed()

// ReviewControllerTest
shouldReturn201WhenReviewIsCreatedOnCompletedOrder()
shouldReturn403WhenOrderIsNotCompleted()
shouldReturn200WithUserReviews()
```

---

## Configuration Files

### `application.yml`
```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/yala_db}
    username: ${DB_USER:postgres}
    password: ${DB_PASS:postgres}
  jpa:
    hibernate.ddl-auto: update
    show-sql: false
    properties.hibernate.format_sql: true
  thymeleaf:
    prefix: classpath:/templates/
    suffix: .html
  task.execution.pool:
    core-size: 4
    max-size: 10
    queue-capacity: 100

jwt:
  secret: ${JWT_SECRET:yala-secret-key-change-in-production}
  access-token-expiration-ms: 86400000    # 24 hours
  refresh-token-expiration-ms: 604800000  # 7 days

stripe:
  secret-key: ${STRIPE_SECRET_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}

supabase:
  url: ${SUPABASE_URL}
  key: ${SUPABASE_KEY}
  bucket: collectibles

resend:
  api-key: ${RESEND_API_KEY}
  from: noreply@yala.pe
```

### `docker-compose.yml`
```yaml
version: '3.9'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: yala_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports: ["5432:5432"]
    volumes: [pgdata:/var/lib/postgresql/data]
volumes:
  pgdata:
```

### `.github/workflows/ci.yml`
```yaml
name: CI
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env: {POSTGRES_DB: yala_test, POSTGRES_USER: postgres, POSTGRES_PASSWORD: postgres}
        ports: ["5432:5432"]
        options: --health-cmd pg_isready --health-interval 10s --health-timeout 5s --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: {java-version: '17', distribution: temurin}
      - run: mvn clean verify
        env:
          DATABASE_URL: jdbc:postgresql://localhost:5432/yala_test
          DB_USER: postgres
          DB_PASS: postgres
          JWT_SECRET: test-secret-key-for-ci
```

### `data.sql` (seed)
3 categories ("Pokémon TCG", "Funko Pop", "Comics"), 1 USER (dniVerified=true), 1 SELLER (isVerifiedSeller=true), 2 listings (1 FIXED + 1 AUCTION mode), 1 active auction.

---

## Email Templates (Thymeleaf HTML — `resources/templates/email/`)
- `welcome.html` — sent on user/store register
- `auction-won.html` — "You won! Pay S/. {amount} within 48 hours"
- `payment-confirmed.html` — "Your payment was processed successfully"
- `payment-expired.html` — "Your payment deadline has passed"
- `store-approved.html` — sent when ADMIN approves a store

---

## Bonus Items (implement all — compensate any lost points)
- Swagger/OpenAPI: `@Tag` on every controller, `@Operation` on every endpoint
- Structured logging with SLF4J + Logback
- Pagination with `Pageable` + `Page<XxxResponse>` on ALL list endpoints
- Advanced filters on `GET /api/v1/listings`
- Image upload to Supabase Storage
- Docker Compose (required above)
- CI/CD with GitHub Actions (required above)

---

## Git Workflow (GitFlow — graded)

```
main        ← production-ready only; merge via PR with code review
develop     ← integration branch; all features merge here first
feature/*   ← one branch per domain (feature/auth, feature/listings, feature/auctions, feature/bids, feature/orders, feature/payments, feature/reviews, feature/notifications, feature/admin)
fix/*       ← bug fixes
```

**Commit format**: `feat: add bid validation`, `fix: null pointer in auction scheduler`, `test: add BidServiceTest`

**PRs**: feature/* → PR into `develop` → review → merge. Milestone complete: `develop` → PR into `main`.

**GitHub Issues**: one issue per domain with labels (`feature`, `test`, `fix`) and milestones. CI must pass before merging to `main`.

---

## Build Order (recommended)

1. Project scaffold + `pom.xml` + `application.yml` + `docker-compose.yml`
2. Entities + enums (all 11)
3. Repositories (with custom queries)
4. DTOs (records with static `.from()` factory methods)
5. Exception classes + `GlobalExceptionHandler`
6. `AsyncConfig` + `SecurityConfig` + `WebSocketConfig`
7. `JwtService` + `JwtAuthFilter`
8. Services (Auth → User → Category → Listing → Auction → Bid → Order → Payment → Review → Notification → Image → Admin)
9. Controllers (thin — delegate all logic to services)
10. Spring Events + `EventListeners` (`@Async` + `@TransactionalEventListener`)
11. `AuctionScheduler` + `OrderScheduler`
12. Email templates (Thymeleaf) + `EmailService` (Resend)
13. `SupabaseStorageClient` + `ImageService`
14. Tests: Repositories → Services → Controllers (BDD on every method)
15. `data.sql` seed + `.github/workflows/ci.yml`
16. `README.md` (1000–2000 words, all required sections per rubric)
17. `postman_collection.json` at repo root
