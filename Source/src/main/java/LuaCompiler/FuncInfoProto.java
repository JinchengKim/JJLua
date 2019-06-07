package LuaCompiler;

import BinaryChunk.Proto;
import BinaryChunk.UpValue;

import java.util.List;

/**
 * Created by lijin on 5/22/19.
 */
class FuncInforProto {
    static Proto toProto(FuncInformation fi) {
        Proto proto = new Proto();
        proto.numParams = (byte) fi.numParams;
        proto.maxStackSize = (byte) fi.maxRegs;
        proto.code = fi.insts.stream().mapToInt(Integer::intValue).toArray();
        proto.constants = getConstants(fi);
        proto.upvalues = getUpvalues(fi);
        proto.protos = toProtos(fi.subFuncs);

        if (proto.maxStackSize < 2) {
            proto.maxStackSize = (byte) 2;
        }
        if (fi.isVararg) {
            proto.isVararg = (byte) 1;
        }

        return proto;
    }

    private static Proto[] toProtos(List<FuncInformation> fis) {
        return fis.stream()
                .map(FuncInforProto::toProto)
                .toArray(Proto[]::new);
    }

    private static Object[] getConstants(FuncInformation fi) {
        Object[] consts = new Object[fi.constants.size()];
        fi.constants.forEach((c, idx) -> consts[idx] = c);
        return consts;
    }

    private static UpValue[] getUpvalues(FuncInformation fi) {
        UpValue[] upvals = new UpValue[fi.upvalues.size()];

        for (FuncInformation.UpvalInfo uvInfo : fi.upvalues.values()) {
            UpValue upval = new UpValue();
            upvals[uvInfo.index] = upval;
            if (uvInfo.locVarSlot >= 0) {
                upval.idx = (byte) uvInfo.locVarSlot;
            } else {
                upval.instack = (byte) 0;
                upval.idx = (byte) uvInfo.upvalIndex;
            }
        }

        return upvals;
    }
}
