package edu.ufl.cise.plc;

import java.util.List;
import java.util.Map;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.ast.Types.Type;

import static edu.ufl.cise.plc.ast.Types.Type.*;

public class TypeCheckVisitor implements ASTVisitor {

	SymbolTable symbolTable = new SymbolTable();
	Program root;

	record Pair<T0,T1>(T0 t0, T1 t1){};  //may be useful for constructing lookup tables.
	Type typeWidth = FLOAT;
	Type typeHeight = FLOAT;
	boolean isReading = false;


	private void check(boolean condition, ASTNode node, String message) throws TypeCheckException {
		if (!condition) {
			throw new TypeCheckException(message, node.getSourceLoc());
		}
	}

	private boolean assignmentCompatible(Type targetType, Type rhsType, VarDeclaration declaration) {
		if (targetType != IMAGE) {
			if (targetType == rhsType) {
				declaration.getExpr().setCoerceTo(targetType);
			}
			else if (targetType == Type.INT && rhsType == Type.FLOAT) {
				declaration.getExpr().setCoerceTo(INT);
			}
			else if (targetType == Type.FLOAT && rhsType == Type.INT) {
				declaration.getExpr().setCoerceTo(FLOAT);
			}
			else if (targetType == Type.INT && rhsType == Type.COLOR) {
				declaration.getExpr().setCoerceTo(INT);
			}
			else if (targetType == Type.COLOR && rhsType == Type.INT) {
				declaration.getExpr().setCoerceTo(COLOR);
			} else {
				return false;
			}
		} else {
			if (rhsType == Type.INT) {
				declaration.getExpr().setCoerceTo(COLOR);
			}
			else if (rhsType == Type.FLOAT) {
				declaration.getExpr().setCoerceTo(COLORFLOAT);
			}
			else if (rhsType == Type.COLOR) {
				declaration.getExpr().setCoerceTo(IMAGE);
			}
			else if (rhsType == Type.COLORFLOAT) {
				declaration.getExpr().setCoerceTo(COLORFLOAT);
			}
			else if (rhsType == Type.IMAGE) {
				declaration.getExpr().setCoerceTo(IMAGE);
			} else {
				return false;
			}
		}
		return true;
	}

