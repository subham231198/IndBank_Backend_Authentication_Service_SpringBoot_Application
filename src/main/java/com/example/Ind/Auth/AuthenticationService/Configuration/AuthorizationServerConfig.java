package com.example.Ind.Auth.AuthenticationService.Configuration;

import com.example.Ind.Auth.AuthenticationService.DTO.UserAuthDTO;
import com.example.Ind.Auth.AuthenticationService.Entity.OAuthClientEntity;
import com.example.Ind.Auth.AuthenticationService.Entity.UsernameAuthEntity;
import com.example.Ind.Auth.AuthenticationService.Repository.OAuthClientRepository;
import com.example.Ind.Auth.AuthenticationService.Repository.UsernameAuthRepo;
import com.example.Ind.Auth.AuthenticationService.Utility.GeneratorService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import java.time.Duration;
import java.util.*;

@Slf4j
@Configuration
public class AuthorizationServerConfig {

    @Autowired
    private UserAuthDTO userAuthDTO;

    @Autowired
    private OAuthClientRepository oAuthClientRepository;

    @Autowired
    private GeneratorService generatorService;

    @Autowired
    private UsernameAuthRepo usernameAuthRepo;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

//        authorizationServerConfigurer.token(customTokenCustomizer);

        authorizationServerConfigurer.oidc(Customizer.withDefaults());

        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        http
                .securityMatcher(endpointsMatcher)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .with(authorizationServerConfigurer, Customizer.withDefaults());

