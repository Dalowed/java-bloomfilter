package io.github.dalowed.filter;

import io.github.dalowed.configuration.BloomConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * auto assembly class
 * @author dalowed
 * @since 0.0.1
 */
@Configuration
@EnableConfigurationProperties(BloomConfigurationProperties.class)
public class BloomConfiguration {

    /**
     *
     * @param properties bloomfilter configuration
     * @return {@link BloomFilter}
     */
    @Bean
    @ConditionalOnMissingBean
    public BloomFilter bloomFilter(BloomConfigurationProperties properties) {
       return BloomFilter.getBloomFilter(properties.getExpectedInsertions(), properties.getFalsePositiveProbability(), properties.isEnableLogging(), properties.isRecovery());
    }

}
