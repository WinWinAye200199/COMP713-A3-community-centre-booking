package nz.ac.aut.communitycentrebooking;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

import nz.ac.aut.communitycentrebooking.model.NotificationResponse;
import nz.ac.aut.communitycentrebooking.network.ApiClient;
import nz.ac.aut.communitycentrebooking.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationsActivity extends AppCompatActivity {

    private static final String CHANNEL_ID =
            "booking_updates";

    private static final int NOTIFICATION_PERMISSION_REQUEST =
            100;

    private LinearLayout notificationsContainer;
    private ProgressBar notificationsProgress;
    private TextView notificationsMessage;
    private Button refreshNotificationsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        createNotificationChannel();
        requestNotificationPermission();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left + dp(20),
                            systemBars.top + dp(20),
                            systemBars.right + dp(20),
                            systemBars.bottom + dp(20)
                    );

                    return insets;
                }
        );

        notificationsContainer =
                findViewById(R.id.notificationsContainer);

        notificationsProgress =
                findViewById(R.id.notificationsProgress);

        notificationsMessage =
                findViewById(R.id.notificationsMessage);

        refreshNotificationsButton =
                findViewById(R.id.refreshNotificationsButton);

        refreshNotificationsButton.setOnClickListener(
                view -> loadNotifications()
        );

        loadNotifications();
    }

    // Retrieves notifications belonging to the logged-in member.
    private void loadNotifications() {
        setLoading(true);
        notificationsMessage.setText("");
        notificationsContainer.removeAllViews();

        ApiClient.getService()
                .getMyNotifications(
                        SessionManager.getAuthorization()
                )
                .enqueue(new Callback<List<NotificationResponse>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<List<NotificationResponse>> call,
                            @NonNull Response<List<NotificationResponse>> response) {

                        setLoading(false);

                        if (response.code() == 401
                                || response.code() == 403) {

                            notificationsMessage.setText(
                                    "You are not authorised to view notifications."
                            );
                            return;
                        }

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            notificationsMessage.setText(
                                    "Notifications could not be loaded."
                            );
                            return;
                        }

                        List<NotificationResponse> notifications =
                                response.body();

                        if (notifications.isEmpty()) {
                            notificationsMessage.setText(
                                    "You do not have any notifications."
                            );
                            return;
                        }

                        showSystemNotificationIfNew(
                                notifications.get(0)
                        );

                        for (NotificationResponse notification
                                : notifications) {

                            addNotificationView(notification);
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<NotificationResponse>> call,
                            @NonNull Throwable throwable) {

                        setLoading(false);

                        notificationsMessage.setText(
                                "Could not connect to the server."
                        );
                    }
                });
    }

    // Creates one card for a notification returned by the API.
    private void addNotificationView(
            NotificationResponse notification) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#F1F3F4"));
        background.setCornerRadius(dp(12));
        card.setBackground(background);

        LinearLayout.LayoutParams parameters =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        parameters.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(parameters);

        TextView message = new TextView(this);
        message.setText(notification.getMessage());
        message.setTextSize(17);
        message.setTextColor(Color.BLACK);
        message.setTypeface(null, Typeface.BOLD);

        TextView booking = new TextView(this);
        booking.setText(
                "Booking: " + notification.getBookingId()
        );
        booking.setTextSize(15);
        booking.setTextColor(Color.DKGRAY);

        TextView createdAt = new TextView(this);
        createdAt.setText(
                formatCreatedAt(notification.getCreatedAt())
        );
        createdAt.setTextSize(14);
        createdAt.setTextColor(Color.DKGRAY);

        card.addView(message);
        card.addView(booking);
        card.addView(createdAt);

        notificationsContainer.addView(card);
    }

    private String formatCreatedAt(String createdAt) {
        if (createdAt == null) {
            return "";
        }

        return createdAt
                .replace("T", " ")
                .replace("Z", " UTC");
    }

    private void setLoading(boolean loading) {
        notificationsProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        refreshNotificationsButton.setEnabled(!loading);
    }

    // Displays the newest backend notification once on the Android device.
    private void showSystemNotificationIfNew(
            NotificationResponse notification) {

        if (notification.getId() == null) {
            return;
        }

        if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
        ) {
            return;
        }

        SharedPreferences preferences =
                getSharedPreferences(
                        "notification_preferences",
                        MODE_PRIVATE
                );

        long lastNotificationId =
                preferences.getLong(
                        "last_notification_id",
                        0L
                );

        if (notification.getId() <= lastNotificationId) {
            return;
        }

        Intent intent = new Intent(
                this,
                NotificationsActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                R.drawable.ic_launcher_foreground
                        )
                        .setContentTitle("Booking update")
                        .setContentText(notification.getMessage())
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(
                notification.getId().intValue(),
                builder.build()
        );

        preferences.edit()
                .putLong(
                        "last_notification_id",
                        notification.getId()
                )
                .apply();
    }

    // Creates the Android channel used for booking-status notifications.
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Booking updates",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "Updates when a community centre booking changes status."
            );

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);

            notificationManager.createNotificationChannel(channel);
        }
    }

    // Android 13 and newer require notification permission at runtime.
    private void requestNotificationPermission() {
        if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    NOTIFICATION_PERMISSION_REQUEST
            );
        }
    }

    private int dp(int value) {
        return (int) (
                value *
                        getResources().getDisplayMetrics().density
        );
    }
}