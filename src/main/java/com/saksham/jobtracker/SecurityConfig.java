package com.saksham.jobtracker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(JdbcTemplate jdbcTemplate) {
        return username -> {
            var users = jdbcTemplate.query(
                    """
                    SELECT username, password_hash
                    FROM users
                    WHERE username = ?
                    """,
                    (resultSet, rowNumber) -> (UserDetails) User
                            .withUsername(resultSet.getString("username"))
                            .password(resultSet.getString("password_hash"))
                            .roles("USER")
                            .build(),
                    username
            );

            if (users.isEmpty()) {
                throw new UsernameNotFoundException("User not found");
            }

            return users.getFirst();
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login.html",
                                "/register.html",
                                "/security.js",
                                "/api/auth/register",
                                "/api/auth/csrf",
                                "/error",
								"/manifest.json",
                                "/pwa.js",
                                "/sw.js",
                                "/icons/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login.html?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login.html?logout")
                        .permitAll()
                )
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor(
                                (request, response, exception) ->
                                        response.sendError(401),
                                request -> request.getServletPath()
                                        .startsWith("/api/")
                        )
                );

        return http.build();
    }
}