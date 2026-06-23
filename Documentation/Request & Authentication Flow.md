sequenceDiagram
    participant Client
    participant JwtFilter as JwtAuthenticationFilter
    participant JwtServ as JwtService
    participant UserDetails as CustomUserDetailsService
    participant SecurityContext as SecurityContextHolder
    participant Controller as REST Controller

    Client->>JwtFilter: HTTP Request (Header: Authorization Bearer <token>)
    JwtFilter->>JwtServ: extractUsername(token)
    JwtServ-->>JwtFilter: username
    JwtFilter->>UserDetails: loadUserByUsername(username)
    UserDetails-->>JwtFilter: UserDetails (loaded from DB)
    JwtFilter->>JwtServ: isTokenValid(token, userDetails)
    JwtServ-->>JwtFilter: true / false
    Note over JwtFilter: If valid, build UsernamePasswordAuthenticationToken
    JwtFilter->>SecurityContext: Set authentication context
    JwtFilter->>Controller: Forward request
