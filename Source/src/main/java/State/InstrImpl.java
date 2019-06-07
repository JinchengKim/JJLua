package State;

import Operation.ArithEnum;
import Operation.LDataStructure;
import Operation.OprEnum;

import static State.LState.LUA_REGISTRYINDEX;

public class InstrImpl{
    public static final int LFIELDS_PER_FLUSH = 50;
    public static void move(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.copy(b, a);
    }

    public static void jmp(int i, LState vm) {
        int a = Instr.getA(i);
        int sBx = Instr.getSBx(i);
        vm.addPC(sBx);
        if (a != 0) {
            vm.closeUpvalues(a);
        }
    }

    // load
    public static void loadNil(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        vm.pushNil();
        for (int j = a; j <= a+b; j++) {
            vm.copy(-1, j);
        }
        vm.pop(1);
    }
    public static void loadBool(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        int c = Instr.getC(i);
        vm.pushBoolean(b != 0);
        vm.replace(a);
        if (c != 0) {
            vm.addPC(1);
        }
    }
    public static void loadK(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int bx = Instr.getBx(i);
        vm.getConst(bx);
        vm.replace(a);
    }

    public static void loadKx(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int ax = Instr.getAx(vm.fetch());
        vm.getConst(ax);
        vm.replace(a);
    }

