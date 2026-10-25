# Community Centre Booking – COMP713 Assessment 3

A distributed booking system for a community centre. Members book rooms from a **native Android app**, staff review bookings in a **web client**, and both clients share data through one **Spring Boot REST API** backed by **MySQL**. Booking decisions are published to **RabbitMQ** and processed by a separately running **notification consumer** (advanced integration Path B).

- **Repository:** https://github.com/WinWinAye200199/COMP713-A3-community-centre-booking
- **Final version:** tag `TBD` / commit `TBD`
- **Baseline (Assessment 2):** community centre booking system by Kelsea (original repo `kelseak1/COMP713-individual-project`, commit `69e2b83`, tagged `a2-baseline` in this repository)

---

## 1. Architecture

```
 Android app (MEMBER)          Web client (STAFF / MEMBER)
        │  REST + HTTP Basic          │  REST + HTTP Basic
        └──────────────┬──────────────┘
                       ▼
         Booking API (Spring Boot, :8080)
          controller → service → repository
                       │                    │ BookingStatusChanged event
                       ▼                    │ (sent after DB commit)
                 MySQL database             ▼
     users, room, bookings ◄────┐   RabbitMQ (:5672)
     notifications (read)       │   exchange booking.events
                                │   queue notification.booking-status (+ DLQ)
                                │           │
                                │           ▼
                                └── notification-consumer (separate app)
                                    writes notifications, processed_events
```

| Component | Folder | Responsibility |
|---|---|---|
| Booking API + web client | `community-centre-booking/community-centre-booking` | Users/roles, rooms, bookings, approval, cancellation, notifications read API; serves the web client from `src/main/resources/static` |
| Notification consumer | `notification-consumer` | Consumes booking events, creates member notifications, ignores repeated events, retries and dead-letters failures |
| Android app | `android-app` *(teammate)* | Member login, rooms, create booking, my bookings, notifications |
| RabbitMQ | `docker-compose.yml` | Message broker |
| Database schema | `database/schema.sql` | Table definitions (tables are also created automatically on start) |

---

## 2. Prerequisites

- Java 21 (Maven wrapper included – no separate Maven install needed)
- MySQL 8+ (local server or the lecturer-provided server)
- Docker Desktop (for RabbitMQ)
- Android Studio (for the Android app)
- `curl` (optional, for API checks)

---

## 3. Setup and start

### 3.1 Database
Create an empty database:
```sql
CREATE DATABASE community_centre_booking;
```
Tables are created automatically by Hibernate on first start. `database/schema.sql` documents the final schema.

### 3.2 Environment variables
See `.env.example`. In **every** terminal used for the backend or the consumer:
```bash
export DB_URL="jdbc:mysql://localhost:3306/community_centre_booking?allowPublicKeyRetrieval=true&useSSL=false"
export DB_USERNAME="root"
export DB_PASSWORD="your-password"
```

### 3.3 Start in this order
```bash
# 1. RabbitMQ (repository root)
docker compose up -d

# 2. Booking API + web client (terminal 1)
cd community-centre-booking/community-centre-booking
./mvnw spring-boot:run

# 3. Notification consumer (terminal 2)
cd notification-consumer
./mvnw spring-boot:run

# 4. Android app: open android-app in Android Studio and run on an emulator
```

### 3.4 Ports
| Service | URL |
|---|---|
| Booking API + web client | http://localhost:8080 |
| API from Android emulator | http://10.0.2.2:8080 |
| RabbitMQ AMQP | localhost:5672 |
| RabbitMQ management UI | http://localhost:15672 (guest / guest) |

---

## 4. Test accounts and sample data
Created automatically on first start (`DataInitializer`):

| Email | Password | Role |
|---|---|---|
| member@test.com | member123 | MEMBER |
| member2@test.com | member123 | MEMBER |
| staff@test.com | staff123 | STAFF |

Rooms: 1 Main Hall (120), 2 Meeting Room (20), 3 Activity Room (40).

