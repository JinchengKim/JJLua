import LuaCompiler.ProgramStatus;
import Operation.LDataStructure;
import State.LState;
import State.LStateInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lijin on 5/27/19.
 */
public class MainTest {
    public static void main(String[] args) throws IOException {
        List<String> paths = new ArrayList<>();
//        paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/expressions/hello_world.lua");
//        paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/expressions/simple_exp.lua");
//        paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/expressions/arith_exp.lua");

//        paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/functions/io_func.lua");
          paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/functions/local_func.lua");

//        paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/general/comments.lua");
//        paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/general/escape_character.lua");
//        paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/general/func_type.lua");

//        paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/statements/assign_statement.lua");
//        paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/statements/for_statement.lua");
//        paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/statements/if_statement.lua");
//        paths.add("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/tests/statements/while_statement.lua");
        for (String path:paths){
            runTest(path);
        }
    }

    static void runTest(String path) throws IOException {
        System.out.println("=========test of the file -" + path + "-  =========");
        byte[] data = Files.readAllBytes(Paths.get(path));
        LState ls = new LStateInstance();
        ls.register("print", MainTest::print);
        ls.register("getmetatable", MainTest::getMetatable);
        ls.register("setmetatable", MainTest::setMetatable);
        ls.register("next", MainTest::next);
        ls.register("pairs", MainTest::pairs);
        ls.register("ipairs", MainTest::iPairs);
        ls.register("error", MainTest::error);
        ls.register("pcall", MainTest::pCall);
        ls.load(data, path, "bt");
        ls.call(0, 0);
    }


    static int print(LState ls) {
        int nArgs = ls.getTop();
        for (int i = 1; i <= nArgs; i++) {
            if (ls.isBoolean(i)) {
                System.out.print(ls.toBoolean(i));
            } else if (ls.isString(i)) {
                System.out.print(ls.toString(i));
            } else {
                System.out.print(ls.typeName(ls.type(i)));
            }
            if (i < nArgs) {
                System.out.print("\t");
            }
        }
        System.out.println();
        return 0;
    }

    static int getMetatable(LState ls) {
        if (!ls.getMetatable(1)) {
            ls.pushNil();
        }
        return 1;
    }

    static int setMetatable(LState ls) {
        ls.setMetatable(1);
        return 1;
    }

    static int next(LState ls) {
        ls.setTop(2); /* create a 2nd argument if there isn't one */
        if (ls.next(1)) {
            return 2;
        } else {
            ls.pushNil();
            return 1;
        }
    }

    static int pairs(LState ls) {
        ls.pushJavaFunction(MainTest::next); /* will return generator, */
        ls.pushValue(1);                 /* state, */
        ls.pushNil();
        return 3;
    }

    static int iPairs(LState ls) {
        ls.pushJavaFunction(MainTest::iPairsAux); /* iteration function */
        ls.pushValue(1);                      /* state */
        ls.pushInteger(0);                    /* initial value */
        return 3;
    }

    static int iPairsAux(LState ls) {
        long i = ls.toInteger(2) + 1;
        ls.pushInteger(i);
        return ls.getI(1, i) != LDataStructure.NIL ? 1 : 2;
    }

    static int  error(LState ls) {
        return ls.error();
    }

    static int  pCall(LState ls) {
        int nArgs = ls.getTop() - 1;
        ProgramStatus status = ls.pCall(nArgs, -1, 0);
        ls.pushBoolean(status == ProgramStatus.OK);
        ls.insert(1);
        return ls.getTop();
    }
}
