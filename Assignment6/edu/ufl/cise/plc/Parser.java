package edu.ufl.cise.plc;
import edu.ufl.cise.plc.ast.*;

import java.util.ArrayList;
import java.util.List;


public class Parser implements IParser {

    private ILexer lexer;

    public Parser(ILexer lexer) {
        this.lexer = lexer;
    }


    @Override
    public ASTNode parse() throws PLCException {
        return this.program();
    }

    private Program program() throws LexicalException, SyntaxException
    {
        try {
            IToken nextToken = lexer.peek();
            Types.Type type = type();
            String name = lexer.next().getText();
            if (name.length() == 0) {
                throw new SyntaxException("");
            }
            List<NameDef> params = params();
            List<ASTNode> decsAndStatements = decsAndStatements();
            return new Program(nextToken, type, name, params, decsAndStatements);
        } catch (LexicalException e) {
            throw new LexicalException("");
        }
        catch (Exception e)
        {
            throw new SyntaxException("");
        }

    }

    private Types.Type type() throws LexicalException
    {
        IToken nextToken = lexer.next();
        return Types.Type.toType(nextToken.getText());
    }

    private List<ASTNode> decsAndStatements() throws LexicalException, SyntaxException
    {
        IToken nextToken = lexer.peek();
        List<ASTNode> decsAndStatements = new ArrayList<>();
        while (nextToken.getKind() != IToken.Kind.EOF)
        {

                boolean withDimension = false;

                IToken varType = nextToken;

                if (varType.getKind() == IToken.Kind.RETURN) {
                    IToken operation = lexer.next();
                    Expr exp = expr();

                    ReturnStatement returnStatement = new ReturnStatement(operation, exp);
                    decsAndStatements.add(returnStatement);

                    lexer.next();
                    nextToken = lexer.peek();
                } else if (varType.getKind() == IToken.Kind.KW_WRITE) {
                    IToken operation = lexer.next();
                    Expr source = expr();
                    lexer.next();
                    //lexer.next();
                    Expr dest = expr();

                    WriteStatement writeStatement = new WriteStatement(operation, source, dest);
                    decsAndStatements.add(writeStatement);

                    lexer.next();
                    nextToken = lexer.peek();
                } else if (varType.getKind() != IToken.Kind.TYPE) {
                    IToken name = lexer.next();
                    PixelSelector ps = null;

                    if (lexer.peek().getKind() == IToken.Kind.LSQUARE) {
                        nextToken = lexer.next();
                        IToken type = nextToken;
                        Expr x = expr();
                        lexer.next();
                        Expr y = expr();
                        ps = new PixelSelector(type, x, y);
                        match(IToken.Kind.RSQUARE);
                    }

                    if (lexer.peek().getKind() == IToken.Kind.LARROW) {
                        lexer.next();
                        Expr exp = expr();

                        ReadStatement readStatement = new ReadStatement(name, name.getText(), ps, exp);
                        decsAndStatements.add(readStatement);

                        lexer.next();
                        nextToken = lexer.peek();
                    } else if (lexer.peek().getKind() == IToken.Kind.ASSIGN) {
                        lexer.next();
                        Expr exp = expr();

                        AssignmentStatement assignmentStatement = new AssignmentStatement(name, name.getText(), ps, exp);
                        decsAndStatements.add(assignmentStatement);

                        lexer.next();
                        nextToken = lexer.peek();
                    }
                }
                else {
                    Dimension dimension = null;
                    lexer.next();
                    if (lexer.peek().getKind() == IToken.Kind.LSQUARE) {
                        IToken tempToken = lexer.next();
                        Expr x = expr();
                        lexer.next();
                        Expr y = expr();
                        dimension = new Dimension(tempToken, x, y);
                        match(IToken.Kind.RSQUARE);
                        withDimension = true;
                    }

                    IToken name = lexer.next();
                    if (withDimension) {
                        if (lexer.peek().getKind() == IToken.Kind.SEMI) {
                            NameDef nameDef = new NameDefWithDim(nextToken, varType, name, dimension);
                            VarDeclaration vardec = new VarDeclaration(nextToken, nameDef, null, null);
                            decsAndStatements.add(vardec);
                            lexer.next();
                            nextToken = lexer.peek();
                        } else if (lexer.peek().getKind() == IToken.Kind.ASSIGN || lexer.peek().getKind() == IToken.Kind.LARROW) {
                            NameDef nameDef = new NameDefWithDim(nextToken, varType, name, dimension);
                            IToken operation = lexer.next();
                            Expr exp = expr();

                            VarDeclaration vardec = new VarDeclaration(nextToken, nameDef, operation, exp);
                            decsAndStatements.add(vardec);

                            lexer.next();
                            nextToken = lexer.peek();
                        }
                    } else {
                        if (lexer.peek().getKind() == IToken.Kind.SEMI) {
                            NameDef nameDef = new NameDef(nextToken, varType, name);
                            VarDeclaration vardec = new VarDeclaration(nextToken, nameDef, null, null);
                            decsAndStatements.add(vardec);
                            lexer.next();
                            nextToken = lexer.peek();
                        } else if (lexer.peek().getKind() == IToken.Kind.ASSIGN || lexer.peek().getKind() == IToken.Kind.LARROW) {
                            NameDef nameDef = new NameDef(nextToken, varType, name);
                            IToken operation = lexer.next();
                            Expr exp = expr();

                            VarDeclaration vardec = new VarDeclaration(nextToken, nameDef, operation, exp);
                            decsAndStatements.add(vardec);

                            lexer.next();
                            nextToken = lexer.peek();
                        }
                    }
                }
        }
        return decsAndStatements;
    }

    private List<NameDef> params() throws LexicalException, SyntaxException {
        lexer.next();
        IToken nextToken = lexer.peek();
        List<NameDef> params = new ArrayList<>();

        while(nextToken.getKind() != IToken.Kind.RPAREN)
        {
            boolean withDimension = false;
            IToken type = lexer.next();

            //possible dim
            Dimension dimension = null;
            if (lexer.peek().getKind() == IToken.Kind.LSQUARE) {
                IToken tempToken = lexer.next();
                Expr x = expr();
                lexer.next();
                Expr y = expr();
                dimension = new Dimension(tempToken, x, y);
                match(IToken.Kind.RSQUARE);
                withDimension = true;
            }

            IToken name = lexer.next();

            if (withDimension) {
                NameDef param = new NameDefWithDim(nextToken, type, name, dimension);
                params.add(param);
            } else {
                NameDef param = new NameDef(nextToken, type, name);
                params.add(param);
            }

            if(lexer.peek().getKind() == IToken.Kind.COMMA)
            {
                nextToken = lexer.next();
            }
            else
            {
                nextToken = lexer.peek();
            }
        }
        lexer.next();
        return params;
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
        else if (nextToken.getKind() == IToken.Kind.LANGLE)
        {
            IToken op = nextToken;
            lexer.next();
            Expr r = expr();
            lexer.next();
            Expr g = expr();
            lexer.next();
            Expr b = expr();
            e = new ColorExpr(op, r, g, b);
            match(IToken.Kind.RANGLE);
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
            Expr right = factor();
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
        else if (nextToken.getKind() == IToken.Kind.COLOR_CONST)
        {
            Expr first = new ColorConstExpr(nextToken);
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
        else if (nextToken.getKind() == IToken.Kind.KW_CONSOLE)
        {
            Expr first = new ConsoleExpr(nextToken);
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