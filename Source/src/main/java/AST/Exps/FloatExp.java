package AST.Exps;

import AST.Exp;

/**
 * Created by lijin on 5/9/19.
 */
public class FloatExp extends Exp{
    public double val;
    public FloatExp(double val){
        this.val = val;
    }
}
