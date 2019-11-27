/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ${package}.app.exception;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ${author}
 */
@ControllerAdvice
public class GlobalControllerExceptionHandler {

    private static final Logger LOG =
        LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

    private static final String SERVER_ERROR_MESSAGE = "系统繁忙，请稍后重试";
    private static final String INVALID_FORMAT_MESSAGE = "参数错误，请检查参数";
    private static final String MISSING_PARAMS_MESSAGE = "缺少参数，请检查参数";
    private static final String MEDIA_TYPE_NOT_ACCEPTABLE_MESSAGE = "Unsupported MediaType";
    private static final String HTTP_METHOD_UNSUPPORTED_MESSAGE = "Unsupported HttpMethod";

    private final String errorCodeKey;
    private final String errorMessageKey;

    protected GlobalControllerExceptionHandler() {
        this("code", "message");
    }

    protected GlobalControllerExceptionHandler(String errorCodeKey, String errorMessageKey) {
        this.errorCodeKey = errorCodeKey;
        this.errorMessageKey = errorMessageKey;
    }

    @ExceptionHandler({
        HttpMediaTypeNotAcceptableException.class,
        HttpRequestMethodNotSupportedException.class,
    })
    @ResponseStatus(HttpStatus.OK)
    public ModelAndView handleMediaTypeException(HttpServletRequest request, Exception ex) {
        String requestURI = getRequestURI(request);
        LOG.error("{} {}, Msg: {}", request.getMethod(), requestURI, ex.getMessage(), ex);

        int code;
        String message;
        if (ex instanceof HttpMediaTypeNotAcceptableException) {
            code = HttpStatus.NOT_ACCEPTABLE.value();
            message = MEDIA_TYPE_NOT_ACCEPTABLE_MESSAGE;
        } else if (ex instanceof HttpRequestMethodNotSupportedException) {
            HttpRequestMethodNotSupportedException exception =
                (HttpRequestMethodNotSupportedException) ex;
            String method = exception.getMethod();
            String[] supportedMethods = exception.getSupportedMethods();

            code = HttpStatus.METHOD_NOT_ALLOWED.value();
            message =
                String.format(
                    "%s: %s, supported methods: %s",
                    HTTP_METHOD_UNSUPPORTED_MESSAGE, method, Arrays.toString(supportedMethods));
        } else {
            code = HttpStatus.INTERNAL_SERVER_ERROR.value();
            message = SERVER_ERROR_MESSAGE;
        }

        return genModelAndView(code, message);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
    })
    @ResponseStatus(HttpStatus.OK)
    public ModelAndView handleBadRequestException(HttpServletRequest request, Exception ex) {
        String requestURI = getRequestURI(request);
        LOG.error("{} {}, Msg: {}", request.getMethod(), requestURI, ex.getMessage(), ex);

        String message;
        if (ex instanceof MissingServletRequestParameterException) {
            String parameterName = ((MissingServletRequestParameterException) ex).getParameterName();
            message = String.format("%s：[ %s ]", MISSING_PARAMS_MESSAGE, parameterName);
        } else {
            message = INVALID_FORMAT_MESSAGE;
        }
        return genModelAndView(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler({
        BindException.class,
        MethodArgumentNotValidException.class,
    })
    @ResponseStatus(HttpStatus.OK)
    public ModelAndView handleArgumentException(HttpServletRequest request, Exception ex) {
        String requestURI = getRequestURI(request);
        LOG.error("{} {}, Msg: {}", request.getMethod(), requestURI, ex.getMessage(), ex);

        String message;
        Errors errors = getErrors(ex);
        if (errors != null) {
            List<String> errList =
                errors.getFieldErrors().stream().map(FieldError::getField).collect(Collectors.toList());

            message =
                String.format(
                    "%s：[ %s ]", INVALID_FORMAT_MESSAGE, Joiner.on(", ").skipNulls().join(errList));
        } else {
            message = INVALID_FORMAT_MESSAGE;
        }

        return genModelAndView(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ModelAndView handleException(HttpServletRequest request, Exception ex) {
        String requestURI = getRequestURI(request);
        LOG.error("{} {}, Msg: {}", request.getMethod(), requestURI, ex.getMessage(), ex);
        return genModelAndView(HttpStatus.INTERNAL_SERVER_ERROR.value(), SERVER_ERROR_MESSAGE);
    }

    private String getRequestURI(HttpServletRequest request) {
        String requestURI;
        String queryString = request.getQueryString();
        if (queryString != null && queryString.length() > 0) {
            requestURI = String.format("%s?%s", request.getRequestURI(), queryString);
        } else {
            requestURI = request.getRequestURI();
        }

        return requestURI;
    }

    private Map<String, Object> genModelMap(int code, String message) {
        Map<String, Object> model = new HashMap<>(2);
        model.put(this.errorCodeKey, code);

        if (message != null) {
            model.put(this.errorMessageKey, message);
        }

        return model;
    }

    private ModelAndView genModelAndView(int code, String message) {
        MappingJackson2JsonView jsonView = new MappingJackson2JsonView();
        jsonView.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        ModelAndView mav = new ModelAndView();
        mav.setView(jsonView);
        mav.addAllObjects(genModelMap(code, message));

        return mav;
    }

    private Errors getErrors(Exception ex) {
        if (ex == null) {
            return null;
        }
        if (ex instanceof BindException) {
            return ((BindException) ex).getBindingResult();
        } else if (ex instanceof MethodArgumentNotValidException) {
            return ((MethodArgumentNotValidException) ex).getBindingResult();
        }

        return null;
    }
}
