package io.github.dalowed.filter;

import com.alibaba.fastjson.JSON;
import io.github.dalowed.bean.BloomInformation;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.github.dalowed.constants.CommonConstants.MD5;
import static io.github.dalowed.constants.CommonConstants.SHA_1;
import static io.github.dalowed.constants.CommonConstants.SHA_256;


/**
 * bloomfilter
 * @author dalowed
 * @since 0.0.1
 */
public class BloomFilter {

    private static final Logger log = LoggerFactory.getLogger(BloomFilter.class);
    private long[] bitArray;
    private final long size; // 位数组的大小
    private final int hashFunctions; // 哈希函数的数量
    private final Function<String, BigInteger>[] hashFunctionsArray; // 哈希函数数组
    private final List<String> hashFunctionsSaltList = new ArrayList<>();

    private static long expectedInsertions;
    private static double falsePositiveProbability;

    private static volatile BloomFilter bloomFilter;

    private final boolean isLogging;


    /**
     * Init BloomFilter
     *
     * @param expectedInsertions       expectedInsertions
     * @param falsePositiveProbability falsePositiveProbability
     * @param isLogging                Whether to enable logs
     */
    private BloomFilter(long expectedInsertions, double falsePositiveProbability, boolean isLogging) {
        this.isLogging = isLogging;
        this.expectedInsertions = expectedInsertions;
        this.falsePositiveProbability = falsePositiveProbability;

        logIfEnabled(log::info, "预计插入: " + expectedInsertions + ", 误判率: " + falsePositiveProbability);

        this.size = optimalNumOfBits(expectedInsertions, falsePositiveProbability);
//        log.info("位图大小:{}", this.size);
        this.hashFunctions = optimalNumOfHashFunctions(expectedInsertions, size);
//        log.info("哈希函数个数:{}", this.hashFunctions);

        // 计算 bitArray 的长度，确保不会超过 Integer.MAX_VALUE
        long bitArrayLength = (size + 64) / 64;
        if (bitArrayLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("The required bit array length exceeds the maximum integer value.");
        }
        this.bitArray = new long[(int) bitArrayLength]; // 每个 long 占 64 位

        this.hashFunctionsArray = createHashFunctions(hashFunctions);

//        log.info("生成的位图大小:{}, 哈希函数个数:{}", size, hashFunctions);

        logIfEnabled(log::info, "生成的位图大小: " + size + " , 哈希函数个数: " + hashFunctions);
    }

    /**
     * Recovery bloomFilter constructor
     *
     * @param bloomInformation bloomfilter information
     */
    private BloomFilter(BloomInformation bloomInformation) {
        // 是否开启日志
        this.isLogging = bloomInformation.isLogging();
        // size 大小
        this.size = bloomInformation.getSize();

        this.expectedInsertions = bloomInformation.getExpectedInsertions();
        this.falsePositiveProbability = bloomInformation.getFalsePositiveProbability();
        // hash函数个数
        this.hashFunctions = bloomInformation.getHashFunctions();
        // 位图信息
        this.bitArray = loadingBitMap(this.size);

        // 哈希函数
        this.hashFunctionsArray = createHashFunctions(hashFunctions);
        // 种子
        this.hashFunctionsSaltList.clear();
        Stream.ofNullable(bloomInformation.getSeeds())
                .flatMap(List::stream)
                .forEach(this.hashFunctionsSaltList::add);

        logIfEnabled(log::info, "恢复过滤器:" + "预计过滤数:" + bloomInformation.getExpectedInsertions() + ", 误判率:" + bloomInformation.getFalsePositiveProbability());
    }

    /**
     * Recovery BloomFilter
     *
     * @return {@link BloomFilter}
     */
    private static BloomFilter recovery() {
        // 获取信息
        BloomInformation information = getBloomInfo();

        // 初始化
        bloomFilter = new BloomFilter(information);

        return bloomFilter;
    }

