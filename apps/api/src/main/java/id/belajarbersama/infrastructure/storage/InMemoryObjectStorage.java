package id.belajarbersama.infrastructure.storage;

import id.belajarbersama.domain.storage.ObjectKey;
import id.belajarbersama.domain.storage.ObjectMetadata;
import id.belajarbersama.domain.storage.ObjectStorage;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryObjectStorage implements ObjectStorage {
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public String provider() {
        return "memory";
    }

    @Override
    public void put(ObjectKey key, byte[] content, ObjectMetadata metadata) {
        objects.put(key.value(), content.clone());
    }

    @Override
    public Optional<byte[]> get(ObjectKey key) {
        byte[] value = objects.get(key.value());
        return value == null ? Optional.empty() : Optional.of(value.clone());
    }

    @Override
    public void delete(ObjectKey key) {
        objects.remove(key.value());
    }

    @Override
    public boolean ping() {
        return true;
    }
}
