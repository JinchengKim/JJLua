package AST.Stats;

import AST.Block;
import AST.Exp;
import AST.Stat;

/**
 * Created by lijin on 5/9/19.
 */
public class ForNumStat  extends Stat{
    public String var;
    public Exp initExp;
    public Exp controllExp;
    public Exp modifyExp;
    public Block block;


}