    /**
     * getBloomFilter
     *
     * @param expectedInsertions       expectedInsertions
     * @param falsePositiveProbability falsePositiveProbability
     * @param isLogging                enable log
     * @param recovery                 enable recovery
     * @return {@link BloomFilter}
     */
    protected static final BloomFilter getBloomFilter(long expectedInsertions, double falsePositiveProbability, boolean isLogging, boolean recovery) {
        if (bloomFilter == null)
            synchronized (BloomFilter.class) {
                if (bloomFilter == null)
                    bloomFilter = (recovery ? recovery() : new BloomFilter(expectedInsertions, falsePositiveProbability, isLogging));
            }
        return bloomFilter;
    }

    /**
     * getBloomFilter
     * @return {@link BloomFilter}
     */
    public static final BloomFilter getBloomFilter() {
        return bloomFilter;
    }



    /**
     * Calculate bitmap size
     * @param expectedInsertions expectedInsertions
     * @param falsePositiveProbability falsePositiveProbability
     * @return {@link long}
     */
    private static long optimalNumOfBits(long expectedInsertions, double falsePositiveProbability) {
        if (expectedInsertions <=0) {
            throw new IllegalArgumentException("expectedInsertions number must be greater than zero(预插入值必须大于0), insert value:" + expectedInsertions);
        }
        if (falsePositiveProbability >= 1 || falsePositiveProbability <= 0) {
            throw new IllegalArgumentException("falsePositiveProbability rate must be between 0 and 1(误判率必须在0到1之间), insert value:" + falsePositiveProbability);
        }

        return (long) Math.ceil((-expectedInsertions * Math.log(falsePositiveProbability) / (Math.log(2) * Math.log(2))));
    }

    /**
     * Calculate the optimal number of hash functions
     * @param expectedInsertions expectedInsertions
     * @param size bitmap size
     * @return {@link int}
     */
    private static int optimalNumOfHashFunctions(long expectedInsertions, long size) {
        return Math.max(1, (int) Math.ceil((double) size / expectedInsertions * Math.log(2)));
    }

    /**
     * Create an array of hash functions
     * @param count Number of hash functions
     * @return {@link Function }
     */
    private Function<String, BigInteger>[] createHashFunctions(int count) {

        Function<String, BigInteger>[] functions = new Function[count];

        for (int i = 0; i < count; i++) {
            final int seed = i; // 使用不同的种子值来生成不同的哈希函数
            if (hashFunctionsSaltList.size() != hashFunctions) {
                hashFunctionsSaltList.add(UUID.randomUUID().toString().replace("-", "").substring(7, 17));
            }
            // 生成盐
            functions[i] = (value) -> hash(value, seed);
        }
        return functions;
    }

    /**
     * add element to bloomfilter
     * @param element element
     * @return {@link Boolean}
     */
    public final boolean add(String element) {

        element = element.trim();
        if (StringUtils.isEmpty(element)) {
            logIfEnabled(log::debug, "添加元素 " + element + " 失败, 元素不合法！");
            return false;
        }

        for (Function<String, BigInteger> hashFunction : hashFunctionsArray) {
            setBit(hashFunction.apply(element));
        }
//        log.info("添加元素 {} 成功", element);
        logIfEnabled(log::info, "添加元素 " + element + " 成功");
        return true;
    }

    /**
     * check if element exists
     * @param element element
     * @return {@link Boolean}
     */
    public final boolean isContain(String element) {
        for (Function<String, BigInteger> hashFunction : hashFunctionsArray) {
            if (!getBit(hashFunction.apply(element))) {
//                log.info("元素 {} 不存在", element);
                return false;
            }
        }
//        log.info("元素 {} 可能存在", element);
        return true;
    }


    /**
     * Set bitmap
     * @param hash hash of element
     */
    // 设置位数组中的某一位
    private void setBit(BigInteger hash) {
//        long arrayIndex = index / 64;
//        long bitIndex = index % 64;
//        System.out.println("hash值:" + index);
        int arrayIndex = hash.remainder(BigInteger.valueOf(bitArray.length)).intValue();
        int bitIndex = hash.remainder(BigInteger.valueOf(64)).intValue();
        bitArray[arrayIndex] |= (1L << bitIndex);
    }

