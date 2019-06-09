package State;

import Operation.ArithEnum;
import Operation.LDataStructure;
import Operation.OprEnum;

import static State.LState.LUA_REGISTRYINDEX;

public class InstrImpl{
    /* number of list items to accumulate before a SETLIST instruction */
    public static final int LFIELDS_PER_FLUSH = 50;

    /* misc */

    // R(A) := R(B)
    public static void move(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.copy(b, a);
    }

    // pc+=sBx; if (A) close all upvalues >= R(A - 1)
    public static void jmp(int i, LState vm) {
        int a = Instr.getA(i);
        int sBx = Instr.getSBx(i);
        vm.addPC(sBx);
        if (a != 0) {
            vm.closeUpvalues(a);
        }
    }

    /* load */

    // R(A), R(A+1), ..., R(A+B) := nil
    public static void loadNil(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        vm.pushNil();
        for (int j = a; j <= a+b; j++) {
            vm.copy(-1, j);
        }
        vm.pop(1);
    }

    // R(A) := (bool)B; if (C) pc++
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

    // R(A) := Kst(Bx)
    public static void loadK(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int bx = Instr.getBx(i);
        vm.getConst(bx);
        vm.replace(a);
    }

    // R(A) := Kst(extra arg)
    public static void loadKx(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int ax = Instr.getAx(vm.fetch());
        vm.getConst(ax);
        vm.replace(a);
    }

    /* arith */

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

    // R(A) := RK(B) op RK(C)
    private static void binaryArith(int i, LState vm, ArithEnum op) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        int c = Instr.getC(i);
        vm.getRK(b);
        vm.getRK(c);
        vm.arith(op);
        vm.replace(a);
    }

    // R(A) := op R(B)
    private static void unaryArith(int i, LState vm, ArithEnum op) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.pushValue(b);
        vm.arith(op);
        vm.replace(a);
    }

    /* compare */

    public static void eq(int i, LState vm) { compare(i, vm, OprEnum.EQ); } // ==
    public static void lt(int i, LState vm) { compare(i, vm, OprEnum.LT); } // <
    public static void le(int i, LState vm) { compare(i, vm, OprEnum.LE); } // <=

    // if ((RK(B) op RK(C)) ~= A) then pc++
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

    /* logical */

    // R(A) := not R(B)
    public static void not(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.pushBoolean(!vm.toBoolean(b));
        vm.replace(a);
    }

    // if not (R(A) <=> C) then pc++
    public static void test(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int c = Instr.getC(i);
        if (vm.toBoolean(a) != (c != 0)) {
            vm.addPC(1);
        }
    }

    // if (R(B) <=> C) then R(A) := R(B) else pc++
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

    /* len & concat */

    // R(A) := length of R(B)
    public static void length(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.len(b);
        vm.replace(a);
    }

    // R(A) := R(B).. ... ..R(C)
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

    /* for */

    // R(A)-=R(A+2); pc+=sBx
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

    // R(A)+=R(A+2);
    // if R(A) <?= R(A+1) then {
    //   pc+=sBx; R(A+3)=R(A)
    // }
    public static void forLoop(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int sBx = Instr.getSBx(i);

        // R(A)+=R(A+2);
        vm.pushValue(a + 2);
        vm.pushValue(a);
        vm.arith(ArithEnum.OP_ADD);
        vm.replace(a);

        boolean isPositiveStep = vm.toNumber(a+2) >= 0;
        if (isPositiveStep && vm.compare(a, a+1, OprEnum.LT) ||
                !isPositiveStep && vm.compare(a+1, a, OprEnum.LE)) {
            // pc+=sBx; R(A+3)=R(A)
            vm.addPC(sBx);
            vm.copy(a, a+3);
        }
    }

    // R(A+3), ... ,R(A+2+C) := R(A)(R(A+1), R(A+2));
    public static void tForCall(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int c = Instr.getC(i);
        pushFuncAndArgs(a, 3, vm);
        vm.call(2, c);
        popResults(a+3, c+1, vm);
    }

    // if R(A+1) ~= nil then {
    //   R(A)=R(A+1); pc += sBx
    // }
    public static void tForLoop(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int sBx = Instr.getSBx(i);
        if (!vm.isNil(a + 1)) {
            vm.copy(a+1, a);
            vm.addPC(sBx);
        }
    }

    /* table */

    // R(A) := {} (size = B,C)
    public static void newTable(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        int c = Instr.getC(i);

        vm.createTable(LuaCompiler.Processor.float2int(b), LuaCompiler.Processor.float2int(c));
        vm.replace(a);
    }

    // R(A) := R(B)[RK(C)]
    public static void getTable(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        int c = Instr.getC(i);
        vm.getRK(c);
        vm.getTable(b);
        vm.replace(a);
    }

    // R(A)[RK(B)] := RK(C)
    public static void setTable(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        int c = Instr.getC(i);
        vm.getRK(b);
        vm.getRK(c);
        vm.setTable(a);
    }

    // R(A)[(C-1)*FPF+i] := R(A+i), 1 <= i <= B
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

            // clear stack
            vm.setTop(vm.registerCount());
        }
    }

    /* call */

    // R(A+1) := R(B); R(A) := R(B)[RK(C)]
    public static void self(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        int c = Instr.getC(i);
        vm.copy(b, a+1);
        vm.getRK(c);
        vm.getTable(b);
        vm.replace(a);
    }

    // R(A) := closureLState
    public static void closure(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int bx = Instr.getBx(i);
        vm.loadProto(bx);
        vm.replace(a);
    }

    // R(A), R(A+1), ..., R(A+B-2) = vararg
    public static void vararg(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        if (b != 1) { // b==0 or b>1
            vm.loadVararg(b - 1);
            popResults(a, b, vm);
        }
    }

    // return R(A)(R(A+1), ... ,R(A+B-1))
    public static void tailCall(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        // todo: optimize tail call!
        int c = 0;
        int nArgs = pushFuncAndArgs(a, b, vm);
        vm.call(nArgs, c-1);
        popResults(a, c, vm);
    }

    // R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1))
    public static void call(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        int c = Instr.getC(i);
        int nArgs = pushFuncAndArgs(a, b, vm);
        vm.call(nArgs, c-1);
        popResults(a, c, vm);
    }

    // return R(A), ... ,R(A+B-2)
    public static void _return(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i);
        if (b == 1) {
            // no return values
        } else if (b > 1) {
            // b-1 return values
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
            // no results
        } else if (c > 1) {
            for (int i = a + c - 2; i >= a; i--) {
                vm.replace(i);
            }
        } else {
            // leave results on stack
            vm.checkStack(1);
            vm.pushInteger(a);
        }
    }

    /* upvalues */

    // R(A) := UpValue[B]
    public static void getUpval(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.copy(luaUpvalueIndex(b), a);
    }

    // UpValue[B] := R(A)
    public static void setUpval(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        vm.copy(a, luaUpvalueIndex(b));
    }

    // R(A) := UpValue[B][RK(C)]
    public static void getTabUp(int i, LState vm) {
        int a = Instr.getA(i) + 1;
        int b = Instr.getB(i) + 1;
        int c = Instr.getC(i);
        vm.getRK(c);
        vm.getTable(luaUpvalueIndex(b));
        vm.replace(a);
    }

    // UpValue[A][RK(B)] := RK(C)
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
