package AST.Stats;

import AST.Block;
import AST.Exp;
import AST.Stat;

import java.util.List;

/**
 * Created by lijin on 5/9/19.
 */
public class IfStat extends Stat {
    public List<Exp> exps;
    public List<Block> blocks;
    public IfStat(List<Exp> exps, List<Block> blocks){
        this.exps = exps;
        this.blocks = blocks;
    }
}
