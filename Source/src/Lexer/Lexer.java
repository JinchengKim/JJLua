package Lexer;


import AST.Exception.LexerException;
import com.sun.org.apache.bcel.internal.classfile.Code;
import com.sun.xml.internal.ws.model.RuntimeModelerException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lijin on 5/8/19.
 */

class CodeSeq implements CharSequence{
    private String code;
    private int offset = 0;
    CodeSeq(String code){
        this.code = code;
    }

    public void next(int count){
        offset+=count;
    }
    public void next(){
        offset+=1;
    }

    public boolean startWith(String prefix){
        return code.startsWith(prefix, offset);
    }


    public int indexOf(String str){
        return code.indexOf(str, offset) - offset;
    }
    public String matchStr(Pattern pattern){
        Matcher matcher = pattern.matcher(this);
        return matcher.find() ? matcher.group(0) : null;
    }

    public boolean isWhiteSpace(){
        char c = this.getChar(0);
        if (c == '\t' || c == '\n' || c == '\f' || c == ' ') return true;
        return false;
    }

    public boolean isNewLine(){
        return this.getChar(0) == '\r' || this.getChar(0) == '\n';
    }

    public char getChar(int idx){
        return code.charAt(idx+offset);
    }
    public boolean isDigit(int idx){
        char c = getChar(idx);
        if (c >= '0' && c <= '9') return true;
        return false;
    }

    public String subString(int bidx, int eidx){
        return code.substring(bidx+offset, eidx+offset);
    }

    @Override
    public int length() {
        return this.code.length() - offset;
    }

    @Override
    public char charAt(int index) {
        return this.code.charAt(index + offset);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return this.code.subSequence(start + offset, end + offset);

    }
}
public class Lexer {
    private CodeSeq chunk;
    private int curLine = 1;

