


package com.university.exam_lecture_timetable.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AdminTimetableSecurity {

    @Bean
    @Order(1)
    SecurityFilterChain timetableSecurity(HttpSecurity http,
                                          DaoAuthenticationProvider authenticationProvider) throws Exception {
        return http
                .securityMatcher("/admin/**", "/api/admin/**", "/login", "/logout")
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers("/admin/**", "/api/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                        .anyRequest().permitAll())
                .authenticationProvider(authenticationProvider)
                .formLogin(form -> form.defaultSuccessUrl("/admin/timetable.html", false))
                .logout(Customizer.withDefaults())
                .build();
    }
}