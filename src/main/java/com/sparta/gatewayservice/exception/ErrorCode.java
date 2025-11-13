package com.sparta.gatewayservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    GATEWAY_ERROR(11000, HttpStatus.INTERNAL_SERVER_ERROR, "게이트웨이 에러 발생"),
    GATEWAY_JSON_PROCESSING_EXCEPTION(11001, HttpStatus.INTERNAL_SERVER_ERROR, "게이트웨이에서 에러처리하는 중에 객체를 json으로 변환하지 못했습니다"),
    ;

    private final int code;
    private final HttpStatus status;
    private final String details;
}