	//The type of a BooleanLitExpr is always BOOLEAN.
	//Set the type in AST Node for later passes (code generation)
	//Return the type for convenience in this visitor.
	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		booleanLitExpr.setType(Type.BOOLEAN);
		booleanLitExpr.setCoerceTo(BOOLEAN);
		return Type.BOOLEAN;
	}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		stringLitExpr.setType(Type.STRING);
		stringLitExpr.setCoerceTo(STRING);
		return Type.STRING;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		intLitExpr.setType(Type.INT);
		intLitExpr.setCoerceTo(INT);
		return Type.INT;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		floatLitExpr.setType(Type.FLOAT);
		floatLitExpr.setCoerceTo(FLOAT);
		return Type.FLOAT;
	}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		//TODO:  implement this method
		colorConstExpr.setType(Type.COLOR);
		colorConstExpr.setCoerceTo(COLOR);
		return Type.COLOR;
	}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		consoleExpr.setType(Type.CONSOLE);
		if (isReading) {
			consoleExpr.setCoerceTo(((Declaration) arg).getType());
		}
		return Type.CONSOLE;
	}

	//Visits the child expressions to get their type (and ensure they are correctly typed)
	//then checks the given conditions.String name = declaration.getName();
	//boolean inserted = symbolTable.insert(name,declaration);
	//check(inserted, declaration, "variable " + name + "already declared");
	//Expr initializer = declaration.getInitializer();
	//if (initializer != null) {
	////infer type of initializer
	//Type initializerType = (Type) initializer.visit(this,arg);
	//check(assignmentCompatible(declaration.getType(), initializerType),declaration,
	//"type of expression and declared type do not match");
	//declaration.setAssigned(true);
	//}
	//return null;
	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		Type redType = (Type) colorExpr.getRed().visit(this, arg);
		Type greenType = (Type) colorExpr.getGreen().visit(this, arg);
		Type blueType = (Type) colorExpr.getBlue().visit(this, arg);
		check(redType == greenType && redType == blueType, colorExpr, "color components must have same type");
		check(redType == Type.INT || redType == Type.FLOAT, colorExpr, "color component type must be int or float");
		Type exprType = (redType == Type.INT) ? Type.COLOR : Type.COLORFLOAT;
		colorExpr.setCoerceTo(COLOR);
		colorExpr.setType(exprType);
		return exprType;
	}



	//Maps forms a lookup table that maps an operator expression pair into result type.
	//This more convenient than a long chain of if-else statements.
	//Given combinations are legal; if the operator expression pair is not in the map, it is an error.
	Map<Pair<Kind,Type>, Type> unaryExprs = Map.of(
			new Pair<Kind,Type>(Kind.BANG,BOOLEAN), BOOLEAN,
			new Pair<Kind,Type>(Kind.MINUS, FLOAT), FLOAT,
			new Pair<Kind,Type>(Kind.MINUS, INT),INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,INT), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,COLOR), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,IMAGE), IMAGE,
			new Pair<Kind,Type>(Kind.IMAGE_OP,IMAGE), INT
	);

	//Visits the child expression to get the type, then uses the above table to determine the result type
	//and check that this node represents a legal combination of operator and expression type.
	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		// !, -, getRed, getGreen, getBlue
		Kind op = unaryExpr.getOp().getKind();
		Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);
		//Use the lookup table above to both check for a legal combination of operator and expression, and to get result type.
		Type resultType = unaryExprs.get(new Pair<Kind,Type>(op,exprType));
		check(resultType != null, unaryExpr, "incompatible types for unaryExpr");
		//Save the type of the unary expression in the AST node for use in code generation later.
		unaryExpr.setType(resultType);
		//return the type for convenience in this visitor.
		return resultType;
	}


	//This method has several cases. Work incrementally and test as you go.
	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Kind op = binaryExpr.getOp().getKind();
		Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
		Type rightType = (Type) binaryExpr.getRight().visit(this, arg);
		Type resultType = null;
		switch(op) {//AND, OR, PLUS, MINUS, TIMES, DIV, MOD, EQUALS, NOT_EQUALS, LT, LE, GT,GE
			case AND, OR -> {
				if (leftType == Type.BOOLEAN && rightType == Type.BOOLEAN) resultType = Type.BOOLEAN;
				else check(false, binaryExpr, "incompatible types for operator");
			}
			case EQUALS,NOT_EQUALS -> {
				check(leftType == rightType, binaryExpr, "incompatible types for comparison");
				resultType = Type.BOOLEAN;

			}
			case PLUS, MINUS -> {
				if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
				else if (leftType == Type.FLOAT && rightType == Type.FLOAT) resultType = Type.FLOAT;
				else if (leftType == Type.INT && rightType == Type.FLOAT) {
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = Type.FLOAT;
				}
				else if (leftType == Type.FLOAT && rightType == Type.INT) {
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = Type.FLOAT;
				}
				else if (leftType == Type.COLOR && rightType == Type.COLOR) resultType = Type.COLOR;
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLORFLOAT) resultType = Type.COLORFLOAT;
				else if (leftType == Type.COLOR && rightType == Type.COLORFLOAT) {
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLOR) {
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
				else check(false, binaryExpr, "incompatible types for operator");
			}
			case TIMES, DIV, MOD -> {
				if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
				else if (leftType == Type.FLOAT && rightType == Type.FLOAT) resultType = Type.FLOAT;
				else if (leftType == Type.INT && rightType == Type.FLOAT) {
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = Type.FLOAT;
				}
				else if (leftType == Type.FLOAT && rightType == Type.INT) {
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = Type.FLOAT;
				}
				else if (leftType == Type.COLOR && rightType == Type.COLOR) resultType = Type.COLOR;
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLORFLOAT) resultType = Type.COLORFLOAT;
				else if (leftType == Type.COLOR && rightType == Type.COLORFLOAT) {
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLOR) {
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if (leftType == Type.STRING && rightType == Type.STRING) resultType = Type.STRING;
				else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
				else if (leftType == Type.IMAGE && rightType == Type.INT) resultType = Type.IMAGE;
				else if (leftType == Type.IMAGE && rightType == Type.FLOAT) resultType = Type.IMAGE;
				else if (leftType == Type.INT && rightType == Type.COLOR) {
					binaryExpr.getLeft().setCoerceTo(COLOR);
					resultType = COLOR;
				}
				else if (leftType == Type.COLOR && rightType == Type.INT) {
					binaryExpr.getRight().setCoerceTo(COLOR);
					resultType = COLOR;
				}
				else if (leftType == Type.FLOAT && rightType == Type.COLOR) {
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if (leftType == Type.COLOR && rightType == Type.FLOAT) {
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else check(false, binaryExpr, "incompatible types for operator");
			}
			case LT, LE, GT, GE -> {
				if (leftType == Type.INT && rightType == Type.INT) resultType = Type.BOOLEAN;
				else if (leftType == Type.FLOAT && rightType == Type.FLOAT) resultType = Type.BOOLEAN;
				else if (leftType == Type.INT && rightType == Type.FLOAT) {
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = Type.BOOLEAN;
				}
				else if (leftType == Type.FLOAT && rightType == Type.INT) {
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = Type.BOOLEAN;
				}
				else check(false, binaryExpr, "incompatible types for operator");
			}
			default -> {
				throw new Exception("compiler error");
			}
		}
		binaryExpr.setType(resultType);
		binaryExpr.setCoerceTo(resultType);
		return resultType;
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		// String name = identExpr.getText();
		// symbolTable.insert(identExpr.getName(), identExpr.getDec());
		// return identExpr.getType();

		String name = identExpr.getText();
		Declaration dec = symbolTable.lookup(name);
		check(dec != null, identExpr, "undefined identifier " + name);
		//symbolTable.insert(name, dec);
		check(dec.isInitialized(), identExpr, "using uninitialized variable");
		identExpr.setDec(dec);  //save declaration--will be useful later.
		Type type = null;
		type = dec.getType();
		identExpr.setType(type);
		return type;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		Type condType = (Type) conditionalExpr.getCondition().visit(this, arg);
		Type trueCase = (Type) conditionalExpr.getTrueCase().visit(this, arg);
		Type falseCase = (Type) conditionalExpr.getFalseCase().visit(this, arg);
		check(condType == Type.BOOLEAN, conditionalExpr, "type of condition must be boolean");
		check(trueCase == falseCase, conditionalExpr, "type of trueCase must be the same as the type of falseCase");
		//conditionalExpr.visit(this, arg);
		conditionalExpr.setType(trueCase);
		return trueCase;
	}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		//TODO  implement this method
		throw new UnsupportedOperationException();
	}

	@Override
	//This method can only be used to check PixelSelector objects on the right hand side of an assignment.
	//Either modify to pass in context info and add code to handle both cases, or when on left side
	//of assignment, check fields from parent assignment statement.
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {

		//NameDef tempDef = new NameDef(pixelSelector.getX().getFirstToken(), "int", pixelSelector.getX().getText());
		//tempDef.setInitialized(true);
		//NameDef tempDef2 = new NameDef(pixelSelector.getY().getFirstToken(), "int", pixelSelector.getY().getText());
		//tempDef2.setInitialized(true);
		Type xType = (Type) pixelSelector.getX().visit(this, arg);
		Type yType = (Type) pixelSelector.getY().visit(this, arg);

		//symbolTable.insert(pixelSelector.getX().getText(), tempDef);
		//symbolTable.insert(pixelSelector.getY().getText(), tempDef2);

		check(xType == Type.INT, pixelSelector.getX(), "only ints as pixel selector components");
		check(yType == Type.INT, pixelSelector.getY(), "only ints as pixel selector components");

		return null;
	}

	@Override
	//This method several cases--you don't have to implement them all at once.
	//Work incrementally and systematically, testing as you go.
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		String name = assignmentStatement.getName();
		Declaration dec = symbolTable.lookup(name);
		Type targetType = dec.getType();

		if (targetType != IMAGE) {
			Type rhs = (Type) assignmentStatement.getExpr().visit(this,arg);

			if (targetType == Type.INT && rhs == Type.FLOAT) {
				assignmentStatement.getExpr().setCoerceTo(INT);
			}
			else if (targetType == Type.FLOAT && rhs == Type.INT) {
				assignmentStatement.getExpr().setCoerceTo(FLOAT);
			}
			else if (targetType == Type.INT && rhs == Type.COLOR) {
				assignmentStatement.getExpr().setCoerceTo(INT);
			}
			else if (targetType == Type.COLOR && rhs == Type.INT) {
				assignmentStatement.getExpr().setCoerceTo(COLOR);
			}
			else if (targetType == rhs) {

			} else {
				check(false, assignmentStatement, "incompatible types for operator");
			}
		} else if (targetType == IMAGE && assignmentStatement.getSelector() == null) {
			Type rhs = (Type) assignmentStatement.getExpr().visit(this,arg);
			if (rhs == Type.INT) {
				assignmentStatement.getExpr().setCoerceTo(COLOR);
			}
			else if (rhs == Type.FLOAT) {
				assignmentStatement.getExpr().setCoerceTo(COLORFLOAT);
			}
			else if (rhs == Type.COLOR) {

			}
			else if (rhs == Type.COLORFLOAT) {

			}
			else if (rhs == Type.IMAGE) {

			} else {
				check(false, assignmentStatement, "incompatible types for operator");
			}
		} else {
			PixelSelector ps = assignmentStatement.getSelector();
			check(symbolTable.lookup(ps.getX().getText()) == null, assignmentStatement, "name cannot be declared");
			check(symbolTable.lookup(ps.getY().getText()) == null, assignmentStatement, "name cannot be declared");

			if (typeWidth != Type.INT || typeHeight != Type.INT) {
				throw new TypeCheckException("Dimensions should be ints", dec.getSourceLoc());
			}

			NameDef tempDef = new NameDef(dec.getFirstToken(), "int", dec.getText());
			tempDef.setInitialized(true);
			symbolTable.insert(ps.getX().getText(), tempDef);
			check(symbolTable.insert(ps.getY().getText(), tempDef), tempDef, "x and y cannot have same name");
			//symbolTable.insert(ps.getY().getText(), tempDef);
			Type xType = (Type) ps.getX().visit(this,arg);
			Type yType = (Type) ps.getY().visit(this,arg);
			check(xType == Type.INT && yType == Type.INT, assignmentStatement, "Pixel Selector x and y must be ints");
			Type rhs = (Type) assignmentStatement.getExpr().visit(this,arg);
			if (rhs == Type.INT) {
				assignmentStatement.getExpr().setCoerceTo(COLOR);
			}
			else if (rhs == Type.FLOAT) {
				assignmentStatement.getExpr().setCoerceTo(COLOR);
			}
			else if (rhs == Type.COLOR) {
				assignmentStatement.getExpr().setCoerceTo(COLOR);
			}
			else if (rhs == Type.COLORFLOAT) {
				assignmentStatement.getExpr().setCoerceTo(COLOR);
			}
			else {
				check(false, assignmentStatement, "incompatible types for operator");
			}
			symbolTable.removeEntry(ps.getX().getText(), tempDef);
			symbolTable.removeEntry(ps.getY().getText(), tempDef);
			tempDef.setInitialized(false);
		}
		dec.setInitialized(true);
		return targetType;
	}


	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
		Type destType = (Type) writeStatement.getDest().visit(this, arg);
		check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement,
				"illegal destination type for write");
		check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
		return null;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		String name = readStatement.getName();
		Declaration dec = symbolTable.lookup(name);
		dec.setInitialized(true);
		check(dec != null, readStatement, "undefined identifier " + name);
		PixelSelector ps = readStatement.getSelector();
		isReading = true;
		arg = dec;
		Type rhs = (Type) readStatement.getSource().visit(this, arg);
		isReading = false;
		check(ps == null, readStatement, "read statements don't have pixel selectors");
		check(rhs == Type.STRING || rhs == Type.CONSOLE, readStatement, "illegal rhs type for read");
		return dec.getType();
	}

	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		String name = declaration.getName();
		boolean inserted = symbolTable.insert(name,declaration);
		check(inserted, declaration, "variable " + name + "already declared");
		Expr initializer = declaration.getExpr();

		if (declaration.getDim() != null) {
			typeWidth = (Type) declaration.getDim().getWidth().visit(this, arg);
			typeHeight = (Type) declaration.getDim().getHeight().visit(this, arg);
		} else if (declaration.getType() == IMAGE && declaration.getDim() != null) {
			if (typeWidth != Type.INT || typeHeight != Type.INT) {
				throw new TypeCheckException("Dimensions should be ints", declaration.getSourceLoc());
			}
		}  else if (declaration.getType() == IMAGE && declaration.getDim() == null && initializer == null) {
			throw new TypeCheckException("image declaration should have either a dimension or initializer expression of type image", declaration.getSourceLoc());
		}

		if (initializer != null) {
			//infer type of initializer
			isReading = true;
			arg = declaration;
			Type initializerType = (Type) initializer.visit(this,arg);
			isReading = false;
			if (declaration.getOp().getKind() == IToken.Kind.ASSIGN) {
				check(assignmentCompatible(declaration.getType(), initializerType, declaration),declaration,
						"type of expression and declared type do not match");
			} else if (declaration.getOp().getKind() == IToken.Kind.LARROW) {
				check(initializerType == Type.STRING || initializerType == Type.CONSOLE, declaration, "illegal rhs type for VarDeclaration");
			} else {
				throw new TypeCheckException("type of expression and declared type do not match", declaration.getSourceLoc());
			}

			declaration.setInitialized(true);
		}
		return null;
	}


	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		//TODO:  this method is incomplete, finish it.  kkb

		//Save root of AST so return type can be accessed in return statements
		root = program;

		//Check declarations and statements
		List<NameDef> params = program.getParams();
		for (NameDef p : params) {
			p.setInitialized(true);
			p.visit(this, arg);
		}
		List<ASTNode> decsAndStatements = program.getDecsAndStatements();
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}
		return program;
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		String name = nameDef.getName();
		check(symbolTable.insert(name, nameDef), nameDef, "undefined identifier " + name);
		Type type = nameDef.getType();
		return type;
	}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		String name = nameDefWithDim.getName();
		check(symbolTable.insert(name, nameDefWithDim), nameDefWithDim, "undefined identifier " + name);
		Dimension dim = nameDefWithDim.getDim();
		check(dim.getWidth().getType() == Type.INT && dim.getHeight().getType() == Type.INT, nameDefWithDim, "Dimension width and height must be ints");
		Type type = nameDefWithDim.getType();
		return type;
	}

	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		Type returnType = root.getReturnType();  //This is why we save program in visitProgram.
		Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);
		check(returnType == expressionType, returnStatement, "return statement with invalid type");
		return null;
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		Type expType = (Type) unaryExprPostfix.getExpr().visit(this, arg);
		check(expType == Type.IMAGE, unaryExprPostfix, "pixel selector can only be applied to image");
		unaryExprPostfix.getSelector().visit(this, arg);
		unaryExprPostfix.setType(Type.INT);
		unaryExprPostfix.setCoerceTo(COLOR);
		return Type.COLOR;
	}

}
