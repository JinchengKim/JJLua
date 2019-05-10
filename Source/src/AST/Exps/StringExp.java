package AST.Exps;

import AST.Exp;
import Lexer.Token;

/**
 * Created by lijin on 5/9/19.
 */
public class StringExp extends Exp {
    public String str;
    public StringExp(Token token){
        this.beginLine = token.line;
        this.str = token.source;
    }
}
