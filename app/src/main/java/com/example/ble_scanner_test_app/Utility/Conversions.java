package com.example.ble_scanner_test_app.Utility;

public class Conversions {


    /**
     Function for converting bytes to hex
      */
    private static final char hexDigits[] =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String bytes2HexString(final byte[] bytes) {
        if (bytes == null) return null;
        int len = bytes.length;
        if (len <= 0) return null;
        char[] ret = new char[len << 1];
        for (int i = 0, j = 0; i < len; i++) {
            ret[j++] = hexDigits[bytes[i] >>> 4 & 0x0f];
            ret[j++] = hexDigits[bytes[i] & 0x0f];
        }
        return new String(ret);
    }

    /**
     * Function for converting hex to bytes
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
    Function for converting java int to UNIT8
     */
    public static byte intToUint8Byte(int value) {
        return (byte) value;
    }

    /**
     * Function for building CMD from integer input by the user
     */

    protected static final int PACKAGE_MAX_LENGTH = 80;
    protected static final int PACKAGE_BASE_LENGTH = 7;
    protected static final byte PACKAGE_START1_VALUE = intToUint8Byte(0xA0);
    protected static final byte PACKAGE_START2_VALUE = intToUint8Byte(0xA1);
    protected static final byte CR = intToUint8Byte(0x0D);
    protected static final byte LF = intToUint8Byte(0x0A);

    public static final synchronized byte[] buildCMD(byte[] cmdPayload) {
        int cmdPayloadLength = cmdPayload.length;
        if (cmdPayloadLength > (PACKAGE_MAX_LENGTH - PACKAGE_BASE_LENGTH)) {
            throw new UnsupportedOperationException("only support 73 bytes");
        }

        byte[] pkgData = new byte[cmdPayloadLength + PACKAGE_BASE_LENGTH];
        pkgData[0] = PACKAGE_START1_VALUE;
        pkgData[1] = PACKAGE_START2_VALUE;
        byte[] msb16PayloadLength = msbUint16ToBytes(cmdPayloadLength);
        pkgData[2] = msb16PayloadLength[0];
        pkgData[3] = msb16PayloadLength[1];
        System.arraycopy(cmdPayload, 0, pkgData, 4, cmdPayloadLength);
        pkgData[cmdPayloadLength + 4] = u8CheckSum(cmdPayload);
        pkgData[cmdPayloadLength + 5] = CR;
        pkgData[cmdPayloadLength + 6] = LF;
        return pkgData;
    }

    static final byte u8CheckSum(final byte[] buffer) {
        byte payloadCheckSum = 0;
        for (byte aBuffer : buffer) {
            payloadCheckSum = (byte) (payloadCheckSum ^ aBuffer);
        }
        return payloadCheckSum;
    }

    // Converting msb unit16 to bytes
    public static byte[] msbUint16ToBytes(int value) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) ((value & 0xFF00) >> 8);
        bytes[1] = (byte) (value & 0xFF);
        return bytes;
    }
}
