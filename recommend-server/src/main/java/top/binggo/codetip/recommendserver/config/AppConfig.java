package top.binggo.codetip.recommendserver.config;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author binggo
 */
@SpringBootConfiguration
@EnableElasticsearchRepositories
public class AppConfig {
    @Value("${spring.data.elasticsearch.cluster-name}")
    private String clusterName;

    @Value("${spring.data.elasticsearch.cluster-nodes}")
    private String address;

    @Value("${codeTip.es.recommend.indexName}")
    private String indexName;

    @Value("${codeTip.es.recommend.indexType}")
    private String indexType;

    public String getIndexName() {
        return indexName;
    }

    public String getIndexType() {
        return indexType;
    }

    @Bean
    public PreBuiltTransportClient preBuiltTransportClient() throws UnknownHostException {
        Settings setting = Settings.builder().put("cluster.name", this.clusterName).build();
        PreBuiltTransportClient preBuiltTransportClient = new PreBuiltTransportClient(setting);
        InetAddress address = InetAddress.getByName(
                this.address.substring(0, this.address.indexOf(':')));
        preBuiltTransportClient.addTransportAddress(new TransportAddress(address, 9300));
        return preBuiltTransportClient;
    }
}
