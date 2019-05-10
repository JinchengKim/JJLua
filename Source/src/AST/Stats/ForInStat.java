package AST.Stats;

import AST.Block;
import AST.Exp;
import AST.Stat;

import java.util.List;

/**
 * Created by lijin on 5/9/19.
 */
public class ForInStat extends Stat{
    public int locOfDO;
    public List<String> nameList;
    public List<Exp> expList;
    public Block block;

}
