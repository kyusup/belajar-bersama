package id.belajarbersama.interfaces.rest;

import id.belajarbersama.interfaces.rest.dto.HealthResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/health")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Platform")
public class HealthResource {
    @GET
    @Operation(summary = "API liveness health")
    public HealthResponse health() {
        return new HealthResponse("UP", "belajar-bersama-api");
    }
}
