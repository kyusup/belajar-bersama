package id.belajarbersama.interfaces.rest;

import id.belajarbersama.domain.competency.CompetencyRepository;
import id.belajarbersama.interfaces.rest.dto.CompetencyResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/competencies")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Taxonomy")
public class CompetencyResource {
    private final CompetencyRepository competencies;

    public CompetencyResource(CompetencyRepository competencies) {
        this.competencies = competencies;
    }

    @GET
    public List<CompetencyResponse> list() {
        return competencies.listActive().stream()
                .map(
                        item ->
                                new CompetencyResponse(
                                        item.id(), item.slug(), item.name(), item.description()))
                .toList();
    }
}
