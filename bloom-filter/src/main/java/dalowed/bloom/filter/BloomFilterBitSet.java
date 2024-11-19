package dalowed.bloom.filter;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.UUID;
import java.util.function.Function;

/**
 * @Description // 基于 BitSet 实现的布隆过滤器
 * @Author dalowed
 * @Date 2024-11-12 14:58
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

    public BloomFilterBitSet(long expectedInsertions, double falsePositiveProbability) {
        this.size = optimalNumOfBits(expectedInsertions, falsePositiveProbability);
        this.hashFunctions = optimalNumOfHashFunctions(expectedInsertions, size);
        this.bitSet = new BitSet(size);
        this.hashFunctionsArray = createHashFunctions(hashFunctions);
    }

    // 计算最佳位数组大小
    private static int optimalNumOfBits(long n, double p) {
        if (p == 0) {
            p = Double.MIN_VALUE;
        }
        return (int) Math.ceil((-n * Math.log(p) / (Math.log(2) * Math.log(2))));
    }

    // 计算最佳哈希函数数量
    private static int optimalNumOfHashFunctions(long n, long m) {
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }

    // 创建哈希函数数组
    private Function<String, Integer>[] createHashFunctions(int count) {
        // 生成盐
        for (int i = 0; i < count; i++) {
            // TODO 生成盐
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

    // 添加元素到布隆过滤器
    public final void add(String element) {
        for (Function<String, Integer> hashFunction : hashFunctionsArray) {
            bitSet.set(hashFunction.apply(element));
        }
        log.info("元素 {} 添加成功", element);
    }

    // 检查元素是否可能存在于布隆过滤器中
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

    // 使用不同的哈希算法生成多个哈希值
    private int hash(String value, int hashFunctionIndex) {
        StringBuilder stringBuilder = new StringBuilder(value);
        stringBuilder.append(hashFunctionsSalt.get(hashFunctionIndex));
        value = stringBuilder.toString();
        switch (hashFunctionIndex % 3) { // 使用模运算来循环选择不同的哈希函数
            case 0:
                return hashHashMap(value);
            case 1:
                return hashMurmurHash3(value);
            case 2:
                return hash3(value);
            default:
                throw new IllegalArgumentException("Invalid hash function index: " + hashFunctionIndex);
        }
    }

    // 第一个哈希函数：HashMap哈希算法
    private int hashHashMap(String value) {
        int h;
        return (value == null) ? 0 :
                Math.abs(((h = value.hashCode()) ^ (h >>> 16)) % size);
    }

    // 第二个哈希函数：MurmurHash3
    private int hashMurmurHash3(String value) {
        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;
        final int r1 = 15;
        final int r2 = 13;
        final int m = 5;
        final int n = 0xe6546b64;

        int h1 = 0;
        byte[] data = value.getBytes();
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


    private int hash3(String value) {
        int hash = 0;
        for (int i = 0; i < value.length(); i++) {
            hash = hash * 17 + value.charAt(i);
        }
        return Math.abs(hash % size);
    }

    public int getSize() {
        return size;
    }

    public ArrayList<String> getHashFunctionsList() {
        return hashFunctionsList;
    }
}