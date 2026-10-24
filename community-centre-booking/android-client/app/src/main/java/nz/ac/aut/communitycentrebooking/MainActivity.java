package nz.ac.aut.communitycentrebooking;

import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.nio.charset.StandardCharsets;

import nz.ac.aut.communitycentrebooking.model.UserResponse;
import nz.ac.aut.communitycentrebooking.network.ApiClient;
import android.content.Intent;
import nz.ac.aut.communitycentrebooking.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private ProgressBar loginProgress;
    private TextView loginMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        loginProgress = findViewById(R.id.loginProgress);
        loginMessage = findViewById(R.id.loginMessage);

        loginButton.setOnClickListener(view -> login());
    }

    // Checks the entered credentials with the Spring Boot API.
    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            loginMessage.setText("Enter your email and password.");
            return;
        }

        setLoading(true);
        loginMessage.setText("");

        String credentials = email + ":" + password;

        String authorization = "Basic " + Base64.encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8),
                Base64.NO_WRAP
        );

        ApiClient.getService()
                .getCurrentUser(authorization)
                .enqueue(new Callback<UserResponse>() {
                    @Override
                    public void onResponse(
                            Call<UserResponse> call,
                            Response<UserResponse> response) {

                        setLoading(false);

                        if (response.isSuccessful()
                                && response.body() != null) {

                            UserResponse user = response.body();

                            if (!"MEMBER".equals(user.getRole())) {
                                loginMessage.setText(
                                        "This Android app is for members."
                                );
                                return;
                            }

                            SessionManager.startSession(authorization, user);

                            Intent intent = new Intent(
                                    MainActivity.this,
                                    MemberHomeActivity.class
                            );

                            startActivity(intent);
                            finish();

                        } else if (response.code() == 401) {
                            loginMessage.setText(
                                    "Incorrect email or password."
                            );
                        } else {
                            loginMessage.setText(
                                    "Login failed. Server returned "
                                            + response.code() + "."
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<UserResponse> call,
                            Throwable throwable) {

                        setLoading(false);
                        loginMessage.setText(
                                "Could not connect to the server."
                        );
                    }
                });
    }

    private void setLoading(boolean loading) {
        loginProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        loginButton.setEnabled(!loading);
    }
}