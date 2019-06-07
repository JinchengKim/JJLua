package AST.Exception;

/**
 * Created by lijin on 5/23/19.
 */
public class DataStrutureException extends Exception {
    public DataStrutureException(String msg) {
        super("DataStrutureException: " + msg);
    }
}
