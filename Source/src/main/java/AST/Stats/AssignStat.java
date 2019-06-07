package AST.Stats;

import AST.Exp;
import AST.Stat;

import java.util.List;

/**
 * Created by lijin on 5/9/19.
 */
public class AssignStat extends Stat {
    public List<Exp> varList;
    public List<Exp> expList;
    public AssignStat(List<Exp> varList, List<Exp> expList){
        this.varList = varList;
        this.expList = expList;
    }
}
