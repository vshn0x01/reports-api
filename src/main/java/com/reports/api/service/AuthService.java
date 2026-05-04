package com.reports.api.service;

import com.reports.api.dto.UserDetailsAssignmentResponse;
import com.reports.api.dto.UserDetailsResponse;
import com.reports.api.model.User;
import com.reports.api.repository.UserRepository;
import com.reports.api.security.JwtService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {
    private static final String USER_ASSIGNMENTS_SQL = """
            select
              r.code as role_code,
              r.name as role_name,
              usa.scope_type,
              usa.scope_id,
              coalesce(s.code, z.code, c.code, rgn.code, un.code, b.code) as scope_code,
              coalesce(s.name, z.name, c.name, rgn.name, un.name, b.name) as scope_name
            from main.user_roles ur
            inner join main.roles r on r.id = ur.role_id
            inner join main.user_scope_assignments usa on usa.user_id = ur.user_id
            left join main.sbu s on usa.scope_type = 'sbu' and s.id = usa.scope_id
            left join main.zones z on usa.scope_type = 'zone' and z.id = usa.scope_id
            left join main.clusters c on usa.scope_type = 'cluster' and c.id = usa.scope_id
            left join main.regions rgn on usa.scope_type = 'region' and rgn.id = usa.scope_id
            left join main.units un on usa.scope_type = 'unit' and un.id = usa.scope_id
            left join main.branches b on usa.scope_type = 'branch' and b.id = usa.scope_id
            where ur.user_id = cast(:userId as uuid)
            """;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid username or password");
        }

        return jwtService.createToken(user.getId());
    }

    /**
     * Issues an API JWT when an Entra ID / Microsoft 365 sign-in matches an active local user by email.
     */
    public Optional<String> tryIssueTokenForMicrosoftEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository
                .findFirstByEmailIgnoreCaseAndActiveTrue(email.trim())
                .map(user -> jwtService.createToken(user.getId()));
    }

    public UserDetailsResponse getUserDetails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid token user"));
        List<UserDetailsAssignmentResponse> assignments = loadAssignments(userId);
        return new UserDetailsResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.isActive(),
                assignments
        );
    }

    private List<UserDetailsAssignmentResponse> loadAssignments(UUID userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(USER_ASSIGNMENTS_SQL, params, (rs, rowNum) -> new UserDetailsAssignmentResponse(
                rs.getString("role_code"),
                rs.getString("role_name"),
                rs.getString("scope_type"),
                rs.getObject("scope_id") != null ? rs.getLong("scope_id") : null,
                rs.getString("scope_code"),
                rs.getString("scope_name")
        ));
    }
}
