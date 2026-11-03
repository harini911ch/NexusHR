package com.nexushr.employee.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

@Component
@ConditionalOnProperty(name = "app.documents.storage", havingValue = "s3")
public class S3DocumentStorage implements DocumentStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3DocumentStorage(
        S3Client s3Client,
        @Value("${app.documents.s3.bucket}") String bucket
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build(),
                RequestBody.fromBytes(content)
            );
        } catch (SdkException exception) {
            throw new DocumentStorageException("Could not store employee document", exception);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()).asByteArray();
        } catch (SdkException exception) {
            throw new DocumentStorageException("Could not read employee document", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
        } catch (SdkException exception) {
            throw new DocumentStorageException("Could not delete employee document", exception);
        }
    }
}