    private Token cacheToken;
    private int cacheLine;


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
                // commment
                // TODO
            }else if(chunk.startWith("\n\r") || chunk.startWith("\r\n")){
                chunk.next(2);
                curLine +=1;
            }else if(chunk.isWhiteSpace()){
                chunk.next(1);
            }else if(chunk.isNewLine()) {
                chunk.next(1);
                curLine += 1;
            }else {
                break;
            }
        }

        if (chunk.length() <= 0){
            return new Token(curLine, TokenType.EOF, "");
        }
        switch (chunk.charAt(0)){
            case '(': chunk.next(1); return new Token(curLine, TokenType.SEP_LPAREN, "(");
            case ')': chunk.next(1); return new Token(curLine, TokenType.SEP_RPAREN, ")");
            case ']': chunk.next(1); return new Token(curLine, TokenType.SEP_RBRACK, "]");
            case '{': chunk.next(1); return new Token(curLine, TokenType.SEP_LCURLY, "{");
            case '}': chunk.next(1); return new Token(curLine, TokenType.SEP_RCURLY, "}");
            case '#': chunk.next(1); return new Token(curLine, TokenType.OP_LEN,     "#");
            case '+': chunk.next(1); return new Token(curLine, TokenType.OP_ADD,     "+");
            case '-': chunk.next(1); return new Token(curLine, TokenType.OP_MINUS,   "-");
            case '*': chunk.next(1); return new Token(curLine, TokenType.OP_MUL,     "*");
            case '^': chunk.next(1); return new Token(curLine, TokenType.OP_POW,     "^");
            case '%': chunk.next(1); return new Token(curLine, TokenType.OP_MOD,     "%");
            case '&': chunk.next(1); return new Token(curLine, TokenType.OP_BAND,    "&");
            case '|': chunk.next(1); return new Token(curLine, TokenType.OP_BOR,     "|");
            case ';': chunk.next(1); return new Token(curLine, TokenType.SEP_SEMI,   ";");
            case ',': chunk.next(1); return new Token(curLine, TokenType.SEP_COMMA,  ",");
            case '/':{
                if (chunk.startWith("//")){
                    chunk.next(2);
                    return new Token(curLine, TokenType.OP_IDIV, "//");
                }else{
                    chunk.next(1);
                    return new Token(curLine, TokenType.OP_DIV, "/");
                }
            }

            case '=':{
                if (chunk.startWith("==")){
                    chunk.next(2);
                    return new Token(curLine, TokenType.OP_EQ, "==");
                }else {
                    chunk.next();
                    return new Token(curLine, TokenType.OP_ASSIGN, "=");
                }
            }

            case '>':{
                if (chunk.startWith(">=")){
                    chunk.next(2);
                    return new Token(curLine, TokenType.OP_GE, ">=");
                }else {
                    chunk.next();
                    return new Token(curLine, TokenType.OP_GT, ">");
                }
            }

            case '<':{
                if (chunk.startWith("<=")){
                    chunk.next(2);
                    return new Token(curLine, TokenType.OP_LE, "<=");
                }else {
                    chunk.next();
                    return new Token(curLine, TokenType.OP_LT, "<");
                }
            }

            case ':':{
                if (chunk.startWith("::")){
                    chunk.next(2);
                    return new Token(curLine, TokenType.SEP_LABEL, "::");
                }else{
                    chunk.next(1);
                    return new Token(curLine, TokenType.SEP_COLON, ":");
                }
            }

            case '~':{
                if (chunk.startWith("~=")){
                    chunk.next(2);
                    return new Token(curLine, TokenType.OP_NE, "~=");
                }else {
                    chunk.next();
                    return new Token(curLine, TokenType.OP_WAVE, "~");
                }
            }

            case '[':{
                if (chunk.startWith("[[") || chunk.startWith("[=")){
                    return new Token(curLine, TokenType.STRING, parseLongStr());
                }else {
                    chunk.next();
                    return new Token(curLine, TokenType.SEP_LBRACK, "[");
                }
            }
            case '.':{
                if (chunk.startWith("...")){
                    chunk.next(3);
                    return new Token(curLine, TokenType.VARARG, "...");
                }else if(chunk.startWith("..")){
                    chunk.next(2);
                    return new Token(curLine, TokenType.OP_CONCAT, "..");
                }else if (chunk.length() == 1 || !chunk.isDigit(1) ){
                    chunk.next();
                    return new Token(curLine, TokenType.SEP_DOT, ".");
                }
            }

            case '\'':
            case '"':{
                return new Token(curLine, TokenType.STRING, parseShortStr());
            }
        }
        char c = chunk.charAt(0);
        if (c == '.' || chunk.isDigit(0)){
            return new Token(curLine, TokenType.NUMBER, parseNumber());
        }

        throw new LexerException("unexpected symbol near at line" + curLine);
    }

    public String parseNumber() throws LexerException {
        return parse(Pattern.compile("^0[xX][0-9a-fA-F]*(\\.[0-9a-fA-F]*)?([pP][+\\-]?[0-9]+)?|^[0-9]*(\\.[0-9]*)?([eE][+\\-]?[0-9]+)?"));
    }

    public String parseLongStr() throws LexerException {
        String openLongStr = chunk.matchStr(Pattern.compile("^\\[=*\\["));
        if (openLongStr == null){
            throw new LexerException("match long string error at line " + curLine);
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
                curLine += Pattern.compile("\r\n|\n\r|\n|\r").split(str).length - 1;
                // TODO
            }

            return str;
        }
        throw new LexerException("parseShortStr error at line " + curLine);
    }

    public String parse(Pattern pattern) throws LexerException {
        String token = chunk.matchStr(pattern);
        if (token == null){
            throw new LexerException("lexer error");
        }
        chunk.next(token.length());
        return token;
    }



    public TokenType getTopTokenType() throws LexerException {
        if (cacheToken == null){
            cacheLine = curLine;
            cacheToken = getNextToken();
        }
        return cacheToken.type;
    }

    public int getCurLine(){
        return curLine;
    }

    public Token getValidNextToken(TokenType type) throws Exception{
        Token t = getNextToken();
        if (t.type != type){
            throw new LexerException("func getValidNextToken");
        }
        return t;
    }
}
