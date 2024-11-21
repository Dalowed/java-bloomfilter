package dalowed.bloom.bean;

import java.util.List;


/**
 * @Description // 配置文件类
 * @Author dalowed
 * @Date 2024-11-19 14:13
 */
public class BloomInformation {
    private String description; // 描述
    private long size; // 位图大小
    private int hashFunctions; // 哈希函数个数
    private List<String> seeds; // 种子数
    private long expectedInsertions;
    private double falsePositiveProbability;
    private boolean isLogging;


    public BloomInformation(String description, long size, int hashFunctions, List<String> seeds, long expectedInsertions, double falsePositiveProbability, boolean isLogging) {
        this.description = description;
        this.size = size;
        this.hashFunctions = hashFunctions;
        this.seeds = seeds;
        this.expectedInsertions = expectedInsertions;
        this.falsePositiveProbability = falsePositiveProbability;
        this.isLogging = isLogging;
    }

    public BloomInformation() {
    }



    public long getExpectedInsertions() {
        return expectedInsertions;
    }

    public void setExpectedInsertions(long expectedInsertions) {
        this.expectedInsertions = expectedInsertions;
    }

    public double getFalsePositiveProbability() {
        return falsePositiveProbability;
    }

    public void setFalsePositiveProbability(double falsePositiveProbability) {
        this.falsePositiveProbability = falsePositiveProbability;
    }

    public boolean isLogging() {
        return isLogging;
    }

    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getHashFunctions() {
        return hashFunctions;
    }

    public void setHashFunctions(int hashFunctions) {
        this.hashFunctions = hashFunctions;
    }

    public List<String> getSeeds() {
        return seeds;
    }

    public void setSeeds(List<String> seeds) {
        this.seeds = seeds;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"description\":\"").append(escapeJson(description)).append("\",");
        sb.append("\"size\":").append(size).append(",");
        sb.append("\"hashFunctions\":").append(hashFunctions).append(",");
        sb.append("\"expectedInsertions\":").append(expectedInsertions).append(",");
        sb.append("\"falsePositiveProbability\":").append(falsePositiveProbability).append(",");
        sb.append("\"isLogging\":").append(isLogging).append(",");
        sb.append("\"seeds\":[");
        if (seeds != null && !seeds.isEmpty()) {
            for (int i = 0; i < seeds.size(); i++) {
                sb.append("\"").append(escapeJson(seeds.get(i))).append("\"");
                if (i < seeds.size() - 1) {
                    sb.append(",");
                }
            }
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
