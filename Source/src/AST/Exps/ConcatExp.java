package AST.Exps;

import AST.Exp;

import java.util.List;

/**
 * Created by lijin on 5/9/19.
 */
public class ConcatExp extends Exp{
    public List<Exp> exps;
    public ConcatExp(int line, List<Exp> exps){
        this.beginLine = line;
        this.exps = exps;
    }
}
