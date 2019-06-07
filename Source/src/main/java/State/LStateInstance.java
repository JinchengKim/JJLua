package State;

import AST.Exception.DataStrutureException;
import BinaryChunk.Proto;
import BinaryChunk.UpValue;
import LuaCompiler.LCompiler;
import LuaCompiler.ProgramStatus;
import Operation.*;
import com.sun.codemodel.internal.JBlock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by lijin on 5/25/19.
 */
public class LStateInstance implements LState{
    public LTable registry = new LTable(0, 0);
    private LStack stack = new LStack();

    public LStateInstance() {
        registry.put(LUA_RIDX_GLOBALS, new LTable(0, 0));
        LStack stack = new LStack();
        stack.state = this;
        pushLuaStack(stack);
    }

    private void pushLuaStack(LStack newTop) {
        newTop.prev = this.stack;
        this.stack = newTop;
    }

    private void popLuaStack() {
        LStack top = this.stack;
        this.stack = top.prev;
        top.prev = null;
    }


    // metatable
    private LTable getMetatable(Object val) {
        if (val instanceof LTable) {
            return ((LTable) val);
        }
        String key = null;
        try {
            key = "_MT" + LVal.trans2Type(val);
        } catch (DataStrutureException e) {
            e.printStackTrace();
        }
        Object mt = registry.get(key);
        return mt != null ? (LTable) mt : null;
    }

    private void setMetatable(Object val, LTable mt) {
        if (val instanceof LTable) {
            ((LTable) val).metatable = mt;
            return;
        }
        String key = null;
        try {
            key = "_MT" + LVal.trans2Type(val);
        } catch (DataStrutureException e) {
            e.printStackTrace();
        }
        registry.put(key, mt);
    }

    private Object getMetafield(Object val, String fieldName) {
        LTable mt = getMetatable(val);
        return mt != null ? mt.get(fieldName) : null;
    }

    public Object getMetamethod(Object a, Object b, String mmName) {
        Object mm = getMetafield(a, mmName);
        if (mm == null) {
            mm = getMetafield(b, mmName);
        }
        return mm;
    }

    public Object callMetamethod(Object a, Object b, Object mm) {
        stack.push(mm);
        stack.push(a);
        stack.push(b);
        call(2, 1);
        return stack.pop();
    }

    @Override
    public int getTop() {
        return stack.top();
    }

    @Override
    public boolean checkStack(int n) {
        return true; // TODO
    }

    @Override
    public void pop(int n) {
        for (int i = 0; i < n; i++) {
            stack.pop();
        }
    }

    @Override
    public void copy(int fromIdx, int toIdx) {
        stack.set(toIdx, stack.get(fromIdx));
    }

    @Override
    public void pushValue(int idx) {
        stack.push(stack.get(idx));
    }

    @Override
    public void replace(int idx) {
        stack.set(idx, stack.pop());
    }

    @Override
    public void insert(int idx) {
        rotate(idx, 1);
    }

    @Override
    public void rotate(int idx, int n) {
        int t = stack.top() - 1;
        int p = stack.absIndex(idx) - 1;
        int m = n >= 0 ? t - n : p - n - 1;
        stack.reverse(p, m);
        stack.reverse(m + 1, t);
        stack.reverse(p, t);
    }

    @Override
    public void setTop(int idx) {
        int newTop = stack.absIndex(idx);
        if (newTop < 0) {
            throw new RuntimeException("stack underflow!");
        }

        int n = stack.top() - newTop;
        if (n > 0) {
            for (int i = 0; i < n; i++) {
                stack.pop();
            }
        } else if (n < 0) {
            for (int i = 0; i > n; i--) {
                stack.push(null);
            }
        }
    }

    @Override
    public String typeName(LDataStructure ds) {
        switch (ds) {
            case NULL:     return "no value";
            case NIL:      return "nil";
            case BOOLEAN:  return "boolean";
            case NUMBER:   return "number";
            case STRING:   return "string";
            case TABLE:    return "table";
            case FUNCTION: return "function";
            default:            return "userdata";
        }
    }

