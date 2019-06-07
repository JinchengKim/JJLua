package Parser;

import AST.Block;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Created by lijin on 5/21/19.
 */
public class ParserTest {
    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(new File("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/java/Parser/test1.lua")));
        String str;
        String code = "";
        while ((str = in.readLine()) != null) {
            code = code + str + '\n';
        }


        Block block = Parser.parse(code);
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(block);
        System.out.println(json);
    }
}
