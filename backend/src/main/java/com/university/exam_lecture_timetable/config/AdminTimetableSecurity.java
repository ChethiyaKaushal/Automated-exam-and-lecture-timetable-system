package com.university.exam_lecture_timetable.config;

import com.university.exam_lecture_timetable.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AdminTimetableSecurity {

    @Bean
    SecurityFilterChain timetableSecurity(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(rules -> rules
                        .requestMatchers("/admin/**", "/api/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/timetablesetup/setup", "/api/timetablesetup/requirements")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                        .anyRequest().permitAll())
                .formLogin(form -> form.defaultSuccessUrl("/admin/timetable.html", false))
                .logout(Customizer.withDefaults())
                .build();
    }

    @Bean
    UserDetailsService timetableUsers(AppUserRepository users) {
        return username -> {
            var user = users.findWithRolesByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
            return User.withUsername(user.getUsername())
                    .password(user.getPasswordHash())
                    .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                    .accountLocked(Boolean.TRUE.equals(user.getLocked()))
                    .authorities(user.getRoles().stream().map(role -> role.getName()).toArray(String[]::new))
                    .build();
        };
    }

    @Bean
    PasswordEncoder timetablePasswords() {
        return new BCryptPasswordEncoder();
    }
}
