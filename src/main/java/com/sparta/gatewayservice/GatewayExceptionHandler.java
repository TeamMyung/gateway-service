package com.sparta.gatewayservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class GatewayExceptionHandler implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(ex -> handleException(exchange, ex));
    }

    private Mono<Void> handleException(ServerWebExchange exchange, Throwable ex) {

        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse apiResponse = new ApiResponse(
                ErrorCode.GATEWAY_ERROR.getStatus().value(),
                ErrorCode.GATEWAY_ERROR.getStatus().name(),
                null,
                new ApiResponse.ApiError(
                        ErrorCode.GATEWAY_ERROR.getCode(),
                        ErrorCode.GATEWAY_ERROR.getDetails() + " : " + ex.getMessage()
                )
        );

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(apiResponse);
        } catch (JsonProcessingException e) {
            String fallback = "{" +
                    "\"status\":"+ErrorCode.GATEWAY_JSON_PROCESSING_EXCEPTION.getStatus().value()+"," +
                    "\"message\":\""+ErrorCode.GATEWAY_JSON_PROCESSING_EXCEPTION.getStatus().name()+"\"," +
                    "\"data\":\"null\"," +
                    "\"error\":{" +
                        "\"code\":\""+ErrorCode.GATEWAY_JSON_PROCESSING_EXCEPTION.getCode()+ "\"," +
                        "\"details\":\""+ErrorCode.GATEWAY_JSON_PROCESSING_EXCEPTION.getDetails()+"\"" +
                    "}" +
                "}";
            bytes = fallback.getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}

