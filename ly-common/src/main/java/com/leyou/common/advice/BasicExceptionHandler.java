package com.leyou.common.advice;

import com.leyou.common.exception.LyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-16 14:48
 **/
@ControllerAdvice
@Slf4j
public class BasicExceptionHandler {

    @ExceptionHandler(LyException.class)
    public ResponseEntity<String> handleException(LyException e){
        // 记录日志
        log.error(e.getMessage(), e);
        int code = e.getStatus() == null ? e.getStatusCode() : e.getStatus().value();
        return ResponseEntity.status(code).body(e.getMessage());
    }
}
