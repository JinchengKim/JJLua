package State;

/**
 * Created by lijin on 5/25/19.
 */
@FunctionalInterface
public interface InstrAction{
    void execute(int i, LState vm);
}
