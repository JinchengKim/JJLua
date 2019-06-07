package Operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lijin on 5/23/19.
 */
public class LTable {
    public LTable metatable;
    public List<Object> arr;
    public Map<Object, Object> map;
    // used by next()
    public Map<Object, Object> keys;
    public Object lastKey;
    public boolean changed;

    public LTable(int nArr, int nRec) {
        if (nArr > 0) {
            arr = new ArrayList<>(nArr);
        }
        if (nRec > 0) {
            map = new HashMap<>(nRec);
        }
    }

    public boolean hasMetafield(String fieldName) {
        return metatable != null && metatable.get(fieldName) != null;
    }

    public int length() {
        return arr == null ? 0 : arr.size();
    }

    public Object get(Object key) {
        key = floatToInteger(key);

        if (arr != null && key instanceof Long) {
            int idx = ((Long) key).intValue();
            if (idx >= 1 && idx <= arr.size()) {
                return arr.get(idx - 1);
            }
        }

        return map != null ? map.get(key) : null;
    }

    public void put(Object key, Object val) {
        if (key == null) {
            throw new RuntimeException("table index is nil!");
        }
        if (key instanceof Double && ((Double) key).isNaN()) {
            throw new RuntimeException("table index is NaN!");
        }

        changed = true;
        key = floatToInteger(key);
        if (key instanceof Long) {
            int idx = ((Long) key).intValue();
            if (idx >= 1) {
                if (arr == null) {
                    arr = new ArrayList<>();
                }

                int arrLen = arr.size();
                if (idx <= arrLen) {
                    arr.set(idx - 1, val);
                    if (idx == arrLen && val == null) {
                        shrinkArray();
                    }
                    return;
                }
                if (idx == arrLen + 1) {
                    if (map != null) {
                        map.remove(key);
                    }
                    if (val != null) {
                        arr.add(val);
                        expandArray();
                    }
                    return;
                }
            }
        }

        if (val != null) {
            if (map == null) {
                map = new HashMap<>();
            }
            map.put(key, val);
        } else {
            if (map != null) {
                map.remove(key);
            }
        }
    }

    public Object floatToInteger(Object key) {
        if (key instanceof Double) {
            Double f = (Double) key;
            if (LNum.isLInteger(f)) {
                return f.longValue();
            }
        }
        return key;
    }

    public void shrinkArray() {
        for (int i = arr.size() - 1; i >= 0; i--) {
            if (arr.get(i) == null) {
                arr.remove(i);
            }
        }
    }

    public void expandArray() {
        if (map != null) {
            for (int idx = arr.size() + 1; ; idx++) {
                Object val = map.remove((long) idx);
                if (val != null) {
                    arr.add(val);
                } else {
                    break;
                }
            }
        }
    }

    public Object nextKey(Object key) {
        if (keys == null || (key == null && changed)) {
            initKeys();
            changed = false;
        }

        Object nextKey = keys.get(key);
        if (nextKey == null && key != null && key != lastKey) {
            throw new RuntimeException("invalid key to 'next'");
        }

        return nextKey;
    }

    public void initKeys() {
        if (keys == null) {
            keys = new HashMap<>();
        } else {
            keys.clear();
        }
        Object key = null;
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                if (arr.get(i) != null) {
                    long nextKey = i + 1;
                    keys.put(key, nextKey);
                    key = nextKey;
                }
            }
        }
        if (map != null) {
            for (Object k : map.keySet()) {
                Object v = map.get(k);
                if (v != null) {
                    keys.put(key, k);
                    key = k;
                }
            }
        }
        lastKey = key;
    }
}
