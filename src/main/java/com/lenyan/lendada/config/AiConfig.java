package com.lenyan.lendada.config;


import ai.z.openapi.ZhipuAiClient;
import com.zhipu.oapi.ClientV4;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.*;
import ai.z.openapi.core.Constants;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
@Slf4j
public class AiConfig {

    /**
     * apiKey，需要从开放平台获取
     */
    private String apiKey;

    @Bean
    public ZhipuAiClient zhipuAiClient() {
        log.info("Zhipu apiKey loaded: {}", apiKey);
        return ZhipuAiClient.builder().ofZHIPU()
                .apiKey(apiKey)
                .build();
    }
}

//@Bean
//public ClientV4 getClientV4()  { return new ClientV4.Builder(apiKey).build();}