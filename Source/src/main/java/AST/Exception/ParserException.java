package AST.Exception;

/**
 * Created by lijin on 5/9/19.
 */
public class ParserException extends Exception {
    public ParserException(String msg){
        super("ParserException: " + msg);
    }
}
