package Parser;

import AST.Block;
import AST.Exception.LexerException;
import AST.Exp;
import AST.Exps.*;
import AST.Stat;
import AST.Stats.*;
import Lexer.Lexer;
import Lexer.Token;
import Lexer.TokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by lijin on 5/8/19.
 */
public class Parser {
    public static Block parse(String src){
        Lexer lexer = new Lexer(src);
        Block block = BlockParser.parseBlock(lexer);
        try {
            lexer.getValidNextToken(TokenType.EOF);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return block;
    }
}

class BlockParser{
    static Block parseBlock(Lexer lexer){
        Block block = new Block();
        block.stats = getStats(lexer);
        block.exps = getExps(lexer);
        return block;
    }

    static List<Stat> getStats(Lexer lexer){
        List<Stat> stats = new ArrayList<>();
        while (!lexer.isTopTheEndOfStat()){
            Stat stat = StatParser.getStat(lexer);
            if (!(stat instanceof EmptyStat)){
                stats.add(stat);
            }
        }
        return stats;
    }

    static List<Exp> getExps(Lexer lexer){
        try {
            if (lexer.getTopTokenType() != TokenType.KW_RETURN){
                return null;
            }

            lexer.getNextToken();
            List<Exp> exps = new ArrayList<>();
            switch (lexer.getTopTokenType()){
                case EOF:
                case KW_END:
                case KW_ELSE:
                case KW_ELSEIF:
                case KW_UNTIL:
                    return exps;
                case SEP_SEMI:
                    lexer.getNextToken();
                    return exps;
                default:
                    exps = ExpParser.parseExpList(lexer);
                    if (lexer.getTopTokenType() == TokenType.SEP_SEMI){
                        lexer.getNextToken();
                    }
                    return exps;
            }
        } catch (LexerException e) {
            e.printStackTrace();
        }

        return null;
    }
}

class ExpParser{
    static List<Exp> parseExpList(Lexer lexer) throws LexerException {
        List <Exp> exps = new ArrayList<>();
        exps.add(parseExp(lexer));
        while (lexer.getTopTokenType() == TokenType.SEP_COMMA) {
            lexer.getNextToken();
            exps.add(parseExp(lexer));
        }
        return exps;
    }

    static Exp parseExp(Lexer lexer){
        return parseOrExp(lexer);
    }

    // or
    static Exp parseOrExp(Lexer lexer){
//        System.out.print("parseOrExp" + lexer.getTopTokenType());
        Exp exp = parseAndExp(lexer);
        while (lexer.getTopTokenType() == TokenType.OP_OR){
            try {
                Token token = lexer.getNextToken();
                BinaryOpExp or = new BinaryOpExp(token, exp, parseAndExp(lexer));
                return or;
            } catch (LexerException e) {
                e.printStackTrace();
            }

        }
        return exp;
    }

    // and
    static Exp parseAndExp(Lexer lexer){
        Exp exp = parseCmpExp(lexer);
        while (lexer.getTopTokenType() == TokenType.OP_AND){
            try {
                Token token = lexer.getNextToken();
                BinaryOpExp or = new BinaryOpExp(token, exp, parseCmpExp(lexer));
                return or;
            } catch (LexerException e) {
                e.printStackTrace();
            }

        }
        return exp;
    }

    // cmp
    static Exp parseCmpExp(Lexer lexer){
        Exp exp = parseBOrExp(lexer);
        while (true) {
            switch (lexer.getTopTokenType()) {
                case OP_LT:
                case OP_GT:
                case OP_NE:
                case OP_LE:
                case OP_GE:
                case OP_EQ:
                    Token op = null;
                    try {
                        op = lexer.getNextToken();
                    } catch (LexerException e) {
                        e.printStackTrace();
                    }
                    exp = new BinaryOpExp(op, exp, parseBOrExp(lexer));
                    break;
                default:
                    return exp;
            }
        }
    }

