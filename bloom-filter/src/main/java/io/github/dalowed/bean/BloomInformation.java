package io.github.dalowed.bean;

import java.util.List;


/**
 * bloomfilter information bean
 * @author dalowed
 * @since 0.0.1
 */
public class BloomInformation {
    private String description; // 描述
    private long size; // 位图大小
    private int hashFunctions; // 哈希函数个数
    private List<String> seeds; // 种子数
    private long expectedInsertions;
    private double falsePositiveProbability;
    private boolean isLogging;


    /**
     * init BloomInformation
     * @param description description
     * @param size bitmap size
     * @param hashFunctions hashFunction counts
     * @param seeds seeds
     * @param expectedInsertions expectedInsertions
     * @param falsePositiveProbability falsePositiveProbability
     * @param isLogging enable log
     */
    public BloomInformation(String description, long size, int hashFunctions, List<String> seeds, long expectedInsertions, double falsePositiveProbability, boolean isLogging) {
        this.description = description;
        this.size = size;
        this.hashFunctions = hashFunctions;
        this.seeds = seeds;
        this.expectedInsertions = expectedInsertions;
        this.falsePositiveProbability = falsePositiveProbability;
        this.isLogging = isLogging;
    }

    /**
     * No-argument constructor
     */
    public BloomInformation() {
    }


    /**
     * getExpectedInsertions
     * @return {@link Long}
     */
    public long getExpectedInsertions() {
        return expectedInsertions;
    }

    /**
     * setExpectedInsertions
     * @param expectedInsertions expectedInsertions
     */
    public void setExpectedInsertions(long expectedInsertions) {
        this.expectedInsertions = expectedInsertions;
    }

    /**
     * getFalsePositiveProbability
     * @return {@link Double}
     */
    public double getFalsePositiveProbability() {
        return falsePositiveProbability;
    }

    /**
     * setFalsePositiveProbability
     * @param falsePositiveProbability falsePositiveProbability
     */
    public void setFalsePositiveProbability(double falsePositiveProbability) {
        this.falsePositiveProbability = falsePositiveProbability;
    }

    /**
     * isLogging
     * @return {@link Boolean}
     */
    public boolean isLogging() {
        return isLogging;
    }

    /**
     * setLogging
     * @param logging enable log
     */
    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    /**
     * getDescription
     * @return {@link String}
     */
    public String getDescription() {
        return description;
    }

    /**
     * setDescription
     * @param description description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * getSize
     * @return {@link Long}
     */
    public long getSize() {
        return size;
    }

    /**
     * setSize
     * @param size bitmap size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * getHashFunctions
     * @return {@link Integer}
     */
    public int getHashFunctions() {
        return hashFunctions;
    }

    /**
     * setHashFunctions
     * @param hashFunctions hashFunctions counts
     */
    public void setHashFunctions(int hashFunctions) {
        this.hashFunctions = hashFunctions;
    }

    /**
     * getSeeds
     * @return {@link List<String> }
     */
    public List<String> getSeeds() {
        return seeds;
    }

    /**
     * setSeeds
     * @param seeds seeds
     */
    public void setSeeds(List<String> seeds) {
        this.seeds = seeds;
    }

    /**
     * toJsonString
     * @return {@link String}
     */
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

    /**
     * escapeJson
     * @param value value
     * @return {@link String}
     */
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
