package com.pms_parkin_mobile.util;

public class Hex {

    private static final char[] HEX_STRING_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9','a', 'b', 'c', 'd', 'e', 'f'};
    private static final char[] HEX_STRING_UPPER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9','A', 'B', 'C', 'D', 'E', 'F'};
    public static final int HEX_LOWER = 0;
    public static final int HEX_UPPER = 1;



    public static String bytesToHex(byte[] bytes, int start, int length, int hexType) {
        if (bytes == null || bytes.length <= start + length) {
            throw new IllegalArgumentException("bytes cannot be null or invalid length");
        }

        char[] HEX = HEX_STRING_LOWER;
        if (hexType == 1) {
            HEX = HEX_STRING_UPPER;
        }

        char[] result = new char[2 * length];
        int j = 0;
        for (int i = start; i < start + length; i++) {
            result[j++] = HEX[(0xf0 & bytes[i]) >>> 4];
            result[j++] = HEX[(0x0f & bytes[i])];
        }
        return new String(result);
    }
}