    // bit or |
    static Exp parseBOrExp(Lexer lexer){
        Exp exp = parseBNotExp(lexer);
        while (lexer.getTopTokenType() == TokenType.OP_BOR){
            Token op = null;
            try {
                op = lexer.getNextToken();
            } catch (LexerException e) {
                e.printStackTrace();
            }
            BinaryOpExp bor = new BinaryOpExp(op, exp, parseBNotExp(lexer));
            return bor;
        }
        return exp;
    }

    // bit xor ~
    static Exp parseBNotExp(Lexer lexer){
        Exp exp = parseBAndExp(lexer);
        while (lexer.getTopTokenType() == TokenType.OP_WAVE){
            Token op = null;
            try {
                op = lexer.getNextToken();
            } catch (LexerException e) {
                e.printStackTrace();
            }
            BinaryOpExp bnot = new BinaryOpExp(op, exp, parseBAndExp(lexer));
            return bnot;
        }
        return exp;
    }

    // bit and &
    static Exp parseBAndExp(Lexer lexer){
        Exp exp = parseBShiftExp(lexer);
        while (lexer.getTopTokenType() == TokenType.OP_BAND){
            Token op = null;
            try {
                op = lexer.getNextToken();
            } catch (LexerException e) {
                e.printStackTrace();
            }
            BinaryOpExp bnot = new BinaryOpExp(op, exp, parseBShiftExp(lexer));
            return bnot;
        }
        return exp;
    }

    // slli << >>
    static Exp parseBShiftExp(Lexer lexer){
        Exp exp = parseBConcatExp(lexer);
        while (true){
            switch (lexer.getTopTokenType()){
                case OP_SHR:
                case OP_SHL:
                    Token token = null;
                    try {
                        token = lexer.getNextToken();
                    } catch (LexerException e) {
                        e.printStackTrace();
                    }
                    exp = new BinaryOpExp(token, exp, parseBConcatExp(lexer));
                    break;
                default:
                    return exp;
            }
        }
    }

    // concat ..
    static Exp parseBConcatExp(Lexer lexer){
        Exp exp = parseLowCalExp(lexer);
        if (lexer.getTopTokenType() != TokenType.OP_CONCAT){
            return exp;
        }

        List<Exp> exps = new ArrayList<>();
        exps.add(exp);
        int line = 0;
        while (lexer.getTopTokenType() == TokenType.OP_CONCAT){
            exps.add(parseLowCalExp(lexer));
        }
        return new ConcatExp(exps);
    }

    // number add or minus + -
    static Exp parseLowCalExp(Lexer lexer){
        Exp exp = parseHighCalExp(lexer);
        while (true){
            switch (lexer.getTopTokenType()){
                case OP_ADD:
                case OP_MINUS:
                    Token token = null;
                    try {
                        token = lexer.getNextToken();
                    } catch (LexerException e) {
                        e.printStackTrace();
                    }
                    BinaryOpExp bexp = new BinaryOpExp(token, exp, parseHighCalExp(lexer));
                    exp = ExpOptimizer.optimizeArithBinaryOp(bexp);
                    break;
                default:
                    return exp;
            }
        }
    }

    // * % / //
    static Exp parseHighCalExp(Lexer lexer){
        Exp exp = parseUnaryExp(lexer);
        while (true){
            switch (lexer.getTopTokenType()){
                case OP_MOD:
                case OP_MUL:
                case OP_DIV:
                case OP_IDIV:
                    Token t = null;
                    try {
                        t = lexer.getNextToken();
                    } catch (LexerException e) {
                        e.printStackTrace();
                    }
                    BinaryOpExp bexp = new BinaryOpExp(t, exp, parseUnaryExp(lexer));
                    break;
                default:
                    return exp;
            }
        }
    }

    // unary
    static Exp parseUnaryExp(Lexer lexer){
        switch (lexer.getTopTokenType()){
            case OP_MINUS:
            case OP_WAVE:
            case OP_NOT:
            case OP_LEN:
                Token t = null;
                try {
                    t = lexer.getNextToken();
                } catch (LexerException e) {
                    e.printStackTrace();
                }
                UnaryOpExp exp = new UnaryOpExp(t, parseBXORExp(lexer));
                return exp;
        }
        Exp exp = parseBXORExp(lexer);
        return exp;
    }

