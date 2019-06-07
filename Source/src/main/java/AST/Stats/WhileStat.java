package AST.Stats;

import AST.Block;
import AST.Exp;
import AST.Stat;

/**
 * Created by lijin on 5/9/19.
 */
public class WhileStat extends Stat{
    public Exp exp;
    public Block block;
    public WhileStat(Exp exp, Block block){
        this.exp = exp; this.block = block;
    }
}
