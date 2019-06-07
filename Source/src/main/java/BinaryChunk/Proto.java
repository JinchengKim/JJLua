package BinaryChunk;

import Util.LuaStringUtil;

import java.nio.ByteBuffer;

/**
 * Created by lijin on 5/22/19.
 */
public class Proto {
    private static final int TAG_NIL       = 0x00;
    private static final int TAG_BOOLEAN   = 0x01;
    private static final int TAG_NUMBER    = 0x03;
    private static final int TAG_INTEGER   = 0x13;
    private static final int TAG_SHORT_STR = 0x04;
    private static final int TAG_LONG_STR  = 0x14;


    public String source; // debug
    public byte numParams;
    public byte isVararg;
    public byte maxStackSize;
    public int[] code;
    public Object[] constants;
    public UpValue[] upvalues;
    public Proto[] protos;



    public void read(ByteBuffer buf, String parentSource) {
        source = LuaStringUtil.getLuaString(buf);
        if (source.isEmpty()) {
            source = parentSource;
        }
        numParams = buf.get();
        isVararg = buf.get();
        maxStackSize = buf.get();
        readCode(buf);
        readConstants(buf);
        readUpvalues(buf);
        readProtos(buf, source);
    }

    public void readCode(ByteBuffer buf) {
        code = new int[buf.getInt()];
        for (int i = 0; i < code.length; i++) {
            code[i] = buf.getInt();
        }
    }

    public void readConstants(ByteBuffer buf) {
        constants = new Object[buf.getInt()];
        for (int i = 0; i < constants.length; i++) {
            constants[i] = readConstant(buf);
        }
    }

    public Object readConstant(ByteBuffer buf) {
        switch (buf.get()) {
            case TAG_NIL: return null;
            case TAG_BOOLEAN: return buf.get() != 0;
            case TAG_INTEGER: return buf.getLong();
            case TAG_NUMBER: return buf.getDouble();
            case TAG_SHORT_STR: return LuaStringUtil.getLuaString(buf);
            case TAG_LONG_STR: return LuaStringUtil.getLuaString(buf);
            default: throw new RuntimeException("corrupted!"); // todo
        }
    }

    public void readUpvalues(ByteBuffer buf) {
        upvalues = new UpValue[buf.getInt()];
        for (int i = 0; i < upvalues.length; i++) {
            upvalues[i] = new UpValue();
            upvalues[i].read(buf);
        }
    }

    private void readProtos(ByteBuffer buf, String parentSource) {
        protos = new Proto[buf.getInt()];
        for (int i = 0; i < protos.length; i++) {
            protos[i] = new Proto();
            protos[i].read(buf, parentSource);
        }
    }
}
