package AST.Stats;

import AST.Exps.FuncCallExp;
import AST.Stat;

/**
 * Created by lijin on 5/20/19.
 */
public class FuncCallStat extends Stat{
    public FuncCallExp exp;
    public FuncCallStat(FuncCallExp exp){
        this.exp = exp;
    }
}
