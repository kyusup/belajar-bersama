package id.belajarbersama.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.belajarbersama.domain.storage.ObjectKey;
import id.belajarbersama.domain.storage.ObjectMetadata;
import org.junit.jupiter.api.Test;

class InMemoryObjectStorageTest {
    @Test
    void roundTrip() {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        ObjectKey key = ObjectKey.of("content", "abc.pdf");
        byte[] payload = {1, 2, 3};
        storage.put(key, payload, new ObjectMetadata("application/pdf", 3, "notes.pdf"));
        assertEquals("memory", storage.provider());
        assertTrue(storage.ping());
        assertArrayEquals(payload, storage.get(key).orElseThrow());
        storage.delete(key);
        assertTrue(storage.get(key).isEmpty());
    }
}