        return http.build();
    }

    @PostConstruct
    public void initDefaultUser() {
        long userCount = usernameAuthRepo.count();
        if (userCount == 0) {
            log.info("No users found in database. Creating default user.");

            UsernameAuthEntity usernameAuthEntity = new UsernameAuthEntity();
            usernameAuthEntity.setUsername(generatorService.generateCustomerId());
            usernameAuthEntity.setCustomerServiceId(generatorService.generateCustomerServiceId());
            usernameAuthEntity.setPassword(passwordEncoder().encode("1q2w3e4r"));
            usernameAuthEntity.setPhone("john1.doe@test.com");
            usernameAuthEntity.setIsActive(true);
            usernameAuthEntity.setAuth_level("30");
            usernameAuthEntity.setIsProfileSuspended(false);
            usernameAuthEntity.setIsProfileProvisioned(false);
            usernameAuthEntity.setIsProfileLocked(false);
            usernameAuthEntity.setIsPasswordLocked(false);
            usernameAuthEntity.setIsOTPLocked(false);
            usernameAuthEntity.setFailedPasswordAttempts(0);
            usernameAuthEntity.setIsInterstitialPageShown(false);
            usernameAuthEntity.setFailedOTPAttempts(0);
            usernameAuthEntity.setLastSuccessfulLogin(null);
            usernameAuthEntity.setLastFailedLogin(null);

            log.info("Default user created: {}", usernameAuthEntity.toString());

            UsernameAuthEntity savedEntity = usernameAuthRepo.save(usernameAuthEntity);
            log.info("Default user saved with ID: {}", savedEntity.getId());
            log.info("Default username: {}", savedEntity.getUsername());
            log.info("Default password: 1q2w3e4r");
        }
    }


    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/v1/api/dsp/authenticate",
                                "/v1/api/authenticate",
                                "/v1/auth/otp",
                                "/api/v1/rest-sts/logout",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api/v1/sessions",
                                "/v1/oauth/authorize",
                                "/v1/oauth/access_token",
                                "/api/customer/session",
                                "/api/customer/password/session",
                                "/api/admin/reset-lock-password",
                                "/api/admin/reset-lock-otp",
                                "/callback",
                                "/v1/oauth/introspect",
                                "/api/security/rest-sts/logout",
                                "/api/admin/reset-password",
                                "/api/v1/secrets/oauth"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth2 ->
                        oauth2.opaqueToken(Customizer.withDefaults())
                )
                .formLogin(form -> form
                        .defaultSuccessUrl("/oauth2/authorize", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        String username = UUID.randomUUID().toString();
        String password = UUID.randomUUID().toString();

        UserDetails user = User.withUsername(username)
                .password(passwordEncoder().encode(password))
                .roles("USER")
                .build();

        log.info("Generated user — Username: {}, Password: {}", username, password);

        UserDetails admin = User.withUsername("admin_auth@indbank.com")
                .password(passwordEncoder().encode("foobar12"))
                .roles("ADMIN")
                .build();

        userAuthDTO.setUsername(username);
        userAuthDTO.setPassword(password);

        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public TokenSettings tokenSettings() {
        return TokenSettings.builder()
                .authorizationCodeTimeToLive(Duration.ofSeconds(30))
                .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                .accessTokenTimeToLive(Duration.ofMinutes(30))
                .refreshTokenTimeToLive(Duration.ofHours(5))
                .reuseRefreshTokens(true)
                .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        List<OAuthClientEntity> clients = oAuthClientRepository.findAll();
        List<RegisteredClient> registeredClients = new ArrayList<>();

        for (OAuthClientEntity client : clients) {
            AuthorizationGrantType primaryGrantType =
                    "authorization_code".equalsIgnoreCase(client.getGrantTypes())
                            ? AuthorizationGrantType.AUTHORIZATION_CODE
                            : AuthorizationGrantType.CLIENT_CREDENTIALS;

            RegisteredClient registeredClient = RegisteredClient
                    .withId(client.getId().toString())
                    .clientId(client.getClientId())
                    .clientSecret(client.getClientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(primaryGrantType)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri(client.getRedirectUri())
                    .scope(client.getScopes())
                    .clientSettings(ClientSettings.builder()
                            .requireProofKey(false)
                            .build())
                    .tokenSettings(tokenSettings())
                    .build();

            registeredClients.add(registeredClient);
        }
        if(clients.isEmpty()) {
            RegisteredClient registeredClient = RegisteredClient
                    .withId(UUID.randomUUID().toString())
                    .clientId("emergency-dummy-clientId")
                    .clientSecret(passwordEncoder().encode("1q2w3e4r"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("http://localhost:7079/callback")
                    .scope("openid")
                    .clientSettings(ClientSettings.builder()
                            .requireProofKey(false)
                            .build())
                    .tokenSettings(tokenSettings())
                    .build();

            registeredClients.add(registeredClient);
        }

        return new InMemoryRegisteredClientRepository(registeredClients);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector() {
        return token -> {
            try {
                log.info("=== Token Introspection ===");

                OAuth2AuthorizationService authService = authorizationService();
                OAuth2Authorization authorization = authService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);

                if (authorization != null) {
                    log.info("Found authorization in memory");

                    Map<String, Object> claims = new HashMap<>();
                    claims.put("active", true);
                    claims.put("sub", authorization.getPrincipalName());

                    String customerId = (String) authorization.getAttribute("customerId");
                    String customerSessionId = (String) authorization.getAttribute("customerSessionId");

                    log.info("Retrieved - customerId: {}", customerId);

                    if (customerId != null) {
                        claims.put("customerId", customerId);
                        claims.put("customerSessionId", customerSessionId);
                        log.info("Added customerId to introspection response");
                    }

                    return new OAuth2IntrospectionAuthenticatedPrincipal(
                            authorization.getPrincipalName(),
                            claims,
                            null
                    );
                }

                log.warn("Authorization not found in memory");
                return createInactivePrincipal();

            } catch (Exception e) {
                log.error("Token introspection failed", e);
                return createInactivePrincipal();
            }
        };
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:7079")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .build();
    }

    private OAuth2AuthenticatedPrincipal createInactivePrincipal() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("active", false);
        return new OAuth2IntrospectionAuthenticatedPrincipal("inactive", attributes, null);
    }
}