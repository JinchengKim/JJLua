package AST.Exps;

import AST.Exp;

/**
 * Created by lijin on 5/20/19.
 */
public class LabelExp extends Exp{
    public String name;
    public LabelExp(String name){
        this.name = name;
    }
}
