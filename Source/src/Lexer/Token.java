package Lexer;

/**
 * Created by lijin on 5/8/19.
 */

public class Token {
    public int line;
    public TokenType type;
    public String source;
    public Token(int line, TokenType type, String source){
        this.line = line;
        this.type = type;
        this.source = source;
    }

}
