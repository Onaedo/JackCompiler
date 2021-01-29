import java.util.ArrayList;
import java.util.List;

// Symbol Table stores Symbols for a single scope
public class SymbolTable {
    private List<Symbol> symbolList;

    // counts of symbols of certain type. Hold current number -1
    private int staticCount, fieldCount, argCount, varCount, subrCount, classCount;

    public SymbolTable(){
        symbolList = new ArrayList<Symbol>();
        staticCount = 0;
        fieldCount = 0;
        argCount = 0;
        varCount = 0;
        subrCount = 0;
        classCount = 0;
    }

    // appends symbol onto bottom of table
    public void insert(Symbol symbol) {
        symbolList.add(symbol);
    }

    // returns true if symbol is in table, false if not
    public boolean lookUp(String symbolString) {
        for (Symbol s:symbolList) {
            if (symbolString.compareTo(s.getName()) == 0) {
                return true;
            }
        }
        return false;
    }

    // sets the value 'initialised' to true for given symbol
    public void setSymInitialised(String symbolString) {
        for (Symbol s:symbolList) {
            if (symbolString.compareTo(s.getName()) == 0) {
                s.setInitialised(true);
            }
        }
    }

    // increments one of the counts for symbol types depending on character given
    public void incrementCount (String s) {
        if(s.compareTo("static") == 0){
            ++staticCount;
        } else if (s.compareTo("field") == 0){
            ++fieldCount;
        } else if (s.compareTo("arg") == 0){
            ++argCount;
        } else if (s.compareTo("var") == 0){
            ++varCount;
        } else if (s.compareTo("subr") == 0){
            ++subrCount;
        } else if (s.compareTo("class") == 0){
            ++classCount;
        }
    }

    // returns current value of selected counter
    public int getCount(String s) {
        if (s.compareTo("static") == 0){
            return staticCount;
        } else if (s.compareTo("field") == 0){
            return fieldCount;
        } else if (s.compareTo("arg") == 0){
            return argCount;
        } else if (s.compareTo("var") == 0){
            return varCount;
        } else if (s.compareTo("subr") == 0){
           return subrCount;
        } else if (s.compareTo("class") == 0){
            return classCount;
        }

        return 0;
    }

    // returns symbol of given name
    public Symbol getSymbol(String symbolString) {
        for (Symbol s:symbolList) {
            if (symbolString.compareTo(s.getName()) == 0) {
                return s;

            }
        }
        return null;
    }

    public int getIndex(String symbolString) {
        return getSymbol(symbolString).getIndex();
    }

    // returns true if given symbol within scope has been initialised
    public boolean symIsInitialised(String symbolString) {
        for (Symbol s:symbolList) {
            if (symbolString.compareTo(s.getName()) == 0) {
                return s.isInitialised();
            }
        }
        return false;
    }
}