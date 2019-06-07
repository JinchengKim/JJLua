package AST.Exps;

import AST.Exp;

/**
 * Created by lijin on 5/9/19.
 */
public class IntExp extends Exp {
    public long val;
    public IntExp(long val){
        this.val = val;
    }
}
