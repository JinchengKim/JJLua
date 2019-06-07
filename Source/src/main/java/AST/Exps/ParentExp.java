package AST.Exps;

import AST.Exp;

/**
 * Created by lijin on 5/20/19.
 */
public class ParentExp extends Exp {
    public Exp exp;
    public ParentExp(Exp exp){this.exp = exp;}
}
