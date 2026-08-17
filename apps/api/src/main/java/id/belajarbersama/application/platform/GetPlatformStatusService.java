package id.belajarbersama.application.platform;

import id.belajarbersama.domain.search.SearchIndex;
import id.belajarbersama.domain.storage.ObjectStorage;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GetPlatformStatusService {
    private final DataSource dataSource;
    private final ObjectStorage objectStorage;
    private final SearchIndex searchIndex;
    private final String version;

    public GetPlatformStatusService(
            DataSource dataSource,
            ObjectStorage objectStorage,
            SearchIndex searchIndex,
            @ConfigProperty(name = "bb.application.version") String version) {
        this.dataSource = dataSource;
        this.objectStorage = objectStorage;
        this.searchIndex = searchIndex;
        this.version = version;
    }

    public PlatformStatus execute() {
        ComponentStatus database = pingDatabase();
        ComponentStatus storage =
                objectStorage.ping()
                        ? ComponentStatus.up(objectStorage.provider())
                        : ComponentStatus.down(objectStorage.provider(), "unreachable");
        ComponentStatus search =
                searchIndex.ping()
                        ? ComponentStatus.up(searchIndex.provider())
                        : ComponentStatus.down(searchIndex.provider(), "unreachable");

        Map<String, ComponentStatus> components = new LinkedHashMap<>();
        components.put("database", database);
        components.put("storage", storage);
        components.put("search", search);

        boolean up = "UP".equals(database.status());
        return new PlatformStatus("belajar-bersama-api", version, up ? "UP" : "DOWN", components);
    }

    private ComponentStatus pingDatabase() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT 1");
                ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return ComponentStatus.up("postgresql");
            }
            return ComponentStatus.down("postgresql", "empty result");
        } catch (Exception exception) {
            return ComponentStatus.down("postgresql", "connection failed");
        }
    }
}
