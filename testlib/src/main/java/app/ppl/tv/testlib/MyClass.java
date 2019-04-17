package app.ppl.tv.testlib;

import java.nio.ByteBuffer;

import static app.ppl.tv.testlib.ByteUtil.byteArrayToString;
import static app.ppl.tv.testlib.ByteUtil.uint32Buffer;
import static app.ppl.tv.testlib.ByteUtil.uint32BufferBE;

public class MyClass {

    public static void main(String [ ] args){



        System.out.println("Test");

        System.out.println(byteArrayToString(uint32Buffer(1234)));
        System.out.println(byteArrayToString(uint32BufferBE(1234)));
        /*for (int i = 0; i < 1000; i++) {
            System.out.println(byteArrayToString(uint32Buffer(i)));
        }*/

    }






}
