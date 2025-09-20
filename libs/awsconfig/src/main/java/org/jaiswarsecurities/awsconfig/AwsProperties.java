package org.jaiswarsecurities.awsconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The AwsProperties class holds configuration properties for AWS services.
 * It includes properties for S3, DynamoDB, and AWS region settings.
 *
 * @author Sandeep Jaiswar
 */
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {
    private String awsRegion;
    private String accessKey;
    private String secretKey;
    private String endpointUrl;
    private boolean useLocalStack;
    private String profile;

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public boolean isUseLocalStack() {
        return useLocalStack;
    }

    public void setUseLocalStack(boolean useLocalStack) {
        this.useLocalStack = useLocalStack;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}