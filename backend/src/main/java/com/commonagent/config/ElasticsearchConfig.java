package com.commonagent.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchConfig {

    /** 是否启用 ES，默认 false 避免无 ES 时启动失败 */
    private boolean enabled = false;
    private List<String> hosts = List.of("localhost:9200");
    private String username;
    private String password;
    private String scheme = "http";
    private String indexPrefix = "ca_knowledge";

    @Bean
    @ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
    public ElasticsearchClient elasticsearchClient() {
        log.info("[ElasticsearchConfig] Creating ElasticsearchClient, hosts={}", hosts);
        HttpHost[] httpHosts = hosts.stream()
                .map(host -> {
                    String[] parts = host.split(":");
                    return new HttpHost(parts[0], Integer.parseInt(parts[1]), scheme);
                })
                .toArray(HttpHost[]::new);

        RestClient restClient;
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            restClient = RestClient.builder(httpHosts)
                    .setHttpClientConfigCallback(hcb -> hcb.setDefaultCredentialsProvider(cp))
                    .build();
        } else {
            restClient = RestClient.builder(httpHosts).build();
        }

        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
