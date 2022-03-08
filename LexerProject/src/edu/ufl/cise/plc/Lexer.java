package edu.ufl.cise.plc;

import java.util.ArrayList;
import java.util.List;

public class Lexer implements ILexer {

	private String chars = "";
	private List<IToken> tokens = new ArrayList<>();
	private static String input;
	private int currTokenPos = 0;
	private int pos = 0;
	private int start = 0;
	private int line = 0;
	private boolean isFloat;
	private boolean hasDot;
	private boolean is_escaping;
	private boolean isBad;

	/* Implements DFA Pass in String containing source to tokenize in constructor */
	public Lexer(String input) throws LexicalException {
		this.input = input;
		scanTokens();
	}

	@Override
	public IToken next() throws LexicalException {
		IToken currToken = tokens.get(currTokenPos);
		if(currToken.getKind() == IToken.Kind.INT_LIT)
		{
			try {
				currToken.getIntValue();
			}
			catch (Exception e) {
				throw new LexicalException("");
			}
		}
		if(currToken.getKind() == IToken.Kind.ERROR)
		{
			throw new LexicalException("");
		}
		currTokenPos++;
		return currToken;
	}

	@Override
	public IToken peek() throws LexicalException {
		IToken currToken = tokens.get(currTokenPos);
		if(currToken.getKind() == IToken.Kind.INT_LIT)
		{
			try {
				currToken.getIntValue();
			}
			catch (Exception e) {
				throw new LexicalException("");
			}
		}
		if(currToken.getKind() == IToken.Kind.ERROR)
		{
			throw new LexicalException("");
		}
		return currToken;
	}

	// 1. Define an enum for the internal states.
	private enum state{START, IN_IDENT, HAVE_ZERO, HAVE_DOT, IN_FLOAT, IN_NUM, HAVE_EQ, HAVE_GREATER, HAVE_LESS, HAVE_MINUS, HAVE_BANG, HAVE_COMMENT, IN_STRING}
	state currentState = state.START;

