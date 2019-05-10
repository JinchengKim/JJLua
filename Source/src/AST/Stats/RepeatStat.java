package AST.Stats;

import AST.Block;
import AST.Exp;
import AST.Stat;

/**
 * Created by lijin on 5/9/19.
 */
public class RepeatStat extends Stat {
    public Block block;
    public Exp exp;
    public RepeatStat(Block block, Exp exp){
        this.block = block;
        this.exp = exp;
    }
}
