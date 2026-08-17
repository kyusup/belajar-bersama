package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.error.InfrastructureException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;

final class JdbcSupport {
    private JdbcSupport() {}

    static void setUuid(PreparedStatement statement, int index, UUID value) throws SQLException {
        if (value == null) {
            statement.setObject(index, null);
        } else {
            statement.setObject(index, value);
        }
    }

    static void setInstant(PreparedStatement statement, int index, Instant value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
        } else {
            statement.setTimestamp(index, Timestamp.from(value));
        }
    }

    static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    static UUID uuid(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, UUID.class);
    }

    static InfrastructureException wrap(Exception exception, String message) {
        return new InfrastructureException(message, exception);
    }

    static DataSource require(DataSource dataSource) {
        return dataSource;
    }
}
