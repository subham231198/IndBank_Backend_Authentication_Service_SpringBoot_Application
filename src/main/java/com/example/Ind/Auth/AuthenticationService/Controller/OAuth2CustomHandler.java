package com.example.Ind.Auth.AuthenticationService.Controller;

import com.example.Ind.Auth.AuthenticationService.Configuration.AuthorizationServerConfig;
import com.example.Ind.Auth.AuthenticationService.DTO.CustomAccessTokenDTO;
import com.example.Ind.Auth.AuthenticationService.DTO.CustomAuthCodeDTO;
import com.example.Ind.Auth.AuthenticationService.DTO.CustomerSessionDTO;
import com.example.Ind.Auth.AuthenticationService.DTO.UserAuthDTO;
import com.example.Ind.Auth.AuthenticationService.Entity.OAuthClientEntity;
import com.example.Ind.Auth.AuthenticationService.Repository.OAuthClientRepository;
import com.example.Ind.Auth.AuthenticationService.Repository.UsernameAuthRepo;
import com.example.Ind.Auth.AuthenticationService.Service.AccessTokenCache;
import com.example.Ind.Auth.AuthenticationService.Service.AuthCodeCacheService;
import com.example.Ind.Auth.AuthenticationService.Service.CustomerSessionCacheService;
import com.example.Ind.Auth.AuthenticationService.Service.SessionAttributesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;

@Slf4j
@RestController
public class OAuth2CustomHandler extends SessionAttributesService {

    @Autowired
    private UserAuthDTO userAuthDTO;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CustomerSessionCacheService customerSessionCacheService;

    @Autowired
    private AuthCodeCacheService authCodeCacheService;

    @Autowired
    private UsernameAuthRepo usernameAuthRepo;

    @Autowired
    private OAuthClientRepository oAuthClientRepository;

    @Autowired
    private PasswordEncoder bryptPasswordEncoder;

    @Autowired
    private AccessTokenCache accessTokenCache;

    @Value("${server.port:7079}")
    private int serverPort;

    @Value("${server.host:172.19.0.4}")
    private String serverHost;

    @Autowired
    private AuthorizationServerConfig  authorizationServerConfig;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    private String baseUrl;

    private static final Logger logger = LogManager.getLogger(OAuth2CustomHandler.class);

    @PostConstruct
    public void initBaseUrl() {
        try {
            String host = serverHost;
            if ("localhost".equals(host)) {
                String hostAddress = InetAddress.getLocalHost().getHostAddress();
                baseUrl = "http://" + hostAddress + ":" + serverPort;
            } else {
                baseUrl = "http://" + host + ":" + serverPort;
            }
            logger.info("Base URL initialized: {}", baseUrl);
        } catch (UnknownHostException e) {
            logger.error("Failed to get host address, using default localhost", e);
            baseUrl = "http://localhost:" + serverPort;
        }
    }

    private String getBaseUrl() {
        return baseUrl;
    }

