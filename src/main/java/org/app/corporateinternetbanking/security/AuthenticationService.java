package org.app.corporateinternetbanking.security;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.dto.authDto.LoginRequest;
import org.app.corporateinternetbanking.user.dto.authDto.LoginResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public LoginResponse Authenticate(LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());
        log.info("Authenticating user={}", usernamePasswordAuthenticationToken);

        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        log.info("User authenticated={}", authentication);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Optional<User> user = userRepository.findByEmail(loginRequest.getEmail());
        if (user.isEmpty()) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String token = jwtService.generateToken(user.get());
        return new LoginResponse(user.get(), token);
    }
}
