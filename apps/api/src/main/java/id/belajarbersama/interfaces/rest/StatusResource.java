package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.platform.GetPlatformStatusService;
import id.belajarbersama.application.platform.PlatformStatus;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/status")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Platform")
public class StatusResource {
    private final GetPlatformStatusService getPlatformStatusService;

    public StatusResource(GetPlatformStatusService getPlatformStatusService) {
        this.getPlatformStatusService = getPlatformStatusService;
    }

    @GET
    @Operation(summary = "Platform status including database reachability")
    public Response status() {
        PlatformStatus status = getPlatformStatusService.execute();
        int http = "UP".equals(status.status()) ? 200 : 503;
        return Response.status(http).entity(status).build();
    }
}
