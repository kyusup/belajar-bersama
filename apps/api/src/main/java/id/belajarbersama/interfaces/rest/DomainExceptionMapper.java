package id.belajarbersama.interfaces.rest;

import id.belajarbersama.domain.error.AuthorizationException;
import id.belajarbersama.domain.error.BusinessRuleViolationException;
import id.belajarbersama.domain.error.ConflictException;
import id.belajarbersama.domain.error.DomainException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.NotFoundException;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.interfaces.rest.dto.ApiErrorResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {
    private static final Logger LOG = Logger.getLogger(DomainExceptionMapper.class);

    @Context ContainerRequestContext requestContext;

    @Override
    public Response toResponse(DomainException exception) {
        int status = statusOf(exception);
        String correlationId = correlationId();
        LOG.infof(
                "Domain error code=%s status=%s correlationId=%s",
                exception.code(), status, correlationId);
        return Response.status(status)
                .entity(
                        new ApiErrorResponse(
                                exception.code(),
                                exception.getMessage(),
                                exception.details(),
                                correlationId))
                .build();
    }

    private static int statusOf(DomainException exception) {
        if (exception instanceof ValidationException) {
            return 400;
        }
        if (exception instanceof AuthorizationException) {
            return ErrorCodes.UNAUTHENTICATED.equals(exception.code()) ? 401 : 403;
        }
        if (exception instanceof NotFoundException) {
            return 404;
        }
        if (exception instanceof ConflictException) {
            return 409;
        }
        if (exception instanceof BusinessRuleViolationException) {
            return 422;
        }
        return 400;
    }

    private String correlationId() {
        Object value =
                requestContext == null
                        ? null
                        : requestContext.getProperty(CorrelationIdFilter.PROPERTY);
        return value == null ? null : value.toString();
    }
}
