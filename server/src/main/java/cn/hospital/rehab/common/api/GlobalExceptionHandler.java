package cn.hospital.rehab.common.api;

import jakarta.validation.ConstraintViolationException;
import cn.hospital.rehab.common.importing.ImportError;
import cn.hospital.rehab.common.importing.ImportValidationException;
import cn.hospital.rehab.common.importing.ImportFailure;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ImportValidationException.class)
    public ApiResponse<ImportFailure> handleImportValidation(ImportValidationException exception) {
        return ApiResponse.error(exception.getMessage(), new ImportFailure(exception.getBatchNo(), exception.getErrors()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            IllegalArgumentException.class})
    public ApiResponse<Void> handleBadRequest(Exception exception) {
        return ApiResponse.error(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return ApiResponse.error("请求参数格式或取值不正确");
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Void> handleNotFound(NoResourceFoundException exception) {
        return ApiResponse.error("请求资源不存在");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpected(Exception exception) {
        log.error("Unhandled request exception", exception);
        return ApiResponse.error("系统处理失败，请联系管理员");
    }
}
