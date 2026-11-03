package com.nexushr.employee.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@ConditionalOnProperty(name = "app.documents.storage", havingValue = "s3")
public class DocumentStorageConfiguration {

    @Bean(destroyMethod = "close")
    S3Client documentS3Client(
        @Value("${app.documents.s3.region}") String region,
        @Value("${app.documents.s3.endpoint:}") String endpoint,
        @Value("${app.documents.s3.path-style-access:false}") boolean pathStyleAccess
    ) {
        var builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build());
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
