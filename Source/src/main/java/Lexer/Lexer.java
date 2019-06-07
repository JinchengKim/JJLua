package Lexer;


import AST.Exception.LexerException;

import java.util.HashMap;
import java.util.regex.Pattern;

import static Lexer.TokenType.IDENTIFIER;
import static Lexer.TokenType.KW_RETURN;

public class Lexer {
    public CodeSeq chunk;
    private Token cacheToken;
    HashMap<String, TokenType > keyWordMap = new HashMap<String, TokenType>(){{
        put("and",TokenType.KW_AND);
        put("break",TokenType.KW_BREAK);
        put("do",TokenType.KW_DO);
        put("else",TokenType.KW_ELSE);
        put("elseif",TokenType.KW_ELSEIF);
        put("end",TokenType.KW_END);
        put("false",TokenType.KW_FALSE);
        put("for",TokenType.KW_FOR);
        put("function",TokenType.KW_FUNCTION);
        put("if",TokenType.KW_IF);
        put("in",TokenType.KW_IN);
        put("local",TokenType.KW_LOCAL);
        put("nil",TokenType.KW_NIL);
        put("not",TokenType.KW_NOT);
        put("or",TokenType.KW_OR);
        put("repeat",TokenType.KW_REPEAT);
        put("return", KW_RETURN);
        put("then",TokenType.KW_THEN);
        put("true",TokenType.KW_TRUE);
        put("until",TokenType.KW_UNTIL);
        put("while",TokenType.KW_WHILE);
    }};

    public Lexer(String src){
        this.chunk = new CodeSeq(src);
    }

    public Token getNextToken() throws LexerException {
        if (cacheToken != null){
            Token token = cacheToken;
            cacheToken = null;
            return token;
        }

        // skip blank
        while (chunk.length() > 0){
            if (chunk.startWith("--")){
                // TODO
            }else if(chunk.startWith("\n\r") || chunk.startWith("\r\n")){
                chunk.next(2);
            }else if(chunk.isWhiteSpace()){
                chunk.next(1);
            }else if(chunk.isNewLine()) {
                chunk.next(1);
            }else {
                break;
            }
        }

        if (chunk.length() <= 0){
            return new Token(TokenType.EOF, "");
        }
        switch (chunk.charAt(0)){
            case '(': chunk.next(1); return new Token( TokenType.SEP_LPAREN, "(");
            case ')': chunk.next(1); return new Token( TokenType.SEP_RPAREN, ")");
            case ']': chunk.next(1); return new Token( TokenType.SEP_RBRACK, "]");
            case '{': chunk.next(1); return new Token( TokenType.SEP_LCURLY, "{");
            case '}': chunk.next(1); return new Token( TokenType.SEP_RCURLY, "}");
            case '#': chunk.next(1); return new Token( TokenType.OP_LEN,     "#");
            case '+': chunk.next(1); return new Token( TokenType.OP_ADD,     "+");
            case '-': chunk.next(1); return new Token( TokenType.OP_MINUS,   "-");
            case '*': chunk.next(1); return new Token( TokenType.OP_MUL,     "*");
            case '^': chunk.next(1); return new Token( TokenType.OP_POW,     "^");
            case '%': chunk.next(1); return new Token( TokenType.OP_MOD,     "%");
            case '&': chunk.next(1); return new Token( TokenType.OP_BAND,    "&");
            case '|': chunk.next(1); return new Token( TokenType.OP_BOR,     "|");
            case ';': chunk.next(1); return new Token( TokenType.SEP_SEMI,   ";");
            case ',': chunk.next(1); return new Token( TokenType.SEP_COMMA,  ",");
            case '/':{
                if (chunk.startWith("//")){
                    chunk.next(2);
                    return new Token( TokenType.OP_IDIV, "//");
                }else{
                    chunk.next(1);
                    return new Token( TokenType.OP_DIV, "/");
                }
            }

            case '=':{
                if (chunk.startWith("==")){
                    chunk.next(2);
                    return new Token( TokenType.OP_EQ, "==");
                }else {
                    chunk.next();
                    return new Token( TokenType.OP_ASSIGN, "=");
                }
            }

            case '>':{
                if (chunk.startWith(">=")){
                    chunk.next(2);
                    return new Token( TokenType.OP_GE, ">=");
                }else {
                    chunk.next();
                    return new Token( TokenType.OP_GT, ">");
                }
            }

            case '<':{
                if (chunk.startWith("<=")){
                    chunk.next(2);
                    return new Token( TokenType.OP_LE, "<=");
                }else {
                    chunk.next();
                    return new Token( TokenType.OP_LT, "<");
                }
            }

            case ':':{
                if (chunk.startWith("::")){
                    chunk.next(2);
                    return new Token( TokenType.SEP_LABEL, "::");
                }else{
                    chunk.next(1);
                    return new Token( TokenType.SEP_COLON, ":");
                }
            }

            case '~':{
                if (chunk.startWith("~=")){
                    chunk.next(2);
                    return new Token( TokenType.OP_NE, "~=");
                }else {
                    chunk.next();
                    return new Token( TokenType.OP_WAVE, "~");
                }
            }

            case '[':{
                if (chunk.startWith("[[") || chunk.startWith("[=")){
                    return new Token( TokenType.STRING, parseLongStr());
                }else {
                    chunk.next();
                    return new Token( TokenType.SEP_LBRACK, "[");
                }
            }
            case '.':{
                if (chunk.startWith("...")){
                    chunk.next(3);
                    return new Token( TokenType.VARARG, "...");
                }else if(chunk.startWith("..")){
                    chunk.next(2);
                    return new Token( TokenType.OP_CONCAT, "..");
                }else if (chunk.length() == 1 || !chunk.isDigit(1) ){
                    chunk.next();
                    return new Token( TokenType.SEP_DOT, ".");
                }
            }

            case '\'':
            case '"':{
                return new Token( TokenType.STRING, parseShortStr());
            }
        }
        char c = chunk.charAt(0);
        if (c == '.' || chunk.isDigit(0)){
            return new Token( TokenType.NUMBER, parseNumber());
        }
        if (c == '_' || chunk.isLetter(0)){
            String str = parse(Pattern.compile("^[_\\d\\w]+"));
            if (this.keyWordMap.containsKey(str)){
                return new Token( this.keyWordMap.get(str), str);
            }else {
                return new Token( TokenType.IDENTIFIER, str);
            }
        }

        throw new LexerException("unexpected symbol" );
    }

