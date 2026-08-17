package id.belajarbersama.infrastructure.storage;

import id.belajarbersama.domain.error.InfrastructureException;
import id.belajarbersama.domain.storage.ObjectKey;
import id.belajarbersama.domain.storage.ObjectMetadata;
import id.belajarbersama.domain.storage.ObjectStorage;
import java.net.URI;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public final class S3CompatibleObjectStorage implements ObjectStorage {
    private final S3Client client;
    private final String bucket;

    public S3CompatibleObjectStorage(
            String endpoint, String region, String bucket, String accessKey, String secretKey) {
        this.bucket = bucket;
        this.client =
                S3Client.builder()
                        .endpointOverride(URI.create(endpoint))
                        .region(Region.of(region))
                        .credentialsProvider(
                                StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create(accessKey, secretKey)))
                        .forcePathStyle(true)
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .build();
    }

    @Override
    public String provider() {
        return "s3";
    }

    @Override
    public void put(ObjectKey key, byte[] content, ObjectMetadata metadata) {
        try {
            ensureBucket();
            PutObjectRequest request =
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key.value())
                            .contentType(metadata.contentType())
                            .contentLength((long) content.length)
                            .contentDisposition("attachment")
                            .build();
            client.putObject(request, RequestBody.fromBytes(content));
        } catch (Exception exception) {
            throw new InfrastructureException("Failed to store object", exception);
        }
    }

    @Override
    public Optional<byte[]> get(ObjectKey key) {
        try {
            GetObjectRequest request =
                    GetObjectRequest.builder().bucket(bucket).key(key.value()).build();
            return Optional.of(client.getObjectAsBytes(request).asByteArray());
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (Exception exception) {
            throw new InfrastructureException("Failed to read object", exception);
        }
    }

    @Override
    public void delete(ObjectKey key) {
        try {
            client.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucket).key(key.value()).build());
        } catch (Exception exception) {
            throw new InfrastructureException("Failed to delete object", exception);
        }
    }

    @Override
    public boolean ping() {
        try {
            ensureBucket();
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private void ensureBucket() {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                return;
            }
            throw exception;
        }
    }
}
