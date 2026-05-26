package org.app.corporateinternetbanking.security;

import lombok.RequiredArgsConstructor;
import org.app.corporateinternetbanking.user.enums.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthFilter jwtAuthFilter;

    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        String[] publicEndPoints = new String[]{
                "/auth/login",
                "/auth/password/token",
                "/auth/forgotten/password",
                "/users/create",
                "/organizations/create",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/v3/api-docs",
                "/webjars/**",
                "/swagger-resources/**",
                "/external/fund",
                "/webhook/paystack",
        };

        httpSecurity
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(publicEndPoints).permitAll()

                        .requestMatchers("/auth/password/reset").authenticated()

                        .requestMatchers("/users/invitation").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/users/viewAll").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/users/users").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/users/profile").authenticated()

                        .requestMatchers("/organizations/findBy").hasAnyRole(
                                UserRole.SUPER_ADMIN.name(), UserRole.ADMIN.name())
                        .requestMatchers("/organizations/viewAll").hasAnyRole(
                                UserRole.SUPER_ADMIN.name(), UserRole.ADMIN.name())
                        .requestMatchers("/organizations/approve").hasRole(
                                UserRole.SUPER_ADMIN.name())

                        .requestMatchers("/accounts/create").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/accounts/all").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/accounts/find").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())

                        .requestMatchers("/transactions/initiate").hasAnyRole(
                                UserRole.MAKER.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/transactions/approve").hasAnyRole(
                                UserRole.APPROVER.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/transactions/pending").hasAnyRole(
                                UserRole.APPROVER.name(), UserRole.ADMIN.name(),
                                UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/transactions/transactions").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())

                        .requestMatchers("/external/payout").hasAnyRole(
                                UserRole.MAKER.name(), UserRole.SUPER_ADMIN.name())

                        .requestMatchers("/currencies/status").hasRole(
                                UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/currencies/all").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())

                        .requestMatchers("/dashboard/stats").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
