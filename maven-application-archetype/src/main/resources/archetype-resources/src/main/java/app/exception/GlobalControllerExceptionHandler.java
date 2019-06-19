package ${package}.app.exception;

import com.google.common.base.Joiner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

@ControllerAdvice
public class GlobalControllerExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

    private static final String SERVER_ERROR_MESSAGE = "系统繁忙，请稍后重试";
    private static final String INVALID_FORMAT_MESSAGE = "参数错误，请检查参数";
    private static final String MISSING_PARAMS_MESSAGE = "缺少参数，请检查参数";

    private static final String ERROR_CODE = "code";
    private static final String ERROR_MESSAGE = "message";

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(HttpServletRequest request, Exception ex) {
        String requestURI = getRequestURI(request);
        LOG.error("{} {}, Msg: {}", request.getMethod(), requestURI, ex.getMessage(), ex);

        return genModelAndView(HttpStatus.INTERNAL_SERVER_ERROR.value(), SERVER_ERROR_MESSAGE);
    }

    @ExceptionHandler({
        BindException.class,
        MethodArgumentNotValidException.class,
        MissingServletRequestParameterException.class
    })
    public ModelAndView handleArgumentException(HttpServletRequest request, Exception ex) {
        String requestURI = getRequestURI(request);
        LOG.error("{} {}, Msg: {}", request.getMethod(), requestURI, ex.getMessage(), ex);

        String message;

        if (ex instanceof MissingServletRequestParameterException) {
            message = String.format("%s：[ %s ]", MISSING_PARAMS_MESSAGE,
                ((MissingServletRequestParameterException) ex).getParameterName());
        } else {
            Errors errors = getErrors(ex);

            if (errors != null) {
                List<String> errList = errors
                    .getFieldErrors()
                    .stream()
                    .map(FieldError::getField)
                    .collect(Collectors.toList());

                message = String.format("%s：[ %s ]", INVALID_FORMAT_MESSAGE, Joiner.on(", ")
                    .skipNulls()
                    .join(errList));
            } else {
                message = INVALID_FORMAT_MESSAGE;
            }
        }

        return genModelAndView(HttpStatus.BAD_REQUEST.value(), message);
    }

    private String getRequestURI(HttpServletRequest request) {
        String requestURI;
        String queryString = request.getQueryString();
        if (!StringUtils.isEmpty(queryString)) {
            requestURI = String.format("%s?%s", request.getRequestURI(), queryString);
        } else {
            requestURI = request.getRequestURI();
        }

        return requestURI;
    }

    private Map<String, Object> genModelMap(int code, String message) {
        Map<String, Object> model = new HashMap<>(2);
        model.put(ERROR_CODE, code);

        if (!StringUtils.isEmpty(message)) {
            model.put(ERROR_MESSAGE, message);
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

        if (ex instanceof MethodArgumentNotValidException) {
            return ((MethodArgumentNotValidException) ex).getBindingResult();
        } else if (ex instanceof BindException) {
            return ((BindException) ex).getBindingResult();
        }

        return null;
    }
}
