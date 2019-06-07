package LuaCompiler;

import AST.Exps.FuncDefExp;
import Lexer.TokenType;
import Operation.OprEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lijin on 5/22/19.
 */
public class FuncInformation {
    public static final int MAXARG_Bx = (1 << 18) - 1;   // 262143
    public static final int MAXARG_sBx = MAXARG_Bx >> 1; // 131071
    private static final Map<TokenType, OprEnum> arithAndBitwiseBinops = new HashMap<>();
    static {
        arithAndBitwiseBinops.put(TokenType.OP_ADD, OprEnum.ADD);
        arithAndBitwiseBinops.put(TokenType.OP_SUB,  OprEnum.SUB);
        arithAndBitwiseBinops.put(TokenType.OP_MINUS,  OprEnum.SUB);
        arithAndBitwiseBinops.put(TokenType.OP_MUL,  OprEnum.MUL);
        arithAndBitwiseBinops.put(TokenType.OP_MOD,  OprEnum.MOD);
        arithAndBitwiseBinops.put(TokenType.OP_POW,  OprEnum.POW);
        arithAndBitwiseBinops.put(TokenType.OP_DIV,  OprEnum.DIV);
        arithAndBitwiseBinops.put(TokenType.OP_IDIV, OprEnum.IDIV);
        arithAndBitwiseBinops.put(TokenType.OP_BAND, OprEnum.BAND);
        arithAndBitwiseBinops.put(TokenType.OP_BOR,  OprEnum.BOR);
        arithAndBitwiseBinops.put(TokenType.OP_BXOR, OprEnum.BXOR);
        arithAndBitwiseBinops.put(TokenType.OP_SHL,  OprEnum.SHL);
        arithAndBitwiseBinops.put(TokenType.OP_SHR,  OprEnum.SHR);
    }

    static class UpvalInfo {
        int locVarSlot;
        int upvalIndex;
        int index;
    }

    static class LocVarInfo {
        LocVarInfo prev;
        String name;
        int scopeLv;
        int slot;
        int startPC;
        int endPC;
        boolean captured;
    }

    private FuncInformation parent;
    List<FuncInformation> subFuncs = new ArrayList<>();
    int usedRegs;
    int maxRegs;
    private int scopeLv;
    List<LocVarInfo> locVars = new ArrayList<>();
    private Map<String, LocVarInfo> locNames = new HashMap<>();
    Map<String, UpvalInfo> upvalues = new HashMap<>();
    Map<Object, Integer> constants = new HashMap<>();
    private List<List<Integer>> breaks = new ArrayList<>();
    List<Integer> insts = new ArrayList<>();
    List<Integer> lineNums = new ArrayList<>();
    int numParams;
    boolean isVararg;

    FuncInformation(FuncInformation parent, FuncDefExp fd) {
        this.parent = parent;;
        numParams = fd.parList != null ? fd.parList.size() : 0;
        isVararg = fd.isMultiVar;
        breaks.add(null);
    }

    /* constants */

    int indexOfConstant(Object k) {
        Integer idx = constants.get(k);
        if (idx != null) {
            return idx;
        }

        idx = constants.size();
        constants.put(k, idx);
        return idx;
    }

    /* registers */

    int allocReg() {
        usedRegs++;
        if (usedRegs >= 255) {
            throw new RuntimeException("function or expression needs too many registers");
        }
        if (usedRegs > maxRegs) {
            maxRegs = usedRegs;
        }
        return usedRegs - 1;
    }

    void freeReg() {
        if (usedRegs <= 0) {
            throw new RuntimeException("usedRegs <= 0 !");
        }
        usedRegs--;
    }

    int allocRegs(int n) {
        if (n <= 0) {
            throw new RuntimeException("n <= 0 !");
        }
        for (int i = 0; i < n; i++) {
            allocReg();
        }
        return usedRegs - n;
    }

    void freeRegs(int n) {
        if (n < 0) {
            throw new RuntimeException("n < 0 !");
        }
        for (int i = 0; i < n; i++) {
            freeReg();
        }
    }

    /* lexical scope */

    void enterScope(boolean breakable) {
        scopeLv++;
        if (breakable) {
            breaks.add(new ArrayList<>());
        } else {
            breaks.add(null);
        }
    }

    void exitScope(int endPC) {
        List<Integer> pendingBreakJmps = breaks.remove(breaks.size() - 1);

        if (pendingBreakJmps != null) {
            int a = getJmpArgA();
            for (int pc : pendingBreakJmps) {
                int sBx = pc() - pc;
                int i = (sBx+MAXARG_sBx)<<14 | a<<6 | OprEnum.JMP.ordinal();
                insts.set(pc, i);
            }
        }

        scopeLv--;
        for (LocVarInfo locVar : new ArrayList<>(locNames.values())) {
            if (locVar.scopeLv > scopeLv) { // out of scope
                locVar.endPC = endPC;
                removeLocVar(locVar);
            }
        }
    }

