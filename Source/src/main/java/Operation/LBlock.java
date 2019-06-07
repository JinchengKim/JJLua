package Operation;

import BinaryChunk.Proto;
import State.JFunc;

/**
 * Created by lijin on 5/23/19.
 */
public class LBlock {
    public final Proto proto;
    public final JFunc javaFunc;
    public final UpValueContainer[] upvals;

    // Lua Closure
    public LBlock(Proto proto) {
        this.proto = proto;
        this.javaFunc = null;
        this.upvals = new UpValueContainer[proto.upvalues.length];
    }

    public LBlock(JFunc javaFunc, int nUpvals) {
        this.proto = null;
        this.javaFunc = javaFunc;
        this.upvals = new UpValueContainer[nUpvals];
    }
}
