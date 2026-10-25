package nz.ac.aut.communitycentrebooking;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

import nz.ac.aut.communitycentrebooking.model.Room;
import nz.ac.aut.communitycentrebooking.network.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MemberHomeActivity extends AppCompatActivity {

    private LinearLayout roomsContainer;
    private ProgressBar roomsProgress;
    private TextView roomsMessage;
    private Button refreshRoomsButton;
    private Button createBookingButton;
    private Button myBookingsButton;
    private Button notificationsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_home);

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

        roomsContainer = findViewById(R.id.roomsContainer);
        roomsProgress = findViewById(R.id.roomsProgress);
        roomsMessage = findViewById(R.id.roomsMessage);
        refreshRoomsButton = findViewById(R.id.refreshRoomsButton);
        createBookingButton = findViewById(R.id.createBookingButton);
        myBookingsButton = findViewById(R.id.myBookingsButton);
        notificationsButton = findViewById(R.id.notificationsButton);

        notificationsButton.setOnClickListener(view -> {
            Intent intent = new Intent(
                    MemberHomeActivity.this,
                    NotificationsActivity.class
            );

            startActivity(intent);
        });

        refreshRoomsButton.setOnClickListener(view -> loadRooms());

        createBookingButton.setOnClickListener(
                view -> {
                    Intent intent = new Intent(
                            MemberHomeActivity.this,
                            BookingActivity.class
                    );

                    startActivity(intent);
                }
        );

        myBookingsButton.setOnClickListener(
                view -> {
                    Intent intent = new Intent(
                            MemberHomeActivity.this,
                            MyBookingsActivity.class
                    );

                    startActivity(intent);
                }
        );

        loadRooms();
    }

    // Retrieves the current room list from the Spring Boot API.
    private void loadRooms() {
        setLoading(true);
        roomsMessage.setText("");
        roomsContainer.removeAllViews();

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

                            roomsMessage.setText(
                                    "Rooms could not be loaded."
                            );
                            return;
                        }

                        List<Room> rooms = response.body();

                        if (rooms.isEmpty()) {
                            roomsMessage.setText(
                                    "There are currently no rooms."
                            );
                            return;
                        }

                        for (Room room : rooms) {
                            addRoomView(room);
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<Room>> call,
                            @NonNull Throwable throwable) {

                        setLoading(false);
                        roomsMessage.setText(
                                "Could not connect to the server."
                        );
                    }
                });
    }

    // Creates a simple card for one room returned by the API.
    private void addRoomView(Room room) {
        LinearLayout roomCard = new LinearLayout(this);
        roomCard.setOrientation(LinearLayout.VERTICAL);
        roomCard.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#F1F3F4"));
        background.setCornerRadius(dp(12));
        roomCard.setBackground(background);

        LinearLayout.LayoutParams cardParameters =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParameters.setMargins(0, 0, 0, dp(12));
        roomCard.setLayoutParams(cardParameters);

        TextView roomName = new TextView(this);
        roomName.setText(room.getName());
        roomName.setTextSize(20);
        roomName.setTextColor(Color.BLACK);
        roomName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView roomCapacity = new TextView(this);
        roomCapacity.setText("Capacity: " + room.getCapacity());
        roomCapacity.setTextSize(16);
        roomCapacity.setTextColor(Color.DKGRAY);

        TextView roomDescription = new TextView(this);
        roomDescription.setText(room.getDescription());
        roomDescription.setTextSize(16);
        roomDescription.setTextColor(Color.DKGRAY);

        roomCard.addView(roomName);
        roomCard.addView(roomCapacity);
        roomCard.addView(roomDescription);

        roomsContainer.addView(roomCard);
    }

    private void setLoading(boolean loading) {
        roomsProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        refreshRoomsButton.setEnabled(!loading);
    }

    private int dp(int value) {
        return (int) (
                value * getResources().getDisplayMetrics().density
        );
    }
}