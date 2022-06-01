package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.Declaration;

import java.util.HashMap;

public class SymbolTable {

//TODO:  Implement a symbol table class that is appropriate for this language. 
HashMap<String, Declaration> entries = new HashMap<>();
    //returns true if name successfully inserted in symbol table, false if already present
    public boolean insert(String name, Declaration declaration) {
        return (entries.putIfAbsent(name,declaration) == null);
    }

    public boolean removeEntry(String name, Declaration declaration) {
        return (entries.remove(name,declaration));
    }

    //returns Declaration if present, or null if name not declared.
    public Declaration lookup(String name) {
        Declaration nameReturn = entries.get(name);
        return nameReturn;
    }
}