---

## 5. API summary
All endpoints except `GET /api/rooms` require HTTP Basic authentication.

| Method | Path | Role | Description |
|---|---|---|---|
| GET | /api/auth/me | any | Logged-in user and role |
| GET | /api/rooms | public | Rooms *(A2, retained)* |
| POST | /api/bookings | MEMBER | Create booking → `PENDING` *(A2, revised)* |
| GET | /api/bookings/my | MEMBER | Own bookings |
| GET | /api/bookings?status= | STAFF | All bookings, optional status filter *(A2, revised)* |
| PATCH | /api/bookings/{id}/status | STAFF | `{"status":"APPROVED"}` or `REJECTED` (only from `PENDING`) |
| PATCH | /api/bookings/{id}/cancel | owner or STAFF | Cancel `PENDING`/`APPROVED` booking *(replaces A2 DELETE)* |
| GET | /api/notifications/my | MEMBER | Notifications created by the consumer |

Errors use `{"code","message","path"}` with 400, 401, 403, 404 or 409.

**Event contract** (exchange `booking.events`, routing key `booking.status.changed`):
```json
{"eventId":"uuid","bookingId":3,"memberId":1,"roomName":"Meeting Room","bookingDate":"2026-10-31",
 "startTime":"09:00:00","oldStatus":"PENDING","newStatus":"APPROVED","occurredAt":"2026-10-25T00:19:10Z"}
```

---

## 6. Workflows
1. **Booking approval** – member creates a booking on Android (`PENDING`) → staff sees it on the web and approves/rejects → member sees the new status and receives a notification.
2. **Cancellation frees a slot** – member cancels → the slot no longer blocks other members, who can now book the same time.

Quick check with curl:
```bash
curl -u member@test.com:member123 -H "Content-Type: application/json" \
  -d '{"purpose":"Yoga","bookingDate":"2026-12-01","startTime":"10:00","endTime":"11:00","roomId":1}' \
  http://localhost:8080/api/bookings
curl -u staff@test.com:staff123 -H "Content-Type: application/json" -X PATCH \
  -d '{"status":"APPROVED"}' http://localhost:8080/api/bookings/1/status
curl -u member@test.com:member123 http://localhost:8080/api/notifications/my
```

---

## 7. Tests
```bash
# Booking API (H2 in-memory database, RabbitMQ mocked) – 14 tests
cd community-centre-booking/community-centre-booking && ./mvnw test

# Notification consumer (unit tests incl. repeated event) – 3 tests
cd notification-consumer && ./mvnw test

# Concurrency check against the running API and MySQL:
# 20 simultaneous requests for one slot → expected "1 201" and "19 409"
bash scripts/race-test.sh 2026-12-05 20
```

### Failure demonstrations
| Case | How |
|---|---|
| Consumer offline | Stop the consumer, approve a booking → message stays in the queue (Ready 1) → start the consumer → notification created |
| Retry and dead-letter | `./mvnw spring-boot:run -Dspring-boot.run.arguments=--notification.simulate-failure=true` in `notification-consumer` → 3 attempts, then the message moves to `notification.booking-status.dlq` |
| Repeated event | Re-publish an event JSON in the RabbitMQ UI (property `content_type=application/json`) → consumer logs "Duplicate event … ignored" |
| Broker offline | `docker compose stop rabbitmq` → approvals still succeed; the API logs that the event was not published |

---

## 8. Known limitations
- Events are published after the database commit; if RabbitMQ is down at that moment the event is lost (no outbox table).
- The API and the consumer share one MySQL database; the API only reads the `notifications` table written by the consumer.
- HTTP Basic over plain HTTP is used for the demo; production would need HTTPS and token-based login.
- Tests use H2; the concurrency fix is verified against MySQL with `scripts/race-test.sh`.
- Android checks notifications by polling the API rather than push messages.
- *(Add Android / web / WebSocket limitations here.)*
