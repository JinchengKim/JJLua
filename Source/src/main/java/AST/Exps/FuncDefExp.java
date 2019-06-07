package AST.Exps;

import AST.Block;
import AST.Exp;
import AST.Stat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lijin on 5/9/19.
 */
public class FuncDefExp extends Exp {
    public List<String> parList = new ArrayList<>();
    public boolean isMultiVar;
    public Block block;

}
