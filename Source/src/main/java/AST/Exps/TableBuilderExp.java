package AST.Exps;

import AST.Exp;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lijin on 5/20/19.
 */
public class TableBuilderExp extends Exp {
    public List<Exp> valExps = new ArrayList<>();
    public List<Exp> keyExps = new ArrayList<>();

    public void addVal(Exp val) {
        valExps.add(val);
    }
    public void addKey(Exp key) {
        keyExps.add(key);
    }

}
