package edu.ufl.cise.plc;

//This class eliminates hard coded dependencies on the actual Lexer class.  You can
//call your lexer whatever you
//want as long as it implements the ILexer interface and you have provided an
//appropriate body for the getLexer method.

import edu.ufl.cise.plc.ast.ASTVisitor;

public class CompilerComponentFactory {

	// This method will be invoked to get an instance of your lexer.
	public static ILexer getLexer(String input)
	{
		try {
			ILexer lexer = new Lexer(input);
			return lexer;
		}
		catch (Exception e) {
			throw new UnsupportedOperationException("CompilerComponentFactory must be modified to return an instance of your lexer");
		}
	}

	public static IParser getParser(String input) {
		ILexer lexer = getLexer(input);
		return new Parser(lexer);
	}

	public static ASTVisitor getTypeChecker() {
		return new edu.ufl.cise.plc.TypeCheckVisitor();
	}

	public static ASTVisitor getCodeGenerator(String packageName) {
		CodeGenVisitor visit = new CodeGenVisitor(packageName);
		return new CodeGenVisitor(packageName);
	}

}