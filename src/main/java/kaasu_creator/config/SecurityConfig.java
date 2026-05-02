package kaasu_creator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import kaasu_creator.dao.UserDao;

/**
 * SecurityConfig - Spring Security configuration.
 *
 * Key concepts:
 *
 * 1. SecurityFilterChain: A chain of filters that intercept every HTTP request.
 *    Each filter can allow, reject, or modify the request. Spring Security
 *    adds filters for authentication and authorization automatically.
 *
 * 2. authorizeHttpRequests: Defines which URLs require authentication.
 *    - permitAll(): Anyone can access these without logging in
 *    - authenticated(): Must be logged in to access
 *
 * 3. formLogin: Tells Spring Security to handle login automatically.
 *    - loginPage("/login"): Use our custom login page
 *    - defaultSuccessUrl("/dashboard"): Go here after successful login
 *    - We DON'T need to write login logic ourselves - Spring Security handles it!
 *
 * 4. UserDetailsService: An interface that Spring Security uses to load
 *    user data during authentication. We implement it using our UserDao.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // authorization rules - who can access what without logging in
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()  // all other pages require login
            )
            // Configure form-based login (Spring Security handles this automatically)
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")    // form posts to this URL
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            // Configure logout — AntPathRequestMatcher without a method accepts GET and POST,
            // so plain <a href="/logout"> links work without changing every template.
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            // CSRF enabled — Thymeleaf th:action forms auto-include the token
            ;

        return http.build();
    }

    /**
     * UserDetailsService - Spring Security uses this to load user data during login.
     *
     * How it works:
     * 1. User submits login form with email (username) and password
     * 2. Spring Security calls loadUserByUsername(email)
     * 3. We look up the user in our database via UserDao
     * 4. If found, we return a Spring Security User object with the BCrypt hash
     * 5. Spring Security compares the submitted password with the stored hash
     * 6. If they match, the user is "authenticated"
     */
    @Bean
    public UserDetailsService userDetailsService(UserDao userDao) {
        return username -> {
            kaasu_creator.model.User user = userDao.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            // Build a Spring Security User object
            return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles("USER")
                .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}