package AST.Exps;

import AST.Exp;
import Lexer.Token;
import Lexer.TokenType;

/**
 * Created by lijin on 5/13/19.
 */
public class UnaryOpExp extends Exp {
    public TokenType op;
    public Exp exp;
    public UnaryOpExp(Token token, Exp exp){
        this.exp = exp;
        if (token.type == TokenType.OP_MINUS){
            this.op = TokenType.OP_UNM;
        }else if(token.type == TokenType.OP_WAVE){
            this.op = TokenType.OP_BNOT;
        }else {
            this.op = token.type;
        }
    }
}
