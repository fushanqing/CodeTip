package top.binggo.codetipcontrolcenter.config;

import codetip.commons.crawl.RedisTaskAccessor;
import io.lettuce.core.resource.ClientResources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import top.binggo.codetip.filesystem.core.MavenRedisFileSystemAccessor;

import java.time.Duration;

/**
 * @author binggo
 */
@Configuration
public class AppConfig {


    @Value("${spring.redis.host}")
    private String redisHost;


    @Bean
    public RedisConnectionFactory lettuceRedisConnectionFactory() {
        LettuceConnectionFactory lettuceClient = new LettuceConnectionFactory(new RedisStandaloneConfiguration(redisHost, 6379),
                LettuceClientConfiguration.builder().clientName("lettuceClient").clientResources(ClientResources.builder().build()).commandTimeout(Duration.ofMillis(1000)).build());
        lettuceClient.setDatabase(1);
        return lettuceClient;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory lettuceRedisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        RedisSerializer<String> string = RedisSerializer.string();
        redisTemplate.setValueSerializer(string);
        redisTemplate.setConnectionFactory(lettuceRedisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler("http://localhost:8080/");
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler("http://localhost:8080/");
    }


    @Bean
    @SuppressWarnings("unchecked")
    public RedisTemplate jsonRedisTemplate(RedisConnectionFactory lettuceRedisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setValueSerializer(RedisSerializer.json());
        redisTemplate.setHashValueSerializer(RedisSerializer.json());
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setConnectionFactory(lettuceRedisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public RedisTaskAccessor redisTaskAccessor(RedisTemplate jsonRedisTemplate, StringRedisTemplate stringRedisTemplate) {
        return new RedisTaskAccessor(jsonRedisTemplate, stringRedisTemplate);
    }

    @Bean
    public MavenRedisFileSystemAccessor mavenRedisFileSystemAccessor(StringRedisTemplate stringRedisTemplate) {
        return new MavenRedisFileSystemAccessor(stringRedisTemplate);
    }
}
