package id.belajarbersama.domain.storage;

import java.util.Optional;

/**
 * S3-compatible object storage port. Implementations must not execute stored bytes.
 *
 * <p>Malware scanning is a future capability.
 */
public interface ObjectStorage {
    String provider();

    void put(ObjectKey key, byte[] content, ObjectMetadata metadata);

    Optional<byte[]> get(ObjectKey key);

    void delete(ObjectKey key);

    boolean ping();
}
