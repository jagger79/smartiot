package cz.smartiot.zigbee.core.controller;

import cz.smartiot.api.core.model.ErrorResponseDto;
import cz.smartiot.zigbee.core.ServiceProperties;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private final ServiceProperties props;

    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object bodyNotUsed,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        var h = new HttpHeaders();

        HttpStatus status = getStatusFrom(ex);
        var body = new ErrorResponseDto()
                .status(status.value())
                .error(ex.getClass().getSimpleName())
                .message(ex.getMessage())
                .timestamp(OffsetDateTime.now());
        //.path(request.getRequestURI());

        if (status.is4xxClientError()) {
            if (status != HttpStatus.UNAUTHORIZED) {
                log.warn("exception,{},{}", body.getError(), body.getMessage());
            }
        } else {
            log.error("", ex);
        }

        return new ResponseEntity<>(body, h, status);
    }

//    @ResponseBody
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<?> handleControllerException(HttpServletRequest request,
//                                                       Exception ex) {
//        return handleExceptionInternal(ex, null, null, null, request);
//    }

    private HttpStatus getStatusFrom(Exception throwable) {
        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(throwable.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            return responseStatus.code();
        }
        return props.getStatus(throwable)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus getStatus(HttpServletRequest request,
                                 WebRequest request2) {
        Integer code = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = HttpStatus.resolve(code);
        return (status != null) ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}