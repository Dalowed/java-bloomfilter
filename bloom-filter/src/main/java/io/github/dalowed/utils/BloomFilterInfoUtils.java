package io.github.dalowed.utils;

import com.alibaba.fastjson.JSON;
import io.github.dalowed.bean.BloomInformation;
import io.github.dalowed.filter.BloomFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;


/**
 * @author dalowed
 * @since 0.0.3
 */
public class BloomFilterInfoUtils {
    private static final Logger log = LoggerFactory.getLogger(BloomFilterInfoUtils.class);

    /**
     * generate bitmap
     * @return {@link Boolean}
     */
    public static boolean generatorBitmapFile(long[] bitArray) {
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
//            logIfEnabled(log::info, "bitmap保存成功,文件位置: " + filePath);
            log.info("bitmap generate success: {}", file.getAbsoluteFile());
            return true;
        } catch (IOException e) {
//            log.error("bitmap信息保存失败:{}", e.getMessage());
//            logIfEnabled(log::error, "bitmap信息保存失败: " + e.getMessage());
            log.error("bitmap generate error", e.getMessage());
            return false;
        }finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
//                    logIfEnabled(log::error, "关闭流失败: " + e.getMessage());
                    log.error("bitmap stream close error: ", e.getMessage());
                }
            }
        }
    }
    /**
     * checkFile
     * @return {@link Boolean}
     */
    private static boolean checkFile(File file) {
        // 确保父目录存在
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
//            logIfEnabled(log::error, "Failed to create directories.");
            log.error("Failed to create directories.");
            return false;
        }

        // 确保文件存在
        try {
            if (!file.exists() && !file.createNewFile()) {
//                logIfEnabled(log::error, "Failed to create file.");
                log.error("Failed to create file.");
                return false;
            }
        } catch (IOException e) {
//            logIfEnabled(log::error, "创建文件失败: " + e.getMessage());
            log.error("create file failed. {}", e.getMessage());
        }

        return true;
    }

    /**
     * Generate filter information(json)
     * @param message message
     * @return {@link Boolean}
     */
    // 生成状态信息
    public static boolean generatorInfo(BloomFilter bloomFilter, String message) {
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

//            logIfEnabled(log::info, "过滤器信息保存成功, 文件位置:" + filePath);
//            System.out.println("过滤器信息保存成功, 文件位置:" + filePath);
            log.info("create done: {}", file.getAbsolutePath());
            return true;
        } catch (IOException e) {
//            logIfEnabled(log::info, "生成信息文件失败: " + e.getMessage());
//            System.out.println("生成信息文件失败: " + e.getMessage());
            log.error("create info file failed. {}", e.getMessage());
            return false;
        }finally {
//            log.info("过滤器信息保存成功");
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
//                log.error(e.getMessage());
//                logIfEnabled(log::error, e.getMessage());
                log.error("close file failed. {}", e.getMessage());
            }
        }
    }

    /**
     * loading bitmap
     * @param size bitmap size
     * @return {@link Long[] }
     */
    public static long[] loadingBitMap(long size) {
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
            throw new RuntimeException("could not found file: " + e);
        } catch (IOException e) {
            throw new RuntimeException("read file error: " + e.getMessage());
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
    public static BloomInformation getBloomInfo() {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("bitmap/info.txt");
            byte[] bytes = fileInputStream.readAllBytes();
            String s = new String(bytes);

            // json to object
            BloomInformation info = JSON.parseObject(s, BloomInformation.class);

            return info;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("read file error: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("read file content error: " + e.getMessage());
        }
    }
}
