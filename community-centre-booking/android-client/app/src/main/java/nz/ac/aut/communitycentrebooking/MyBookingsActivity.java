package nz.ac.aut.communitycentrebooking;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

import java.util.List;

import nz.ac.aut.communitycentrebooking.model.ApiError;
import nz.ac.aut.communitycentrebooking.model.BookingResponse;
import nz.ac.aut.communitycentrebooking.network.ApiClient;
import nz.ac.aut.communitycentrebooking.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyBookingsActivity extends AppCompatActivity {

    private LinearLayout bookingsContainer;
    private ProgressBar bookingsProgress;
    private TextView bookingsMessage;
    private Button refreshBookingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        bookingsContainer = findViewById(R.id.bookingsContainer);
        bookingsProgress = findViewById(R.id.bookingsProgress);
        bookingsMessage = findViewById(R.id.bookingsMessage);
        refreshBookingsButton = findViewById(
                R.id.refreshBookingsButton
        );

        refreshBookingsButton.setOnClickListener(
                view -> loadBookings()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }

    // Retrieves bookings belonging to the logged-in member.
    private void loadBookings() {
        if (!SessionManager.isLoggedIn()) {
            bookingsMessage.setText(
                    "Your login session has expired."
            );
            return;
        }

        setLoading(true);
        bookingsMessage.setText("");
        bookingsContainer.removeAllViews();

        ApiClient.getService()
                .getMyBookings(
                        SessionManager.getAuthorization()
                )
                .enqueue(new Callback<List<BookingResponse>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<List<BookingResponse>> call,
                            @NonNull Response<List<BookingResponse>> response) {

                        setLoading(false);

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            bookingsMessage.setText(
                                    readErrorMessage(response)
                            );
                            return;
                        }

                        List<BookingResponse> bookings =
                                response.body();

                        if (bookings.isEmpty()) {
                            bookingsMessage.setText(
                                    "You do not have any bookings."
                            );
                            return;
                        }

                        for (BookingResponse booking : bookings) {
                            addBookingView(booking);
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<BookingResponse>> call,
                            @NonNull Throwable throwable) {

                        setLoading(false);
                        bookingsMessage.setText(
                                "Could not connect to the server."
                        );
                    }
                });
    }

    // Creates a card showing one booking and its current status.
    private void addBookingView(BookingResponse booking) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#F1F3F4"));
        background.setCornerRadius(dp(12));
        card.setBackground(background);

        LinearLayout.LayoutParams cardParameters =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParameters.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParameters);

        TextView purpose = new TextView(this);
        purpose.setText(booking.getPurpose());
        purpose.setTextSize(20);
        purpose.setTextColor(Color.BLACK);
        purpose.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        TextView room = new TextView(this);
        room.setText("Room: " + booking.getRoomName());
        room.setTextSize(16);
        room.setTextColor(Color.DKGRAY);

        TextView date = new TextView(this);
        date.setText("Date: " + booking.getBookingDate());
        date.setTextSize(16);
        date.setTextColor(Color.DKGRAY);

        TextView time = new TextView(this);
        time.setText(
                "Time: "
                        + formatTime(booking.getStartTime())
                        + "–"
                        + formatTime(booking.getEndTime())
        );
        time.setTextSize(16);
        time.setTextColor(Color.DKGRAY);

        TextView status = new TextView(this);
        status.setText("Status: " + booking.getStatus());
        status.setTextSize(16);
        status.setTextColor(Color.BLACK);
        status.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        card.addView(purpose);
        card.addView(room);
        card.addView(date);
        card.addView(time);
        card.addView(status);

        if ("PENDING".equals(booking.getStatus())
                || "APPROVED".equals(booking.getStatus())) {

            Button cancelButton = new Button(this);
            cancelButton.setText("Cancel booking");

            cancelButton.setOnClickListener(
                    view -> confirmCancellation(booking)
            );

            card.addView(cancelButton);
        }

        bookingsContainer.addView(card);
    }

    private void confirmCancellation(BookingResponse booking) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel booking")
                .setMessage(
                        "Are you sure you want to cancel this booking?"
                )
                .setNegativeButton("Keep booking", null)
                .setPositiveButton(
                        "Cancel booking",
                        (dialog, which) ->
                                cancelBooking(booking.getId())
                )
                .show();
    }

    private void cancelBooking(Long bookingId) {
        setLoading(true);

        ApiClient.getService()
                .cancelBooking(
                        SessionManager.getAuthorization(),
                        bookingId
                )
                .enqueue(new Callback<BookingResponse>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<BookingResponse> call,
                            @NonNull Response<BookingResponse> response) {

                        setLoading(false);

                        if (response.isSuccessful()) {
                            Toast.makeText(
                                    MyBookingsActivity.this,
                                    "Booking cancelled.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadBookings();
                        } else {
                            bookingsMessage.setText(
                                    readErrorMessage(response)
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<BookingResponse> call,
                            @NonNull Throwable throwable) {

                        setLoading(false);
                        bookingsMessage.setText(
                                "Could not connect to the server."
                        );
                    }
                });
    }

    private String readErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                ApiError error = new Gson().fromJson(
                        response.errorBody().string(),
                        ApiError.class
                );

                if (error != null && error.getMessage() != null) {
                    return error.getMessage();
                }
            }
        } catch (Exception ignored) {
            // A fallback message is returned below.
        }

        return "The request could not be completed.";
    }

    private String formatTime(String time) {
        if (time != null && time.length() >= 5) {
            return time.substring(0, 5);
        }

        return time;
    }

    private void setLoading(boolean loading) {
        bookingsProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        refreshBookingsButton.setEnabled(!loading);
    }

    private int dp(int value) {
        return (int) (
                value * getResources().getDisplayMetrics().density
        );
    }
}