package BinaryChunk;

import java.nio.ByteBuffer;

/**
 * Created by lijin on 5/22/19.
 */
public class UpValue {
    public byte instack;
    public byte idx;

    void read(ByteBuffer buf) {
        instack = buf.get();
        idx = buf.get();
    }
}
