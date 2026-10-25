const loginSection = document.getElementById("login-section");
const dashboardSection = document.getElementById(
    "dashboard-section"
);

const loginForm = document.getElementById("login-form");
const loginButton = document.getElementById("login-button");
const loginMessage = document.getElementById("login-message");

const staffDetails = document.getElementById("staff-details");
const dashboardMessage = document.getElementById(
    "dashboard-message"
);
const bookingsList = document.getElementById("bookings-list");

const showPendingButton = document.getElementById(
    "show-pending-button"
);
const showAllButton = document.getElementById("show-all-button");
const refreshButton = document.getElementById("refresh-button");
const logoutButton = document.getElementById("logout-button");

let authorization = null;
let currentFilter = "PENDING";
let bookingSocket = null;
let reconnectTimer = null;
let shouldReconnect = false;

loginForm.addEventListener("submit", login);
showPendingButton.addEventListener("click", showPending);
showAllButton.addEventListener("click", showAll);
refreshButton.addEventListener("click", loadBookings);
logoutButton.addEventListener("click", logout);

// Verifies the credentials and confirms the account has the staff role.
async function login(event) {
    event.preventDefault();

    loginButton.disabled = true;
    loginMessage.textContent = "Logging in...";

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    const credentials = `${email}:${password}`;
    const attemptedAuthorization = `Basic ${btoa(credentials)}`;

    try {
        const response = await fetch("/api/auth/me", {
            headers: {
                Authorization: attemptedAuthorization
            }
        });

        if (!response.ok) {
            loginMessage.textContent =
                response.status === 401
                    ? "Incorrect email or password."
                    : await readErrorMessage(
                        response,
                        "Login failed."
                    );

            return;
        }

        const user = await response.json();

        if (user.role !== "STAFF") {
            loginMessage.textContent =
                "This dashboard is only available to staff.";
            return;
        }

        authorization = attemptedAuthorization;

        staffDetails.textContent =
            `Logged in as ${user.name} (${user.email})`;

        loginMessage.textContent = "";
        loginSection.hidden = true;
        dashboardSection.hidden = false;

        await loadBookings();
        connectBookingWebSocket();
    } catch (error) {
        loginMessage.textContent =
            "Could not connect to the server.";
    } finally {
        loginButton.disabled = false;
    }
}

async function showPending() {
    currentFilter = "PENDING";
    await loadBookings();
}

async function showAll() {
    currentFilter = null;
    await loadBookings();
}

// Retrieves bookings using the logged-in staff credentials.
async function loadBookings() {
    if (!authorization) {
        return;
    }

    bookingsList.textContent = "";
    dashboardMessage.textContent = "Loading bookings...";

    const url = currentFilter
        ? `/api/bookings?status=${currentFilter}`
        : "/api/bookings";

    try {
        const response = await fetch(url, {
            headers: {
                Authorization: authorization
            }
        });

        if (response.status === 401) {
            logout();
            loginMessage.textContent =
                "Your login session has expired.";
            return;
        }

        if (!response.ok) {
            dashboardMessage.textContent =
                await readErrorMessage(
                    response,
                    "Bookings could not be loaded."
                );

            return;
        }

        const bookings = await response.json();

        dashboardMessage.textContent = currentFilter
            ? "Showing pending bookings."
            : "Showing all bookings.";

        if (bookings.length === 0) {
            const message = document.createElement("p");
            message.textContent = currentFilter
                ? "There are no pending bookings."
                : "There are currently no bookings.";

            bookingsList.appendChild(message);
            return;
        }

        for (const booking of bookings) {
            bookingsList.appendChild(
                createBookingCard(booking)
            );
        }
    } catch (error) {
        dashboardMessage.textContent =
            "Could not connect to the server.";
    }
}

