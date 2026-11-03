package com.nexushr.insights.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class AiProviderConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
    ChatClient openAiChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
    RestClientCustomizer aiProviderHttpTimeouts(
        @Value("${app.ai.connect-timeout:PT3S}") Duration connectTimeout,
        @Value("${app.ai.read-timeout:PT15S}") Duration readTimeout
    ) {
        return builder -> {
            HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
            JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(readTimeout);
            builder.requestFactory(requestFactory);
        };
    }
}
