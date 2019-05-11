package Parser;

import AST.Block;
import AST.Stat;
import AST.Stats.BreakStat;
import AST.Stats.EmptyStat;
import Lexer.Lexer;
import Lexer.TokenType;

import java.rmi.dgc.Lease;

/**
 * Created by lijin on 5/8/19.
 */
public class Parser {
    public static Block parse(String src, String srcFile){
        Lexer lexer = new Lexer(src, srcFile);
        Block block = BlockParser.parseBlock(lexer);

        return block;
    }
}

class BlockParser{
    static Block parseBlock(Lexer lexer){
        Block block = new Block();

        return block;
    }
}

class StatParser{
    static Stat getStat(Lexer lexer){
        switch (lexer.getTopTokenType()){
//            case TokenType.KW_BREAK: return
        }
    }


    private static EmptyStat getEmptyStat(Lexer lexer){
        try {
            lexer.getValidNextToken(TokenType.SEP_SEMI);
        }catch (Exception e){}

        return EmptyStat.EMPTY_STAT;
    }

    private static BreakStat getBreakStat(Lexer lexer){
        try {
            lexer.getValidNextToken(TokenType.KW_BREAK);
        }catch (Exception e){}
        return new BreakStat(lexer.getCurLine());
    }

    private
}
