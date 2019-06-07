package Operation;

import AST.Exception.DataStrutureException;

/**
 * Created by lijin on 5/23/19.
 */
public class LVal {
    public static LDataStructure trans2Type(Object o) throws DataStrutureException {
        if (o == null) {
            return LDataStructure.NIL;
        } else if (o instanceof Boolean) {
            return LDataStructure.BOOLEAN;
        } else if (o instanceof Long || o instanceof Double) {
            return LDataStructure.NUMBER;
        } else if (o instanceof String) {
            return LDataStructure.STRING;
        } else if (o instanceof LTable) {
            return LDataStructure.TABLE;
        } else if (o instanceof LBlock) {
            return LDataStructure.FUNCTION;
        } else {
            throw new DataStrutureException(o.toString());
        }
    }

    public static boolean trans2Boolean(Object val) {
        if (val == null) {
            return false;
        } else if (val instanceof Boolean) {
            return (Boolean) val;
        } else {
            return true;
        }
    }

    public static Double trans2Float(Object val) {
        if (val instanceof Double) {
            return (Double) val;
        } else if (val instanceof Long) {
            return ((Long) val).doubleValue();
        } else if (val instanceof String) {
            return LNum.trans2LFloat((String) val);
        } else {
            return null;
        }
    }

    public static Long trans2Integer(Object val) {
        if (val instanceof Long) {
            return (Long) val;
        } else if (val instanceof Double) {
            double n = (Double) val;
            return LNum.isLInteger(n) ? (long) n : null;
        } else if (val instanceof String) {
            return trans2Integer((String) val);
        } else {
            return null;
        }
    }

    public static Long trans2Integer(String s) {
        Long i = LNum.trans2LInteger(s);
        if (i != null) {
            return i;
        }
        Double f = LNum.trans2LFloat(s);
        if (f != null && LNum.isLInteger(f)) {
            return f.longValue();
        }
        return null;
    }




}
