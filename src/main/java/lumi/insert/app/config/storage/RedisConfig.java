package lumi.insert.app.config.storage;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.Map;

@Configuration
public class RedisConfig {

  @Bean
  @Profile("test")
  public CacheManager simpleCacheManager() {
    return new ConcurrentMapCacheManager();
  }

  @Bean
  @Profile("!test")
  RedisTemplate<String, Object> redisTemplate (RedisConnectionFactory redisConnectionFactory){
    RedisTemplate<String, Object> redis = new RedisTemplate<>();

    BasicPolymorphicTypeValidator.Builder builder = BasicPolymorphicTypeValidator
        .builder()
          .allowIfSubType(Object.class);

    JsonMapper mapper = JsonMapper.builder()
        .addModules(SecurityJacksonModules.getModules(this.getClass().getClassLoader(), builder))
        .build();

    GenericJacksonJsonRedisSerializer genericJacksonJsonRedisSerializer = new GenericJacksonJsonRedisSerializer(mapper);

    redis.setConnectionFactory(redisConnectionFactory);

    redis.setKeySerializer(new StringRedisSerializer());
    redis.setHashKeySerializer(new StringRedisSerializer());

    redis.setValueSerializer(genericJacksonJsonRedisSerializer);
    redis.setHashValueSerializer(genericJacksonJsonRedisSerializer);

    return redis;
  }

  @Bean
  @Profile("!test")
  RedisCacheManager redisCacheManager (RedisConnectionFactory redisConnectionFactory){
    BasicPolymorphicTypeValidator.Builder builder = BasicPolymorphicTypeValidator
        .builder()
        .allowIfSubType(Object.class);

    JsonMapper mapper = JsonMapper.builder()
        .addModules(SecurityJacksonModules.getModules(this.getClass().getClassLoader(), builder))
        .build();

    GenericJacksonJsonRedisSerializer genericJacksonJsonRedisSerializer = new GenericJacksonJsonRedisSerializer(mapper);

    RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofHours(1))
        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericJacksonJsonRedisSerializer))
        .disableCachingNullValues();

    Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = Map.of(
        "products:first-page", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30))
    );

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(redisCacheConfiguration)
        .withInitialCacheConfigurations(redisCacheConfigurationMap)
        .build();
  }
}
