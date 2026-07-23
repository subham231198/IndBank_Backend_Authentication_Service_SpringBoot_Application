# AGENTS.md — Guidance for AI coding agents

Purpose: Give an AI agent the minimal, high-value facts to be immediately productive in this repository (small Spring Boot OAuth2 authorization microservice).

Quick start (commands)
- Build: ./mvnw -DskipTests package
- Run:   ./mvnw spring-boot:run
- Run tests: ./mvnw test

Big picture — architecture and responsibilities
- This is a Spring Boot application exposing an OAuth2 Authorization Server and resource-server components. Key entrypoint: `AuthenticationServiceApplication.java`.
- Primary responsibilities:
  - Configure Spring Security / OAuth2 authorization server behavior: `Configuration/AuthorizationServerConfig.java` (two ordered SecurityFilterChain beans, AuthorizationServerSettings).
  - Expose HTTP endpoints that act as a thin proxy/adapter to an external auth server (localhost:9091): `Controller/OAuth2CustomHandler.java`.
  - Persist OAuth client registrations in a JPA table `oauth_clients` and adapt them into Spring Security's `RegisteredClient` in `AuthorizationServerConfig.registeredClientRepository(...)`.
  - Provide a RestTemplate with Apache HttpClient (redirects disabled, cookie store) in `Configuration/RestTemplateConfig.java` for cross-service calls.

Important files (where to look first)
- `src/main/java/.../Configuration/AuthorizationServerConfig.java` — core of security setup (userDetailsService, RegisteredClientRepository, token introspection settings, two SecurityFilterChain definitions with @Order(1) and @Order(2)).
- `src/main/java/.../Controller/OAuth2CustomHandler.java` — proxy logic for /v1/oauth/authorize and token exchange flows; shows how clients and redirects are validated and how the app proxies to an external server (9091).
- `src/main/java/.../Repository/OAuthClientRepository.java` and `Entity/OAuthClientEntity.java` — DB schema mapping for client registrations. Table name: `oauth_clients`.
- `src/main/resources/application.yaml` — DB URL, credentials, server port (7079) and JPA settings (ddl-auto: update, show-sql: true).
- `src/main/java/.../Utility/JwtUtil.java` — local JWT utilities (hard-coded secret) used by the app; note the mix of jjwt and auth0 libraries.

Project-specific patterns and gotchas
- Two SecurityFilterChains: the Authorization Server chain is ordered first (@Order(1)) and uses OAuth2AuthorizationServerConfigurer; default app rules are @Order(2). Modify order carefully when editing security.
- RegisteredClientRepository is implemented inline as an anonymous class that reads `OAuthClientEntity` and returns a built `RegisteredClient` based on the `grantTypes` string. The mapping is conditional on string values `client_credentials` and `authorization_code` — new grant types should follow that exact pattern.
- The app expects an external authorization server at http://localhost:9091 for several flows. `OAuth2CustomHandler` constructs authorization and token requests against that host and performs a programmatic form login against `/login` to obtain a redirect. Tests or local debugging will often require running that external server or mocking the endpoints.
- RestTemplate has redirects disabled and uses a cookie store intentionally: authorization flow code reads the Location header of 3xx responses rather than following redirects automatically. When changing RestTemplate behavior, tests and the proxy logic must be updated accordingly.
- The `userDetailsService()` creates a single in-memory user with a random username/password and writes them into the Spring component `UserAuthDTO`. This runtime behavior is relied upon by `OAuth2CustomHandler.formLogin()` which posts those values to the external server's `/login`. This means the app generates credentials at startup and prints them to stdout — important for debugging.
- Sensitive values are hard-coded in the repo: MySQL password in `application.yaml`, JWT secret in `JwtUtil`, and client credentials in `OAuth2CustomHandler` (e.g., `5acb384e-...:your-client-secret`). Treat these as secrets and rotate or externalize to environment variables when making changes.

DB + schema notes
- JPA entity: `OAuthClientEntity` (table `oauth_clients`) columns: id, clientId (unique), clientSecret, grantTypes, redirectUri, scopes.
- Example SQL to add an authorization_code client:
  INSERT INTO oauth_clients (client_id, client_secret, grant_types, redirect_uri, scopes)
  VALUES ('example-client', 'secret', 'authorization_code', 'http://localhost:9091/callback', 'read write');

Debugging and developer workflows
- Logs: SQL is enabled (`spring.jpa.show-sql: true`) and SQL output is formatted. Watch startup STDOUT for the generated in-memory username/password printed from `AuthorizationServerConfig.userDetailsService()`.
- To debug the proxied OAuth flow locally, either run the external auth server expected at port 9091 or stub its endpoints (/oauth2/authorize, /oauth2/token, /login). The handler expects 3xx redirects and inspects Location headers.
- Common mistakes agents introduce: changing RestTemplate to follow redirects, modifying RegisteredClientRepository signature, or altering security filter order. Any change to those requires re-checking integration logic in `OAuth2CustomHandler` and token introspection bean.

Tests
- Tests live under `src/test/java/...` — run with `./mvnw test`. There are Spring Boot tests (sample app test present). There are no integration test harnesses for the external 9091 service; add mocks or testcontainers if you add integration tests.

Make changes carefully (recommended PR checklist for agents)
1. If you edit security configuration, run the app locally and verify startup prints the generated user credentials.
2. If you change RegisteredClientRepository, add a DB row and verify client lookup with existing grant types works (both client_credentials and authorization_code are used).
3. If you touch RestTemplate or proxy flows, add or update unit tests that mock RestTemplate.exchange/postForEntity to validate Location header behavior.
4. Externalize hard-coded secrets to environment variables or Spring properties (do not commit credentials).

Where to add new code
- Keep configuration classes in `Configuration/`. Controllers in `Controller/`, DTOs in `DTO/`, entities in `Entity/`, and repositories in `Repository/` — follow existing package structure.

Contact points for further exploration
- `AuthorizationServerConfig.java` — start here to understand the security surface.
- `OAuth2CustomHandler.java` — follow the runtime request paths to see how the service communicates externally.

End of AGENTS.md

