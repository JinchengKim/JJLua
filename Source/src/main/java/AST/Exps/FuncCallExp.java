package AST.Exps;

import AST.Exp;

import java.util.List;

/**
 * Created by lijin on 5/9/19.
 */
public class FuncCallExp extends Exp{
    public List<Exp> args;
    public Exp prefixExp;
    public StringExp nameExp;

}
