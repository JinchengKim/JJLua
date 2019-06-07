package AST.Stats;

import AST.Exps.FuncDefExp;
import AST.Stat;

/**
 * Created by lijin on 5/9/19.
 */
public class LocalFuncStat extends Stat {
    public String name;
    public FuncDefExp exp;
    public LocalFuncStat(String name, FuncDefExp exp){
        this.name = name; this.exp = exp;
    }
}
