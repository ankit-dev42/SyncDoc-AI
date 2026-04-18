package com.syncdoc.collaboration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "integrations")
public class BusinessValidationProperties {

    private final Stripe stripe = new Stripe();
    private final Github github = new Github();
    private final Openai openai = new Openai();

    public Stripe getStripe() {
        return stripe;
    }

    public Github getGithub() {
        return github;
    }

    public Openai getOpenai() {
        return openai;
    }

    public static class Stripe {
        private String secretKey;
        private String publishableKey;
        private String webhookSecret;

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getPublishableKey() {
            return publishableKey;
        }

        public void setPublishableKey(String publishableKey) {
            this.publishableKey = publishableKey;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }

    public static class Github {
        private String clientId;
        private String clientSecret;
        private String webhookSecret;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }

    public static class Openai {
        private String apiKey;
        private String model;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}