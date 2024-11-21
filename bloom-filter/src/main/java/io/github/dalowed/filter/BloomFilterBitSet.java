package io.github.dalowed.filter;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.function.Function;



/**
 * bloomfilter based on BitSet
 * @author dalowed
 * @since 0.0.1
 */

@Deprecated
public class BloomFilterBitSet {

    private static final Logger log = LoggerFactory.getLogger(BloomFilterBitSet.class);

    private final BitSet bitSet;
    private final int size; // 位数组的大小
    private final int hashFunctions; // 哈希函数的数量
    private final Function<String, Integer>[] hashFunctionsArray; // 哈希函数数组
    private final ArrayList<String> hashFunctionsList = new ArrayList<>();
    private final ArrayList<String> hashFunctionsSalt = new ArrayList<>();

    /**
     * Init BloomFilterBitSet
     * @param expectedInsertions expectedInsertions
     * @param falsePositiveProbability falsePositiveProbability
     */
    public BloomFilterBitSet(long expectedInsertions, double falsePositiveProbability) {
        this.size = optimalNumOfBits(expectedInsertions, falsePositiveProbability);
        this.hashFunctions = optimalNumOfHashFunctions(expectedInsertions, size);
        this.bitSet = new BitSet(size);
        this.hashFunctionsArray = createHashFunctions(hashFunctions);
    }

    /**
     * caculate bitmap size
     * @param expectedInsertions expectedInsertions
     * @param falsePositiveProbability falsePositiveProbability
     * @return {@link Integer}
     */
    private static int optimalNumOfBits(long expectedInsertions, double falsePositiveProbability) {
        if (falsePositiveProbability == 0) {
            falsePositiveProbability = Double.MIN_VALUE;
        }
        return (int) Math.ceil((-expectedInsertions * Math.log(falsePositiveProbability) / (Math.log(2) * Math.log(2))));
    }

    /**
     * caculate number of hash functions
     * @param expectedInsertions expectedInsertions
     * @param size bitmap size
     * @return {@link Integer}
     */
    private static int optimalNumOfHashFunctions(long expectedInsertions, long size) {
        return Math.max(1, (int) Math.round((double) size / expectedInsertions * Math.log(2)));
    }

    /**
     * create arry of hash functions
     * @param count hash functions counts
     * @return {@link Function<String,Integer>[] }
     */
    private Function<String, Integer>[] createHashFunctions(int count) {
        for (int i = 0; i < count; i++) {
            // create salt
            hashFunctionsSalt.add("hello");
        }

        hashFunctionsList.add("一共有 " + count + " 个哈希函数");
        Function<String, Integer>[] functions = new Function[count];
        for (int i = 0; i < count; i++) {
            final int index = i;
            functions[i] = (value) -> hash(value, index);
            switch (i % 3) {
                case 0:
                    hashFunctionsList.add("\n第 " + i + " 个哈希函数是 hashmap 哈希算法");
                    break;
                case 1:
                    hashFunctionsList.add("\n第 " + i + " 个哈希函数是 MurmurHash3 哈希算法");
                    break;
                case 2:
                    hashFunctionsList.add("\n第 " + i +" 个哈希函数是 简单的字符串哈希算法");
                    break;
            }
        }
        return functions;
    }

    /**
     * add element to bloomfilter
     * @param element element
     */
    public final void add(String element) {
        for (Function<String, Integer> hashFunction : hashFunctionsArray) {
            bitSet.set(hashFunction.apply(element));
        }
        log.info("元素 {} 添加成功", element);
    }

    /**
     * check element
     * @param element element
     * @return {@link Boolean}
     */
    public final boolean mightContain(String element) {
        for (Function<String, Integer> hashFunction : hashFunctionsArray) {
            if (!bitSet.get(hashFunction.apply(element))) {
                log.info("元素 {} 不存在！" , element);
                return false;
            }
        }
        log.info("元素 {} 可能存在！", element);
        return true;
    }

    /**
     * use different hash functions
     * @param element element
     * @param hashFunctionIndex
     * @return {@link Integer}
     */
    private int hash(String element, int hashFunctionIndex) {
        StringBuilder stringBuilder = new StringBuilder(element);
        stringBuilder.append(hashFunctionsSalt.get(hashFunctionIndex));
        element = stringBuilder.toString();
        switch (hashFunctionIndex % 3) { // 使用模运算来循环选择不同的哈希函数
            case 0:
                return hashHashMap(element);
            case 1:
                return hashMurmurHash3(element);
            case 2:
                return hash3(element);
            default:
                throw new IllegalArgumentException("Invalid hash function index: " + hashFunctionIndex);
        }
    }

    /**
     * bad hash
     * @param element element
     * @return {@link Integer}
     */
    // 第一个哈希函数：HashMap哈希算法
    @Deprecated
    private int hashHashMap(String element) {
        int h;
        return (element == null) ? 0 :
                Math.abs(((h = element.hashCode()) ^ (h >>> 16)) % size);
    }

    /**
     * MurmurHash3
     * @param element element
     * @return {@link Integer}
     */
    private int hashMurmurHash3(String element) {
        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;
        final int r1 = 15;
        final int r2 = 13;
        final int m = 5;
        final int n = 0xe6546b64;

        int h1 = 0;
        byte[] data = element.getBytes();
        int len = data.length;
        int roundedEnd = len & 0xfffffffc;

        for (int i = 0; i < roundedEnd; i += 4) {
            int k1 = (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8) |
                    ((data[i + 2] & 0xff) << 16) | ((data[i + 3] & 0xff) << 24);

            k1 *= c1;
            k1 = Integer.rotateLeft(k1, r1);
            k1 *= c2;

            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, r2) * m + n;
        }

        int k1 = 0;
        switch (len & 3) {
            case 3:
                k1 ^= (data[roundedEnd + 2] & 0xff) << 16;
            case 2:
                k1 ^= (data[roundedEnd + 1] & 0xff) << 8;
            case 1:
                k1 ^= (data[roundedEnd] & 0xff);
                k1 *= c1;
                k1 = Integer.rotateLeft(k1, r1);
                k1 *= c2;
                h1 ^= k1;
        }

        h1 ^= len;
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;

        return Math.abs(h1 % size);
    }


    /**
     * simple of hash
     * @param element
     * @return {@link Integer}
     */
    private int hash3(String element) {
        int hash = 0;
        for (int i = 0; i < element.length(); i++) {
            hash = hash * 17 + element.charAt(i);
        }
        return Math.abs(hash % size);
    }

    /**
     * getSize
     * @return {@link Integer}
     */
    public int getSize() {
        return size;
    }

    /**
     * getHashFunctionsList
     * @return {@link ArrayList<String>}
     */
    public ArrayList<String> getHashFunctionsList() {
        return hashFunctionsList;
    }
}