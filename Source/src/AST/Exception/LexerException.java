package AST.Exception;

/**
 * Created by lijin on 5/9/19.
 */
public class LexerException extends Exception {
    public LexerException(String msg){
        super("LexerException: " + msg);
    }
}