	// 2. Loop over characters
	List<IToken> scanTokens() throws LexicalException {
		while (!isAtEnd()) {

			// Read a char
			char ch = input.charAt(pos); // get current char

			// Handle it according to the current state
			switch (currentState) {
				case START -> {
					isFloat = false;
					hasDot = false;
					is_escaping = false;
					isBad = false;
					switch (ch) {
						case ' ', '\t', '\r' -> {
							pos++;
							start++;
						}
						case '\n' -> {
							start = 0;
							pos++;
							line++;
						} case '"'-> { //handle all single char tokens like this
							currentState = state.IN_STRING;
						}
						case '>'-> {
							currentState = state.HAVE_GREATER;
						}
						case '<'-> {
							currentState = state.HAVE_LESS;
						}
						case '+' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							Token currentToken = new Token(IToken.Kind.PLUS, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case '=' -> {
							currentState = state.HAVE_EQ;
						}
						case '-' -> {
							currentState = state.HAVE_MINUS;
						}
						case '!' -> {
							currentState = state.HAVE_BANG;
						}
						case '.' -> {
							currentState = state.HAVE_DOT;
						}
						case '*' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.TIMES, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case '/' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.DIV, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case '%' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.MOD, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case '#' -> {
							currentState = state.HAVE_COMMENT;
						}
						case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {

							currentState = state.IN_NUM;
						}
						case '&' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.AND, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case '|' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.OR, chars, srcLocation,chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case 'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O',
								'P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m',
								'n','o','p','q','r','s','t','u','v','w','x','y','z','_','$'-> {
							currentState = state.IN_IDENT;
						}
						case '(' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							Token currentToken = new Token(IToken.Kind.LPAREN, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case ')' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							Token currentToken = new Token(IToken.Kind.RPAREN, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case '[' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							Token currentToken = new Token(IToken.Kind.LSQUARE, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case ']' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							Token currentToken = new Token(IToken.Kind.RSQUARE, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case ';' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							Token currentToken = new Token(IToken.Kind.SEMI, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case ',' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							Token currentToken = new Token(IToken.Kind.COMMA, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						case '^' -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							Token currentToken = new Token(IToken.Kind.RETURN, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							chars="";
							start++;
							pos++;
						}
						default -> {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.ERROR, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start++;
							pos++;
							chars="";
							currentState = state.START;
						}
					}
				}
				case IN_IDENT -> {
					if ("1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_$".indexOf(ch) == -1)
					{
						IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
						IToken currentToken;
						if(chars.equals("float") || chars.equals("int") || chars.equals("string") || chars.equals("boolean") || chars.equals("color") || chars.equals("image")){
							currentToken = new Token(IToken.Kind.TYPE, chars, srcLocation, chars.length());
						}
						else if(chars.equals("BLACK")|| chars.equals("BLUE")|| chars.equals("CYAN") || chars.equals("DARK_GRAY") || chars.equals("GRAY")|| chars.equals("GREEN")
								|| chars.equals("LIGHT_GRAY") || chars.equals("MAGENTA") || chars.equals("ORANGE") || chars.equals("PINK")|| chars.equals("RED")|| chars.equals("WHITE")|| chars.equals("YELLOW")) {
							currentToken = new Token(IToken.Kind.COLOR_CONST, chars, srcLocation, chars.length());
						}
						else if(chars.equals("getRed") || chars.equals("getGreen") || chars.equals("getBlue")){
							currentToken = new Token(IToken.Kind.COLOR_OP, chars, srcLocation, chars.length());
						}
						else if(chars.equals("getWidth") || chars.equals("getHeight")){
							currentToken = new Token(IToken.Kind.IMAGE_OP, chars, srcLocation, chars.length());
						}
						else if(chars.equals("true") || chars.equals("false")){
							currentToken = new Token(IToken.Kind.BOOLEAN_LIT, chars, srcLocation, chars.length());
						}
						else if(chars.equals("if")){
							currentToken = new Token(IToken.Kind.KW_IF, chars, srcLocation, chars.length());
						}
						else if(chars.equals("fi")){
							currentToken = new Token(IToken.Kind.KW_FI, chars, srcLocation,chars.length());
						}
						else if(chars.equals("else")){
							currentToken = new Token(IToken.Kind.KW_ELSE, chars, srcLocation, chars.length());
						}
						else if(chars.equals("write")){
							currentToken = new Token(IToken.Kind.KW_WRITE, chars, srcLocation, chars.length());
						}
						else if(chars.equals("console")){
							currentToken = new Token(IToken.Kind.KW_CONSOLE, chars, srcLocation, chars.length());
						}
						else if(chars.equals("void")){
							currentToken = new Token(IToken.Kind.KW_VOID, chars, srcLocation, chars.length());
						}
						else{
							currentToken = new Token(IToken.Kind.IDENT, chars, srcLocation, chars.length());
						}
						tokens.add(currentToken);
						start+=chars.length();
						chars="";

						currentState = state.START;
					}
					else {
						if ((pos+1) >= input.length()) {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.IDENT, chars, srcLocation, chars.length());
							tokens.add(currentToken);
						}
						chars += ch;
						pos++;
					}

				}
				case HAVE_COMMENT -> {
					if ('\n' == (ch))
					{
						currentState = state.START;
					}
					else{
						start++;
						pos++;
					}
				}
				case HAVE_ZERO -> {
				}
				case HAVE_DOT -> {
					hasDot = true;
					chars = chars + ch;
					pos++;
					currentState = state.IN_NUM;
				}
				case IN_FLOAT -> {
				}
				case IN_NUM -> {
					if (ch == '.') {
						if (isFloat) {
							if (hasDot) {
								IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
								IToken currentToken = new Token(IToken.Kind.ERROR, chars, srcLocation, chars.length());
								tokens.add(currentToken);
								start+=chars.length();
								chars = "";
							} else {
								IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
								IToken currentToken = new Token(IToken.Kind.FLOAT_LIT, chars, srcLocation, chars.length());
								tokens.add(currentToken);
								start+=chars.length();
								chars = "";
							}
							currentState = state.START;
						} else {
							chars += ch;
							pos++;
							isFloat = true;
						}
					}
					else if ("1234567890".indexOf(ch) == -1)
					{
						if (hasDot) {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.ERROR, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start += chars.length();
							chars = "";
						} else if (isFloat && chars.charAt(chars.length() - 1) == '.') {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.ERROR, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start += chars.length();
							chars = "";
						} else if (isFloat) {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.FLOAT_LIT, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start += chars.length();
							chars = "";
						} else {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.INT_LIT, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start += chars.length();
							chars = "";
						}

						currentState = state.START;
					}
					else{
						if (chars.equals("0") && chars.length() == 1){
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.INT_LIT, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start++;
							chars = "";
							currentState = state.START;
						} else {
							chars += ch;
							pos++;
						}
					}
				}
				case HAVE_EQ -> {

					if (ch != '=')
					{
						if(chars.length() == 1)
						{
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.ASSIGN, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start++;
							chars="";
						}
						if(chars.length() == 2)
						{
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.EQUALS, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start+=2;
							chars="";
						}
						currentState = state.START;
					}
					else{
						chars = chars + ch;
						pos++;
						if(chars.length() >= 2)
						{
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.EQUALS, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start+=chars.length();
							chars="";
							currentState = state.START;
						}

					}
				}
				case HAVE_BANG -> {
					if (chars.length() == 0 && ch == '!')
					{
						chars+=ch;
						pos++;
					}
					else if (chars.length() == 1 && ch =='=')
					{
						chars+=ch;
						pos++;
					}
					else if (chars.length() == 1)
					{
						IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
						IToken currentToken = new Token(IToken.Kind.BANG, chars, srcLocation, chars.length());
						tokens.add(currentToken);
						start++;
						chars="";
						currentState = state.START;
					}
					else if (chars.length() == 2)
					{
						IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
						IToken currentToken = new Token(IToken.Kind.NOT_EQUALS, chars, srcLocation, chars.length());
						tokens.add(currentToken);
						start+=2;
						chars="";
						currentState = state.START;
					}
				}
				case HAVE_MINUS -> {

					if (ch != '-' && ch != '>') {
						if (chars.length() == 1) {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.MINUS, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start++;
							chars = "";
						} else if (chars.length() == 2) {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.RARROW, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start += 2;
							chars = "";
						}
						currentState = state.START;
					}
					else{
						chars = chars + ch;
						pos++;
						if(chars.length() >= 2)
						{
							if(ch == '>')
							{
								IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
								IToken currentToken = new Token(IToken.Kind.RARROW, chars, srcLocation, chars.length());
								tokens.add(currentToken);
								start+=chars.length();
								chars="";
								currentState = state.START;
							}
							if(ch == '-') {
								IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start - 1);
								IToken currentToken = new Token(IToken.Kind.MINUS, "-", srcLocation, 1);
								tokens.add(currentToken);
								srcLocation = new IToken.SourceLocation(line, start);
								currentToken = new Token(IToken.Kind.MINUS, "-", srcLocation, 1);
								tokens.add(currentToken);
								start+=chars.length();
								chars="";
								currentState = state.START;
							}
						}
					}
				}
				case IN_STRING-> {
					if (ch == '\\' && !is_escaping){
						is_escaping = true;
						pos++;
					} else if (ch == '"' && isBad){
						IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
						IToken currentToken = new Token(IToken.Kind.ERROR, chars, srcLocation, chars.length());
						tokens.add(currentToken);
						start++;
						pos++;
						chars="";
					}else if (ch == '"' && !is_escaping && chars.length() > 0) {
						chars+='"';
						IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
						IToken currentToken = new Token(IToken.Kind.STRING_LIT, chars, srcLocation, chars.length());
						tokens.add(currentToken);
						start+=chars.length();
						pos++;
						chars="";
						currentState = state.START;
					} else if (pos == input.length() - 1) {
						IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
						IToken currentToken = new Token(IToken.Kind.ERROR, chars, srcLocation, chars.length());
						tokens.add(currentToken);
						start++;
						pos++;
						chars="";
					}
					else {
						if (is_escaping)
						{
							if (ch != 'b' && ch != 't' && ch != 'n' && ch != 'f' && ch != 'r' && ch != '"' && ch != '\'' && ch != ' ' && ch != '\\') {
								chars += '\\';
								chars += ch;
								pos++;
								isBad = true;
								is_escaping = false;
							} else {
								chars += '\\';
								chars += ch;
								pos++;
								is_escaping = false;
							}
						}
						else {
							pos++;
							chars += ch;
						}
					}
				}
				case HAVE_GREATER -> {

					if (ch != '>' && ch != '=') {
						if (chars.length() == 1) {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.GT, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start++;
							chars = "";
						} else if (chars.length() == 2) {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.RANGLE, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start += 2;
							chars = "";
						}
						currentState = state.START;
					}
					else{
						chars = chars + ch;
						pos++;
						if(chars.length() >= 2)
						{
							if(ch == '>')
							{
								IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
								IToken currentToken = new Token(IToken.Kind.RANGLE, chars, srcLocation, chars.length());
								tokens.add(currentToken);
								start+=chars.length();
								chars="";
								currentState = state.START;
							}
							if(ch == '=')
							{
								IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
								IToken currentToken = new Token(IToken.Kind.GE, chars, srcLocation, chars.length());
								tokens.add(currentToken);
								start+=chars.length();
								chars="";
								currentState = state.START;
							}

						}
					}
				}
				case HAVE_LESS -> {

					if (ch != '<' && ch != '=' && ch != '-') { //START HERE
						if (chars.length() == 1) {
							IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
							IToken currentToken = new Token(IToken.Kind.LT, chars, srcLocation, chars.length());
							tokens.add(currentToken);
							start++;
							chars = "";
						} else if (chars.length() == 2) {
							if (ch == '=') {
								IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
								IToken currentToken = new Token(IToken.Kind.LE, chars, srcLocation, chars.length());
								tokens.add(currentToken);
								start += 2;
								chars = "";
							} else if (ch == '-') {
								IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
								IToken currentToken = new Token(IToken.Kind.LARROW, chars, srcLocation, chars.length());
								tokens.add(currentToken);
								start += 2;
								chars = "";
							}
						}
						currentState = state.START;
					}
					else{
						chars = chars + ch;
						pos++;
						if(chars.length() >= 2)
						{
							if(ch == '<')
							{
								IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
								IToken currentToken = new Token(IToken.Kind.LANGLE, chars, srcLocation, chars.length());
								tokens.add(currentToken);
								start+=chars.length();
								chars="";
								currentState = state.START;
							}
							if(ch == '=')
							{
								IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
								IToken currentToken = new Token(IToken.Kind.LE, chars, srcLocation, chars.length());
								tokens.add(currentToken);
								start+=chars.length();
								chars="";
								currentState = state.START;
							}
							if(ch == '-')
							{
								IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
								IToken currentToken = new Token(IToken.Kind.LARROW, chars, srcLocation, chars.length());
								tokens.add(currentToken);
								start+=chars.length();
								chars="";
								currentState = state.START;
							}
						}
					}
				}
				default -> {
					IToken.SourceLocation srcLocation = new IToken.SourceLocation(line, start);
					IToken currentToken = new Token(IToken.Kind.ERROR, chars, srcLocation, chars.length());
					tokens.add(currentToken);
					start++;
					pos++;
					chars="";
					currentState = state.START;
				}
			} //Switch statement end
		} //While loop ends

		// When a token is recognized,  create a Token and reset the state to START.
		// When in start state, save position of first char of token; this is token pos.
		tokens.add(new Token(IToken.Kind.EOF, "", null, chars.length()));
		return tokens;
	}

	private boolean isAtEnd() {
		return pos >= input.length();
	}
}