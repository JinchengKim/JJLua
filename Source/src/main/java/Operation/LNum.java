package Operation;

/**
 * Created by lijin on 5/23/19.
 */
public class LNum {
    public static boolean isLInteger(double f) {
        return f == (long) f;
    }

    public static Long trans2LInteger(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double trans2LFloat(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
