package State;

import LuaCompiler.ProgramStatus;
import Operation.ArithEnum;
import Operation.LDataStructure;
import Operation.OprEnum;

/**
 * Created by lijin on 5/24/19.
 */
public interface LState {
    int LUAI_MAXSTACK = 8000;
    int LUA_REGISTRYINDEX = -LUAI_MAXSTACK - 1000;
    long LUA_RIDX_GLOBALS = 2;

    int getTop();
    boolean checkStack(int n);
    void pop(int n);
    void copy(int fromIdx, int toIdx);
    void pushValue(int idx);
    void replace(int idx);
    void insert(int idx);
    void rotate(int idx, int n);
    void setTop(int idx);

    String typeName(LDataStructure tp);
    LDataStructure type(int idx);

    boolean isNil(int idx);
    boolean isBoolean(int idx);
    boolean isString(int idx);
    boolean toBoolean(int idx);
    long toInteger(int idx);
    Long toIntegerX(int idx);
    double toNumber(int idx);
    Double toNumberX(int idx);
    String toString(int idx);


    void pushNil();
    void pushBoolean(boolean b);
    void pushInteger(long n);
    void pushNumber(double n);
    void pushString(String s);
    void pushJavaFunction(JFunc f);

    void arith(ArithEnum op);
    boolean compare(int idx1, int idx2, OprEnum op);

    void createTable(int nArr, int nRec);
    LDataStructure getTable(int idx);
    LDataStructure getI(int idx, long i);
    boolean getMetatable(int idx);


    void setTable(int idx);
    void setI(int idx, long i);
    void setMetatable(int idx);
    void setGlobal(String name);
    void register(String name, JFunc f);

    ProgramStatus load(byte[] chunk, String chunkName, String mode);
    ProgramStatus pCall(int nArgs, int nResults, int msgh);
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
