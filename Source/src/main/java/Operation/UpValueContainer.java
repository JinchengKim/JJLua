package Operation;

/**
 * Created by lijin on 5/23/19.
 */
public class UpValueContainer {
    public final int index;
    public LStack stack;
    public Object value;

    public UpValueContainer(Object value) {
        this.value = value;
        this.index = 0;
    }

    public UpValueContainer(LStack stack, int index) {
        this.stack = stack;
        this.index = index;
    }

    Object get() {
        return stack != null ? stack.get(index + 1) : value;
    }

    void set(Object value) {
        if (stack != null) {
            stack.set(index + 1, value);
        } else {
            this.value = value;
        }
    }

    public void migrate() {
        if (stack != null) {
            value = stack.get(index + 1);
            stack = null;
        }
    }
}
