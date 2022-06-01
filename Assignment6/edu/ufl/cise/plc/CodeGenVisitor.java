package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.javaCompilerClassLoader.DynamicCompiler;

import java.util.Locale;

import java.util.ArrayList;
import java.util.List;

public class CodeGenVisitor implements ASTVisitor {
    SymbolTable symbolTable = new SymbolTable();
    public class CodeGenStringBuilder
    {
        StringBuilder delegate = new StringBuilder();

        //methods reimplemented—just call the delegates method
        public CodeGenStringBuilder append(String s) {
            delegate.append(s);
            return this;
        }
        //
        public CodeGenStringBuilder insert(String x) {
            delegate.insert(0,x);
            return this;
        }
        public CodeGenStringBuilder comma() {
            delegate.append(',');
            return this;
        }
        public CodeGenStringBuilder lParen() {
            delegate.append('(');
            return this;
        }
        public CodeGenStringBuilder rParen() {
            delegate.append(')');
            return this;
        }
        public CodeGenStringBuilder semi() {
            delegate.append(';');
            return this;
        }
        public CodeGenStringBuilder lSquiggly() {
            delegate.append('{');
            return this;
        }
        public CodeGenStringBuilder rSquiggly() {
            delegate.append('}');
            return this;
        }
        public CodeGenStringBuilder newline() {
            delegate.append('\n');
            return this;
        }
        public CodeGenStringBuilder space() {
            delegate.append(' ');
            return this;
        }
        //
        @Override
        public String toString()
        {
            return delegate.toString();
        }
    }

    public byte[] genBytecode(String code, String packageName, String className) throws Exception{
        String fullName = packageName != "" ? packageName+ '.' + className : className;
        byte[] byteCode = DynamicCompiler.compile(fullName,code);
        return byteCode;
    }

    public String genCode(ASTNode ast, String packageName, String className) throws Exception {
        CodeGenVisitor v = (CodeGenVisitor)
                CompilerComponentFactory.getCodeGenerator(packageName);
        String[] names = {packageName, className};
        String code = (String) ast.visit(v, names);
        return code;
    }

    String packageName;
    // CodeGenVisitor constructor should accept a String with the package name as a parameter.
    public CodeGenVisitor (String packageName)
    {

    }



    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Expr expr = returnStatement.getExpr();

