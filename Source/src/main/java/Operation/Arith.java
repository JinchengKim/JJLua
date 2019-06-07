package Operation;

import State.LStateInstance;

import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

class ArithMath{
    public static long lshf(long a, long n){
        return n >= 0 ? a << n : a >>> -n;
    }

    public static long rshf(long a, long n){
        return n >= 0 ? a >>> n : a << -n;
    }

    public static double fdiv(double a, double b){
        return Math.floor(a / b);
    }

    public static double fmod(double a, double b){
        if (a > 0 && b == Double.POSITIVE_INFINITY
                || a < 0 && b == Double.NEGATIVE_INFINITY) {
            return a;
        }
        if (a > 0 && b == Double.NEGATIVE_INFINITY
                || a < 0 && b == Double.POSITIVE_INFINITY) {
            return b;
        }
        return a - Math.floor(a / b) * b;
    }
}

public class Arith {
    private static final LongBinaryOperator[] integerOps = {
            (a, b) -> a + b,     // LUA_OPADD
            (a, b) -> a - b,     // LUA_OPSUB
            (a, b) -> a * b,     // LUA_OPMUL
            Math::floorMod,      // LUA_OPMOD
            null,                // LUA_OPPOW
            null,                // LUA_OPDIV
            Math::floorDiv,      // LUA_OPIDIV
            (a, b) -> a & b,     // LUA_OPBAND
            (a, b) -> a | b,     // LUA_OPBOR
            (a, b) -> a ^ b,     // LUA_OPBXOR
            ArithMath::lshf,  // LUA_OPSHL
            ArithMath::rshf, // LUA_OPSHR
            (a, b) -> -a,        // LUA_OPUNM
            (a, b) -> ~a,        // LUA_OPBNOT
    };

    private static final DoubleBinaryOperator[] floatOps = {
            (a, b) -> a + b,   // LUA_OPADD
            (a, b) -> a - b,   // LUA_OPSUB
            (a, b) -> a * b,   // LUA_OPMUL
            ArithMath::fmod, // LUA_OPMOD
            Math::pow,         // LUA_OPPOW
            (a, b) -> a / b,   // LUA_OPDIV
            ArithMath::fdiv, // LUA_OPIDIV
            null,              // LUA_OPBAND
            null,              // LUA_OPBOR
            null,              // LUA_OPBXOR
            null,              // LUA_OPSHL
            null,              // LUA_OPSHR
            (a, b) -> -a,      // LUA_OPUNM
            null,              // LUA_OPBNOT
    };

    private static final String[] metamethods = {
            "__add",
            "__sub",
            "__mul",
            "__mod",
            "__pow",
            "__div",
            "__idiv",
            "__band",
            "__bor",
            "__bxor",
            "__shl",
            "__shr",
            "__unm",
            "__bnot",
    };

    public static Object arith(Object a, Object b, ArithEnum op, LStateInstance ls) {
        LongBinaryOperator integerFunc = integerOps[op.ordinal()];
        DoubleBinaryOperator floatFunc = floatOps[op.ordinal()];

        if (floatFunc == null) { // bitwise
            Long x = LVal.trans2Integer(a);
            if (x != null) {
                Long y = LVal.trans2Integer(b);
                if (y != null) {
                    return integerFunc.applyAsLong(x, y);
                }
            }
        } else { // arith
            if (integerFunc != null) { // add,sub,mul,mod,idiv,unm
                if (a instanceof Long && b instanceof Long) {
                    return integerFunc.applyAsLong((Long) a, (Long) b);
                }
            }
            Double x = LVal.trans2Float(a);
            if (x != null) {
                Double y = LVal.trans2Float(b);
                if (y != null) {
                    return floatFunc.applyAsDouble(x, y);
                }
            }
        }

        Object mm = ls.getMetamethod(a, b, metamethods[op.ordinal()]);
        if (mm != null) {
            return ls.callMetamethod(a, b, mm);
        }

        throw new RuntimeException("arithmetic error!");
    }
}
