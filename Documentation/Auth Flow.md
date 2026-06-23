flowchart TD

    A["Incoming Client Request<br/>(Authorization: Bearer eyJhbGciOi...)"]

    A --> B["1. JwtAuthenticationFilter<br/>Intercepts request before Controller"]

    B --> C["JwtService.extractUsername()<br/>Extract username from JWT"]

    C --> D["Username = player1"]

    D --> E["2. SecurityContextHolder<br/>Check if already authenticated"]

    E --> F{"Already Authenticated?"}

    F -->|No| G["3. CustomUserDetailsService"]

    G --> H["loadUserByUsername('player1')"]

    H --> I["Database Query<br/>SELECT * FROM users WHERE username='player1'"]

    I --> J["Return User Entity<br/>(implements UserDetails)"]

    J --> K["4. JwtService"]

    K --> L["isTokenValid(token, userDetails)"]

    L --> M{"Username Match?<br/>Token Expired?"}

    M -->|Valid| N["5. SecurityContextHolder<br/>Set Authentication"]

    N --> O["Create UsernamePasswordAuthenticationToken<br/>Authorities: ROLE_USER"]

    O --> P["Register Authentication<br/>in Security Context"]

    P --> Q["6. DispatcherServlet<br/>Route Request to Controller"]

    Q --> R["MatchesController.getHistory()"]

    R --> S["Outgoing JSON Response"]