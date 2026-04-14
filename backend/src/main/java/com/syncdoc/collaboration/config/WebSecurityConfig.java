package com.syncdoc.collaboration.config;

import com.syncdoc.collaboration.tenancy.security.HeaderAuthenticationFilter;
import com.syncdoc.collaboration.tenancy.security.WorkspaceMembershipFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;
    private final WorkspaceMembershipFilter workspaceMembershipFilter;

    public WebSecurityConfig(
        HeaderAuthenticationFilter headerAuthenticationFilter,
        WorkspaceMembershipFilter workspaceMembershipFilter
    ) {
        this.headerAuthenticationFilter = headerAuthenticationFilter;
        this.workspaceMembershipFilter = workspaceMembershipFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/v1/workspaces/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(headerAuthenticationFilter, AnonymousAuthenticationFilter.class)
            .addFilterAfter(workspaceMembershipFilter, HeaderAuthenticationFilter.class);

        return http.build();
    }
}