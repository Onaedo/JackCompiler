import java.util.ArrayList;
import java.util.List;

// represents a symbol in the symbol table
public class Symbol {

    public enum SymbolKind {STATIC, FIELD, ARGUMENT, VARIABLE, SUBROUTINE, CLASS}
    private String name;
    // data type of symbol or return type for subroutine
    private String type;
    private SymbolKind kind;
    private int index;
    // boolean to represent whether the symbol has been initialised
    private boolean initialised = false;
    // list for parameter types of a subroutine
    private List<String> paramTypes = null;

    // set the name of the symbol
    public void setName(String name) {
        this.name = name;
    }

    // set the symbol type
    public void setType(String type) {
        this.type = type;
    }

    // sets the symbol kind
    public void setKind(String kind) {
        if (kind.compareTo("static") == 0) {
            this.kind = SymbolKind.STATIC;
        } else if (kind.compareTo("field") == 0) {
            this.kind = SymbolKind.FIELD;
        } else if (kind.compareTo("arg") == 0) {
            this.kind = SymbolKind.ARGUMENT;
        } else if (kind.compareTo("var") == 0) {
            this.kind = SymbolKind.VARIABLE;
        } else if (kind.compareTo("subr") == 0) {
            this.kind = SymbolKind.SUBROUTINE;
        } else if (kind.compareTo("class") == 0) {
            this.kind = SymbolKind.CLASS;
        }

    }
    // sets the parameter types list of the symbol
    public void setParamTypes(List<String> paramTypes) {
        this.paramTypes = paramTypes;
    }

    // sets the index of the symbol
    public void setIndex (int index) {
        this.index = index;
    }

    // sets the value 'initialised'
    public void setInitialised(boolean initialised) {
        this.initialised = initialised;
    }

    // returns name of symbol
    public String getName() {
        return name;
    }

    // returns type of symbol
    public String getType() {
        return type;
    }

    // returns kind of symbol
    public String getKind() {
        return kind.name();
    }

    // returns index
    public int getIndex() {
        return index;
    }

    // returns param types
    public List<String> getParamTypes() {
        return paramTypes;
    }

    // returns the value 'initialised' of symbol
    public boolean isInitialised() {
        return initialised;
    }
}
