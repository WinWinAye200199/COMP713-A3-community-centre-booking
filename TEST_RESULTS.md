# COMP713 Assessment 3 – Integration Test Results

Final commit tested: 8761dd5d29aa42e8ed15130f2a227e87efadb957
Test environment: macOS, Android emulator API 37, Chrome, Java 21, Spring Boot, local MySQL and local RabbitMQ 
Test date: 26 October 2026

| ID | Test | Expected result | Actual result | Status | Evidence |
|---|---|---|---|---|---|
| T01 | Valid Android member login | Member home screen opens and rooms load | Login succeeded and the member home screen displayed the room list from the backend | Pass | T01-valid-android-login.png |
| T02 | Invalid Android login | Clear authentication error appears | Invalid credentials were rejected and the app displayed an incorrect email or password message without opening the member area | Pass | T02-invalid-android-login.png |
| T03 | Member creates booking | Booking is saved as PENDING and appears in My Bookings | The Android request succeeded and the new Activity Room booking appeared in My Bookings with PENDING status | Pass | T03-created-pending-booking.png |
| T04 | WebSocket live update | New Android booking appears on the open staff dashboard without refresh | The Android-created booking appeared automatically in the pending staff view while live updates showed as connected | Pass | T04-websocket-live-update.png |
| T05 | Staff approves booking | Status changes to APPROVED and persists | Staff approved the pending booking and Android retrieved the updated APPROVED status from the shared backend | Pass | T05-approved-booking.png |
| T06 | RabbitMQ notification workflow | Consumer creates a notification and Android displays it | The consumer received the approval event, stored a member notification, and Android displayed it in-app and as a system notification | Pass | T06-notification-list.png; T06-android-system-notification.png |
| T07 | Member cancellation workflow | Cancellation persists and staff dashboard updates without refresh | The member cancelled the approved booking in Android and the open staff dashboard automatically displayed CANCELLED through WebSocket | Pass | T07-cancellation-live-update.png |
| T08 | Invalid booking times | Request is rejected with useful validation feedback | A booking with an end time before its start time was rejected and Android displayed a clear validation message without creating the booking | Pass | T08-invalid-booking-times.png |
| T09 | Overlapping booking | Second booking is rejected with 409 Conflict | The valid booking was created, but an overlapping request for the same room and date was rejected with a booking-conflict message | Pass | T09-overlapping-booking.png |
| T10 | Role/access control | Member cannot access staff-only API operations | An authenticated member request to the staff bookings endpoint returned HTTP 403 and did not expose staff booking data | Pass | T10-member-forbidden-staff-api.png |
| T11 | Persistent state after restart | Existing bookings and notifications remain after services restart | After restarting the backend and consumer, the existing cancelled booking and its approval notification were still available from MySQL | Pass | T11-booking-after-restart.png; T11-notification-after-restart.png |
| T12 | WebSocket reconnection | Dashboard reconnects and retrieves changes missed while disconnected | The dashboard reconnected without a page refresh, retrieved the booking created while offline, and restored current state in 0.236 seconds | Pass | T12-websocket-reconnection.png |
| T13 | Backend unavailable and recovery | Android shows an error without crashing and recovers after restart | With the backend stopped, Android displayed a connection error and remained usable; after restart, refreshing successfully restored the notification data | Pass | T13-backend-unavailable.png; T13-backend-recovered.png |
| T14 | Notification consumer unavailable | Failure is logged and the system remains usable; recovery is demonstrated | While the consumer was stopped, RabbitMQ retained the approval event in its queue. After the consumer restarted, it processed the waiting event and Android displayed the resulting notification | Pass | T14-message-waiting-in-queue.png; T14-consumer-recovery.png |
| T15 | API response-time requirement | Representative operation meets the agreed response-time target | Five authenticated GET /api/bookings/my requests averaged 0.383 seconds, with a maximum of 0.706 seconds; all were below the 2-second target | Pass | T15-api-response-times.png |
| T16 | Automated build checks | Android, backend and consumer test/build commands succeed | Android test and debug build completed successfully, and both Spring Boot projects completed their Maven test suites with BUILD SUCCESS | Pass | T16-android-build.png; T16-backend-tests.png; T16-consumer-tests.png |

## Non-functional requirements

### API response time

- Acceptance target: An authenticated request to load the member's bookings should complete within 2 seconds.
- Operation tested: GET /api/bookings/my
- Test conditions: Five sequential requests against the locally running Spring Boot backend and MySQL database using the member test account, with no artificial network delay.
- Observed results: 0.706, 0.222, 0.194, 0.188 and 0.603 seconds.
- Average result: 0.383 seconds.
- Maximum result: 0.706 seconds.
- Outcome: Pass. All five requests completed within the 2-second target.

### WebSocket state restoration

- Acceptance target: Current state displayed within 2 seconds after reconnection
- Test conditions: Browser disconnected while a booking changed, then reconnected
- Observed result: 0.236 seconds
- Outcome: Pass

## Notes and defects

- Android system notifications are generated when the app retrieves new notification records. They are not remote push notifications and do not appear immediately while the app is completely closed.
- If Android notification permission is denied, the update remains available inside the notification screen.
- Messages remained safely queued while the notification consumer was unavailable and were processed after it restarted.
- During an earlier RabbitMQ broker outage, the publisher logged the failure but could not publish the event. That event was not automatically recovered. This remains a reliability limitation.
- The response-time measurements were taken on a local development environment and do not represent production network conditions.
- Running the complete system requires MySQL, RabbitMQ, the booking backend and the notification consumer.