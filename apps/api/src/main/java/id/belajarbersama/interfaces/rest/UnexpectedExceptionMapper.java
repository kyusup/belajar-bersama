package id.belajarbersama.interfaces.rest;

import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.InfrastructureException;
import id.belajarbersama.interfaces.rest.dto.ApiErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import org.jboss.logging.Logger;

@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(UnexpectedExceptionMapper.class);

    @Context ContainerRequestContext requestContext;

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException webApplicationException) {
            return webApplicationException.getResponse();
        }
        String correlationId = correlationId();
        if (exception instanceof InfrastructureException) {
            LOG.errorf(exception, "Infrastructure failure correlationId=%s", correlationId);
            return Response.status(503)
                    .entity(
                            new ApiErrorResponse(
                                    ErrorCodes.INFRASTRUCTURE_FAILURE,
                                    "A dependent system is unavailable.",
                                    Map.of(),
                                    correlationId))
                    .build();
        }
        LOG.errorf(exception, "Unexpected failure correlationId=%s", correlationId);
        return Response.status(500)
                .entity(
                        new ApiErrorResponse(
                                ErrorCodes.UNEXPECTED_FAILURE,
                                "An unexpected error occurred.",
                                Map.of(),
                                correlationId))
                .build();
    }

    private String correlationId() {
        Object value =
                requestContext == null
                        ? null
                        : requestContext.getProperty(CorrelationIdFilter.PROPERTY);
        return value == null ? null : value.toString();
    }
}
