package app.ppl.tv.testlib;

import java.nio.ByteBuffer;

public class ByteUtil {
    public static String byteArrayToString(byte[] ba)
    {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for(byte b: ba)
            hex.append(String.format("%02X", b));
        return hex.toString();
    }


    public static byte uint8toByte(int data){
        data = data & 0xFF;
        return (byte) data;
    }

    public static byte[] uint8Buffer(int data){
        byte[] ret = new byte[1];
        ret[0] = uint8toByte(data);
        return ret;
    }

    public static byte[] uint16Buffer(int data) {
        byte[] ret = new byte[2];
        data = data & 0xFFFF;

        ret[0] = uint8toByte(data>>8);
        ret[1] = uint8toByte(data & 0xFF);
        return ret;
    }

    public static byte[] uint32Buffer(int data) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt((int) data);
        return bb.array();
    }



    public static byte[] uint32BufferBE(int data) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        writeUInt16BE(bb, (int) data & 0xFFFF);
        writeUInt16BE(bb, (int) ((data >> 16) & 0xFFFF));
        return bb.array();
    }

    public static void writeUInt32BE(ByteBuffer bb, long u) {
        writeUInt16BE(bb, (int) u & 0xFFFF);
        writeUInt16BE(bb, (int) ((u >> 16) & 0xFFFF));

    }


    public static void writeUInt16(ByteBuffer bb, int i) {
        i = i & 0xFFFF;
        writeUInt8(bb, i >> 8);
        writeUInt8(bb, i & 0xFF);
    }

    public static void writeUInt16BE(ByteBuffer bb, int i) {
        i = i & 0xFFFF;
        writeUInt8(bb, i & 0xFF);
        writeUInt8(bb, i >> 8);
    }

    public static void writeUInt8(ByteBuffer bb, int i) {
        i = i & 0xFF;
        bb.put((byte) i);
    }
}
