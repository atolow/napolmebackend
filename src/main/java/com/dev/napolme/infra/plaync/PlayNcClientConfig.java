package com.dev.napolme.infra.plaync;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PlayNcProperties.class)
public class PlayNcClientConfig {

    @Bean
    public RestClient playNcRestClient(PlayNcProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .defaultHeaders(headers -> {
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                headers.set(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
                headers.set(
                    HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/144.0.0.0 Safari/537.36"
                );
                headers.set(HttpHeaders.ORIGIN, "https://aion2.plaync.com");
                headers.set(HttpHeaders.REFERER, "https://aion2.plaync.com/ko-kr/characters");
            })
            .requestFactory(requestFactory)
            .build();
    }
}