    // ^
    static Exp parseBXORExp(Lexer lexer){
        Exp exp = parseStaticVar(lexer);
        if (lexer.getTopTokenType() == TokenType.OP_POW){
            Token op = null;
            try {
                op = lexer.getNextToken();
            } catch (LexerException e) {
                e.printStackTrace();
            }
            exp = new BinaryOpExp(op, exp, parseStaticVar(lexer));
        }
        return exp;
    }

    private static Exp parseStaticVar(Lexer lexer) {
        switch (lexer.getTopTokenType()) {
            case VARARG:
                return new VarargExp();
            case KW_NIL:
                return new NilExp();
            case KW_TRUE:
                return new TrueExp();
            case KW_FALSE:
                return new FalseExp();
            case STRING:
                try {
                    return new StringExp(lexer.getNextToken());
                } catch (LexerException e) {
                    e.printStackTrace();
                }
            case NUMBER:
                return parseNumberExp(lexer);
            case SEP_LCURLY:
                return parseTableConstructorExp(lexer);
            case KW_FUNCTION:
                try {
                    lexer.getNextToken();
                } catch (LexerException e) {
                    e.printStackTrace();
                }
                return parseFuncDefExp(lexer);
            default:
                return PrefixExpParser.parsePrefixExp(lexer);
        }
    }

