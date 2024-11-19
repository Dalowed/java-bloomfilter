package dalowed.bloom.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Description 布隆过滤器配置文件类
 * @Author dalowed
 * @Date 2024-11-18 16:39
 */
@ConfigurationProperties(prefix = "bloom-filter")
public class BloomConfigurationProperties {

    // 映射 bloomFilterLong.expectedInsertions, 过滤器的期望插入数
    private long expectedInsertions;

    // 映射 bloomFilterLong.falsePositiveProbability, 过滤器的容错率
    private double falsePositiveProbability;

    // 映射 bloomFilterLong.enableLogging,是否开启日志
    private boolean enableLogging = true;

    public void setExpectedInsertions(long expectedInsertions) {
        this.expectedInsertions = expectedInsertions;
    }

    public void setFalsePositiveProbability(double falsePositiveProbability) {
        this.falsePositiveProbability = falsePositiveProbability;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public long getExpectedInsertions() {
        return expectedInsertions;
    }

    public double getFalsePositiveProbability() {
        return falsePositiveProbability;
    }
}