    /**
     * Get bitmap
     * @param hash hash of element
     * @return {@link Boolean}
     */
    // 获取位数组中的某一位
    private Boolean getBit(BigInteger hash) {
//        long arrayIndex = index / 64;
//        long bitIndex = index % 64;
        int arrayIndex = hash.remainder(BigInteger.valueOf(bitArray.length)).intValue();
        int bitIndex = hash.remainder(BigInteger.valueOf(64)).intValue();
        return (bitArray[arrayIndex] & (1L << bitIndex)) != 0;
    }

    /**
     *
     * @param value hash
     * @param hashFunctionIndex Hash function location
     * @return {@link BigInteger}
     */
    // 使用不同的种子值生成多个独立的哈希值
    //  md5 sha-1 sha-256
    @Deprecated
    private BigInteger hashDeprecated(String value, int hashFunctionIndex) {
        MessageDigest messageDigest = null;
        try {
            switch (hashFunctionIndex % 3) {
                case 0:
                    messageDigest = MessageDigest.getInstance(MD5);
                    break;
                case 1:
                    messageDigest = MessageDigest.getInstance(SHA_1);
                    break;
                case 2:
                    messageDigest = MessageDigest.getInstance(SHA_256);
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        StringBuilder stringBuilder = new StringBuilder(value);
        stringBuilder.append(hashFunctionsSaltList.get(hashFunctionIndex));

        byte[] bytes = stringBuilder.toString().getBytes();
        messageDigest.update(bytes);
        byte[] hash = messageDigest.digest(bytes);

        BigInteger bigInteger = new BigInteger(1, hash);

        return bigInteger.mod(BigInteger.valueOf(size));
    }

    /**
     * calculate element`s hash by hash function and salt
     * @param value element
     * @param hashFunctionIndex hashFunctionIndex
     * @return {@link BigInteger}
     */
    private BigInteger hash(String value, int hashFunctionIndex) {
        String seed = hashFunctionsSaltList.get(hashFunctionIndex);

        byte[] bytes = null;
        try {
            bytes = hmacSHA256(value, seed);
        } catch (Exception e) {
//            log.error("计算哈希失败:{}", e.getMessage());
            logIfEnabled(log::error, "计算哈希失败: " + e.getMessage());
        }

        return new BigInteger(1, bytes);
    }


    /**
     * hash algorithm
     * @param element data
     * @param secretKey secretKey
     * @return {@link byte[]}
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    private byte[] hmacSHA256(String element, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        // 创建 Mac 实例并初始化
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        sha256Hmac.init(secretKeySpec);
        // 计算哈希值
        byte[] hashBytes = sha256Hmac.doFinal(element.getBytes());
        return hashBytes;
    }


    /**
     * getSize
     * @return {@link Long}
     */
    public long getSize() {
        return size;
    }

    /**
     * getBitArray
     * @return {@link Long[]}
     */
    public long[] getBitArray() {
        return bitArray;
    }

    /**
     * getHashFunctionsSaltList
     * @return {@link List<String> }
     */
    public List<String> getHashFunctionsSaltList() {
        return hashFunctionsSaltList;
    }

    /**
     * get isLogging
     * @return {@link Boolean}
     */
    public boolean isLogging() {
        return isLogging;
    }
    /**
     * getHashFunctions
     * @return {@link Integer}
     */
    public int getHashFunctions() {
        return hashFunctions;
    }

    /**
     * setBitArray
     * @return
     */
    private void setBitArray(long[] bitArray) {
        this.bitArray = bitArray;
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

    /**
     * generate bitmap
     * @return {@link Boolean}
     */
    public boolean generatorBitmapFile() {
        FileOutputStream fos = null;
        try {
            // 创建一个 FileWriter 对象，然后用它创建一个 BufferedWriter 对象
            String filePath = "bitmap/bitmap.bin";
            File file = new File(filePath);

            checkFile(file);

            fos = new FileOutputStream(filePath);

            for (int i = 0; i < bitArray.length; i++) {

                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.putLong(bitArray[i]);


                fos.write(buffer.array());
            }
//            log.info("bitmap保存成功,文件位置:{}", filePath);
            logIfEnabled(log::info, "bitmap保存成功,文件位置: " + filePath);
            return true;
        } catch (IOException e) {
//            log.error("bitmap信息保存失败:{}", e.getMessage());
            logIfEnabled(log::error, "bitmap信息保存失败: " + e.getMessage());
            return false;
        }finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logIfEnabled(log::error, "关闭流失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * checkFile
     * @return {@link Boolean}
     */
    private boolean checkFile(File file) {
        // 确保父目录存在
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            logIfEnabled(log::error, "Failed to create directories.");
            return false;
        }

        // 确保文件存在
        try {
            if (!file.exists() && !file.createNewFile()) {
                logIfEnabled(log::error, "Failed to create file.");
                return false;
            }
        } catch (IOException e) {
            logIfEnabled(log::error, "创建文件失败: " + e.getMessage());
        }

        return true;
    }

    /**
     * Generate filter information(json)
     * @param message message
     * @return {@link Boolean}
     */
    // 生成状态信息
    public boolean generatorInfo(String message) {
        FileOutputStream fileOutputStream = null;
        try {
            String filePath = "bitmap/info.txt";
            File file = new File(filePath);

            checkFile(file);

            fileOutputStream = new FileOutputStream(file);

            BloomInformation information = new BloomInformation();
            information.setSeeds(bloomFilter.getHashFunctionsSaltList());
            information.setSize(bloomFilter.getSize());
            information.setHashFunctions(bloomFilter.getHashFunctions());
            information.setLogging(bloomFilter.isLogging());
            information.setExpectedInsertions(bloomFilter.getExpectedInsertions());
            information.setFalsePositiveProbability(bloomFilter.getFalsePositiveProbability());

            information.setDescription(message);

            fileOutputStream.write(information.toString().getBytes());
            fileOutputStream.flush();

            logIfEnabled(log::info, "过滤器信息保存成功, 文件位置:" + filePath);
            return true;
        } catch (IOException e) {
            logIfEnabled(log::info, "生成信息文件失败: " + e.getMessage());
            return false;
        }finally {
//            log.info("过滤器信息保存成功");
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
//                log.error(e.getMessage());
                logIfEnabled(log::error, e.getMessage());
            }
        }
    }

    /**
     * logIfEnabled
     * @param logAction logAction
     * @param message message
     */
    private void logIfEnabled(Consumer<String> logAction, String message) {
        if (bloomFilter != null && ! bloomFilter.isLogging) {
            logAction.accept(message);
        }else {
            System.out.println(message);
        }

    }

    /**
     * loading bloomfilter information
     */
    private void loadingInfo() {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("bitmap/info.txt");
            byte[] allBytes = fileInputStream.readAllBytes();

        } catch (FileNotFoundException e) {
            logIfEnabled(log::error, "读取文件失败, 请检查文件是否存在！" + e.getMessage());
        } catch (IOException e) {
            logIfEnabled(log::error, "读取文件失败，请检查文件权限！ " + e.getMessage());
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    logIfEnabled(log::error, "关闭流失败" + e.getMessage());
                }
            }
        }

    }

    /**
     * loading bitmap
     * @param size bitmap size
     * @return {@link Long[] }
     */
    private static long[] loadingBitMap(long size) {
        File filename = new File("bitmap/bitmap.bin");
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(filename));
            byte[] temp = new byte[8];
            long num = 0;
            int count = 0;
            long[] longs = new long[(int)(size + 63) / 64];
            while(in.read(temp) != -1){
                for (byte b : temp) {
                    num = (num << 8) | (b & 0xFF);
                }
                longs[count++] = num;
                num = 0;
            }
            return longs;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("找不到文件: " + e);
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     *  TODO json to object
     * @return {@link BloomInformation }
     */
    private static BloomInformation getBloomInfo() {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("bitmap/info.txt");
            byte[] bytes = fileInputStream.readAllBytes();
            String s = new String(bytes);

            // json to object
            BloomInformation info = JSON.parseObject(s, BloomInformation.class);

            return info;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("读取文件内容失败: " + e.getMessage());
        }
    }
}