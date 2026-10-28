package com.skillgap.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Converts exceptions into friendly pages. Stack traces are logged on the
 * server but never shown to the user.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public String notFound(NotFoundException ex, Model model) {
        log.info("Not found: {}", ex.getMessage());
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("backUrl", "/dashboard");
        return "error/not-found";
    }

    /**
     * Spring MVC throws this for a missing static file or unknown route.
     * It must stay a 404 (with an HTML body), never a 200 error page.
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public String missingResource(NoResourceFoundException ex, Model model) {
        log.info("No resource: {}", ex.getResourcePath());
        model.addAttribute("message", "The page or file you requested was not found.");
        model.addAttribute("backUrl", "/dashboard");
        return "error/not-found";
    }

    @ExceptionHandler(BusinessException.class)
    public String business(BusinessException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/dashboard";
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public String accessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("message", "You do not have permission to view this page.");
        model.addAttribute("backUrl", "/dashboard");
        return "error/forbidden";
    }

    /** A malformed id in the URL (e.g. /analysis/gap) is a client error. */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String badRequest(MethodArgumentTypeMismatchException ex, Model model) {
        log.info("Bad request parameter '{}': {}", ex.getName(), ex.getValue());
        model.addAttribute("message", "That URL is not valid. Please check the link and try again.");
        model.addAttribute("backUrl", "/dashboard");
        return "error/general";
    }

    @ExceptionHandler(Exception.class)
    public String generic(Exception ex, Model model) {
        log.error("Unhandled error", ex);
        model.addAttribute("message", "Something went wrong. Please try again or contact support.");
        model.addAttribute("backUrl", "/dashboard");
        return "error/general";
    }
}
