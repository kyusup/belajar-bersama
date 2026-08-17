package id.belajarbersama.infrastructure.storage;

import id.belajarbersama.domain.storage.ObjectStorage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ObjectStorageProducer {
    @Produces
    @ApplicationScoped
    ObjectStorage objectStorage(
            @ConfigProperty(name = "bb.storage.provider") String provider,
            @ConfigProperty(name = "bb.s3.endpoint") String endpoint,
            @ConfigProperty(name = "bb.s3.region") String region,
            @ConfigProperty(name = "bb.s3.bucket") String bucket,
            @ConfigProperty(name = "bb.s3.access-key") String accessKey,
            @ConfigProperty(name = "bb.s3.secret-key") String secretKey) {
        if ("s3".equalsIgnoreCase(provider)) {
            return new S3CompatibleObjectStorage(endpoint, region, bucket, accessKey, secretKey);
        }
        return new InMemoryObjectStorage();
    }
}
