package dalowed.bloom.filter;

import dalowed.bloom.bean.BloomInformation;
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


/**
 * @Description // 布隆过滤器
 * @Author dalowed
 * @Date 2024-11-12 15:58
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

    private static final String SHA_1 = "SHA-1";
    private static final String SHA_256 = "SHA-256";
    private static final String MD5 = "MD5";

    private BloomFilter(long expectedInsertions, double falsePositiveProbability, boolean isLogging) {
        this.isLogging = isLogging;
        this.expectedInsertions = expectedInsertions;
        this.falsePositiveProbability = falsePositiveProbability;
        System.out.println("预计插入: " + expectedInsertions + ", 误判率: " + falsePositiveProbability);

        this.size = optimalNumOfBits(expectedInsertions, falsePositiveProbability);
//        log.info("位图大小:{}", this.size);
        this.hashFunctions = optimalNumOfHashFunctions(expectedInsertions, size);
//        log.info("哈希函数个数:{}", this.hashFunctions);

        // 计算 bitArray 的长度，确保不会超过 Integer.MAX_VALUE
        long bitArrayLength = (size + 63) / 64;
        if (bitArrayLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("The required bit array length exceeds the maximum integer value.");
        }
        this.bitArray = new long[(int) bitArrayLength]; // 每个 long 占 64 位

        this.hashFunctionsArray = createHashFunctions(hashFunctions);

//        log.info("生成的位图大小:{}, 哈希函数个数:{}", size, hashFunctions);
        System.out.println("生成的位图大小: " + size + " , 哈希函数个数: " + hashFunctions);
    }

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
        this.bitArray = loaddingBitMap(this.size);

        // 哈希函数
        this.hashFunctionsArray = createHashFunctions(hashFunctions);
        // 种子
        this.hashFunctionsSaltList.clear();
        Stream.ofNullable(bloomInformation.getSeeds())
                .flatMap(List::stream)
                .forEach(this.hashFunctionsSaltList::add);
    }

    private static BloomFilter recovery() {
        // 获取信息
        BloomInformation information = getBloomInfo();

        // 初始化
        bloomFilter = new BloomFilter(information);

        return bloomFilter;
    }

    public static final BloomFilter getBloomFilter(long expectedInsertions, double falsePositiveProbability, boolean isLogging, boolean recovery) {
        if(bloomFilter == null)
            synchronized (BloomFilter.class) {
                if (bloomFilter == null)
                    bloomFilter = (recovery ? recovery() : new BloomFilter(expectedInsertions, falsePositiveProbability, isLogging));
            }
        return bloomFilter;
    }


    // 计算最佳位数组大小
    private static long optimalNumOfBits(long n, double p) {
        // 进行判断
        if (n <=0) {
            throw new IllegalArgumentException("expectedInsertions number must be greater than zero(预插入值必须大于0), insert value:" + n);
        }
        if (p >= 1 || p <= 0) {
            throw new IllegalArgumentException("falsePositiveProbability rate must be between 0 and 1(误判率必须在0到1之间), insert value:" + p);
        }

        return (long) Math.ceil((-n * Math.log(p) / (Math.log(2) * Math.log(2))));
    }

    // 计算最佳哈希函数数量
    private static int optimalNumOfHashFunctions(long n, long m) {
        return Math.max(1, (int) Math.ceil((double) m / n * Math.log(2)));
    }

    // 创建哈希函数数组
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

    // 添加元素到布隆过滤器
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

    // 检查元素是否可能存在于布隆过滤器中
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


    // 设置位数组中的某一位
    private void setBit(BigInteger index) {
//        long arrayIndex = index / 64;
//        long bitIndex = index % 64;
//        System.out.println("hash值:" + index);
        int arrayIndex = index.divide(BigInteger.valueOf(64)).intValue();
        int bitIndex = index.remainder(BigInteger.valueOf(64)).intValue();
        bitArray[arrayIndex] |= (1L << bitIndex);
    }

    // 获取位数组中的某一位
    private boolean getBit(BigInteger index) {
//        long arrayIndex = index / 64;
//        long bitIndex = index % 64;
        int arrayIndex = index.divide(BigInteger.valueOf(64)).intValue();
        int bitIndex = index.remainder(BigInteger.valueOf(64)).intValue();
        return (bitArray[arrayIndex] & (1L << bitIndex)) != 0;
    }

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

    private BigInteger hash(String value, int hashFunctionIndex) {
        String seed = hashFunctionsSaltList.get(hashFunctionIndex);

        byte[] bytes = null;
        try {
            bytes = hmacSHA256(value, seed);
        } catch (Exception e) {
//            log.error("计算哈希失败:{}", e.getMessage());
            logIfEnabled(log::error, "计算哈希失败: " + e.getMessage());
        }

        return new BigInteger(1, bytes).mod(BigInteger.valueOf(size));
    }


    private byte[] hmacSHA256(String data, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        // 创建 Mac 实例并初始化
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        sha256Hmac.init(secretKeySpec);

        // 计算哈希值
        byte[] hashBytes = sha256Hmac.doFinal(data.getBytes());

        return hashBytes;
    }


    public long getSize() {
        return size;
    }

    public long[] getBitArray() {
        return bitArray;
    }

    public List<String> getHashFunctionsSaltList() {
        return hashFunctionsSaltList;
    }

    public boolean isLogging() {
        return isLogging;
    }

    public int getHashFunctions() {
        return hashFunctions;
    }

    private void setBitArray(long[] bitArray) {
        this.bitArray = bitArray;
    }

    public long getExpectedInsertions() {
        return expectedInsertions;
    }

    public double getFalsePositiveProbability() {
        return falsePositiveProbability;
    }

    /**
     * 生成二进制文件
     * @return
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

    private boolean checkFile(File file) {
        // 确保父目录存在
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
//                System.out.println("Failed to create directories.");
            logIfEnabled(log::error, "Failed to create directories.");
            return false;
        }

        // 确保文件存在
        try {
            if (!file.exists() && !file.createNewFile()) {
    //                System.out.println("Failed to create file.");
                logIfEnabled(log::error, "Failed to create file.");
                return false;
            }
        } catch (IOException e) {
            logIfEnabled(log::error, "创建文件失败: " + e.getMessage());
        }

        return true;
    }

    // 生成状态信息
    public boolean generatorInfo() {
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

            information.setDescription("位图相关信息");

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

    private void logIfEnabled(Consumer<String> logAction, String message) {
        if (bloomFilter.isLogging()) {
            logAction.accept(message);
        }else {
            System.out.println(message);
        }
    }

    /**
     * 载入相关信息
     */
    private void loaddingInfo() {
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
     * 载入bitmap文件
     */
    private static long[] loaddingBitMap(long size) {
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
     *  TODO json转对象
     * @return BloomInformation
     */
    private static BloomInformation getBloomInfo() {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("bitmap/info.txt");
            byte[] bytes = fileInputStream.readAllBytes();
            String s = new String(bytes);


            return null;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("读取文件内容失败: " + e.getMessage());
        }
    }
}