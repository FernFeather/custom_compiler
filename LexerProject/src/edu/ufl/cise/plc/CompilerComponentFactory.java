package edu.ufl.cise.plc;

//This class eliminates hard coded dependencies on the actual Lexer class.  You can
//call your lexer whatever you
//want as long as it implements the ILexer interface and you have provided an
//appropriate body for the getLexer method.

public class CompilerComponentFactory {

	// This method will be invoked to get an instance of your lexer.
	public static ILexer getLexer(String input)
	{
		//TODO:  modify this method so it returns an instance of your Lexer
		//instead of throwing the exception.
		//for example:
		try {
			ILexer lexer = new Lexer(input);
			return lexer;
		}
		catch (Exception e) {
			throw new UnsupportedOperationException("CompilerComponentFactory must be modified to return an instance of your lexer");
		}
	}

	public static IParser getParser(String input) {
		return null;
	}
}