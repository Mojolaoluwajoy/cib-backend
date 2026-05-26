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
                "/users/create", "/organizations/create",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/v3/api-docs",
                "/webjars/**",
                "/swagger-resources/**",
                "/external/transaction/fund",
                "/webhook/paystack",};
        httpSecurity.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())  // ← add this line
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // ← add this as first rule
                        .requestMatchers(publicEndPoints).permitAll()
                        // ... rest of your existing rules
                        // .requestMatchers(publicEndPoints)
                        // .permitAll()
                        .requestMatchers("/external/transaction/payout/").hasRole(UserRole.MAKER.name())
                        .requestMatchers("/auth/password/reset").authenticated()
                        .requestMatchers("/users/invitation/**").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/organizations/findBy", "/organizations/viewAll").hasAnyRole(
                                UserRole.SUPER_ADMIN.name(), UserRole.ADMIN.name())
                        .requestMatchers("/organizations/approve/**").hasRole(UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/organizations/profile").hasRole(UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/accounts/create/**").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/accounts/organization/**").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/transactions/initiate").hasRole(
                                UserRole.MAKER.name())
                        .requestMatchers("/transactions/approve").hasRole(
                                UserRole.APPROVER.name())
                        .requestMatchers("/transactions/pending").hasAnyRole(
                                UserRole.APPROVER.name(), UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/users/profile").authenticated()
                        .requestMatchers("/users/profile").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name()).requestMatchers("/users/profile").hasAnyRole(
                                UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/currencies/status/**").hasRole(UserRole.SUPER_ADMIN.name())
                        .requestMatchers("/dashboard/stats").authenticated()
                        .requestMatchers("/currencies/all", "/currencies/status/**").hasAnyRole(
                                UserRole.ADMIN.name(),
                                UserRole.SUPER_ADMIN.name()
                        )
                        .anyRequest().authenticated()).sessionManagement(session -> session
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
