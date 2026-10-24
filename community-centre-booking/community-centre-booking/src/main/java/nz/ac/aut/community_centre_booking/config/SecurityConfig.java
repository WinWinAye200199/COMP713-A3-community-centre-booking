package nz.ac.aut.community_centre_booking.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// configures login and which endpoints need authentication
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API with HTTP Basic does not use browser sessions, so CSRF is disabled
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // web page files are public
                        .requestMatchers("/", "/index.html", "/*.js", "/*.css").permitAll()
                        // anyone can view rooms
                        .requestMatchers(HttpMethod.GET, "/api/rooms/**").permitAll()
                        // everything else needs login
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                // return plain 401 so the browser does not show a login popup
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)));

        return http.build();
    }

    // hashes passwords with BCrypt
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}