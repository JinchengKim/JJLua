package AST.Exps;

import AST.Exp;
import Lexer.TokenType;
import Operation.LNum;

/**
 * Created by lijin on 5/28/19.
 */
public class ExpOptimizer {
    static Exp optimizeLogicalOr(BinaryOpExp exp) {
        if (isTrue(exp.exp1)) {
            return exp.exp1; // true or x => true
        }
        if (isFalse(exp.exp1) && !isVarargOrFuncCall(exp.exp2)) {
            return exp.exp2; // false or x => x
        }
        return exp;
    }

    static Exp optimizeLogicalAnd(BinaryOpExp exp) {
        if (isFalse(exp.exp1)) {
            return exp.exp1; // false and x => false
        }
        if (isTrue(exp.exp1) && !isVarargOrFuncCall(exp.exp2)) {
            return exp.exp2; // true and x => x
        }
        return exp;
    }

    static Exp optimizeBitwiseBinaryOp(BinaryOpExp exp) {
        Long i = castToInteger(exp.exp1);
        if (i != null) {
            Long j = castToInteger(exp.exp2);
            if (j != null) {
                switch (exp.op) {
                    case OP_BAND: return new IntExp(i & j);
                    case OP_BOR:  return new IntExp(i | j);
                    case OP_BXOR: return new IntExp(i ^ j);
//                    case OP_SHL:  return new IntExp(expbeginLine(), .shiftLeft(i, j));
//                    case OP_SHR:  return new IntExp(expbeginLine(), LuaMath.shiftRight(i, j));
                }
            }
        }
        return exp;
    }

    public static Exp optimizeArithBinaryOp(BinaryOpExp exp) {
        if (exp.exp1 instanceof IntExp
                && exp.exp2 instanceof IntExp) {
            IntExp x = (IntExp) exp.exp1;
            IntExp y = (IntExp) exp.exp2;
            switch (exp.op) {
                case OP_ADD: return new IntExp(x.val + y.val);
                case OP_MINUS:
                case OP_SUB: return new IntExp(x.val - y.val);
                case OP_MUL: return new IntExp( x.val * y.val);
                case OP_IDIV:
                    if (y.val != 0) {
                        return new IntExp(Math.floorDiv(x.val, y.val));
                    }
                    break;
                case OP_MOD:
                    if (y.val != 0) {
                        return new IntExp( Math.floorMod(x.val, y.val));
                    }
                    break;
            }
        }

        Double f = castToFloat(exp.exp1);
        if (f != null) {
            Double g = castToFloat(exp.exp2);
            if (g != null) {
                switch (exp.op) {
                    case OP_ADD: return new FloatExp(f + g);
                    case OP_MINUS:
                    case OP_SUB: return new FloatExp(f - g);
                    case OP_MUL: return new FloatExp(f * g);
                    case OP_POW: return new FloatExp(Math.pow(f, g));
                }
                if (g != 0) {
                    switch (exp.op) {
                        case OP_DIV:  return new FloatExp(f / g);
                        case OP_IDIV: return new FloatExp(f/ g);
                        case OP_MOD:  return new FloatExp(f/ g);
                    }
                }
            }
        }

        return exp;
    }

    static Exp optimizePow(Exp exp) {
        if (exp instanceof BinaryOpExp) {
            BinaryOpExp binopExp = (BinaryOpExp) exp;
            if (binopExp.op == TokenType.OP_POW) {
                binopExp.exp2 = (optimizePow(binopExp.exp2));
            }
            return optimizeArithBinaryOp(binopExp);
        }
        return exp;
    }

    static Exp optimizeUnaryOp(UnaryOpExp exp) {
        switch (exp.op) {
            case OP_UNM:  return optimizeUnm(exp);
            case OP_NOT:  return optimizeNot(exp);
            case OP_BNOT: return optimizeBnot(exp);
            default: return exp;
        }
    }

    private static Exp optimizeUnm(UnaryOpExp exp) {
        if (exp.exp instanceof IntExp) {
            IntExp iExp = (IntExp) exp.exp;
            iExp.val = (-iExp.val);
            return iExp;
        }
        if (exp.exp instanceof FloatExp) {
            FloatExp fExp = (FloatExp) exp.exp;
            fExp.val = (-fExp.val);
            return fExp;
        }
        return exp;
    }

    private static Exp optimizeNot(UnaryOpExp exp) {
        Exp subExp = exp.exp;
        if (subExp instanceof NilExp
                || subExp instanceof FalseExp) {
            return new TrueExp();
        }
        if (subExp instanceof TrueExp
                || subExp instanceof IntExp
                || subExp instanceof FloatExp
                || subExp instanceof StringExp) {
            return new FalseExp();
        }
        return exp;
    }

    private static Exp optimizeBnot(UnaryOpExp exp) {
        if (exp.exp instanceof IntExp) {
            IntExp iExp = (IntExp) exp.exp;
            iExp.val = (~iExp.val);
            return iExp;
        }
        if (exp.exp instanceof FloatExp) {
            FloatExp fExp = (FloatExp) exp.exp;
            double f = fExp.val;
            if (LNum.isLInteger(f)) {
                return new IntExp(~((int) f));
            }
        }
        return exp;
    }

    private static boolean isFalse(Exp exp) {
        return exp instanceof FalseExp
                || exp instanceof NilExp;
    }

    private static boolean isTrue(Exp exp) {
        return exp instanceof TrueExp
                || exp instanceof IntExp
                || exp instanceof FloatExp
                || exp instanceof StringExp;
    }

    private static boolean isVarargOrFuncCall(Exp exp) {
        return exp instanceof VarargExp
                || exp instanceof FuncCallExp;
    }

    private static Long castToInteger(Exp exp) {
        if (exp instanceof IntExp) {
            return ((IntExp) exp).val;
        }
        if (exp instanceof FloatExp) {
            double f = ((FloatExp) exp).val;
            return LNum.isLInteger(f) ? (long) f : null;
        }
        return null;
    }

    private static Double castToFloat(Exp exp) {
        if (exp instanceof IntExp) {
            return (double) ((IntExp) exp).val;
        }
        if (exp instanceof FloatExp) {
            return ((FloatExp) exp).val;
        }
        return null;
    }
}