    private void removeLocVar(LocVarInfo locVar) {
        freeReg();
        if (locVar.prev == null) {
            locNames.remove(locVar.name);
        } else if (locVar.prev.scopeLv == locVar.scopeLv) {
            removeLocVar(locVar.prev);
        } else {
            locNames.put(locVar.name, locVar.prev);
        }
    }

    int addLocVar(String name, int startPC) {
        LocVarInfo newVar = new LocVarInfo();
        newVar.name = name;
        newVar.prev = locNames.get(name);
        newVar.scopeLv = scopeLv;
        newVar.slot = allocReg();
        newVar.startPC = startPC;
        newVar.endPC = 0;

        locVars.add(newVar);
        locNames.put(name, newVar);

        return newVar.slot;
    }

    int slotOfLocVar(String name) {
        return locNames.containsKey(name)
                ? locNames.get(name).slot
                : -1;
    }

    void addBreakJmp(int pc) {
        for (int i = scopeLv; i >= 0; i--) {
            if (breaks.get(i) != null) { // breakable
                breaks.get(i).add(pc);
                return;
            }
        }

        throw new RuntimeException("<break> at line ? not inside a loop!");
    }

    /* upvalues */

    int indexOfUpval(String name) {
        if (upvalues.containsKey(name)) {
            return upvalues.get(name).index;
        }
        if (parent != null) {
            if (parent.locNames.containsKey(name)) {
                LocVarInfo locVar = parent.locNames.get(name);
                int idx = upvalues.size();
                UpvalInfo upval = new UpvalInfo();
                upval.locVarSlot = locVar.slot;
                upval.upvalIndex = -1;
                upval.index = idx;
                upvalues.put(name, upval);
                locVar.captured = true;
                return idx;
            }
            int uvIdx = parent.indexOfUpval(name);
            if (uvIdx >= 0) {
                int idx = upvalues.size();
                UpvalInfo upval = new UpvalInfo();
                upval.locVarSlot = -1;
                upval.upvalIndex = uvIdx;
                upval.index = idx;
                upvalues.put(name, upval);
                return idx;
            }
        }
        return -1;
    }

    void closeOpenUpvals() {
        int a = getJmpArgA();
        if (a > 0) {
            emitJmp(a, 0);
        }
    }

    int getJmpArgA() {
        boolean hasCapturedLocVars = false;
        int minSlotOfLocVars = maxRegs;
        for (LocVarInfo locVar : locNames.values()) {
            if (locVar.scopeLv == scopeLv) {
                for (LocVarInfo v = locVar; v != null && v.scopeLv == scopeLv; v = v.prev) {
                    if (v.captured) {
                        hasCapturedLocVars = true;
                    }
                    if (v.slot < minSlotOfLocVars && v.name.charAt(0) != '(') {
                        minSlotOfLocVars = v.slot;
                    }
                }
            }
        }
        if (hasCapturedLocVars) {
            return minSlotOfLocVars + 1;
        } else {
            return 0;
        }
    }

    /* code */

    int pc() {
        return insts.size() - 1;
    }

    void fixSbx(int pc, int sBx) {
        int i = insts.get(pc);
        i = i << 18 >> 18;                  // clear sBx
        i = i | (sBx+MAXARG_sBx)<<14; // reset sBx
        insts.set(pc, i);
    }

    // todo: rename?
    void fixEndPC(String name, int delta) {
        for (int i = locVars.size() - 1; i >= 0; i--) {
            LocVarInfo locVar = locVars.get(i);
            if (locVar.name.equals(name)) {
                locVar.endPC += delta;
                return;
            }
        }
    }

    void emitABC(OprEnum op, int a, int b, int c) {
        int i = b<<23 | c<<14 | a<<6 | op.ordinal();
        insts.add(i);
    }

    private void emitABx(OprEnum op, int a, int bx) {
        int i = bx<<14 | a<<6 | op.ordinal();
        insts.add(i);
    }

    private void emitAsBx(OprEnum op , int a, int sBx) {
        int i = (sBx+MAXARG_sBx)<<14 | a<<6 | op.ordinal();
        insts.add(i);
    }

    private void emitAx( OprEnum op , int ax) {
        int i = ax<<6 | op.ordinal();
        insts.add(i);
    }

    // r[a] = r[b]
    void emitMove( int a, int b) {
        emitABC(OprEnum.MOVE, a, b, 0);
    }

    // r[a], r[a+1], ..., r[a+b] = nil
    void emitLoadNil( int a, int n) {
        emitABC(OprEnum.LOADNIL, a, n-1, 0);
    }

    // r[a] = (bool)b; if (c) pc++
    void emitLoadBool( int a, int b, int c) {
        emitABC(OprEnum.LOADBOOL, a, b, c);
    }

    // r[a] = kst[bx]
    void emitLoadK( int a, Object k) {
        int idx = indexOfConstant(k);
        if (idx < (1 << 18)) {
            emitABx( OprEnum.LOADK, a, idx);
        } else {
            emitABx(OprEnum.LOADKX, a, 0);
            emitAx(OprEnum.EXTRAARG, idx);
        }
    }