    private static List<String> parseParList(Lexer lexer){
        List<String> names = new ArrayList<>();
        switch (lexer.getTopTokenType()){
            case SEP_RPAREN:
                return names;
            case VARARG:
                try{
                    lexer.getNextToken();
                } catch (LexerException e) {
                    e.printStackTrace();
                }
                names.add("...");;
                return names;
        }

        names.add(lexer.getIDToken().source);
        while (lexer.getTopTokenType() == TokenType.SEP_COMMA) {
            try {
                lexer.getNextToken();
                if (lexer.getTopTokenType() == TokenType.IDENTIFIER) {
                    names.add(lexer.getIDToken().source);
                } else {
                    lexer.getValidNextToken(TokenType.VARARG);
                    names.add("...");
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return names;
    }

    private static Exp parseNumberExp(Lexer lexer){
        Token token = null;
        try {
            token = lexer.getNextToken();
        } catch (LexerException e) {
            e.printStackTrace();
        }

        Long longNum = Long.parseLong(token.source);
        if (longNum != null){
            return new IntExp(longNum);
        }
        Double doubleNum = Double.parseDouble(token.source);
        if (doubleNum != null){
            return new FloatExp(doubleNum);
        }
        throw new RuntimeException("not a number: " + token);
    }

    static TableBuilderExp parseTableConstructorExp(Lexer lexer) {
        TableBuilderExp tcExp = new TableBuilderExp();

        try {
            lexer.getValidNextToken(TokenType.SEP_LCURLY); //{
        } catch (Exception e) {
            e.printStackTrace();
        }
        parseFieldList(lexer, tcExp);
        try {
            lexer.getValidNextToken(TokenType.SEP_RCURLY); //}
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tcExp;
    }

    static void parseFieldList(Lexer lexer, TableBuilderExp tcExp) {
        if (lexer.getTopTokenType() != TokenType.SEP_RCURLY) {
            parseField(lexer, tcExp);

            while (isFieldSep(lexer.getTopTokenType())) {
                try {
                    lexer.getNextToken();
                } catch (LexerException e) {
                    e.printStackTrace();
                }
                if (lexer.getTopTokenType() != TokenType.SEP_RCURLY) {
                    parseField(lexer, tcExp);
                } else {
                    break;
                }
            }
        }
    }


    static boolean isFieldSep(TokenType kind) {
        return kind == TokenType.SEP_SEMI ||kind == TokenType.SEP_COMMA;
    }

    static void parseField(Lexer lexer, TableBuilderExp tcExp) {
        if (lexer.getTopTokenType() == TokenType.SEP_LBRACK) {
            try {
                lexer.getNextToken();
                tcExp.addKey(parseExp(lexer));
                lexer.getValidNextToken(TokenType.SEP_RBRACK);
                lexer.getValidNextToken(TokenType.OP_ASSIGN);
                tcExp.addVal(parseExp(lexer));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        Exp exp = parseExp(lexer);
        if (exp instanceof LabelExp) {
            if (lexer.getTopTokenType() == TokenType.OP_ASSIGN) {
                // Name ‘=’ exp => ‘[’ LiteralString ‘]’ = exp
                tcExp.addKey(new StringExp(((LabelExp) exp).name));
                try {
                    lexer.getNextToken();
                } catch (LexerException e) {
                    e.printStackTrace();
                }
                tcExp.addVal(parseExp(lexer));
                return;
            }
        }

        tcExp.addKey(null);
        tcExp.addVal(exp);
    }

    public static FuncDefExp parseFuncDefExp(Lexer lexer){
        List<String> parList = null;
        Block block = null;
//        System.out.println(lexer.getTopTokenType());
        try {
            lexer.getValidNextToken(TokenType.SEP_LPAREN);
            parList = parseParList(lexer);
            lexer.getValidNextToken(TokenType.SEP_RPAREN);
            block = BlockParser.parseBlock(lexer);
            lexer.getValidNextToken(TokenType.KW_END);
        } catch (Exception e) {
            e.printStackTrace();
        }

        FuncDefExp exp = new FuncDefExp();
        exp.isMultiVar = parList.remove("...");
        exp.parList = parList;
        exp.block = block;
        return exp;
    }

    public static TableBuilderExp parseTableDefExp(Lexer lexer) throws Exception {
        TableBuilderExp texp = new TableBuilderExp();
        lexer.getValidNextToken(TokenType.SEP_LCURLY);
        if (lexer.getTopTokenType() != TokenType.SEP_RCURLY){
            parseInTableExp(lexer, texp);
            while (lexer.getTopTokenType() == TokenType.SEP_COMMA || lexer.getTopTokenType() == TokenType.SEP_SEMI){
                lexer.getNextToken();
                if (lexer.getTopTokenType() != TokenType.SEP_RCURLY){
                    parseInTableExp(lexer, texp);
                }else {
                    break;
                }
            }

        }
        lexer.getValidNextToken(TokenType.SEP_RCURLY);

        return texp;
    }


    private static void parseInTableExp(Lexer lexer, TableBuilderExp texp) throws Exception {
        if (lexer.getTopTokenType() == TokenType.SEP_LBRACK) {
            lexer.getNextToken();                       // [
            texp.addKey(parseExp(lexer));           // exp
            lexer.getValidNextToken(TokenType.SEP_RBRACK); // ]
            lexer.getValidNextToken(TokenType.OP_ASSIGN);  // =
            texp.addVal(parseExp(lexer));           // exp
            return;
        }

        Exp exp = parseExp(lexer);
        if (exp instanceof LabelExp) {
            if (lexer.getTopTokenType() == TokenType.OP_ASSIGN) {
                // Name ‘=’ exp => ‘[’ LiteralString ‘]’ = exp
                LabelExp lexp = (LabelExp) exp;
                texp.addKey( new StringExp(lexp.name));
                lexer.getTopTokenType();
                texp.addVal(parseExp(lexer));
                return;
            }
        }

        texp.addKey(null);
        texp.addVal(exp);
    }



}

class StatParser{
    static Stat getStat(Lexer lexer){
//        System.out.println("getStat" + lexer.getTopTokenType() + " " + lexer.chunk.offset);
        switch (lexer.getTopTokenType()){
            case KW_FUNCTION: return getFuncDefStat(lexer);
            case KW_LOCAL: return getLocalAssignOrFunDefStat(lexer);

            case KW_BREAK: return getBreakStat(lexer);
            case SEP_SEMI: return getEmptyStat(lexer);
            case SEP_LABEL: return getLabelStat(lexer);
            case KW_DO: return getDoStat(lexer);
            case KW_WHILE: return getWhileStat(lexer);
            case KW_REPEAT: return getRepeatStat(lexer);
            case KW_IF: return getIfStat(lexer);
            case KW_FOR: return getForStat(lexer);

            default: return getAssignOrFuncStat(lexer);
        }
    }

    // basic
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
        return new BreakStat();
    }

    private static LabelStat getLabelStat(Lexer lexer){
        String name = null;
        try {
            lexer.getValidNextToken(TokenType.SEP_LABEL);
            name = lexer.getValidNextToken(TokenType.IDENTIFIER).source;
            lexer.getValidNextToken(TokenType.SEP_LABEL);
        }catch (Exception e){}
        return new LabelStat(name);
    }

    // TODO GOTO Stat

    //
    private static DoStat getDoStat(Lexer lexer){
        Block block = null;
        try {
            lexer.getValidNextToken(TokenType.KW_DO);
            block = BlockParser.parseBlock(lexer);
            lexer.getValidNextToken(TokenType.KW_END);
        }catch (Exception e){}
        return new DoStat(block);
    }


    private static WhileStat getWhileStat(Lexer lexer){
        Exp exp = null;
        Block block = null;
        try {
            lexer.getValidNextToken(TokenType.KW_WHILE);
            exp = ExpParser.parseExp(lexer);
            lexer.getValidNextToken(TokenType.KW_DO);
            block = BlockParser.parseBlock(lexer);
            lexer.getValidNextToken(TokenType.KW_END);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new WhileStat(exp, block);
    }

    private static RepeatStat getRepeatStat(Lexer lexer){
        Block block = null;
        Exp exp = null;
        try {
            lexer.getValidNextToken(TokenType.KW_REPEAT);
            block = BlockParser.parseBlock(lexer);
            lexer.getValidNextToken(TokenType.KW_UNTIL);
            exp = ExpParser.parseExp(lexer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new RepeatStat(block, exp);
    }

    private static IfStat getIfStat(Lexer lexer){
        List<Exp> exps = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        try {
            lexer.getValidNextToken(TokenType.KW_IF);
            exps.add(ExpParser.parseExp(lexer));

            lexer.getValidNextToken(TokenType.KW_THEN);
            blocks.add(BlockParser.parseBlock(lexer));

            while (lexer.getTopTokenType() == TokenType.KW_ELSEIF){
                lexer.getNextToken();
                exps.add(ExpParser.parseExp(lexer));
                lexer.getValidNextToken(TokenType.KW_THEN);
                blocks.add(BlockParser.parseBlock(lexer));
            }

            if (lexer.getTopTokenType() == TokenType.KW_ELSE){
                lexer.getNextToken();
                exps.add(new TrueExp());
                blocks.add(BlockParser.parseBlock(lexer));
            }

            lexer.getValidNextToken(TokenType.KW_END);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new IfStat(exps, blocks);
    }

    private static Stat getForStat(Lexer lexer){
        try {
            String name = lexer.getIDToken().source;
            if (lexer.getTopTokenType() == TokenType.OP_ASSIGN){
                return getForNumStat(lexer, name);
            }else {
                return getForInStat(lexer, name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ForInStat getForInStat(Lexer lexer, String name){
        ForInStat stat = new ForInStat();
        stat.nameList = finishNameList(lexer, name);

        try {
            lexer.getValidNextToken(TokenType.KW_IN);
            stat.expList = ExpParser.parseExpList(lexer);
            lexer.getValidNextToken(TokenType.KW_DO);
            stat.block = BlockParser.parseBlock(lexer);
            lexer.getValidNextToken(TokenType.KW_END);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stat;
    }

    private static ForNumStat getForNumStat(Lexer lexer, String id){
        ForNumStat stat = new ForNumStat();
        stat.var = id;
        try {
            lexer.getValidNextToken(TokenType.OP_ASSIGN);
            stat.initExp = ExpParser.parseExp(lexer);
            lexer.getValidNextToken(TokenType.SEP_COMMA);
            stat.controllExp = ExpParser.parseExp(lexer);
            if (lexer.getTopTokenType() == TokenType.SEP_COMMA){
                lexer.getNextToken();
                stat.modifyExp = ExpParser.parseExp(lexer);
            }else {
                stat.modifyExp = new IntExp(1);
            }
            lexer.getValidNextToken(TokenType.KW_DO);
            stat.block = BlockParser.parseBlock(lexer);
            lexer.getValidNextToken(TokenType.KW_END);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stat;
    }

    private static List<String> finishNameList(Lexer lexer, String name){
        List<String> names = new ArrayList<>();
        names.add(name);
        while (lexer.getTopTokenType() == TokenType.SEP_COMMA) {
            try {
                lexer.getNextToken();
            } catch (LexerException e) {
                e.printStackTrace();
            }
            names.add(lexer.getIDToken().source); // Name
        }
        return names;
    }

    private static Stat getLocalAssignOrFunDefStat(Lexer lexer){
        try {
            lexer.getValidNextToken(TokenType.KW_LOCAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (lexer.getTopTokenType() == TokenType.KW_FUNCTION){
            return getLocalFuncDefStat(lexer);
        }else {
            return getLocalVarStat(lexer);
        }
    }


    private static LocalFuncStat getLocalFuncDefStat(Lexer lexer){
        try {
            lexer.getValidNextToken(TokenType.KW_FUNCTION);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String name = lexer.getIDToken().source;
        FuncDefExp exp = ExpParser.parseFuncDefExp(lexer);
        return new LocalFuncStat(name, exp);
    }

    private static LocalVarStat getLocalVarStat(Lexer lexer){
        String name0 = lexer.getIDToken().source;
        List<String> nameList = finishNameList(lexer, name0); // { , Name }
        List<Exp> expList = null;
        if (lexer.getTopTokenType() == TokenType.OP_ASSIGN) {
            try {
                lexer.getNextToken();                                // ==
                expList = ExpParser.parseExpList(lexer);                    // explist
            } catch (LexerException e) {
                e.printStackTrace();
            }
        }
        return new LocalVarStat(nameList, expList);
    }


    private static Stat getAssignOrFuncStat(Lexer lexer){
//        System.out.println("getAssignOrFuncStat " + lexer.getTopTokenType());
        Exp prefixExp = PrefixExpParser.parsePrefixExp(lexer);
        if (prefixExp instanceof FuncCallExp) {

            return new FuncCallStat((FuncCallExp) prefixExp);
        } else {
            return StatParser.getAssignStat(lexer, prefixExp);
        }
    }

    private static AssignStat getAssignStat(Lexer lexer, Exp var){
        List<Exp> vars = getFinishVarList(lexer, var);
        List<Exp> exps = null;
        try {
            lexer.getValidNextToken(TokenType.OP_ASSIGN);
            exps = ExpParser.parseExpList(lexer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new AssignStat(vars, exps);
    }

    private static List<Exp> getFinishVarList(Lexer lexer, Exp var){
        List<Exp> vars = new ArrayList<>();
        vars.add(checkVar(lexer, var));               // var
        while (lexer.getTopTokenType() == TokenType.SEP_COMMA) { // {
            try {
                lexer.getNextToken();                         // ,
            } catch (LexerException e) {
                e.printStackTrace();
            }
            Exp exp = PrefixExpParser.parsePrefixExp(lexer);           // var
            vars.add(checkVar(lexer, exp));            //
        }                                              // }
        return vars;

    }

    private static Exp checkVar(Lexer lexer, Exp exp) {
        if (exp instanceof LabelExp || exp instanceof  TableVisitExp){
            return exp;
        }
        try {
            lexer.getValidNextToken(null); // trigger error
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("unreachable!");
    }


    private static AssignStat getFuncDefStat(Lexer lexer) {
        try {
            lexer.getValidNextToken(TokenType.KW_FUNCTION);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<Exp, Boolean> map = StatParser.getFuncName(lexer);
        Exp fnExp = map.keySet().iterator().next();
        boolean hasColon = map.values().iterator().next();
        FuncDefExp fdExp = ExpParser.parseFuncDefExp(lexer);
        if (hasColon) { // insert self
            fdExp.parList.add(0, "self");
        }
        ArrayList<Exp> fdExps = new ArrayList<>(); fdExps.add(fdExp);
        ArrayList<Exp> fnExps = new ArrayList<>(); fnExps.add(fnExp);

        return new AssignStat(fnExps, fdExps);
    }


    private static Map<Exp, Boolean> getFuncName(Lexer lexer) {
        Token id = lexer.getIDToken();
        Exp exp = new LabelExp(id.source);
        boolean hasColon = false;

        while (lexer.getTopTokenType() == TokenType.SEP_DOT) {
            try {
                lexer.getNextToken();
            } catch (LexerException e) {
                e.printStackTrace();
            }
            id = lexer.getIDToken();
            Exp idx = new StringExp(id);
            exp = new TableVisitExp( exp, idx);
        }
        if (lexer.getTopTokenType() == TokenType.SEP_COLON) {
            try {
                lexer.getNextToken();
            } catch (LexerException e) {
                e.printStackTrace();
            }
            id = lexer.getIDToken();
            Exp idx = new StringExp(id);
            exp = new TableVisitExp(exp, idx);
            hasColon = true;
        }

        // workaround: return multiple values
        return Collections.singletonMap(exp, hasColon);
    }
}


class PrefixExpParser{
    static Exp parsePrefixExp(Lexer lexer){
        Exp exp;
//        System.out.println("parsePrefixExp " + lexer.getTopTokenType());
        if (lexer.getTopTokenType() == TokenType.IDENTIFIER){
            Token t = lexer.getIDToken();
            exp = new LabelExp(t.source);
        }else {
            exp = parseParentExp(lexer);
        }

        return generatePrefixExp(lexer, exp);
    }

    static Exp parseParentExp(Lexer lexer){
        Exp exp = null;
        try {
            lexer.getValidNextToken(TokenType.SEP_LPAREN);
            exp = ExpParser.parseExp(lexer);
            lexer.getValidNextToken(TokenType.SEP_RPAREN);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (exp instanceof LabelExp || exp instanceof FuncCallExp || exp instanceof VarargExp){
            return new ParentExp(exp);
        }

        return exp;
    }

    static Exp generatePrefixExp(Lexer lexer, Exp exp){
        while (true){
            switch (lexer.getTopTokenType()){
                case SEP_LBRACK:
                    try {
                        lexer.getNextToken();
                        Exp varExp = ExpParser.parseExp(lexer);
                        lexer.getValidNextToken(TokenType.SEP_RBRACK);
                        exp = new TableVisitExp(exp, varExp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case SEP_DOT:
                    try {
                        lexer.getNextToken();
                        Token name = lexer.getIDToken();
                        Exp varExp = new StringExp(name);
                        exp = new TableVisitExp(exp, varExp);
                    } catch (LexerException e) {
                        e.printStackTrace();
                    }
                case SEP_COLON:
                case STRING:
                case SEP_LCURLY:
                case SEP_LPAREN:
                    exp = generaetFuncExp(lexer, exp);
                default:
                    return exp;
            }
        }
    }

    static Exp generaetFuncExp(Lexer lexer, Exp prefixExp){
        FuncCallExp exp = new FuncCallExp();
        exp.nameExp = parseNameExp(lexer);
        exp.prefixExp = prefixExp;
        exp.args =  parseArgs(lexer);
        return exp;
    }

    static StringExp parseNameExp(Lexer lexer){
        StringExp exp = null;
        if (lexer.getTopTokenType() == TokenType.SEP_COLON){
            try {
                lexer.getNextToken();
            } catch (LexerException e) {
                e.printStackTrace();
            }
            Token name = lexer.getIDToken();
            exp = new StringExp(name);
        }

        return exp;
    }

    static List<Exp> parseArgs(Lexer lexer){
        List<Exp> args = new ArrayList<>();
        switch (lexer.getTopTokenType()){
            case SEP_LPAREN:
                try {
                    lexer.getNextToken();
                    if (lexer.getTopTokenType() != TokenType.SEP_RPAREN) {
                        args = ExpParser.parseExpList(lexer);
                    }
                    lexer.getValidNextToken(TokenType.SEP_RPAREN);
                    return args;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            case SEP_LCURLY:

                try {
                    args.add(ExpParser.parseTableDefExp(lexer));
                    return args;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            default:
                Token str = null;
                try {
                    str = lexer.getValidNextToken(TokenType.STRING);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                args.add(new StringExp(str));
                return args;
        }
    }
}
