package AST;

import java.util.List;

/**
 * Created by lijin on 5/8/19.
 */
public class Block extends ASTNode{
    public List<Stat> stats;
    public List<Exp> exps;
}