// Creates one staff booking card from API response data.
function createBookingCard(booking) {
    const card = document.createElement("article");

    const heading = document.createElement("h3");
    heading.textContent = booking.purpose;

    const room = document.createElement("p");
    room.textContent = `Room: ${booking.roomName}`;

    const member = document.createElement("p");
    member.textContent =
        `Member: ${booking.memberName} (${booking.memberEmail})`;

    const date = document.createElement("p");
    date.textContent = `Date: ${booking.bookingDate}`;

    const time = document.createElement("p");
    time.textContent =
        `Time: ${formatTime(booking.startTime)}–` +
        `${formatTime(booking.endTime)}`;

    const status = document.createElement("p");
    status.textContent = `Status: ${booking.status}`;

    card.appendChild(heading);
    card.appendChild(room);
    card.appendChild(member);
    card.appendChild(date);
    card.appendChild(time);
    card.appendChild(status);

    if (booking.status === "PENDING") {
        const approveButton = document.createElement("button");
        approveButton.type = "button";
        approveButton.textContent = "Approve";

        approveButton.addEventListener("click", () => {
            updateBookingStatus(booking.id, "APPROVED");
        });

        const rejectButton = document.createElement("button");
        rejectButton.type = "button";
        rejectButton.textContent = "Reject";

        rejectButton.addEventListener("click", () => {
            updateBookingStatus(booking.id, "REJECTED");
        });

        card.appendChild(approveButton);
        card.appendChild(rejectButton);
    }

    return card;
}

// Approves or rejects a pending booking.
async function updateBookingStatus(bookingId, newStatus) {
    dashboardMessage.textContent =
        `Updating booking ${bookingId}...`;

    try {
        const response = await fetch(
            `/api/bookings/${bookingId}/status`,
            {
                method: "PATCH",
                headers: {
                    Authorization: authorization,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    status: newStatus
                })
            }
        );

        if (!response.ok) {
            dashboardMessage.textContent =
                await readErrorMessage(
                    response,
                    "The booking could not be updated."
                );

            return;
        }

        await loadBookings();

        dashboardMessage.textContent =
            `Booking ${bookingId} changed to ${newStatus}.`;
    } catch (error) {
        dashboardMessage.textContent =
            "Could not connect to the server.";
    }
}

function logout() {
    disconnectBookingWebSocket();
    authorization = null;
    currentFilter = "PENDING";

    document.getElementById("password").value = "";

    bookingsList.textContent = "";
    staffDetails.textContent = "";
    dashboardMessage.textContent = "";

    dashboardSection.hidden = true;
    loginSection.hidden = false;
}

function formatTime(time) {
    if (time && time.length >= 5) {
        return time.substring(0, 5);
    }

    return time;
}

async function readErrorMessage(response, fallback) {
    try {
        const body = await response.json();
        return body.message || fallback;
    } catch (error) {
        return fallback;
    }
}

// Opens the live booking-update connection for the staff dashboard.
function connectBookingWebSocket() {
    if (
        bookingSocket !== null &&
        (
            bookingSocket.readyState === WebSocket.OPEN ||
            bookingSocket.readyState === WebSocket.CONNECTING
        )
    ) {
        return;
    }

    shouldReconnect = true;

    const protocol =
        window.location.protocol === "https:" ? "wss" : "ws";

    bookingSocket = new WebSocket(
        `${protocol}://${window.location.host}/ws/bookings`
    );

    bookingSocket.addEventListener("open", async () => {
        console.log("Booking WebSocket connected");

        if (reconnectTimer !== null) {
            clearTimeout(reconnectTimer);
            reconnectTimer = null;
        }

        // Restores the current state, including changes missed while disconnected.
        await loadBookings();
    });

    bookingSocket.addEventListener("message", async (event) => {
        try {
            const message = JSON.parse(event.data);

            if (message.type === "BOOKING_CHANGED") {
                await loadBookings();
            }
        } catch (error) {
            console.error("Invalid WebSocket message:", error);
        }
    });

    bookingSocket.addEventListener("close", () => {
        bookingSocket = null;

        if (shouldReconnect && reconnectTimer === null) {
            reconnectTimer = window.setTimeout(() => {
                reconnectTimer = null;
                connectBookingWebSocket();
            }, 2000);
        }
    });

    bookingSocket.addEventListener("error", () => {
        console.error("Booking WebSocket connection failed");
    });
}

// Stops reconnecting when the staff member logs out.
function disconnectBookingWebSocket() {
    shouldReconnect = false;

    if (reconnectTimer !== null) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    }

    if (bookingSocket !== null) {
        bookingSocket.close();
        bookingSocket = null;
    }
}