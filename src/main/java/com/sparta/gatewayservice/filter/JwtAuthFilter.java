package com.sparta.gatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String BEARER = "Bearer ";
    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS = "access";
    private static final String ROLE_HEADER = "role";
    private static final String USER_ID_HEADER = "user_id";
    private static final String HUB_ID_HEADER = "hub_id";
    private static final String VENDOR_ID_HEADER = "vendor_id";
    private static final String DELIVERY_TYPE_HEADER = "delivery_type";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/v1/auth/", "/swagger", "/v3/api-docs", "/actuator/health"
    );

    @Value("${jwt.issuer:user-service}")
    private String issuer;

    @Value("${jwt.secret.key}")
    private String secretBase64;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(Base64.getDecoder()
                .decode(secretBase64)
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var req = exchange.getRequest();
        var path = req.getURI().getPath();
        var method = req.getMethod();

        if (method != null && method.name().equals("OPTIONS")) {
            return chain.filter(exchange);
        }

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String bearerToken = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION);
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith(BEARER)) {
            return unauthorized(exchange);
        }
        String access = bearerToken.substring(BEARER.length());

        final Claims claims;
        try {
            claims = Jwts.parser()
                    .requireIssuer(issuer)
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(access)
                    .getPayload();
        } catch (Exception e) {
            return unauthorized(exchange);
        }

        if (!ACCESS.equals(claims.get(TOKEN_TYPE, String.class))) {
            return unauthorized(exchange);
        }

        String userId = claims.getSubject();
        String role = claims.get(ROLE_HEADER, String.class);
        String hubId = claims.get(HUB_ID_HEADER, String.class);
        String vendorId = claims.get(VENDOR_ID_HEADER, String.class);
        String deliveryType = claims.get(DELIVERY_TYPE_HEADER, String.class);

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(USER_ID_HEADER, nullToEmpty(userId))
                .header(ROLE_HEADER, nullToEmpty(role))
                .header(HUB_ID_HEADER, nullToEmpty(hubId))
                .header(VENDOR_ID_HEADER, nullToEmpty(vendorId))
                .header(DELIVERY_TYPE_HEADER, nullToEmpty(deliveryType))
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isPublic(String path) {
        if ("/".equals(path)) return true;
        for (String p : PUBLIC_PATHS) if (path.startsWith(p)) return true;
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private String nullToEmpty(String claim) {
        return claim == null ? "" : claim;
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
