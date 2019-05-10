package AST.Exception;

/**
 * Created by lijin on 5/9/19.
 */
public class LexerException extends Exception {
    LexerException(String msg){
        super("LexerException: " + msg);
    }
}
