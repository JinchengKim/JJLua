package AST.Stats;

import AST.Exp;
import AST.Stat;

import java.util.List;

/**
 * Created by lijin on 5/9/19.
 */
public class LocalVarStat extends Stat{
    public List<String> names;
    public List<Exp> exps;
    public LocalVarStat(int endLine, List<String> names, List<Exp> exps){
        this.endLine = endLine;
        this.names = names;
        this.exps = exps;
    }
}
