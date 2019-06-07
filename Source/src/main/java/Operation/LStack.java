package Operation;

/**
 * Created by lijin on 5/23/19.
 */

import State.LStateInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LStack {
    static int LUA_MINSTACK = 20;
    static int LUAI_MAXSTACK = 1000000;
    static int LUA_REGISTRYINDEX = -LUAI_MAXSTACK - 1000;
    static long LUA_RIDX_GLOBALS = 2;

    /* virtual stack */
    public final ArrayList<Object> slots = new ArrayList<>();
    /* call info */
    public LStateInstance state;
    public LBlock closure;
    public List<Object> varargs;
    public Map<Integer, UpValueContainer> openuvs;
    public int pc;
    /* linked list */
    public LStack prev;

    public int top() {
        return slots.size();
    }

    public void push(Object val) {
        if (slots.size() > 10000) { // TODO
            throw new StackOverflowError();
        }
        slots.add(val);
    }

    public Object pop() {
        return slots.remove(slots.size() - 1);
    }

    public void pushN(List<Object> vals, int n) {
        int nVals = vals == null ? 0 : vals.size();
        if (n < 0) {
            n = nVals;
        }
        for (int i = 0; i < n; i++) {
            push(i < nVals ? vals.get(i) : null);
        }
    }

    public List<Object> popN(int n) {
        List<Object> vals = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            vals.add(pop());
        }
        Collections.reverse(vals);
        return vals;
    }

    public int absIndex(int idx) {
        return idx >= 0 || idx <= LUA_REGISTRYINDEX
                ? idx : idx + slots.size() + 1;
    }

    public boolean isValid(int idx) {
        if (idx < LUA_REGISTRYINDEX) { /* upvalues */
            int uvIdx = LUA_REGISTRYINDEX - idx - 1;
            return closure != null && uvIdx < closure.upvals.length;
        }
        if (idx == LUA_REGISTRYINDEX) {
            return true;
        }
        int absIdx = absIndex(idx);
        return absIdx > 0 && absIdx <= slots.size();
    }

    public Object get(int idx) {
        if (idx < LUA_REGISTRYINDEX) { /* upvalues */
            int uvIdx = LUA_REGISTRYINDEX - idx - 1;
            if (closure != null
                    && closure.upvals.length > uvIdx
                    && closure.upvals[uvIdx] != null) {
                return closure.upvals[uvIdx].get();
            } else {
                return null;
            }
        }
        if (idx == LUA_REGISTRYINDEX) {
            return state.registry;
        }
        int absIdx = absIndex(idx);
        if (absIdx > 0 && absIdx <= slots.size()) {
            return slots.get(absIdx - 1);
        } else {
            return null;
        }
    }

    public void set(int idx, Object val) {
        if (idx < LUA_REGISTRYINDEX) { /* upvalues */
            int uvIdx = LUA_REGISTRYINDEX - idx - 1;
            if (closure != null
                    && closure.upvals.length > uvIdx
                    && closure.upvals[uvIdx] != null) {
                closure.upvals[uvIdx].set(val);
            }
            return;
        }
        if (idx == LUA_REGISTRYINDEX) {
            state.registry = (LTable) val;
            return;
        }
        int absIdx = absIndex(idx);
        slots.set(absIdx - 1, val);
    }

    public void reverse(int from, int to) {
        Collections.reverse(slots.subList(from, to + 1));
    }
}
