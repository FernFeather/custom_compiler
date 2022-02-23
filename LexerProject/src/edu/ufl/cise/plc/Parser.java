package edu.ufl.cise.plc;
import edu.ufl.cise.plc.ast.*;

public class Parser implements IParser {

    private ILexer lexer;

    public Parser(ILexer lexer) {
        this.lexer = lexer;
    }


    @Override
    public ASTNode parse() throws PLCException {
        return this.expr();
    }

    //expr ::= term (( + | - ) term  )*
    private Expr expr() throws SyntaxException, LexicalException {
        Expr left = expr1();
        Expr right = null;
        IToken nextToken = lexer.peek();
        while (nextToken.getKind() == IToken.Kind.OR)
        {
            IToken op = nextToken;
            nextToken = lexer.next();
            right = expr1();
            left = new BinaryExpr(nextToken, left, op, right);
            nextToken = lexer.peek();
        }
        return left;
    }

    //expr ::= term (( + | - ) term  )*
    private Expr expr1() throws SyntaxException, LexicalException {
        Expr left = expr2();
        Expr right = null;
        IToken nextToken = lexer.peek();
        while (nextToken.getKind() == IToken.Kind.AND)
        {
            IToken op = nextToken;
            nextToken = lexer.next();
            right = expr2();
            left = new BinaryExpr(nextToken, left, op, right);
            nextToken = lexer.peek();
        }
        return left;
    }

    //expr ::= term (( + | - ) term  )*
    private Expr expr2() throws SyntaxException, LexicalException {
        Expr left = expr3();
        Expr right = null;
        IToken nextToken = lexer.peek();
        while (nextToken.getKind() == IToken.Kind.LT || nextToken.getKind() == IToken.Kind.GT || nextToken.getKind() == IToken.Kind.EQUALS || nextToken.getKind() == IToken.Kind.NOT_EQUALS || nextToken.getKind() == IToken.Kind.LE || nextToken.getKind() == IToken.Kind.GE)
        {
            IToken op = nextToken;
            nextToken = lexer.next();
            right = expr3();
            left = new BinaryExpr(nextToken, left, op, right);
            nextToken = lexer.peek();
        }
        return left;
    }

    //expr ::= term (( + | - ) term  )*
    private Expr expr3() throws SyntaxException, LexicalException {
        Expr left = term();
        Expr right = null;
        IToken nextToken = lexer.peek();
        while (nextToken.getKind() == IToken.Kind.PLUS || nextToken.getKind() == IToken.Kind.MINUS)
        {
            IToken op = nextToken;
            nextToken = lexer.next();
            right = term();
            left = new BinaryExpr(nextToken, left, op, right);
            nextToken = lexer.peek();
        }
        return left;
    }

    // term ::= factor ( ( * | / )  factor )*
    private Expr term() throws SyntaxException, LexicalException {
        Expr left = factor();
        Expr right = null;
        IToken nextToken = lexer.peek();
        while (nextToken.getKind() == IToken.Kind.TIMES || nextToken.getKind() == IToken.Kind.DIV || nextToken.getKind() == IToken.Kind.MOD)
        {
            IToken op = nextToken;
            nextToken = lexer.next();
            right = factor();
            left = new BinaryExpr(nextToken, left, op, right);
            nextToken = lexer.peek();
        }
        return left;
    }

