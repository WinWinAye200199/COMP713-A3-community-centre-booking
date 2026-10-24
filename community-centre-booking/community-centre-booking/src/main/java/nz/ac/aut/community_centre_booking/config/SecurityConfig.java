package nz.ac.aut.community_centre_booking.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

// configures login and which endpoints need authentication
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // plain 401 without WWW-Authenticate header, so browsers do not show a login
        // popup
        AuthenticationEntryPoint unauthorized = (request, response, ex) -> response
                .sendError(HttpServletResponse.SC_UNAUTHORIZED);

        http
                // REST API with HTTP Basic does not use browser sessions, so CSRF is disabled
                .csrf(csrf -> csrf.disable())
                // credentials are sent with every request, so no server session is needed
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // web page files are public
                        .requestMatchers("/", "/index.html", "/*.js", "/*.css").permitAll()
                        // Allows the browser to establish the WebSocket connection.
                        .requestMatchers("/ws/**").permitAll()
                        // anyone can view rooms
                        .requestMatchers(HttpMethod.GET, "/api/rooms/**").permitAll()
                        // everything else needs login
                        .anyRequest().authenticated())
                // wrong username or password
                .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized))
                // no credentials sent
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized));

        return http.build();
    }

    // hashes passwords with BCrypt
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