    @GetMapping("/v1/oauth/authorize")
    public ResponseEntity<?> initiateAuthorization(
            @RequestParam("client_id") String clientId,
            @RequestParam("grant_type") String grant,
            @RequestParam("response_type") String responseType,
            @RequestParam(value = "redirect_uri", required = false) String redirectURI,
            @RequestParam("scope") String scope,
            @RequestHeader("X-CustomerSessionId") String customerSessionId
    ) throws JsonProcessingException {
        Optional<CustomerSessionDTO> sessionDTO = Optional.ofNullable(customerSessionCacheService.getCustomerSession(customerSessionId));
        if (!sessionDTO.isPresent() || !validSessionExpiry(sessionDTO.get().getExpiresAt())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_customer_authentication"));
        }

        if (!isValidClient(clientId)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_client"));
        }

        if (!isValidRedirectUri(clientId, redirectURI)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "redirect_uri does not match a pre-registered value"));
        }

        if (!formLogin()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_authentication"));
        }

        String authorizationUrl = UriComponentsBuilder
                .fromHttpUrl(getBaseUrl() + "/oauth2/authorize")
                .queryParam("response_type", responseType)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectURI)
                .queryParam("scope", scope)
                .build()
                .toUriString();

        try {

            ResponseEntity<String> response = restTemplate.exchange(
                    authorizationUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    String.class
            );

            logger.info("Response Code: {}", response.getStatusCode());

            String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);

            HttpHeaders outHeaders = new HttpHeaders();

            logger.info("Location: {}", location);

            if (location != null && location.contains("code=")) {
                String code = extractCodeFromLocation(location);
                outHeaders.setLocation(URI.create(redirectURI + "?code=" + code));
                CustomerSessionDTO customerSessionDTO = sessionDTO.get();
                updateAuthCodeCache(customerSessionDTO, code);
                return new ResponseEntity<>(outHeaders, HttpStatus.FOUND);
            }

            outHeaders.setLocation(URI.create(redirectURI + "/error"));
            return new ResponseEntity<>(outHeaders, HttpStatus.FOUND);

        } catch (HttpClientErrorException ex) {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode error = mapper.readTree(ex.getResponseBodyAsString());

            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(error);
        }
    }

    public void updateAuthCodeCache(CustomerSessionDTO customerSessionDTO, String code) {
        CustomAuthCodeDTO authCodeDTO = new CustomAuthCodeDTO();
        authCodeDTO.setCode(code);
        authCodeDTO.setCustomerId(customerSessionDTO.getCustomerId());
        authCodeDTO.setCustomerSessionId(customerSessionDTO.getCustomerSessionId());
        authCodeDTO.setChannel(customerSessionDTO.getChannel());
        authCodeDTO.setIssuedAt(Instant.now());
        authCodeDTO.setExpiresAt(Instant.now().plusSeconds(60));
        authCodeCacheService.createAuthCodeCache(authCodeDTO);
    }

    @GetMapping("/callback")
    public String handleCallback(@RequestParam("code") String code) {

        String tokenUrl = getBaseUrl() + "/oauth2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String clientCredentials = "5acb384e-51d2-4528-b6af-3ba50201216f:your-client-secret";
        headers.set("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(clientCredentials.getBytes()));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", getBaseUrl() + "/callback");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            return response.getBody();

        } catch (Exception e) {
            return "Error exchanging code for token: " + e.getMessage();
        }
    }

    private boolean isValidClient(String clientId) {
        return clientId != null && clientId.matches("[a-zA-Z0-9\\-]+");
    }

    private boolean isValidRedirectUri(String clientId, String redirectUri) {
        return !redirectUri.isEmpty() && !redirectUri.isBlank() && !clientId.isEmpty() && !clientId.isBlank();
    }

    public boolean formLogin() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        logger.info("{}, {}", userAuthDTO.getUsername(), userAuthDTO.getPassword());
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", userAuthDTO.getUsername());
        form.add("password", userAuthDTO.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                getBaseUrl() + "/login",
                request,
                String.class
        );

        return response.getStatusCode().is3xxRedirection();
    }

    private String extractCodeFromLocation(String location) {
        String codeParam = "code=";
        int startIndex = location.indexOf(codeParam) + codeParam.length();
        int endIndex = location.indexOf("&", startIndex);
        if (endIndex == -1) {
            endIndex = location.length();
        }
        return location.substring(startIndex, endIndex);
    }

    @PostMapping(
            value = "/v1/oauth/access_token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> getAccessToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectURL,
            @RequestHeader("Authorization") String authorization
    ) {
        logger.info("=== Getting Access Token ===");
        logger.info("Grant Type: {}", grantType);
        logger.info("Code: {}", code);
        logger.info("Redirect URI: {}", redirectURL);

        if (code == null || code.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_request",
                            "error_description", "code is required"));
        }

        if (redirectURL == null || redirectURL.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_request",
                            "error_description", "redirect_uri is required"));
        }

        if (authorization == null || authorization.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_client",
                            "error_description", "Authorization header required"));
        }

        if (!authorization.startsWith("Basic ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_client",
                            "error_description", "Only Basic authentication supported"));
        }

        try {
            CustomAuthCodeDTO authCodeDTO = authCodeCacheService.getAuthCodeCache(code);
            if (authCodeDTO == null) {
                logger.warn("Invalid or expired authorization code: {}", code);
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "invalid_grant",
                                "error_description", "Invalid or expired authorization code"));
            }

            if (authCodeDTO.getExpiresAt().isBefore(Instant.now())) {
                logger.warn("Authorization code expired: {}", code);
                authCodeCacheService.eviceAuthCodeCache(code);
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "invalid_grant",
                                "error_description", "Authorization code expired"));
            }

            String customerId = authCodeDTO.getCustomerId();
            String customerSessionId = authCodeDTO.getCustomerSessionId();
            logger.info("CustomerId from cache: {}", customerId);

            String tokenUrl = getBaseUrl() + "/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", authorization);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", grantType);
            body.add("code", code);
            body.add("redirect_uri", redirectURL);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            logger.info("Token Response Status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                String accessToken = (String) responseBody.get("access_token");
                if (accessToken != null && customerId != null) {
                    CustomAccessTokenDTO customAccessTokenDTO = new CustomAccessTokenDTO();
                    customAccessTokenDTO.setCustomerId(customerId);
                    customAccessTokenDTO.setCustomerSessionId(customerSessionId);
                    customAccessTokenDTO.setToken(accessToken);
                    customAccessTokenDTO.setChannel(authCodeDTO.getChannel());
                    customAccessTokenDTO.setExpiresAt(Instant.now());
                    customAccessTokenDTO.setExpiresAt(Instant.now().plusSeconds(1798));
                    accessTokenCache.createAccessTokenCache(customAccessTokenDTO);

                }

                authCodeCacheService.eviceAuthCodeCache(code);
                logger.info("Auth code deleted after successful exchange");

                return ResponseEntity.ok(responseBody);
            }

            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());

        } catch (HttpClientErrorException ex) {
            logger.error("Token exchange failed: {}", ex.getMessage());
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode error = mapper.readTree(ex.getResponseBodyAsString());
                return ResponseEntity
                        .status(ex.getStatusCode())
                        .body(error);
            } catch (Exception e) {
                return ResponseEntity
                        .status(ex.getStatusCode())
                        .body(Map.of("error", "server_error",
                                "error_description", ex.getMessage()));
            }
        } catch (Exception e) {
            logger.error("Failed to get access token", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "server_error",
                            "error_description", e.getMessage()));
        }
    }

    @PostMapping(
            value = "/v1/oauth/introspect",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> introspectToken(
            @RequestParam("token") String token,
            @RequestHeader("Authorization") String authorization
    ) {
        logger.info("=== Introspecting Token ===");
        logger.info("Token: {}", token.substring(0, Math.min(token.length(), 10)));

        if (token == null || token.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_request",
                            "error_description", "token is required"));
        }

        if (authorization == null || authorization.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_client",
                            "error_description", "Authorization header required"));
        }

        if (!authorization.startsWith("Basic ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_client",
                            "error_description", "Only Basic authentication supported"));
        }

        try {
            String introspectUrl = getBaseUrl() + "/oauth2/introspect";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", authorization);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("token", token);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    introspectUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            logger.info("Introspect Response Status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                Boolean active = (Boolean) responseBody.getOrDefault("active", false);

                if (active) {
                    CustomAccessTokenDTO accessTokenDTO = accessTokenCache.getAccessTokenCache(token);

                    if (accessTokenDTO != null) {
                        String customerId = accessTokenDTO.getCustomerId();
                        String customerSessionId = accessTokenDTO.getCustomerSessionId();

                        if (customerId != null) {
                            responseBody.put("channel", accessTokenDTO.getChannel());
                            responseBody.put("customerId", customerId);
                            responseBody.put("customerSessionId", customerSessionId);
                            responseBody.put("sub", customerId);
                            responseBody.put("serviceId", usernameAuthRepo.findByUsername(customerId).get().getCustomerServiceId());
                            logger.info("Added customerId: {} from cache to introspection response", customerId);
                        }
                    } else {
                        logger.warn("Access token not found in cache: {}", token.substring(0, Math.min(token.length(), 10)));

                        String customerId = accessTokenCache.getAccessTokenCache(token).getCustomerId();
                        if (customerId != null) {
                            responseBody.put("customerId", customerId);
                            responseBody.put("sub", customerId);
                            logger.info("Added customerId: {} from Redis to introspection response", customerId);
                        }
                    }
                }

                return ResponseEntity.ok(responseBody);
            }

            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());

        } catch (HttpClientErrorException ex) {
            logger.error("Introspect failed: {}", ex.getMessage());
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode error = mapper.readTree(ex.getResponseBodyAsString());
                return ResponseEntity
                        .status(ex.getStatusCode())
                        .body(error);
            } catch (Exception e) {
                return ResponseEntity
                        .status(ex.getStatusCode())
                        .body(Map.of("error", "server_error",
                                "error_description", ex.getMessage()));
            }
        } catch (Exception e) {
            logger.error("Failed to introspect token", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "server_error",
                            "error_description", e.getMessage()));
        }
    }

    @PostMapping(
            value = "/api/v1/secrets/oauth",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> createOAuthClientCredentials(
            @RequestHeader("X-CustomerSessionId") String customerSessionId
    ) {
        log.info("=== Creating OAuth Client Credentials ===");

        Optional<CustomerSessionDTO> sessionDTO = Optional.ofNullable(
                customerSessionCacheService.getCustomerSession(customerSessionId)
        );

        if (!sessionDTO.isPresent() || !validSessionExpiry(sessionDTO.get().getExpiresAt())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_customer_authentication"));
        }

        String clientId = UUID.randomUUID().toString();
        String clientSecret = UUID.randomUUID().toString();

        OAuthClientEntity oAuthClientEntity = new OAuthClientEntity();
        oAuthClientEntity.setClientId(clientId);
        oAuthClientEntity.setClientSecret(bryptPasswordEncoder.encode(clientSecret));
        oAuthClientEntity.setGrantTypes("code");
        oAuthClientEntity.setScopes("openid");
        oAuthClientEntity.setRedirectUri("http://localhost:7079/callback");

        log.info("Client ID: {}, Client Secret: {}", clientId, clientSecret);
        oAuthClientRepository.save(oAuthClientEntity);

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("clientId", clientId);
        responseBody.put("clientSecret", clientSecret);
        responseBody.put("redirect_uri", "http://localhost:7079/callback");
        responseBody.put("scope", "openid");
        responseBody.put("grant_type", "authorization_code");

        RegisteredClient registeredClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(oAuthClientEntity.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(oAuthClientEntity.getRedirectUri())
                .scope(oAuthClientEntity.getScopes())
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(false)
                        .build())
                .tokenSettings(authorizationServerConfig.tokenSettings())
                .build();
        registeredClientRepository.save(registeredClient);
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }
}