package AST.Exps;

import AST.Exp;
import Lexer.Token;
import Lexer.TokenType;

/**
 * Created by lijin on 5/9/19.
 */
public class BinaryOpExp extends Exp{
    public TokenType op; //OP_
    public Exp exp1;
    public Exp exp2;
    public BinaryOpExp(Token op, Exp exp1, Exp exp2){
        this.op = op.type;
        this.exp1 = exp1;
        this.exp2 = exp2;
    }
}
