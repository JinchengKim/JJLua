package Lexer;

/**
 * Created by lijin on 5/8/19.
 */

public class Token {
    public TokenType type;
    public String source;
    public Token(TokenType type, String source){
        this.type = type;
        this.source = source;
    }


}
