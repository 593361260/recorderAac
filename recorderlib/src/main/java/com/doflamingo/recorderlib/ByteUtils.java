package com.doflamingo.recorderlib;

/**
 * 字节转换 网上找的工具
 */
public class ByteUtils {

    private ByteUtils() {
    }

    public static double calcDecibelValue(byte[] buffer, int readSize) {
        if (readSize <= 0) {
            return 0;
        }
        short[] dest = byteToShort(buffer);
        long v = 0;
        // 将 buffer 内容取出，进行平方和运算
        for (short i1 : dest) {
            v += i1 * i1;
        }
        // 平方和除以数据总长度，得到音量大小。
        double mean = v / (double) readSize;
        return 10 * Math.log10(mean);
    }

    public static short[] byteToShort(byte[] src) {
        int count = src.length >> 1;
        short[] dest = new short[count];
        for (int i = 0; i < count; i++) {
            dest[i] = (short) ((src[i * 2] & 0xff) | ((src[2 * i + 1] & 0xff) << 8));
        }
        return dest;
    }
}