    //factor ::= int_lit | ( expr )
    private Expr factor() throws SyntaxException, LexicalException {
        IToken nextToken = lexer.peek();
        Expr e = null;
        if (nextToken.getKind() == IToken.Kind.INT_LIT){
            Expr first = new IntLitExpr(nextToken);
            lexer.next();
            if (lexer.peek().getKind() == IToken.Kind.LSQUARE) {
                IToken op = nextToken;
                nextToken = lexer.next();
                IToken type = nextToken;
                Expr x = expr();
                lexer.next();
                Expr y = expr();
                PixelSelector ps = new PixelSelector(type, x, y);
                e = new UnaryExprPostfix(op, first, ps);
                match(IToken.Kind.RSQUARE);
            } else {
                e = first;
            }
        }
        else if (nextToken.getKind() == IToken.Kind.LPAREN)
        {
            lexer.next();
            e = expr();
            match(IToken.Kind.RPAREN);
        }
        else if (nextToken.getKind() == IToken.Kind.BOOLEAN_LIT)
        {
            Expr first = new BooleanLitExpr(nextToken);
            lexer.next();
            if (lexer.peek().getKind() == IToken.Kind.LSQUARE) {
                IToken op = nextToken;
                nextToken = lexer.next();
                IToken type = nextToken;
                Expr x = expr();
                lexer.next();
                Expr y = expr();
                PixelSelector ps = new PixelSelector(type, x, y);
                e = new UnaryExprPostfix(op, first, ps);
                match(IToken.Kind.RSQUARE);
            } else {
                e = first;
            }
        }
        else if (nextToken.getKind() == IToken.Kind.FLOAT_LIT)
        {
            Expr first = new FloatLitExpr(nextToken);
            lexer.next();
            if (lexer.peek().getKind() == IToken.Kind.LSQUARE) {
                IToken op = nextToken;
                nextToken = lexer.next();
                IToken type = nextToken;
                Expr x = expr();
                lexer.next();
                Expr y = expr();
                PixelSelector ps = new PixelSelector(type, x, y);
                e = new UnaryExprPostfix(op, first, ps);
                match(IToken.Kind.RSQUARE);
            } else {
                e = first;
            }
        }
        else if (nextToken.getKind() == IToken.Kind.BANG || nextToken.getKind() == IToken.Kind.MINUS || nextToken.getKind() == IToken.Kind.COLOR_OP || nextToken.getKind() == IToken.Kind.IMAGE_OP)
        {
            IToken op = nextToken;
            nextToken = lexer.next();
            Expr right = expr();
            e = new UnaryExpr(op, op, right);
            nextToken = lexer.peek();
        }
        else if (nextToken.getKind() == IToken.Kind.STRING_LIT)
        {
            Expr first = new StringLitExpr(nextToken);
            lexer.next();
            if (lexer.peek().getKind() == IToken.Kind.LSQUARE) {
                IToken op = nextToken;
                nextToken = lexer.next();
                IToken type = nextToken;
                Expr x = expr();
                lexer.next();
                Expr y = expr();
                PixelSelector ps = new PixelSelector(type, x, y);
                e = new UnaryExprPostfix(op, first, ps);
                match(IToken.Kind.RSQUARE);
            } else {
                e = first;
            }
        }
        else if (nextToken.getKind() == IToken.Kind.IDENT)
        {
            Expr first = new IdentExpr(nextToken);
            lexer.next();
            if (lexer.peek().getKind() == IToken.Kind.LSQUARE) {
                IToken op = nextToken;
                nextToken = lexer.next();
                IToken type = nextToken;
                Expr x = expr();
                lexer.next();
                Expr y = expr();
                PixelSelector ps = new PixelSelector(type, x, y);
                e = new UnaryExprPostfix(op, first, ps);
                match(IToken.Kind.RSQUARE);
            } else {
                e = first;
            }
        }
        else if (nextToken.getKind() == IToken.Kind.KW_IF)
        {
            IToken type = nextToken;
            nextToken = lexer.next();
            Expr condition = expr();
            Expr trueCase = expr();
            match(IToken.Kind.KW_ELSE);
            e = new ConditionalExpr(type, condition, trueCase, expr());
            match(IToken.Kind.KW_FI);
            nextToken = lexer.peek();
        }
        else throw new SyntaxException("Expecting intLit or paren");
        return e;
    }

    private void match(IToken.Kind kind) throws SyntaxException, LexicalException {
        IToken nextToken = lexer.next();
        if(nextToken.getKind() == kind)
            return;
        else
            throw new SyntaxException("Not expected kind");
    }

}

//
//
// Expr
// ConditionalExpr
// LogicalOrExpr
// LogicalAndExpr
// ComparisonExpr
// AdditiveExpr
// ComparisonExpr
// AdditiveExpr
// MultiplicativeExpr
// UnaryExpr
// UnaryExprPostfix
// PrimaryExpr
// PixelSelector