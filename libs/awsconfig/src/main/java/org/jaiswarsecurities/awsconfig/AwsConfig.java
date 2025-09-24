package org.jaiswarsecurities.awsconfig;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.utils.StringUtils;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsConfig {

    @Bean
    public S3Client s3Client(AwsProperties props) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(props.getAwsRegion()))
                .credentialsProvider(resolveCredentials(props));

        if (StringUtils.isNotBlank(props.getEndpointUrl())) {
            builder.endpointOverride(java.net.URI.create(props.getEndpointUrl()));
            // Enable path-style access for LocalStack compatibility
            builder.forcePathStyle(true);
        }

        return builder.build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient(AwsProperties props) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(props.getAwsRegion()))
                .credentialsProvider(resolveCredentials(props));

        if (StringUtils.isNotBlank(props.getEndpointUrl())) {
            builder.endpointOverride(java.net.URI.create(props.getEndpointUrl()));
        }

        return builder.build();
    }

    @Bean
    public SqsClient sqsClient(AwsProperties props) {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(props.getAwsRegion()))
                .credentialsProvider(resolveCredentials(props));

        if (StringUtils.isNotBlank(props.getEndpointUrl())) {
            builder.endpointOverride(java.net.URI.create(props.getEndpointUrl()));
        }

        return builder.build();
    }

    private static AwsCredentialsProvider resolveCredentials(AwsProperties props) {
        if (props.getAccessKey() != null && props.getSecretKey() != null) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
            );
        }
        if (props.getProfile() != null) {
            return ProfileCredentialsProvider.create(props.getProfile());
        }
        return DefaultCredentialsProvider.create();
    }
}