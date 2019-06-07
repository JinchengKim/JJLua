package Lexer;

import AST.Exception.LexerException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Created by lijin on 5/11/19.
 * [ 1] [IDENTIFIER] x
 [ 1] [operation ] =
 [ 1] [Number    ] 10
 [ 1] [IDENTIFIER] print
 [ 1] [SEP       ] (
 [ 1] [IDENTIFIER] x
 [ 1] [SEP       ] )
 [ 1] [EOF       ]
 */
public class LexerTest {
    public static void main(String[] args) throws Exception {


        BufferedReader in = new BufferedReader(new FileReader(new File("/Users/lijin/Desktop/homework/203/newJavalua/Source/src/main/java/Parser/test1.lua")));
        String str;
        String code = "";
        while ((str = in.readLine()) != null) {
            code = code + str + '\n';
        }
        System.out.print(code);
        test(code);
    }

    private static void test(String code) throws LexerException {
        Lexer lexer = new Lexer(code);
        for (;;){
            Token token = lexer.getNextToken();
            System.out.printf("[%-10s] %s\n", kindName(token.type), token.source);
            if (token.type == TokenType.EOF) {
                break;
            }
        }
    }

    private static String kindName(TokenType type){
        if (type == TokenType.EOF){
            return "EOF";
        }

        if (type.ordinal() <= TokenType.KW_WHILE.ordinal()){
            return "keyword";
        }

        if (type.ordinal() <= TokenType.OP_NOT.ordinal()){
            return "operation";
        }

        if (type.ordinal() <= TokenType.SEP_RCURLY.ordinal()){
            return "SEP";
        }

        if (type == TokenType.NUMBER){
            return "Number";
        }

        if (type == TokenType.IDENTIFIER){
            return "IDENTIFIER";
        }

        if (type == TokenType.STRING){
            return  "String";
        }

        return "other";
    }
}
