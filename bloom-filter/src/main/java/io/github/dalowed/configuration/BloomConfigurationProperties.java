package io.github.dalowed.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * configuration class
 * @author dalowed
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "bloom-filter")
public class BloomConfigurationProperties {

    // 映射 bloomFilterLong.expectedInsertions, 过滤器的期望插入数
    private long expectedInsertions;

    // 映射 bloomFilterLong.falsePositiveProbability, 过滤器的容错率
    private double falsePositiveProbability;

    // 映射 bloomFilterLong.enableLogging,是否开启日志
    private boolean enableLogging = true;

    private boolean recovery = false;

    /**
     * get recovery
     * @return {@link Boolean}
     */
    public boolean isRecovery() {
        return recovery;
    }

    /**
     * enable recovery
     * @param recovery recovery
     */
    public void setRecovery(boolean recovery) {
        this.recovery = recovery;
    }

    /**
     * setExpectedInsertions
     * @param expectedInsertions expectedInsertions
     */
    public void setExpectedInsertions(long expectedInsertions) {
        this.expectedInsertions = expectedInsertions;
    }

    /**
     * setFalsePositiveProbability
     * @param falsePositiveProbability falsePositiveProbability
     */
    public void setFalsePositiveProbability(double falsePositiveProbability) {
        this.falsePositiveProbability = falsePositiveProbability;
    }

    /**
     * setEnableLogging
     * @param enableLogging enable log
     */
    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    /**
     * isEnableLogging
     * @return {@link Boolean}
     */
    public boolean isEnableLogging() {
        return enableLogging;
    }

    /**
     * getExpectedInsertions
     * @return {@link Long}
     */
    public long getExpectedInsertions() {
        return expectedInsertions;
    }

    /**
     * getFalsePositiveProbability
     * @return {@link Double}
     */
    public double getFalsePositiveProbability() {
        return falsePositiveProbability;
    }
}
