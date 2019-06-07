package AST.Exps;

import AST.Exp;

import java.util.List;

/**
 * Created by lijin on 5/9/19.
 */
public class ConcatExp extends Exp{
    public List<Exp> exps;
    public ConcatExp(List<Exp> exps){
        this.exps = exps;
    }
}
