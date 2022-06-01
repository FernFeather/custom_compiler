package edu.ufl.cise.plc;

public class Token implements IToken {
    final Kind kind;
    final SourceLocation srcLocation;
    final String input;
    final int length;

    Token(Kind kind, String input, SourceLocation srcLocation, int length) {
        this.kind = kind;
        this.input = input;
        this.srcLocation = srcLocation;
        this.length = length;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getText() {
        return input;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return srcLocation;
    }

    @Override
    public int getIntValue() {
        return Integer.parseInt(input);
    }

    @Override
    public float getFloatValue() {
        return Float.parseFloat(input);
    }

    @Override
    public boolean getBooleanValue() {
        return Boolean.parseBoolean(input);
    }

    @Override
    public String getStringValue() {
        boolean is_escaping = false;
        String val ="";
        for(int i = 0; i < input.length(); i++)
        {
            if (input.charAt(i) == '\\') //"test"
            {
                if(input.charAt(i+1) == 'b') {
                    val += '\b';
                    i++;
                }
                else if(input.charAt(i+1) == 't') {
                    val += '\t';
                    i++;
                }
                else if(input.charAt(i+1) == 'n') {
                    val += '\n';
                    i++;
                }
                else if(input.charAt(i+1) == 'f') {
                    val += '\f';
                    i++;
                }
                else if(input.charAt(i+1) == 'r') {
                    val += '\r';
                    i++;
                }
                else if(input.charAt(i+1) == '"') {
                    val += '"';
                    i++;
                }
                else if(input.charAt(i+1) == '\'') {
                    val += '\'';
                    i++;
                }
                else if(input.charAt(i+1) == ' ') {
                    val += '\\';
                    val += ' ';
                    i++;
                }
                else{
                    val+=input.charAt(i);
                    i++;
                }

            }
            else
            val+=input.charAt(i);
        }

        return val.substring(1, val.length() - 1);

    }
}
