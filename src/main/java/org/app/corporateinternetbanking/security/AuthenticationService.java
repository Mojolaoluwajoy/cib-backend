package org.app.corporateinternetbanking.security;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.security.session.SessionService;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.domain.repository.UserRepository;
import org.app.corporateinternetbanking.user.dto.authDto.LoginRequest;
import org.app.corporateinternetbanking.user.dto.authDto.LoginResponse;
import org.app.corporateinternetbanking.user.exceptions.TooManyLoginAttempts;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final LoginThrottle loginThrottle;
    private final SessionService sessionService;

    public LoginResponse Authenticate(LoginRequest loginRequest, String clientIp) throws TooManyLoginAttempts {
        String email = loginRequest.getEmail();
        Optional<Duration> blocked = loginThrottle.blockedFor(email, clientIp);
        if (blocked.isPresent()) {
            log.warn("Login blocked for email={} ip={}", email, clientIp);
            throw new TooManyLoginAttempts(blocked.get());
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword()));
        } catch (AuthenticationException e) {
            loginThrottle.recordFailure(email, clientIp);
            throw e;
        }
        loginThrottle.recordSuccess(email);

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        log.info("User {} logged in", user.getId());
        return sessionService.start(user);
    }
}
