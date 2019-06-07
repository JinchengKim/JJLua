package LuaCompiler;

import AST.Block;
import AST.Exps.FuncDefExp;
import BinaryChunk.Proto;
import Parser.Parser;

/**
 * Created by lijin on 5/27/19.
 */
public class LCompiler {
    public static Proto compile(String chunk) {
        Block ast = Parser.parse(chunk);

        FuncDefExp fd = new FuncDefExp();
        fd.isMultiVar = true;
        fd.block = ast;

        FuncInformation fi = new FuncInformation(null, fd);
        fi.addLocVar("_ENV", 0);
        ExpProcessor.processFuncDefExp(fi, fd, 0);
        Proto proto = FuncInforProto.toProto(fi.subFuncs.get(0));
        setCode(proto, chunk);
        return proto;
    }

    private static void setCode(Proto proto, String code) {
        proto.source = code;
        for (Proto subProto : proto.protos) {
            setCode(subProto, code);
        }
    }
}