    public String parseNumber() throws LexerException {
        return parse(Pattern.compile("^0[xX][0-9a-fA-F]*(\\.[0-9a-fA-F]*)?([pP][+\\-]?[0-9]+)?|^[0-9]*(\\.[0-9]*)?([eE][+\\-]?[0-9]+)?"));
    }

    public String parseLongStr() throws LexerException {
        String openLongStr = chunk.matchStr(Pattern.compile("^\\[=*\\["));
        if (openLongStr == null){
            throw new LexerException("match long string error");
        }
        String closeLongStr = openLongStr.replace("[","]");
        int strIdx = chunk.indexOf(closeLongStr);

        String str = chunk.subString(openLongStr.length(), strIdx);
        chunk.next(strIdx + closeLongStr.length());
        str = Pattern.compile("\r\n|\n\r|\n|\r").matcher(str).replaceAll("\n");

        // TODO
        return str;
    }

    public String parseShortStr() throws LexerException {
        String str = chunk.matchStr(Pattern.compile("(?s)(^'(\\\\\\\\|\\\\'|\\\\\\n|\\\\z\\s*|[^'\\n])*')|(^\"(\\\\\\\\|\\\\\"|\\\\\\n|\\\\z\\s*|[^\"\\n])*\")"));
        if (str != null){
            chunk.next(str.length());
            str = str.substring(1, str.length() - 1);
            if (str.indexOf('\\') >= 0){
                // TODO
            }

            return str;
        }
        throw new LexerException("parseShortStr error");
    }

    public String parse(Pattern pattern) throws LexerException {
        String token = chunk.matchStr(pattern);
        if (token == null){
            throw new LexerException("lexer error");
        }
        chunk.next(token.length());
        return token;
    }

    public Token getIDToken(){
        try {
            return getValidNextToken(IDENTIFIER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public TokenType getTopTokenType(){
        if (cacheToken == null){
            try {
                cacheToken = getNextToken();
            } catch (LexerException e) {
                e.printStackTrace();
            }
        }
        return cacheToken.type;
    }

    public Token getValidNextToken(TokenType type) throws Exception{
        Token t = getNextToken();
        if (t.type != type){
            throw new LexerException("func getValidNextToken");
        }
        return t;
    }

    public boolean isTopTheEndOfStat(){
        try {
            TokenType ty = this.getTopTokenType();
            switch (ty){
                case KW_RETURN:
                case EOF:
                case KW_END:
                case KW_ELSE:
                case KW_ELSEIF:
                case KW_UNTIL:
                    return true;
            }
        }catch (Exception e){}

        return false;
    }
}