    // arith
    public static void add (int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_ADD ); } // +
    public static void sub (int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_SUB ); } // -
    public static void mul (int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_MUL ); } // *
    public static void mod (int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_MOD ); } // %
    public static void pow (int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_POW ); } // ^
    public static void div (int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_DIV ); } // /
    public static void idiv(int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_IDIV); } // //
    public static void band(int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_BAND); } // &
    public static void bor (int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_BOR ); } // |
    public static void bxor(int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_BXOR); } // ~
    public static void shl (int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_SHL ); } // <<
    public static void shr (int i, LState vm) { binaryArith(i, vm, ArithEnum.OP_SHR ); } // >>
    public static void unm (int i, LState vm) { unaryArith( i, vm, ArithEnum.OP_UMN ); } // -
    public static void bnot(int i, LState vm) { unaryArith( i, vm, ArithEnum.OP_BNOT); } // ~

    private static void binaryArith(int i, LState vm, ArithEnum op) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        int c = Instr.getC(i);
        vm.getRK(b);
        vm.getRK(c);
        vm.arith(op);
        vm.replace(a);
    }

    private static void unaryArith(int i, LState vm, ArithEnum op) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.pushValue(b);
        vm.arith(op);
        vm.replace(a);
    }

    // bool
    public static void eq(int i, LState vm) { compare(i, vm, OprEnum.EQ); } // ==
    public static void lt(int i, LState vm) { compare(i, vm, OprEnum.LT); } // <
    public static void le(int i, LState vm) { compare(i, vm, OprEnum.LE); } // <=

    private static void compare(int i, LState vm, OprEnum op) {
        int a = Instr.getA(i);
        int b = Instr.getB(i);
        int c = Instr.getC(i);
        vm.getRK(b);
        vm.getRK(c);
        if (vm.compare(-2, -1, op) != (a != 0)) {
            vm.addPC(1);
        }
        vm.pop(2);
    }

    // logical
    public static void not(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.pushBoolean(!vm.toBoolean(b));
        vm.replace(a);
    }

    public static void test(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int c = Instr.getC(i);
        if (vm.toBoolean(a) != (c != 0)) {
            vm.addPC(1);
        }
    }

    public static void testSet(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        int c = Instr.getC(i);
        if (vm.toBoolean(b) == (c != 0)) {
            vm.copy(b, a);
        } else {
            vm.addPC(1);
        }
    }

    public static void length(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.len(b);
        vm.replace(a);
    }

    public static void concat(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        int c = Instr.getC(i) + 1;
        int n = c - b + 1;
        vm.checkStack(n);
        for (int j = b; j <= c; j++) {
            vm.pushValue(j);
        }
        vm.concat(n);
        vm.replace(a);
    }

    // for loop
    public static void forPrep(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int sBx = Instr.getSBx(i);

        if (vm.type(a) == LDataStructure.STRING) {
            vm.pushNumber(vm.toNumber(a));
            vm.replace(a);
        }
        if (vm.type(a+1) == LDataStructure.STRING) {
            vm.pushNumber(vm.toNumber(a + 1));
            vm.replace(a + 1);
        }
        if (vm.type(a+2) == LDataStructure.STRING) {
            vm.pushNumber(vm.toNumber(a + 2));
            vm.replace(a + 2);
        }

        vm.pushValue(a);
        vm.pushValue(a + 2);
        vm.arith(ArithEnum.OP_SUB);
        vm.replace(a);
        vm.addPC(sBx);
    }

    public static void forLoop(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int sBx = Instr.getSBx(i);

        vm.pushValue(a + 2);
        vm.pushValue(a);
        vm.arith(ArithEnum.OP_ADD);
        vm.replace(a);

        boolean isPositiveStep = vm.toNumber(a+2) >= 0;
        if (isPositiveStep && vm.compare(a, a+1, OprEnum.LT) ||
                !isPositiveStep && vm.compare(a+1, a, OprEnum.LE)) {
            vm.addPC(sBx);
            vm.copy(a, a+3);
        }
    }

    public static void tForCall(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int c = Instr.getC(i);
        pushFuncAndArgs(a, 3, vm);
        vm.call(2, c);
        popResults(a+3, c+1, vm);
    }

    public static void tForLoop(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int sBx = Instr.getSBx(i);
        if (!vm.isNil(a + 1)) {
            vm.copy(a+1, a);
            vm.addPC(sBx);
        }
    }

    // table
    public static void newTable(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        int c = Instr.getC(i);

        vm.createTable(b, c);
        vm.replace(a);
    }

    public static void getTable(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        int c = Instr.getC(i);
        vm.getRK(c);
        vm.getTable(b);
        vm.replace(a);
    }

    public static void setTable(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        int c = Instr.getC(i);
        vm.getRK(b);
        vm.getRK(c);
        vm.setTable(a);
    }

    public static void setList(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        int c = Instr.getC(i);
        c = c > 0 ? c - 1 : Instr.getAx(vm.fetch());

        boolean bIsZero = b == 0;
        if (bIsZero) {
            b = ((int) vm.toInteger(-1)) - a - 1;
            vm.pop(1);
        }

        vm.checkStack(1);
        int idx = c * LFIELDS_PER_FLUSH;
        for (int j = 1; j <= b; j++) {
            idx++;
            vm.pushValue(a + j);
            vm.setI(a, idx);
        }

        if (bIsZero) {
            for (int j = vm.registerCount() + 1; j <= vm.getTop(); j++) {
                idx++;
                vm.pushValue(j);
                vm.setI(a, idx);
            }
            vm.setTop(vm.registerCount());
        }
    }

    // runner
    public static void self(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        int c = Instr.getC(i);
        vm.copy(b, a+1);
        vm.getRK(c);
        vm.getTable(b);
        vm.replace(a);
    }

    public static void closure(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int bx = Instr.getBx(i);
        vm.loadProto(bx);
        vm.replace(a);
    }

    public static void vararg(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        if (b != 1) {
            vm.loadVararg(b - 1);
            popResults(a, b, vm);
        }
    }

    public static void tailCall(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        // todo: optimize tail call!
        int c = 0;
        int nArgs = pushFuncAndArgs(a, b, vm);
        vm.call(nArgs, c-1);
        popResults(a, c, vm);
    }

    public static void call(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        int c = Instr.getC(i);
        int nArgs = pushFuncAndArgs(a, b, vm);
        vm.call(nArgs, c-1);
        popResults(a, c, vm);
    }

    public static void _return(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        if (b == 1) {
        } else if (b > 1) {
            vm.checkStack(b - 1);
            for (int j = a; j <= a+b-2; j++) {
                vm.pushValue(j);
            }
        } else {
            fixStack(a, vm);
        }
    }

    private static int pushFuncAndArgs(int a, int b, LState vm) {
        if (b >= 1) {
            vm.checkStack(b);
            for (int i = a; i < a+b; i++) {
                vm.pushValue(i);
            }
            return b - 1;
        } else {
            fixStack(a, vm);
            return vm.getTop() - vm.registerCount() - 1;
        }
    }

    private static void fixStack(int a, LState vm) {
        int x = (int) vm.toInteger(-1);
        vm.pop(1);

        vm.checkStack(x - a);
        for (int i = a; i < x; i++) {
            vm.pushValue(i);
        }
        vm.rotate(vm.registerCount()+1, x-a);
    }

    private static void popResults(int a, int c, LState vm) {
        if (c == 1) {
        } else if (c > 1) {
            for (int i = a + c - 2; i >= a; i--) {
                vm.replace(i);
            }
        } else {
            vm.checkStack(1);
            vm.pushInteger(a);
        }
    }

    // upvalues
    public static void getUpval(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.copy(luaUpvalueIndex(b), a);
    }

    public static void setUpval(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.copy(a, luaUpvalueIndex(b));
    }

    public static void getTabUp(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        int c = Instr.getC(i);
        vm.getRK(c);
        vm.getTable(luaUpvalueIndex(b));
        vm.replace(a);
    }

    public static void setTabUp(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        int c = Instr.getC(i);
        vm.getRK(b);
        vm.getRK(c);
        vm.setTable(luaUpvalueIndex(a));
    }
    private static int luaUpvalueIndex(int i) {
        return LUA_REGISTRYINDEX - i;
    }
}