    @Override
    public LDataStructure type(int idx) {
        try {
            return stack.isValid(idx) ? LVal.trans2Type(stack.get(idx)) : LDataStructure.NULL;
        } catch (DataStrutureException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isNil(int idx) {
        return type(idx) == LDataStructure.NIL;
    }

    @Override
    public boolean isBoolean(int idx) {
        return type(idx) == LDataStructure.BOOLEAN;
    }

    @Override
    public boolean isString(int idx) {
        LDataStructure t = type(idx);
        return t == LDataStructure.STRING || t == LDataStructure.NUMBER;
    }

    @Override
    public boolean toBoolean(int idx) {
        return LVal.trans2Boolean(stack.get(idx));
    }

    @Override
    public long toInteger(int idx) {
        Long i = toIntegerX(idx);
        return i == null ? 0 : i;
    }

    @Override
    public Long toIntegerX(int idx) {
        Object val = stack.get(idx);
        return val instanceof Long ? (Long) val : null;
    }

    @Override
    public double toNumber(int idx) {
        Double n = toNumberX(idx);
        return n == null ? 0 : n;
    }

    @Override
    public Double toNumberX(int idx) {
        Object val = stack.get(idx);
        if (val instanceof Double) {
            return (Double) val;
        } else if (val instanceof Long) {
            return ((Long) val).doubleValue();
        } else {
            return null;
        }
    }

    @Override
    public String toString(int idx) {
        Object val = stack.get(idx);
        if (val instanceof String) {
            return (String) val;
        } else if (val instanceof Long || val instanceof Double) {
            return val.toString();
        } else {
            return null;
        }
    }

    @Override
    public void pushNil() {
        stack.push(null);
    }

    @Override
    public void pushBoolean(boolean b) {
        stack.push(b);
    }

    @Override
    public void pushInteger(long n) {
        stack.push(n);
    }

    @Override
    public void pushNumber(double n) {
        stack.push(n);
    }

    @Override
    public void pushString(String s) {
        stack.push(s);
    }

    @Override
    public void pushJavaFunction(JFunc f) {
        stack.push(new LBlock(f, 0));
    }

    @Override
    public void arith(ArithEnum op) {
        Object b = stack.pop();
        Object a = op != ArithEnum.OP_UMN && op != ArithEnum.OP_BNOT ? stack.pop() : b;
        Object result = Arith.arith(a, b, op, this);
        stack.push(result);
    }


    @Override
    public boolean compare(int idx1, int idx2, OprEnum op) {
        if (!stack.isValid(idx1) || !stack.isValid(idx2)) {
            return false;
        }

        Object a = stack.get(idx1);
        Object b = stack.get(idx2);
        switch (op) {
            case EQ: return BBool.eq(a, b, this);
            case LT: return BBool.lt(a, b, this);
            case LE: return BBool.le(a, b, this);
            default: throw new RuntimeException("invalid compare op!");
        }
    }

    @Override
    public void createTable(int nArr, int nRec) {
        stack.push(new LTable(nArr, nRec));
    }

    @Override
    public LDataStructure getTable(int idx) {
        Object t = stack.get(idx);
        Object k = stack.pop();
        return getTable(t, k, false);
    }

    @Override
    public LDataStructure getI(int idx, long i) {
        Object t = stack.get(idx);
        return getTable(t, i, false);
    }


    @Override
    public boolean getMetatable(int idx) {
        Object val = stack.get(idx);
        Object mt = getMetatable(val);
        if (mt != null) {
            stack.push(mt);
            return true;
        } else {
            return false;
        }
    }

    private LDataStructure getTable(Object t, Object k, boolean raw) {
        if (t instanceof LTable) {
            LTable tbl = (LTable) t;
            Object v = tbl.get(k);
            if (raw || v != null || !tbl.hasMetafield("__index")) {
                stack.push(v);
                try {
                    return LVal.trans2Type(v);
                } catch (DataStrutureException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!raw) {
            Object mf = getMetafield(t, "__index");
            if (mf != null) {
                if (mf instanceof LTable) {
                    return getTable(mf, k, false);
                } else if (mf instanceof JBlock) {
                    Object v = callMetamethod(t, k, mf);
                    stack.push(v);
                    try {
                        return LVal.trans2Type(v);
                    } catch (DataStrutureException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        throw new RuntimeException("not a table!"); // todo
    }

    @Override
    public void setTable(int idx) {
        Object t = stack.get(idx);
        Object v = stack.pop();
        Object k = stack.pop();
        setTable(t, k, v, false);
    }

    @Override
    public void setI(int idx, long i) {
        Object t = stack.get(idx);
        Object v = stack.pop();
        setTable(t, i, v, false);
    }

    @Override
    public void setGlobal(String name) {
        Object t = registry.get(LUA_RIDX_GLOBALS);
        Object v = stack.pop();
        setTable(t, name, v, false);
    }

    @Override
    public void register(String name, JFunc f) {
        pushJavaFunction(f);
        setGlobal(name);
    }

    @Override
    public ProgramStatus load(byte[] chunk, String chunkName, String mode) {
        Proto proto = LCompiler.compile(new String(chunk));
        LBlock closure = new LBlock(proto);
        stack.push(closure);
        if (proto.upvalues.length > 0) {
            Object env = registry.get(LUA_RIDX_GLOBALS);
            closure.upvals[0] = new UpValueContainer(env); // todo
        }
        return ProgramStatus.OK;
    }

    @Override
    public ProgramStatus pCall(int nArgs, int nResults, int msgh) {
        LStack caller = stack;
        try {
            call(nArgs, nResults);
            return ProgramStatus.OK;
        } catch (Exception e) {
            if (msgh != 0) {
                throw e;
            }
            while (stack != caller) {
                popLuaStack();
            }
            stack.push(e.getMessage()); // TODO
            return ProgramStatus.ERRRUN;
        }
    }

    @Override
    public void setMetatable(int idx) {
        Object val = stack.get(idx);
        Object mtVal = stack.pop();

        if (mtVal == null) {
            setMetatable(val, null);
        } else if (mtVal instanceof LTable) {
            setMetatable(val, (LTable) mtVal);
        } else {
            throw new RuntimeException("table expected!"); // todo
        }
    }

    private void setTable(Object t, Object k, Object v, boolean raw) {
        if (t instanceof LTable) {
            LTable tbl = (LTable) t;
            if (raw || tbl.get(k) != null || !tbl.hasMetafield("__newindex")) {
                tbl.put(k, v);
                return;
            }
        }
        if (!raw) {
            Object mf = getMetafield(t, "__newindex");
            if (mf != null) {
                if (mf instanceof LTable) {
                    setTable(mf, k, v, false);
                    return;
                }
                if (mf instanceof JBlock) {
                    stack.push(mf);
                    stack.push(t);
                    stack.push(k);
                    stack.push(v);
                    call(3, 0);
                    return;
                }
            }
        }
        throw new RuntimeException("not a table!");
    }

    @Override
    public void call(int nArgs, int nResults) {
        Object val = stack.get(-(nArgs + 1));
        Object f = val instanceof LBlock ? val : null;

        if (f == null) {
            Object mf = getMetafield(val, "__call");
            if (mf != null && mf instanceof JBlock) {
                stack.push(f);
                insert(-(nArgs + 2));
                nArgs += 1;
                f = mf;
            }
        }

        if (f != null) {
            LBlock c = (LBlock) f;
            if (c.proto != null) {
                callLuaClosure(nArgs, nResults, c);
            } else {
                callJavaClosure(nArgs, nResults, c);
            }
        } else {
            throw new RuntimeException("not function!");
        }
    }

    private void callJavaClosure(int nArgs, int nResults, LBlock c) {
        LStack newStack = new LStack(/*nRegs+LUA_MINSTACK*/);
        newStack.state = this;
        newStack.closure = c;

        if (nArgs > 0) {
            newStack.pushN(stack.popN(nArgs), nArgs);
        }
        stack.pop();

        pushLuaStack(newStack);
        int r = c.javaFunc.invoke(this);
        popLuaStack();

        if (nResults != 0) {
            List<Object> results = newStack.popN(r);
            stack.pushN(results, nResults);
        }
    }

    private void callLuaClosure(int nArgs, int nResults, LBlock c) {
        int nRegs = c.proto.maxStackSize;
        int nParams = c.proto.numParams;
        boolean isVararg = c.proto.isVararg == 1;

        LStack newStack = new LStack(/*nRegs+LUA_MINSTACK*/);
        newStack.closure = c;

        List<Object> funcAndArgs = stack.popN(nArgs + 1);
        newStack.pushN(funcAndArgs.subList(1, funcAndArgs.size()), nParams);
        if (nArgs > nParams && isVararg) {
            newStack.varargs = funcAndArgs.subList(nParams + 1, funcAndArgs.size());
        }

        pushLuaStack(newStack);
        setTop(nRegs);
        runLuaClosure();
        popLuaStack();

        if (nResults != 0) {
            List<Object> results = newStack.popN(newStack.top() - nRegs);
            stack.pushN(results, nResults);
        }
    }

    private void runLuaClosure() {
        for (;;) {
            int i = fetch();
            OprEnum opCode = Instr.getOpCode(i);
            opCode.getAction().execute(i, this);
            if (opCode == OprEnum.RETURN) {
                break;
            }
        }
    }

    @Override
    public void len(int idx) {
        Object val = stack.get(idx);
        if (val instanceof String) {
            pushInteger(((String) val).length());
            return;
        }
        Object mm = getMetamethod(val, val, "__len");
        if (mm != null) {
            stack.push(callMetamethod(val, val, mm));
            return;
        }
        if (val instanceof LTable) {
            pushInteger(((LTable) val).length());
            return;
        }
        throw new RuntimeException("length error!");
    }

    @Override
    public void concat(int n) {
        if (n == 0) {
            stack.push("");
        } else if (n >= 2) {
            for (int i = 1; i < n; i++) {
                if (isString(-1) && isString(-2)) {
                    String s2 = toString(-1);
                    String s1 = toString(-2);
                    pop(2);
                    pushString(s1 + s2);
                    continue;
                }

                Object b = stack.pop();
                Object a = stack.pop();
                Object mm = getMetamethod(a, b, "__concat");
                if (mm != null) {
                    stack.push(callMetamethod(a, b, mm));
                    continue;
                }

                throw new RuntimeException("concatenation error!");
            }
        }
    }

    @Override
    public boolean next(int idx) {
        Object val = stack.get(idx);
        if (val instanceof LTable) {
            LTable t = (LTable) val;
            Object key = stack.pop();
            Object nextKey = t.nextKey(key);
            if (nextKey != null) {
                stack.push(nextKey);
                stack.push(t.get(nextKey));
                return true;
            }
            return false;
        }
        throw new RuntimeException("table expected!");
    }

    @Override
    public int error() {
        Object err = stack.pop();
        throw new RuntimeException(err.toString()); // TODO
    }

    // LuaVM
    @Override
    public void addPC(int n) {
        stack.pc += n;
    }

    @Override
    public int fetch() {
        return stack.closure.proto.code[stack.pc++];
    }

    @Override
    public void getConst(int idx) {
        stack.push(stack.closure.proto.constants[idx]);
    }

    @Override
    public void getRK(int rk) {
        if (rk > 0xFF) {
            getConst(rk & 0xFF);
        } else { // register
            pushValue(rk + 1);
        }
    }

    @Override
    public int registerCount() {
        return stack.closure.proto.maxStackSize;
    }

    @Override
    public void loadVararg(int n) {
        List<Object> varargs = stack.varargs != null
                ? stack.varargs : Collections.emptyList();
        if (n < 0) {
            n = varargs.size();
        }

        stack.pushN(varargs, n);
    }

    @Override
    public void loadProto(int idx) {
        Proto proto = stack.closure.proto.protos[idx];
        LBlock closure = new LBlock(proto);
        stack.push(closure);

        for (int i = 0; i < proto.upvalues.length; i++) {
            UpValue uvInfo = proto.upvalues[i];
            int uvIdx = uvInfo.idx;
            if (uvInfo.instack == 1) {
                if (stack.openuvs == null) {
                    stack.openuvs = new HashMap<>();
                }
                if (stack.openuvs.containsKey(uvIdx)) {
                    closure.upvals[i] = stack.openuvs.get(uvIdx);
                } else {
                    closure.upvals[i] = new UpValueContainer(stack, uvIdx);
                    stack.openuvs.put(uvIdx, closure.upvals[i]);
                }
            } else {
                closure.upvals[i] = stack.closure.upvals[uvIdx];
            }
        }
    }

    public void closeUpvalues(int a) {
        if (stack.openuvs != null) {
            for (Iterator<UpValueContainer> it = stack.openuvs.values().iterator(); it.hasNext(); ) {
                UpValueContainer uv = it.next();
                if (uv.index >= a - 1) {
                    uv.migrate();
                    it.remove();
                }
            }
        }
    }

}
