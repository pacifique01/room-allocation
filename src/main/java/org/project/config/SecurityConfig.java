package org.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/css/**", "/images/**").permitAll()
                        .requestMatchers("/rooms/new", "/rooms/*/edit", "/rooms/*/delete").hasRole("ADMIN")
                        .requestMatchers("/applications", "/applications/*/confirm", "/applications/*/reject",
                                "/bookings/*/delete").hasRole("ADMIN")
                        .requestMatchers("/bookings/new")
                        .hasAnyRole("ADMIN", "LECTURER", "CLASS_REPRESENTATIVE", "CLUB_REPRESENTATIVE")
                        .requestMatchers("/bookings/save").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/rooms", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
