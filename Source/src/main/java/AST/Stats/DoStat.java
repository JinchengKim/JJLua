package AST.Stats;

import AST.Block;
import AST.Stat;

/**
 * Created by lijin on 5/9/19.
 */
public class DoStat extends Stat{
    public Block block;
    public DoStat(Block block){
        this.block = block;
    }

}
