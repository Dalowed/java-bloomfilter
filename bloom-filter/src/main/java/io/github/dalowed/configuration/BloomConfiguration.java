package io.github.dalowed.configuration;

import io.github.dalowed.filter.BloomFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description // automatic assembly class
 * @Author dalowed
 * @Date 2024-11-18 16:13
 */

@Configuration
@EnableConfigurationProperties(BloomConfigurationProperties.class)
public class BloomConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public BloomFilter bloomFilter(BloomConfigurationProperties properties) {
       return BloomFilter.getBloomFilter(properties.getExpectedInsertions(), properties.getFalsePositiveProbability(), properties.isEnableLogging(), properties.isRecovery());
    }

}