    // r[a], r[a+1], ..., r[a+b-2] = vararg
    void emitVararg(int a, int n) {
        emitABC( OprEnum.VARARG, a, n+1, 0);
    }

    // r[a] = emitClosure(proto[bx])
    void emitClosure(int a, int bx) {
        emitABx( OprEnum.CLOSURE, a, bx);
    }

    // r[a] = {}
    void emitNewTable(int a, int nArr, int nRec) {
        emitABC(OprEnum.NEWTABLE, a, nArr, nRec);
    }

    // r[a][(c-1)*FPF+i] = r[a+i], 1 <= i <= b
    void emitSetList( int a, int b, int c) {
        emitABC(OprEnum.SETLIST, a, b, c);
    }

    // r[a] = r[b][rk(c)]
    void emitGetTable( int a, int b, int c) {
        emitABC(OprEnum.GETTABLE, a, b, c);
    }

    // r[a][rk(b)] = rk(c)
    void emitSetTable( int a, int b, int c) {
        emitABC( OprEnum.SETTABLE, a, b, c);
    }

    // r[a] = upval[b]
    void emitGetUpval( int a, int b) {
        emitABC( OprEnum.GETUPVAL, a, b, 0);
    }

    // upval[b] = r[a]
    void emitSetUpval( int a, int b) {
        emitABC( OprEnum.SETUPVAL, a, b, 0);
    }

    // r[a] = upval[b][rk(c)]
    void emitGetTabUp( int a, int b, int c) {
        emitABC( OprEnum.GETTABUP, a, b, c);
    }

    // upval[a][rk(b)] = rk(c)
    void emitSetTabUp( int a, int b, int c) {
        emitABC( OprEnum.SETTABUP, a, b, c);
    }

    // r[a], ..., r[a+c-2] = r[a](r[a+1], ..., r[a+b-1])
    void emitCall( int a, int nArgs, int nRet) {
        emitABC( OprEnum.CALL, a, nArgs+1, nRet+1);
    }

    // return r[a](r[a+1], ... ,r[a+b-1])
    void emitTailCall( int a, int nArgs) {
        emitABC( OprEnum.TAILCALL, a, nArgs+1, 0);
    }

    // return r[a], ... ,r[a+b-2]
    void emitReturn( int a, int n) {
        emitABC( OprEnum.RETURN, a, n+1, 0);
    }

    // r[a+1] = r[b]; r[a] = r[b][rk(c)]
    void emitSelf( int a, int b, int c) {
        emitABC( OprEnum.SELF, a, b, c);
    }

    // pc+=sBx; if (a) close all upvalues >= r[a - 1]
    int emitJmp( int a, int sBx) {
        emitAsBx( OprEnum.JMP, a, sBx);
        return insts.size() - 1;
    }

    void emitTest(int a, int c) {
        emitABC( OprEnum.TEST, a, 0, c);
    }

    void emitTestSet(int a, int b, int c) {
        emitABC( OprEnum.TESTSET, a, b, c);
    }

    int emitForPrep( int a, int sBx) {
        emitAsBx( OprEnum.FORPREP, a, sBx);
        return insts.size() - 1;
    }

    int emitForLoop( int a, int sBx) {
        emitAsBx( OprEnum.FORLOOP, a, sBx);
        return insts.size() - 1;
    }

    void emitTForCall( int a, int c) {
        emitABC( OprEnum.TFORCALL, a, 0, c);
    }

    void emitTForLoop( int a, int sBx) {
        emitAsBx( OprEnum.TFORLOOP, a, sBx);
    }

    // r[a] = op r[b]
    void emitUnaryOp( TokenType op, int a, int b) {
        switch (op) {
            case OP_NOT:  emitABC( OprEnum.NOT,  a, b, 0); break;
            case OP_BNOT: emitABC( OprEnum.BNOT, a, b, 0); break;
            case OP_LEN:  emitABC( OprEnum.LEN,  a, b, 0); break;
            case OP_UNM:  emitABC( OprEnum.UNM,  a, b, 0); break;
        }
    }

    // r[a] = rk[b] op rk[c]
    // arith & bitwise & relational
    void emitBinaryOp( TokenType op, int a, int b, int c) {
        if (arithAndBitwiseBinops.containsKey(op)) {
            emitABC( arithAndBitwiseBinops.get(op), a, b, c);
        } else {
            switch (op) {
                case OP_EQ: emitABC( OprEnum.EQ, 1, b, c); break;
                case OP_NE: emitABC( OprEnum.EQ, 0, b, c); break;
                case OP_LT: emitABC( OprEnum.LT, 1, b, c); break;
                case OP_GT: emitABC( OprEnum.LT, 1, c, b); break;
                case OP_LE: emitABC( OprEnum.LE, 1, b, c); break;
                case OP_GE: emitABC( OprEnum.LE, 1, c, b); break;
            }
            emitJmp( 0, 1);
            emitLoadBool( a, 0, 1);
            emitLoadBool( a, 1, 0);
        }
    }

}
