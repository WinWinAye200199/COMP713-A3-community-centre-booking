package nz.ac.aut.communitycentrebooking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import nz.ac.aut.communitycentrebooking.model.ApiError;
import nz.ac.aut.communitycentrebooking.model.BookingRequest;
import nz.ac.aut.communitycentrebooking.model.BookingResponse;
import nz.ac.aut.communitycentrebooking.model.Room;
import nz.ac.aut.communitycentrebooking.network.ApiClient;
import nz.ac.aut.communitycentrebooking.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingActivity extends AppCompatActivity {

    private EditText purposeInput;
    private Spinner roomSpinner;
    private Button dateButton;
    private Button startTimeButton;
    private Button endTimeButton;
    private Button createBookingButton;
    private ProgressBar bookingProgress;
    private TextView bookingMessage;

    private final List<Room> rooms = new ArrayList<>();

    private String selectedDate;
    private String selectedStartTime;
    private String selectedEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

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

        purposeInput = findViewById(R.id.purposeInput);
        roomSpinner = findViewById(R.id.roomSpinner);
        dateButton = findViewById(R.id.dateButton);
        startTimeButton = findViewById(R.id.startTimeButton);
        endTimeButton = findViewById(R.id.endTimeButton);
        createBookingButton = findViewById(R.id.createBookingButton);
        bookingProgress = findViewById(R.id.bookingProgress);
        bookingMessage = findViewById(R.id.bookingMessage);

        dateButton.setOnClickListener(view -> showDatePicker());

        startTimeButton.setOnClickListener(
                view -> showTimePicker(true)
        );

        endTimeButton.setOnClickListener(
                view -> showTimePicker(false)
        );

        createBookingButton.setOnClickListener(
                view -> createBooking()
        );

        loadRooms();
    }

    // Loads rooms into the room selection spinner.
    private void loadRooms() {
        setLoading(true);
        bookingMessage.setText("");

        ApiClient.getService()
                .getRooms()
                .enqueue(new Callback<List<Room>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<List<Room>> call,
                            @NonNull Response<List<Room>> response) {

                        setLoading(false);

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            bookingMessage.setText(
                                    "Rooms could not be loaded."
                            );
                            return;
                        }

                        rooms.clear();
                        rooms.addAll(response.body());

                        List<String> roomNames = new ArrayList<>();

                        for (Room room : rooms) {
                            roomNames.add(
                                    room.getName()
                                            + " — capacity "
                                            + room.getCapacity()
                            );
                        }

                        ArrayAdapter<String> adapter =
                                new ArrayAdapter<>(
                                        BookingActivity.this,
                                        android.R.layout.simple_spinner_item,
                                        roomNames
                                );

                        adapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item
                        );

                        roomSpinner.setAdapter(adapter);

                        if (rooms.isEmpty()) {
                            bookingMessage.setText(
                                    "There are currently no rooms."
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<Room>> call,
                            @NonNull Throwable throwable) {

                        setLoading(false);
                        bookingMessage.setText(
                                "Could not connect to the server."
                        );
                    }
                });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (datePicker, year, month, day) -> {
                    selectedDate = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            day
                    );

                    dateButton.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        Calendar minimumDate = Calendar.getInstance();
        minimumDate.set(Calendar.HOUR_OF_DAY, 0);
        minimumDate.set(Calendar.MINUTE, 0);
        minimumDate.set(Calendar.SECOND, 0);
        minimumDate.set(Calendar.MILLISECOND, 0);

        dialog.getDatePicker().setMinDate(
                minimumDate.getTimeInMillis()
        );

        dialog.show();
    }

    private void showTimePicker(boolean selectingStartTime) {
        Calendar calendar = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (timePicker, hour, minute) -> {
                    String selectedTime = String.format(
                            Locale.US,
                            "%02d:%02d",
                            hour,
                            minute
                    );

                    if (selectingStartTime) {
                        selectedStartTime = selectedTime;
                        startTimeButton.setText(selectedTime);
                    } else {
                        selectedEndTime = selectedTime;
                        endTimeButton.setText(selectedTime);
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );

        dialog.show();
    }

    // Validates the form and submits a pending booking.
    private void createBooking() {
        String purpose = purposeInput
                .getText()
                .toString()
                .trim();

        if (purpose.isEmpty()) {
            bookingMessage.setText(
                    "Enter a purpose for the booking."
            );
            return;
        }

        if (rooms.isEmpty()) {
            bookingMessage.setText(
                    "Select an available room."
            );
            return;
        }

        if (selectedDate == null
                || selectedStartTime == null
                || selectedEndTime == null) {

            bookingMessage.setText(
                    "Choose a date, start time and end time."
            );
            return;
        }

        if (!LocalTime.parse(selectedStartTime)
                .isBefore(LocalTime.parse(selectedEndTime))) {

            bookingMessage.setText(
                    "The start time must be before the end time."
            );
            return;
        }

        if (!SessionManager.isLoggedIn()) {
            bookingMessage.setText(
                    "Your login session has expired."
            );
            return;
        }

        Room selectedRoom = rooms.get(
                roomSpinner.getSelectedItemPosition()
        );

        BookingRequest request = new BookingRequest(
                purpose,
                selectedDate,
                selectedStartTime,
                selectedEndTime,
                selectedRoom.getId()
        );

        setLoading(true);
        bookingMessage.setText("");

        ApiClient.getService()
                .createBooking(
                        SessionManager.getAuthorization(),
                        request
                )
                .enqueue(new Callback<BookingResponse>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<BookingResponse> call,
                            @NonNull Response<BookingResponse> response) {

                        setLoading(false);

                        if (response.isSuccessful()
                                && response.body() != null) {

                            BookingResponse booking = response.body();

                            bookingMessage.setText(
                                    "Booking submitted successfully. "
                                            + "Status: "
                                            + booking.getStatus()
                            );

                            purposeInput.setText("");
                        } else {
                            bookingMessage.setText(
                                    readErrorMessage(response)
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<BookingResponse> call,
                            @NonNull Throwable throwable) {

                        setLoading(false);
                        bookingMessage.setText(
                                "Could not connect to the server."
                        );
                    }
                });
    }

    // Extracts the useful message from the backend's JSON error.
    private String readErrorMessage(
            Response<BookingResponse> response) {

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

        return "The booking could not be created.";
    }

    private void setLoading(boolean loading) {
        bookingProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        createBookingButton.setEnabled(!loading);
    }
}