package dalowed.bloom.configuration;

import dalowed.bloom.filter.BloomFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @Description // 自动装配类
 * @Author dalowed
 * @Date 2024-11-18 16:13
 */

@Configuration
@EnableConfigurationProperties(BloomConfigurationProperties.class)
public class BloomConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public BloomFilter bloomFilter(BloomConfigurationProperties properties) {
       return new BloomFilter(properties.getExpectedInsertions(), properties.getFalsePositiveProbability(), properties.isEnableLogging());
    }

}