        sb.append("return ");
        expr.visit(this, sb);
        return sb;
    }



    @Override
    public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = declaration.getName();
        symbolTable.insert(name,declaration);

        Expr expr = declaration.getExpr();
        if (expr != null) {
            declaration.getNameDef().visit(this, sb);

            if (declaration.getType().equals(Types.Type.IMAGE))
            {
                sb.append(" = ");
                //sb.insert("import java.awt.image.BufferedImage;\n");
                //sb.insert("import edu.ufl.cise.plc.runtime.ImageOps;\n");

                if(declaration.isInitialized())
                {
                    if (declaration.getExpr() instanceof BinaryExpr) {
                        declaration.getExpr().visit(this, sb);
                    } else {
                        if (declaration.getDim()==null) {
                            //000
                            if (declaration.getOp().getKind() == IToken.Kind.LARROW) {
                                sb.append("FileURLIO.readImage(");
                            } else {
                                sb.append("(");
                            }
                            declaration.getExpr().visit(this,sb);
                            sb.append(")");
                        }
                        else{
                            sb.append("ImageOps.resize(");
                            declaration.getExpr().visit(this,sb);
                            sb.append(", ");
                            declaration.getDim().visit(this, sb);
                            sb.rParen();
                        }
                    }
                }
                else{

                    if (declaration.getDim()==null){
                        throw new Error("This case should have been marked as error during type checking");
                    }
                    else{
                        sb.append("new BufferedImage( ");
                        declaration.getDim().visit(this, sb);
                        sb.comma().space();
                        sb.append("BufferedImage.TYPE_INT_RGB").rParen().semi().newline();
                    }
                }

            }
            else if (declaration.getType().equals(Types.Type.COLOR))
            {
                sb.append(" = ");
                if(declaration.isInitialized())
                {
                    if (declaration.getExpr() instanceof BinaryExpr) {
                        declaration.getExpr().visit(this, sb);
                    }
                    else if (declaration.getExpr() instanceof ColorExpr){
                        sb.lParen();
                        expr.visit(this, sb);
                        sb.rParen();
                    }
                    else {
                        sb.append("(ColorTuple)FileURLIO.readValueFromFile(" );
                        expr.visit(this, sb);
                        sb.append(");\n");
                    }
                }

            }
            else {
                if (declaration.getOp().getKind() == IToken.Kind.LARROW) {
                    if (declaration.getDim() != null) {
                        if (declaration.getType().equals(Types.Type.IMAGE)) {
                            declaration.getExpr().visit(this, sb);
                            sb.append(" = FileURLIO.readImage(");
                            declaration.getExpr().visit(this, sb);
                            sb.append(", ");
                            declaration.getDim().visit(this, sb);
                            sb.append(")");
                        }
                    } else {
                        if (declaration.getExpr().getType().equals(Types.Type.IMAGE)) {
                            declaration.getExpr().visit(this, sb);
                            sb.append(" = FileURLIO.readImage(");
                            declaration.getExpr().visit(this, sb);
                            sb.append(")");
                        } else {
                            if (declaration.getExpr().getType() == Types.Type.STRING) {
                                sb.append(" = (");
                                if (declaration.getType() == Types.Type.STRING) {
                                    sb.append("String");
                                } else if (declaration.getType() == Types.Type.COLOR) {
                                    sb.append("ColorTuple");
                                } else {
                                    sb.append(declaration.getType().toString().toLowerCase(Locale.ROOT));
                                }
                                sb.append(")FileURLIO.readValueFromFile(");
                                declaration.getExpr().visit(this, sb);
                                sb.append(")");
                            } else {
                                sb.append(" = ");
                                declaration.getExpr().visit(this, sb);
                            }
                        }
                    }
                } else {
                    sb.append(" = ");
                    if (expr.getCoerceTo() != null) {
                        sb.lParen();
                        if (expr.getCoerceTo() == Types.Type.STRING) {
                            sb.append("String");
                        } else if (expr.getCoerceTo() == Types.Type.COLOR) {
                            sb.append("ColorTuple");
                        } else {
                            sb.append(expr.getCoerceTo().toString().toLowerCase(Locale.ROOT));
                        }
                        sb.rParen();
                    }
                    sb.lParen();
                    expr.visit(this, sb);
                    sb.rParen();
                }
            }

        }
        else{
            if (declaration.getNameDef() != null) {
                declaration.getNameDef().visit(this, sb);
                if (declaration.getType().equals(Types.Type.IMAGE)) {
                    sb.append(" = new BufferedImage(");
                    declaration.getDim().visit(this, sb);
                    sb.comma().space().append("BufferedImage.TYPE_INT_RGB)");
                }
            }
        }


        return sb;
    }


    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Expr expr = unaryExprPostfix.getExpr();
        sb.append("ColorTuple.unpack(");
        expr.visit(this, sb);
        sb.append(".getRGB(");
        //sb.append(((VarDeclaration)(((IdentExpr)expr).getDec())).getNameDef().getName()).comma();
        sb.append(unaryExprPostfix.getSelector().getX().getText()).comma().append(unaryExprPostfix.getSelector().getY().getText());
        //unaryExprPostfix.getSelector().visit(this, sb)z
        sb.rParen().rParen();

        return sb;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String bool = booleanLitExpr.getText();
        sb.append(bool);
        return sb;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String stringLit = stringLitExpr.getValue();
        //String stringLit = stringLitExpr.getText();
        sb.append("\"\"\"").newline();
        sb.append(stringLit);
        sb.append("\"\"\"");
        return sb;
    }

    @Override
    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String intLit = intLitExpr.getText();
        if (intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Types.Type.INT) {
            sb.lParen();
            if (intLitExpr.getCoerceTo() == Types.Type.STRING) {
                sb.append("String");
            } else if (intLitExpr.getCoerceTo() == Types.Type.COLOR) {
                sb.append("ColorTuple");
            } else {
                sb.append(intLitExpr.getCoerceTo().toString().toLowerCase(Locale.ROOT));
            }
            sb.rParen();
        }
        sb.append(intLit);
        return sb;
    }

    @Override
    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String floatLit = floatLitExpr.getText();
        if (floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Types.Type.FLOAT) {
            sb.lParen();
            if (floatLitExpr.getCoerceTo() == Types.Type.STRING) {
                sb.append("String");
            } else if (floatLitExpr.getCoerceTo() == Types.Type.COLOR) {
                sb.append("ColorTuple");
            } else {
                sb.append(floatLitExpr.getCoerceTo().toString().toLowerCase(Locale.ROOT));
            }

            sb.rParen();
        }
        sb.append(floatLit);
        sb.append("f");
        return sb;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.append("ColorTuple.unpack(Color.").append(colorConstExpr.getText()).append(".getRGB())");
        return sb;
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String console = consoleExpr.getText();

        String type = consoleExpr.getCoerceTo().toString();

        switch (type) {
            case "INT" -> sb.append("(Integer)");
            case "STRING" -> sb.append("(String)");
            case "BOOLEAN" -> sb.append("(Boolean)");
            case "FLOAT" -> sb.append("(Float)");
            case "COLOR" -> sb.append("(ColorTuple)");
            case "IMAGE" -> sb.append("(BufferedImage)");
            case "VOID" -> sb.append("(Void)");
            case "CONSOLE" -> sb.append("(Console)");
            default -> throw new Exception("No conversion from type to box type.");
        }

        sb.append(" ConsoleIO.readValueFromConsole(");
        sb.append("\"").append(type).append("\"").comma().space().append("\"Enter ");

        switch (type) {
            case "INT" -> sb.append("integer:\")");
            case "STRING" -> sb.append("string:\")");
            case "BOOLEAN" -> sb.append("boolean:\")");
            case "FLOAT" -> sb.append("float:\")");
            case "COLOR" -> sb.append("RGB values:\")");
            default -> throw new Exception("No conversion from type to box type.");
        }

        return sb;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.append("new ColorTuple(");

        colorExpr.getRed().visit(this, sb);
        sb.comma().space();
        colorExpr.getGreen().visit(this, sb);
        sb.comma().space();
        colorExpr.getBlue().visit(this, sb);
        sb.rParen();

        return sb;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;

        sb.lParen();

        if (unaryExpression.getExpr().getType() == Types.Type.INT) {
            sb.append("ColorTuple.").append(unaryExpression.getOp().getText()).lParen();
            unaryExpression.getExpr().visit(this, sb);
            sb.rParen().rParen();
        } else if (unaryExpression.getExpr().getType() == Types.Type.COLOR) {
            sb.append("ColorTuple.");
            sb.append(unaryExpression.getOp().getText()).lParen();

            unaryExpression.getExpr().visit(this, sb);
            sb.rParen().rParen();
        } else if (unaryExpression.getExpr().getType() == Types.Type.IMAGE) {

            if (unaryExpression.getOp().getText().equals("getBlue") || unaryExpression.getOp().getText().equals("getGreen") || unaryExpression.getOp().getText().equals("getRed")) {
                sb.append("ImageOps.");
                switch (unaryExpression.getOp().getText()) {
                    case "getBlue" -> sb.append("extractBlue").lParen();
                    case "getRed" -> sb.append("extractRed").lParen();
                    case "getGreen" -> sb.append("extractGreen").lParen();
                }
                sb.lParen();
                unaryExpression.getExpr().visit(this, sb);
                sb.append(")))");
            } else {
                unaryExpression.getExpr().visit(this, sb);
                sb.append(".");
                //sb.rParen().semi().newline();
                sb.append(unaryExpression.getOp().getText()).append("())");
            }

        } else {

            IToken.Kind op = unaryExpression.getOp().getKind();
            switch (op) {
                case BANG -> {
                    sb.append("!");
                    sb.append(unaryExpression.getExpr().getText());
                    sb.rParen();
                }
                case MINUS -> {
                    sb.append("-");
                    sb.append(unaryExpression.getExpr().getText());
                    sb.rParen();
                }
                default -> throw new Exception("Unary expression should only be ! or -");
            }
        }

        return sb;
    }

    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {

        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;

        Expr leftExpr = binaryExpr.getLeft();
        Expr rightExpr = binaryExpr.getRight();

        Types.Type leftType = leftExpr.getCoerceTo() != null ? leftExpr.getCoerceTo() : leftExpr.getType();
        Types.Type rightType = rightExpr.getCoerceTo() != null ? rightExpr.getCoerceTo() : rightExpr.getType();

        if (leftType.equals(Types.Type.COLOR) && rightType.equals(Types.Type.COLOR) )
        {

            if (binaryExpr.getOp().getKind() == IToken.Kind.EQUALS || binaryExpr.getOp().getKind() == IToken.Kind.NOT_EQUALS) {
                sb.append("(ImageOps.binaryTupleOp(ImageOps.BoolOP.");
                sb.append(binaryExpr.getOp().getKind().name());
            } else {
                sb.append("(ImageOps.binaryTupleOp(ImageOps.OP.");
                sb.append(binaryExpr.getOp().getKind().name());
            }

            sb.comma().space();
            binaryExpr.getLeft().visit(this, sb);
            sb.comma().space();
            binaryExpr.getRight().visit(this, sb);
            sb.rParen().rParen();
        }
        else if (leftType.equals(Types.Type.IMAGE) && rightType.equals(Types.Type.IMAGE) )
        {
            sb.append("(ImageOps.binaryImageImageOp(ImageOps.OP.");
            sb.append(binaryExpr.getOp().getKind().name());
            sb.comma().space().append((String)binaryExpr.getLeft().getText()).comma().space();
            sb.append((String)binaryExpr.getRight().getText()).rParen().rParen().semi().newline();
        }
        else if (((leftType.equals(Types.Type.COLOR) && rightType.equals(Types.Type.IMAGE)) || (leftType.equals(Types.Type.IMAGE) && rightType.equals(Types.Type.COLOR))))
        {
            sb.append("(ImageOps.binaryImageScalarOp(ImageOps.OP.");
            sb.append(binaryExpr.getOp().getKind().name());
            sb.comma().space().append((String)binaryExpr.getLeft().getText()).comma().space();
            sb.append((String)binaryExpr.getRight().getText()).rParen().rParen().semi().newline();

            sb.append("ColorTuple.makePackedColor(ColorTuple.getRed(").append((String)binaryExpr.getRight().getText()).lParen().comma().append("ColorTuple.getGreen").lParen();
            sb.append((String)binaryExpr.getRight().getText()).lParen().comma().space();
            sb.append("ColorTuple.getBlue(").lParen().append((String)binaryExpr.getRight().getText()).lParen().lParen().lParen().semi();
        }
        else if (((leftType.equals(Types.Type.IMAGE) && rightType.equals(Types.Type.INT)) || (leftType.equals(Types.Type.INT) && rightType.equals(Types.Type.IMAGE))))
        {
            sb.append("(ImageOps.binaryImageScalarOp(ImageOps.OP.");
            sb.append(binaryExpr.getOp().getKind().name());
            sb.comma().space().append((String)binaryExpr.getLeft().getText());
            sb.comma().space().append((String)binaryExpr.getRight().getText()).rParen().rParen();
        }

        else {

            IToken.Kind op = binaryExpr.getOp().getKind();

            sb.lParen();
            if (leftType == Types.Type.STRING && rightType == Types.Type.STRING && op == IToken.Kind.NOT_EQUALS) {
                sb.append("!");
            }
            binaryExpr.getLeft().visit(this, sb);
            if (leftType == Types.Type.STRING && rightType == Types.Type.STRING && (op == IToken.Kind.EQUALS || op == IToken.Kind.NOT_EQUALS)) {
                sb.append(".equals(");
            } else {
                switch (op) {
                    case PLUS -> sb.append(" + ");
                    case MINUS -> sb.append(" - ");
                    case AND -> sb.append(" & ");
                    case OR -> sb.append(" | ");
                    case EQUALS -> sb.append(" == ");
                    case NOT_EQUALS -> sb.append(" != ");
                    case TIMES -> sb.append(" * ");
                    case DIV -> sb.append(" / ");
                    case MOD -> sb.append(" % ");
                    case LT -> sb.append(" < ");
                    case LE -> sb.append(" <= ");
                    case GT -> sb.append(" > ");
                    case GE -> sb.append(" >= ");
                    default -> throw new Exception("Binary Expression should have specific op");
                }
            }

            binaryExpr.getRight().visit(this, sb);
            if (leftType == Types.Type.STRING && rightType == Types.Type.STRING && (op == IToken.Kind.EQUALS || op == IToken.Kind.NOT_EQUALS)) {
                sb.rParen();
            }
            sb.rParen();

        }

        return sb;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String ident = identExpr.getText();
        if (identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType()) {
            sb.lParen();
            if (identExpr.getCoerceTo() == Types.Type.STRING) {
                sb.append("String");
            } else if (identExpr.getCoerceTo() == Types.Type.COLOR) {
                sb.append("ColorTuple");
            } else {
                sb.append(identExpr.getCoerceTo().toString().toLowerCase(Locale.ROOT));
            }
            sb.rParen();
        }
        sb.append(ident);
        return sb;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.lParen();
        sb.lParen();
        conditionalExpr.getCondition().visit(this, sb);
        sb.rParen();
        sb.append(" ? ");
        conditionalExpr.getTrueCase().visit(this,sb);
        sb.append(" : ");
        conditionalExpr.getFalseCase().visit(this,sb);
        sb.rParen();
        return sb;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        dimension.getWidth().visit(this, sb);
        sb.comma().space();
        dimension.getHeight().visit(this, sb);
        return sb;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        CodeGenStringBuilder left = new CodeGenStringBuilder();
        CodeGenStringBuilder right = new CodeGenStringBuilder();

        left.append("for(int x = 0; x<f.getWidth(); x++)\n\t");
        left.append("for(int y = 0; x<f.getHeight(); y++)");



        List<CodeGenStringBuilder> list = new ArrayList<CodeGenStringBuilder>();
        list.add(left);
        list.add(right);
        return list;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = assignmentStatement.getName();
        PixelSelector ps = assignmentStatement.getSelector();


        if(assignmentStatement.getSelector() != null && assignmentStatement.getExpr().getType().equals(Types.Type.COLOR))
        {
            if(assignmentStatement.getExpr().getCoerceTo() != null && assignmentStatement.getExpr().getCoerceTo() == Types.Type.COLOR)
            {
                sb.append("for(int ");
                sb.append(ps.getX().getText());
                sb.append(" = 0; ");
                sb.append(ps.getX().getText());
                sb.append(" < ");
                sb.append(name).append(".getWidth(); ");
                sb.append(ps.getX().getText());
                sb.append("++)\n\t");
                sb.append("for(int ");
                sb.append(ps.getY().getText());
                sb.append(" = 0; ");
                sb.append(ps.getY().getText());
                sb.append(" < ");
                sb.append(name).append(".getHeight(); ");
                sb.append(ps.getY().getText());
                sb.append("++)\n\t\t");
                sb.append("ImageOps.setColor(");
                sb.append(name).append(", ");
                sb.append(ps.getX().getText());
                sb.append(", ");
                sb.append(ps.getY().getText());
                sb.append(", ");
                assignmentStatement.getExpr().visit(this,sb);
                sb.rParen();
            }
            else if(assignmentStatement.getExpr().getCoerceTo() != null && assignmentStatement.getExpr().getCoerceTo() == Types.Type.INT)
            {
                sb.append("for(int ");
                sb.append(ps.getX().getText());
                sb.append(" = 0; ");
                sb.append(ps.getX().getText());
                sb.append(" < ");
                sb.append(name).append(".getWidth(); ");
                sb.append(ps.getX().getText());
                sb.append("++)\n\t");
                sb.append("for(int ");
                sb.append(ps.getY().getText());
                sb.append(" = 0; ");
                sb.append(ps.getY().getText());
                sb.append(" < ");
                sb.append(name).append(".getHeight(); ");
                sb.append(ps.getY().getText());
                sb.append("++)\n\t\t");
                sb.append("ImageOps.setColor(");
                sb.append(name).append(", ");
                sb.append(ps.getX().getText());
                sb.append(", ");
                sb.append(ps.getY().getText());
                sb.append(", ");
                sb.append("ColorTuple.unpack(ColorTuple.truncate(");
                assignmentStatement.getExpr().visit(this,sb);
                sb.rParen().rParen().rParen();
            }
            else
            {
                sb.append("for(int ");
                sb.append(ps.getX().getText());
                sb.append(" = 0; ");
                sb.append(ps.getX().getText());
                sb.append(" < ");
                sb.append(name).append(".getWidth(); ");
                sb.append(ps.getX().getText());
                sb.append("++)\n\t");
                sb.append("for(int ");
                sb.append(ps.getY().getText());
                sb.append(" = 0; ");
                sb.append(ps.getY().getText());
                sb.append(" < ");
                sb.append(name).append(".getHeight(); ");
                sb.append(ps.getY().getText());
                sb.append("++)\n\t\t");
                sb.append("ImageOps.setColor(");
                sb.append(name).append(", ");
                sb.append(ps.getX().getText());
                sb.append(", ");
                sb.append(ps.getY().getText());
                sb.rParen().rParen().rParen();
            }

        }
        else {
            sb.append(name);
            sb.append(" = ");
            if (assignmentStatement.getExpr().getCoerceTo() != null && assignmentStatement.getExpr().getType() != Types.Type.COLOR)  {
                sb.lParen();
                if (assignmentStatement.getExpr().getCoerceTo() == Types.Type.STRING) {
                    sb.append("String");
                } else if (assignmentStatement.getExpr().getCoerceTo() == Types.Type.COLOR) {
                    sb.append("ColorTuple");
                } else {
                    sb.append(assignmentStatement.getExpr().getCoerceTo().toString().toLowerCase(Locale.ROOT));
                }
                sb.rParen();
            }

            assignmentStatement.getExpr().visit(this, sb);
        }
        return sb;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        if(writeStatement.getSource().getType().equals(Types.Type.IMAGE))
        {
            if(writeStatement.getDest().getType() == Types.Type.CONSOLE) {
                sb.append("ConsoleIO.displayImageOnScreen(");
                writeStatement.getSource().visit(this, sb);
                sb.append(")");
            } else if (writeStatement.getDest().getType() == Types.Type.STRING) {
                sb.append("FileURLIO.writeImage(");
                writeStatement.getSource().visit(this, sb);
                sb.comma().space();
                writeStatement.getDest().visit(this, sb);
                sb.append(")");
            } else {
                sb.append("FileURLIO.writeValue(");
                sb.append(writeStatement.getSource().getText()).comma().space();
                sb.append(writeStatement.getDest().getText());
                sb.append(")");
            }
        }
        else if(writeStatement.getDest().getType().equals(Types.Type.STRING))
        {
            sb.append("FileURLIO.writeValue(");
            writeStatement.getSource().visit(this, sb);
            sb.comma().space();
            writeStatement.getDest().visit(this, sb);
            sb.append(")");
        }

        else
        {
            sb.append("ConsoleIO.console.println(");
            writeStatement.getSource().visit(this, sb);
            sb.append(")");
        }
        return sb;
    }

    @Override // TODO: 4/21/2022
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;

        String name = readStatement.getName();
        Declaration dec = symbolTable.lookup(name);

        sb.append(name);


        if (dec.getDim() != null) {
            if (dec.getType().equals(Types.Type.IMAGE)) {
                //readStatement.getSource().visit(this, sb);
                sb.append(" = FileURLIO.readImage(");
                readStatement.getSource().visit(this, sb);
                sb.append(", ");
                dec.getDim().visit(this, sb);
                sb.append(")");
            }
        }
        else {
            if (readStatement.getSource().getType().equals(Types.Type.IMAGE)) {
                readStatement.getSource().visit(this, sb);
                sb.append(" = FileURLIO.readImage(");
                readStatement.getSource().visit(this, sb);
                sb.append(")");
            } else {
                if (readStatement.getSource().getType() == Types.Type.STRING) {
                    sb.append(" = (");
                    if (dec.getType() == Types.Type.COLOR) {
                        sb.append("ColorTuple");
                    } else if (dec.getType() == Types.Type.STRING) {
                        sb.append("String");
                    } else {
                        sb.append(dec.getType().toString().toLowerCase(Locale.ROOT));
                    }
                    sb.append(")FileURLIO.readValueFromFile(");
                    readStatement.getSource().visit(this, sb);
                    sb.append(")");
                } else {
                    sb.append(" = ");
                    readStatement.getSource().visit(this, sb);
                }
            }
        }

        return sb;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        CodeGenStringBuilder sb = new CodeGenStringBuilder();
        //sb.append(this.packageName).newline();
        sb.append("import edu.ufl.cise.plc.runtime.javaCompilerClassLoader.DynamicCompiler;").newline();
        sb.insert("import edu.ufl.cise.plc.runtime.FileURLIO;\n");
        sb.insert("import java.awt.image.BufferedImage;\n");
        sb.insert("import java.awt.Color;\n");
        sb.append("import edu.ufl.cise.plc.runtime.ConsoleIO;").newline();
        sb.append("import static edu.ufl.cise.plc.runtime.ImageOps.BoolOP.*;").newline();
        sb.append("import edu.ufl.cise.plc.runtime.ColorTuple;").newline();
        sb.append("import edu.ufl.cise.plc.runtime.ImageOps;").newline();
        sb.append("public class ");
        sb.append(program.getName()).space().lSquiggly().newline();
        sb.append("public static ");
        if (program.getReturnType() == Types.Type.STRING) {
            sb.append("String").space();
        } else {
            switch(program.getReturnType()) {
                case  BOOLEAN -> sb.append("boolean").space();
                case  COLOR -> sb.append("ColorTuple").space();
                case  CONSOLE -> sb.append("console").space();
                case  FLOAT -> sb.append("float").space();
                case  IMAGE -> sb.append("BufferedImage").space();
                case  INT -> sb.append("int").space();
                case  VOID -> sb.append("void").space();
                default -> throw new Error("Unexpected string value");
            };
        }
        sb.append("apply").lParen();
        for(int i = 0; i < program.getParams().size(); i++)
        {
            program.getParams().get(i).visit(this, sb);
            if(i < program.getParams().size() - 1)
            {
                sb.comma().space();
            }
        }
        sb.rParen().space().lSquiggly().newline();
        for(int d = 0; d < program.getDecsAndStatements().size(); d++)
        {
            program.getDecsAndStatements().get(d).visit(this, sb);
            if(d < program.getDecsAndStatements().size())
            {
                sb.semi().newline();
            }
        }
        sb.rSquiggly().newline().rSquiggly();
        sb.insert("package cop4020sp22Package;\n");
        return sb.toString();
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = nameDef.getName();
        symbolTable.insert(name, nameDef);
        String type = nameDef.getType().toString().toLowerCase(Locale.ROOT);


        if (type.equals("string")) {
            type = "String";
        }
        if (type.equals("image")) {
            type = "BufferedImage";
        }
        if (type.equals("color")) {
            type = "ColorTuple";
        }
        sb.append(type).space();
        sb.append(name);
        return sb;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = nameDefWithDim.getName();
        symbolTable.insert(name, nameDefWithDim);
        String type = nameDefWithDim.getType().toString().toLowerCase(Locale.ROOT);
        if (type.equals("image")) {
            type = "BufferedImage";
        }
        if (type.equals("color")) {
            type = "ColorTuple";
        }
        if (type.equals("string")) {
            type = "String";
        }
        sb.append(type).space();
        sb.append(name);
        return sb;

    }
}