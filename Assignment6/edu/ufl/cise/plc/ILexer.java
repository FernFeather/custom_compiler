package edu.ufl.cise.plc;

public interface ILexer {
    IToken next() throws LexicalException;   //return next token and advance internal position
    IToken peek() throws LexicalException;  //return next token but do not advance internal position
}