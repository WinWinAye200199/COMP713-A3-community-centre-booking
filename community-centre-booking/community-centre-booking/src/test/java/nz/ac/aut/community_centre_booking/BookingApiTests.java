package nz.ac.aut.community_centre_booking;

import com.jayway.jsonpath.JsonPath;
import nz.ac.aut.community_centre_booking.event.BookingStatusChangedEvent;
import nz.ac.aut.community_centre_booking.messaging.RabbitConfig;
import nz.ac.aut.community_centre_booking.model.BookingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// API tests against an in-memory H2 database; RabbitMQ is replaced by a mock.
// Each test uses its own date so tests do not affect each other.
@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    // ---------- helpers ----------

    private RequestPostProcessor member() {
        return httpBasic("member@test.com", "member123");
    }

    private RequestPostProcessor member2() {
        return httpBasic("member2@test.com", "member123");
    }

    private RequestPostProcessor staff() {
        return httpBasic("staff@test.com", "staff123");
    }

    private LocalDate day(int daysFromToday) {
        return LocalDate.now().plusDays(daysFromToday);
    }

    private String bookingJson(LocalDate date, String start, String end, long roomId) {
        return """
                {"purpose":"Test booking","bookingDate":"%s","startTime":"%s","endTime":"%s","roomId":%d}
                """.formatted(date, start, end, roomId);
    }

    private long createBooking(RequestPostProcessor user, LocalDate date, String start, String end, long roomId)
            throws Exception {
        String body = mockMvc.perform(post("/api/bookings").with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(date, start, end, roomId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String statusJson(String status) {
        return "{\"status\":\"" + status + "\"}";
    }

    // ---------- retained Assessment 2 behaviour ----------

    @Test
    void roomsArePublic() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void validBookingIsCreatedAsPending() throws Exception {
        mockMvc.perform(post("/api/bookings").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(day(10), "10:00", "11:00", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.memberEmail").value("member@test.com"));
    }

    @Test
    void overlappingBookingIsRejectedWith409() throws Exception {
        createBooking(member(), day(11), "10:00", "11:00", 1);

        mockMvc.perform(post("/api/bookings").with(member2())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(day(11), "10:30", "11:30", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_CONFLICT"));
    }

    @Test
    void endTimeBeforeStartTimeIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/bookings").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(day(12), "12:00", "11:00", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void unknownRoomReturns404() throws Exception {
        mockMvc.perform(post("/api/bookings").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(day(13), "10:00", "11:00", 999)))
                .andExpect(status().isNotFound());
    }

    // ---------- authentication and roles ----------

    @Test
    void wrongPasswordReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(httpBasic("member@test.com", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCannotApproveBooking() throws Exception {
        long id = createBooking(member(), day(14), "10:00", "11:00", 2);

        mockMvc.perform(patch("/api/bookings/{id}/status", id).with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("APPROVED")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidStatusValueReturns400() throws Exception {
        long id = createBooking(member(), day(15), "10:00", "11:00", 2);

        mockMvc.perform(patch("/api/bookings/{id}/status", id).with(staff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("DONE")))
                .andExpect(status().isBadRequest());
    }

    // ---------- workflow 1: member books, staff approves, member sees the decision ----------

    @Test
    void staffApprovalIsVisibleToMemberAndPublishesEvent() throws Exception {
        long id = createBooking(member(), day(16), "10:00", "11:00", 3);

        mockMvc.perform(patch("/api/bookings/{id}/status", id).with(staff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("APPROVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/bookings/my").with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].status").value(hasItem("APPROVED")));

        // event sent to RabbitMQ after commit
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE),
                eq(RabbitConfig.ROUTING_KEY),
                argThat((Object event) -> event instanceof BookingStatusChangedEvent e
                        && e.bookingId() == id
                        && e.newStatus() == BookingStatus.APPROVED));
    }

    @Test
    void approvingTwiceReturns409() throws Exception {
        long id = createBooking(member(), day(17), "10:00", "11:00", 3);

        mockMvc.perform(patch("/api/bookings/{id}/status", id).with(staff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("APPROVED")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/bookings/{id}/status", id).with(staff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("APPROVED")))
                .andExpect(status().isConflict());
    }

    // ---------- workflow 2: member cancels, slot becomes available to another member ----------

    @Test
    void memberCannotCancelAnotherMembersBooking() throws Exception {
        long id = createBooking(member(), day(18), "10:00", "11:00", 1);

        mockMvc.perform(patch("/api/bookings/{id}/cancel", id).with(member2()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancellingFreesTheSlotForAnotherMember() throws Exception {
        long id = createBooking(member(), day(19), "10:00", "11:00", 1);

        // slot is taken
        mockMvc.perform(post("/api/bookings").with(member2())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(day(19), "10:00", "11:00", 1)))
                .andExpect(status().isConflict());

        // owner cancels
        mockMvc.perform(patch("/api/bookings/{id}/cancel", id).with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // another member can now book the same slot
        mockMvc.perform(post("/api/bookings").with(member2())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(day(19), "10:00", "11:00", 1)))
                .andExpect(status().isCreated());
    }

    // ---------- notifications ----------

    @Test
    void memberCanReadNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications/my").with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
