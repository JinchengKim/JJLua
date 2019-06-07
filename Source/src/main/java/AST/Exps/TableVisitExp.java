package AST.Exps;

import AST.Exp;

import java.security.spec.ECParameterSpec;

/**
 * Created by lijin on 5/20/19.
 */
public class TableVisitExp extends Exp {
    public Exp prefixExp;
    public Exp varExp;
    public TableVisitExp(Exp prefixExp, Exp varExp){
        this.prefixExp = prefixExp;
        this.varExp = varExp;
    }
}
