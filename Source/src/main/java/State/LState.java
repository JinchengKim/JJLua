package State;

import LuaCompiler.ThreadStatus;
import Operation.ArithEnum;
import Operation.LDataStructure;
import Operation.OprEnum;

/**
 * Created by lijin on 5/24/19.
 */
public interface LState {
    int LUA_MINSTACK = 20;
    int LUAI_MAXSTACK = 1000000;
    int LUA_REGISTRYINDEX = -LUAI_MAXSTACK - 1000;
    long LUA_RIDX_GLOBALS = 2;

    /* basic stack manipulation */
    int getTop();
    int absIndex(int idx);
    boolean checkStack(int n);
    void pop(int n);
    void copy(int fromIdx, int toIdx);
    void pushValue(int idx);
    void replace(int idx);
    void insert(int idx);
    void remove(int idx);
    void rotate(int idx, int n);
    void setTop(int idx);
    /* access functions (stack -> Go); */
    String typeName(LDataStructure tp);
    LDataStructure type(int idx);
    boolean isNone(int idx);
    boolean isNil(int idx);
    boolean isNoneOrNil(int idx);
    boolean isBoolean(int idx);
    boolean isInteger(int idx);
    boolean isNumber(int idx);
    boolean isString(int idx);
    boolean isTable(int idx);
    boolean isFunction(int idx);
    boolean isJavaFunction(int idx);
    boolean toBoolean(int idx);
    long toInteger(int idx);
    Long toIntegerX(int idx);
    double toNumber(int idx);
    Double toNumberX(int idx);
    String toString(int idx);
    JFunc toJavaFunction(int idx);
    int rawLen(int idx);
    /* push functions (Go -> stack); */
    void pushNil();
    void pushBoolean(boolean b);
    void pushInteger(long n);
    void pushNumber(double n);
    void pushString(String s);
    void pushJavaFunction(JFunc f);
    void pushJavaClosure(JFunc f, int n);
    void pushGlobalTable();
    /* comparison and arithmetic functions */
    void arith(ArithEnum op);
    boolean compare(int idx1, int idx2, OprEnum op);

    boolean rawEqual(int idx1, int idx2);
    /* get functions (Lua -> stack) */
    void newTable();
    void createTable(int nArr, int nRec);
    LDataStructure getTable(int idx);
    LDataStructure getField(int idx, String k);
    LDataStructure getI(int idx, long i);
    LDataStructure rawGet(int idx);
    LDataStructure rawGetI(int idx, long i);
    LDataStructure getGlobal(String name);
    boolean getMetatable(int idx);
    /* set functions (stack -> Lua) */
    void setTable(int idx);
    void setField(int idx, String k);
    void setI(int idx, long i);
    void rawSet(int idx);
    void rawSetI(int idx, long i);
    void setMetatable(int idx);
    void setGlobal(String name);
    void register(String name, JFunc f);

    ThreadStatus load(byte[] chunk, String chunkName, String mode);
    ThreadStatus pCall(int nArgs, int nResults, int msgh);
    void call(int nArgs, int nResults);

    void len(int idx);
    void concat(int n);
    boolean next(int idx);
    int error();

    // VM implementation
    void addPC(int n);
    int fetch();
    void getConst(int idx);
    void getRK(int rk);
    int registerCount();
    void loadVararg(int n);
    void loadProto(int idx);
    void closeUpvalues(int a);
}
