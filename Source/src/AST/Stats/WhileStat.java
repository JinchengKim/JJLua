package AST.Stats;

import AST.Block;
import AST.Exp;

/**
 * Created by lijin on 5/9/19.
 */
public class WhileStat {
    public Exp exp;
    public Block block;
    public WhileStat(Exp exp, Block block){
        this.exp = exp; this.block = block;
    }
}
