package com.sparta.gatewayservice.config;

import com.sparta.gatewayservice.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ApiResponse <T> {

    private int status; // 200, 404, 500 등
    private String message; // OK, NOT_FOUND 등
    private T data; // 서비스에서 반환되는 실제 데이터
    private ApiError error; // 커스텀 에러 코드

    // 성공 케이스
    public ApiResponse(T data) {
        this.status = 200;
        this.message = "OK";
        this.data = data;
        this.error = null;
    }

    // 실패 케이스
    public ApiResponse(ErrorCode error) {
        this.status = error.getStatus().value();
        this.message = error.getStatus().name();
        this.data = null;
        this.error = new ApiError(error.getCode(), error.getDetails());
    }

    @Getter
    @AllArgsConstructor
    public static class ApiError {
        private int code; // 1xxx, 2xxx 등 커스텀 에러 코드
        private String details; // 커스텀 에러 메시지
    }
}
