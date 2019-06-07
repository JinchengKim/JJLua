package Util;

import java.nio.ByteBuffer;

/**
 * Created by lijin on 5/29/19.
 */
public class LuaStringUtil {
    public static String getLuaString(ByteBuffer buf) {
        int size = buf.get() & 0xFF;
        if (size == 0) {
            return "";
        }
        if (size == 0xFF) {
            size = (int) buf.getLong(); // size_t
        }

        byte[] a = getBytes(buf, size - 1);
        return new String(a); // todo
    }

    static byte[] getBytes(ByteBuffer buf, int n) {
        byte[] a = new byte[n];
        buf.get(a);
        return a;
    }

    public static boolean isWhiteSpace(char c){
        if (c == '\t' || c == '\n' || c == '\f' || c == ' ') return true;
        return false;
    }

    public static boolean isNewLine(char c){
        return c == '\r' || c == '\n';
    }

    public static boolean isDigit(char c){
        if (c >= '0' && c <= '9') return true;
        return false;
    }

    public static boolean isLetter(char c){
        if (c >= 'a' && c <= 'z') return true;
        if (c >= 'A' && c <= 'Z') return true;
        return false;
    }

}
