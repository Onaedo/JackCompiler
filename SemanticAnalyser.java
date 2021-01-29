import java.util.ArrayList;
import java.util.List;
import java.io.File;

// carries out semantic analysis as tokens are being parsed
public class SemanticAnalyser {

    // List of symbol tables for current class
    private List <SymbolTable> symbolTableList;
    // Program symbol table for classes
    private SymbolTable programSymbolTable = new SymbolTable();

    // List of class symbol tables for entire program
    private List<SymbolTable> classSymbolTables = new ArrayList <SymbolTable>();

    // List of all built in jack classes
    List<String> nativeClasses = List.of("Array", "Keyboard", "Math", "Memory", "Output", "Screen", "String", "Sys");

    // current class in analysis, return type of current subroutine & current type of the lhs of an expression
    private String currentClass = null, currentReturnType = null, lhsType = "-1";
    // Symbol for current subroutine
    private Symbol currentSubroutine = null;

    // true if array index is currently being parsed, false if not
    private boolean isArrayIndex = false;
    // true if expression for return is currently being processed, false if not
    private boolean isReturn = false;
    // true if assignment statement is taking place, false if not
    private boolean isAssignment = false;
    // true if a value has been returned for all paths of code so far
    private boolean isValueReturned = false;
    // true if currently in a statement where return might not be reached
    private boolean unreachableReturn = false;
    // true if currently in an assignment statement for a boolean variable
    private boolean isBooleanStmt = false;
    // true if currently in expression list statement, false if not
    private boolean isExpressionList = false;

    /* Below are are lists filled with various identifiers that need to be checked once all class files have been parsed
    *  identifiers that belong to other classes in the form of a path preceded by a type, or several that must be
    *  matched e.g. {class, subroutine, type}
    *
    *  'origin class' below refers to the class in which the type to be checked was added
    *
    *  variables with declarations to be checked and types to be checked against
    *  {class, name, type, origin class, origin subroutine, type, origin class, origin subroutine...}
    */
    private List <List<String>> varCheckList = new ArrayList<List<String>>();
    // subroutines with declarations to be checked, and return types to be checked against
    // {class, name, type, origin class, origin subroutine}
    private List <List<String>> subrCheckList = new ArrayList<List<String>>();
    // types to be checked
    private List <String> typeCheckList = new ArrayList <String>();
    // subroutine call expression lists to be checked {class, name, origin class, origin subroutine, param1, param2,...}
    private List <List<String>> paramCheckList = new ArrayList <List<String>>();
    // variables called from other classes whose values need to be pushed {class, name, file object, line number}
    private List <List<Object>> objectVarRefList = new ArrayList<List<Object>>();

    //                              METHODS

    // creates a new semantic analyser
    public SemanticAnalyser(){
        symbolTableList = new ArrayList<SymbolTable>();
        newSymbolTable();
        classSymbolTables.add(symbolTableList.get(0));
    }

    // creates new symbol table list and adds it to class symbol table lists
    public void newClassSymbolTableList(){
        symbolTableList = new ArrayList<SymbolTable>();
        newSymbolTable();
        classSymbolTables.add(symbolTableList.get(0));
    }

    public void printTypeErrorMsg(String id, String expectedType, String actualType) {

        if (isReturn && isExpressionList){
            expectedType = lhsType;
        }

        if (isArrayIndex) {
            System.out.println("error: incompatible types: Array index must be type 'int'\n\t'" + id
                    + "' is type '" + actualType + "'");
            System.exit(0);
        }else if (isReturn && !isExpressionList) {
            System.out.println("warning: incompatible types: Expected return type '" + expectedType + "'. '"
                    + id +"' is type '" + actualType + "'");
        } else if (isAssignment) {
            System.out.println("warning: incompatible types: '" + id + "' of type '" + actualType
                    + "' cannot be assigned to '"
                    + expectedType + "'");
        } else {
            System.out.println("warning: incompatible types: '" + id + "' is not of type '" + expectedType + "'");
        }

        System.out.println("  location: subroutine " + currentSubroutine.getName() +"\n  class " + currentClass);
    }

    //                              CREATORS

    // creates a new symbol table and inserts it at the front of the list
    public void newSymbolTable() {
        SymbolTable symbolTable = new SymbolTable();
        symbolTableList.add(0, symbolTable);
    }

    // creates a new Symbol and inserts it at the top of the current symbol table
    // symbol name = {kind, type, name}
    public void newSymbol(String[] symbolName){
        Symbol symbol = new Symbol();

        String s = symbolName[0];
        // set the kind of the variable
        if (s.compareTo("static") == 0){
            symbol.setKind("static");
        } else if (s.compareTo("field") == 0){
            symbol.setKind("field");
        } else if (s.compareTo("arg") == 0){
            symbol.setKind("arg");
        } else if (s.compareTo("var") == 0){
            symbol.setKind("var");
        } else if (s.compareTo("subr") == 0){
            symbol.setKind("subr");
            currentSubroutine = symbol; // set the current subroutine symbol
        } else if (s.compareTo("class") == 0) {
            symbol.setKind("class");
        }

        // set the index of the symbol
        symbol.setIndex(symbolTableList.get(0).getCount(s));
        //increment the count on the symbol table
        symbolTableList.get(0).incrementCount(s);

        // set the symbol type
        symbol.setType(symbolName[1]);

        // set the symbol name
        symbol.setName(symbolName[2]);

        // make sure symbol of same name and type does not already exist within the scope
        if(idExistsInScope(symbolName[2]) && getIdType(symbolName[2]).compareTo(symbolName[1]) == 0  ){
            System.out.println("error: Already exists. '" + symbolName[2] +"' already declared within scope");
            System.exit(0);
        }

        // add the symbol to the current symbol table
        symbolTableList.get(0).insert(symbol);
    }


    //                              MODIFIERS

    // marks symbol of matching identifier as initialised
    public void markVarInitialised(String symbolName) {
        for(SymbolTable st:symbolTableList) {
            if(st.lookUp(symbolName)) {
                st.setSymInitialised(symbolName);
                break;
            }
        }
    }

    // removes symbol table at the front of the list
    public void removeSymbolTable(){
        symbolTableList.remove(0);
    }

    // adds paramater type list to subroutine symbol
    public void addParamTypesList(List<String> list) {
        currentSubroutine.setParamTypes(list);
    }

    // adds variable and its class to check list
    public void addToVarCheckList(String className, String id){
        boolean isContained  = false;
        for (List<String> s: varCheckList) {
            if (s.get(0).compareTo(className) == 0 && s.get(1).compareTo(id) == 0 ) {
                isContained = true;
            }
        }
        if (isContained == false) {
            List<String> details = new ArrayList<String>();
            details.add(className);
            details.add(id);
            varCheckList.add(details);
        }
    }

    // adds subroutine and its class to check list
    public void addToSubrCheckList(String className, String id){
        boolean isContained  = false;
        for (List<String> s: subrCheckList) {
            if (s.get(0).compareTo(className) == 0 && s.get(1).compareTo(id) == 0) {
                isContained = true;
            }
        }
        if (isContained == false) {
            List<String> details = new ArrayList<String>();
            details.add(className);
            details.add(id);
            subrCheckList.add(details);
        }
    }

    // adds type to list of types to be checked
    public void addToTypeCheckList(String type) {
        if (!typeCheckList.contains(type)) {
            typeCheckList.add(type);
        }
    }

    // adds expression list to list be compared with a subroutine param list later
    public void addToParamCheckList(List<String> exprList){
        paramCheckList.add(exprList);
    }

    public void addToObjectVarRefList (List<Object> list){
        objectVarRefList.add(list);
    }


    //                              CHECKERS


    // checks if identifier already exists in current scope. True if so, false if not. 
    // Used to avoid identifiers with same name when creating new symbols
    public boolean idExistsInScope(String id) {
        for(SymbolTable st:symbolTableList) {
            if(st.lookUp(id)) {
                return true;
            }
        }
        return false;
    }

    /* checks if matching symbol of identifier matches certain type. Uses lhsType by default unless a different type is
    *  provided. If the identifier is not within scope, type that needs to be matched will be added to its details in
    * varCheckList or subrCheckList. Also covers type checking for array indices
    */

    public void checkReturnVoid(String id){
        // in the case that no return type is void, nothing should be returned, hence calling check ty
        if (isReturn && currentReturnType.compareTo("void") == 0){
            System.out.println("error: unexpected return value \n\t" + id);
            System.out.println("  location: subroutine " + currentSubroutine.getName() +"\n  class " + currentClass);
            System.exit(0);
        }
    }

    // checks type for identifiers
    public void checkType(String id, String className, boolean isVar) {
        // type only needs to be checked if standards are set up
        if (lhsType.compareTo("-1") != 0 || isReturn && !isExpressionList || isArrayIndex) {
            // get the type the identifier will be checked against
            String type = getMatchType();

            // if a symbol for this identifier exists but is not of matching type, print error message
            if (idExistsInScope(id)) {
                if (type.compareTo(getIdType(id)) != 0 && !((type.compareTo("char") == 0
                        && getIdType(id).compareTo("int") == 0) || (type.compareTo("int") == 0
                        && getIdType(id).compareTo("char") == 0))) {
                    printTypeErrorMsg(id, type, getIdType(id));
                }
            } else if (isVar) { // later, this variable will be checked to see if it's type is suitable
                for (List<String> s : varCheckList) {
                    if (s.get(1).compareTo(id) == 0 && s.get(0).compareTo(className) == 0) {
                        s.add(type);
                        s.add(currentClass);
                        s.add(currentSubroutine.getName());
                    }
                }
            } else {
                for (List<String> s : subrCheckList) {
                    if (s.get(1).compareTo(id) == 0 && s.get(0).compareTo(className) == 0) {
                        s.add(type);
                        s.add(currentClass);
                        s.add(currentSubroutine.getName());
                    }

                }
            }
        }
    }

    // checks type for operand set types
    public void checkOperandTypes(String type, String id) {

        // get the type that the operand must match
        String matchType = getMatchType();
        // type only needs to be checked if standards are set up
        if (lhsType.compareTo("-1") != 0 || isReturn && !isExpressionList || isArrayIndex) {
            // if current match type is not set, or not equal to current type, print error message
            if (matchType.compareTo(type) != 0 && matchType.compareTo("-1") != 0
                    && !((matchType.compareTo("char") == 0
                    && type.compareTo("int") == 0) || (matchType.compareTo("int") == 0
                    && type.compareTo("char") == 0))) {

                if (matchType.compareTo("char") == 0 && type.compareTo("int") == 0){
                    System.out.println("true1");}

                if (matchType.compareTo("int") == 0 && type.compareTo("char") == 0){
                    System.out.println("true2");}

                if (!((matchType.compareTo("char") == 0
                        && type.compareTo("int") == 0) || (matchType.compareTo("int") == 0
                        && type.compareTo("char") == 0)))
                {System.out.println("true3");}


                if (!isReturn) {
                    System.out.println("warning: incompatible types: " + type + " cannot be converted to " + matchType);
                    System.out.println("  symbol: '" + id + "'");
                } else { // if in return statement, print different error message
                    printTypeErrorMsg(id, currentReturnType, type);
                }
                System.out.println("  location: subroutine " + currentSubroutine.getName() +"\n  class "
                        + currentClass);
            }
        }
        // set lhs if not already set
        if (lhsType.compareTo("-1") == 0) {
            setLhsType(type);
        }
    }

    // checks if a variable has been declared within scope and prints error message if not and error is set to true
    // also prints error if array index is being evaluated and variable is not of type integer
    public void checkVarDeclared(String className, String id, boolean error) {
        // if the variable does not exist but is not from another class
        if(!idExistsInScope(id) && (className == currentClass || className == null)){
            if (error) {
                System.out.println("error: '" + id + "' has not been declared in class '"+ currentClass + "'");
                System.exit(0);
            }
        } else if (className != null && className != currentClass) { // if the variable is from another class
            addToVarCheckList(className, id);
        }
    }

    // checks if a subroutine has been declared within scope and adds to list to check later.
    public void checkSubrDeclared(String className, String id) {
        if(!idExistsInScope(id) || className != null){
            addToSubrCheckList(className, id);
        }
    }

    // checks if a local variable has been initialised, issues error message if not
    public void checkInitialised(String id){
        if (!varIsInitialised(id) && symbolTableList.get(0).lookUp(id)) {
            System.out.println("warning: variable '"+id+"' should be initialised before use");
            System.out.println("  location: subroutine " + currentSubroutine.getName() +"\n  class " + currentClass);
        }
    }

    // checks if value has been returned for current subroutine. If so, issues error for unreachable code
    public void checkIfValueReturned(){
        if(isValueReturned){
            System.out.println("warning: unreachable code: value has already been returned for current subroutine "
                    + currentSubroutine.getName());
            System.out.println("  location: subroutine " + currentSubroutine.getName() +"\n  class " + currentClass);
        }
    }

    // checks whether a given variable has been initialised within entire scope
    public boolean varIsInitialised(String symbolName) {
        boolean init = false;
        for(SymbolTable st:symbolTableList) {
            if(st.lookUp(symbolName)) {
                return st.symIsInitialised(symbolName);
            }
        }
        return false;
    }



    //                      post parsing checkers

    // checks to see if a class exists for any specified types in program
    public void solveTypeCheckList() {
        for (String type : typeCheckList){
            if(!programSymbolTable.lookUp(type)){
                System.out.println("error: unknown symbol: type '" + type + "' not found");
                System.exit(0);
            }
        }
    }

    // checks to see if variables in varCheckList have been declared
    public void solveVarCheckList(){
        for(List<String> v: varCheckList) {
            int tableIndex = 0;
            // check if the class it is meant to be in exists
            if(!programSymbolTable.lookUp(v.get(0))){
                System.out.println("error: unknown symbol: class '" + v.get(0) + "' not found");
                System.out.println("  symbol: '"+ v.get(1) + "'");
                System.out.println("  location: subroutine" + v.get(4) + "\nclass " + v.get(3));
                System.exit(0);
            } else {
                tableIndex = programSymbolTable.getSymbol(v.get(0)).getIndex();
            }
            // check if symbol of same name exists
            if(!classSymbolTables.get(tableIndex).lookUp(v.get(1))){
                System.out.println("error: unknown symbol: symbol '" + v.get(1) + "' not found in class " + v.get(0));
                System.out.println("  location: subroutine" + v.get(4) + "\nclass " + v.get(3));
                System.exit(0);
            }

            Symbol s = classSymbolTables.get(tableIndex).getSymbol(v.get(1));

            // check if symbol of same kind exists
            if (s.getKind().compareTo("STATIC") != 0 && s.getKind().compareTo("FIELD") != 0) {
                System.out.println("error: unknown symbol: class variable member '" + v.get(1) + "' not found in class "
                        + v.get(0));
                System.out.println("  location: subroutine" + v.get(4) + "\nclass " + v.get(3));

                System.exit(0);
            }
            // see if the types match the type of the variable
            String sType = s.getType();
            for (int i=2; i < v.size()-2; i+=3) {
                if (sType.compareTo(v.get(i)) != 0 ) {
                    System.out.println("warning: incompatible types: "+v.get(i)+ " cannot be converted to " + sType);
                    System.out.println("  symbol: '"+ v.get(1) + "'");
                    System.out.println("  location: subroutine" + v.get(i+2) + "\nclass " + v.get(i+1));
                }
            }
        }
    }

    // checks to see if subroutines in subrCheckList have been declared
    public void solveSubrCheckList(){
        for(List<String> v: subrCheckList) {
            int tableIndex = 0;
            // check if the class it is meant to be in exists
            if(!programSymbolTable.lookUp(v.get(0))){
                System.out.println("error: unknown symbol: class '" + v.get(0) + "' not found");
                System.out.println("  location: subroutine " + v.get(4) + "\n  class " + v.get(3));
                System.exit(0);
            } else {
                tableIndex = programSymbolTable.getSymbol(v.get(0)).getIndex();
            }
            // check if symbol of same name exists
            if(!classSymbolTables.get(tableIndex).lookUp(v.get(1))){
                System.out.println("error: unknown symbol: symbol '" + v.get(1) + "' not found in class " + v.get(0));
                System.out.println("  location: subroutine " + v.get(4) + "\n  class " + v.get(3));
                System.exit(0);
            }

            Symbol s = classSymbolTables.get(tableIndex).getSymbol(v.get(1));

            // check if symbol of same kind exists
            if (s.getKind().compareTo("SUBROUTINE") != 0) {
                System.out.println("error: unknown symbol: subroutine '" + v.get(1) + "' not found in class "
                        + v.get(0));
                System.out.println("  location: subroutine " + v.get(4) + "\n  class " + v.get(3));
                System.exit(0);
            }
            // see if the types match the return type of the subroutine
            String sType = s.getType();

            for (int i=2; i < v.size()-2; i+=3) {
                if (sType.compareTo(v.get(i)) != 0 ) {
                    System.out.println("warning: incompatible types: subroutine '" + v.get(1) + "' in class '" +v.get(0)
                            +"' has return type " + sType + " which can't be converted to " + v.get(i));
                    System.out.println("  symbol: '"+ v.get(1) + "'");
                    System.out.println("  location: subroutine " + v.get(i+2) + "\n  class " + v.get(i+1));
                    printExprList(v,0);
                }
            }
        }
    }

    // makes sure expressions entered in subroutine call match with its parameters
    public void solveParamCheckList(){

        for(List<String> pl: paramCheckList) {

            // find the symbol of the matching subroutine. Existence of the subroutine has already been checked
            // beforehand
            int tableIndex = 0;
            Symbol s = classSymbolTables.get(programSymbolTable.getSymbol(pl.get(0)).getIndex()).getSymbol(pl.get(1));
            List <String> list;
            if(s == null){
                System.out.println("this");
                list = null;
            } else {
                list = s.getParamTypes();
            }

            int listLength;
            if(list != null) {
                listLength = list.size();
            } else {
                listLength = 0;
            }
            // see if expression list matches that of the subroutine


            // if no arguments were required, but 1 or more was given
            if (listLength == 0 && pl.size() > 4) {
                System.out.println("error: too many arguments : subroutine '" + pl.get(1) + "' in class '" + pl.get(0)
                        + "' cannot be used");
                System.out.print("\n  required: no arguments");
                System.out.print("\n  found: ");
                printExprList(pl,4);
                System.out.println("\n  location: "+pl.get(3)+"\n  class "+ pl.get(2));
                System.exit(0);
            } else if (pl.size()-4 < listLength){ // if subroutine call expression list is too small

                System.out.println("error: too few arguments : subroutine '" + pl.get(1) + "' in class '" + pl.get(0)
                        + "' cannot be used");
                System.out.print("\n  required: ");
                printExprList(list,0);
                System.out.print("\n  found: ");
                if (pl.size() > 4){
                    printExprList(pl,4);
                } else {
                    System.out.print("none");
                }
                System.out.println("\n  location: "+pl.get(3)+"\n  class "+ pl.get(2));
                System.exit(0);

            } else if(pl.size()-4 > listLength){ // if subroutine call expression list is too large
                // if a type doesn't match

                System.out.println("error: too many arguments : subroutine '" + pl.get(1) + "' in class '" + pl.get(0)
                        + "' cannot be used");
                System.out.print("\n  required: ");
                printExprList(list,0);
                System.out.print("\n  found: ");
                printExprList(pl,4);
                System.out.println("\n  location: "+pl.get(3)+"\n  class "+ pl.get(2));
                System.exit(0);

            } else if (listLength == pl.size()-4) {
                for (int i = 0; i < listLength; i++) {
                    // if a type doesn't match
                    if (list.get(i).compareTo(pl.get(i + 4)) != 0 && pl.get(i+4).compareTo("Array") != 0
                            && list.get(i).compareTo("Array") != 0 && pl.get(i+4).compareTo("null") != 0
                            && !((list.get(i).compareTo("int") == 0 && pl.get(i+4).compareTo("char") == 0)
                            || (list.get(i).compareTo("char") == 0 && pl.get(i+4).compareTo("int") == 0))
                            && pl.get(i+4).compareTo("-2") != 0
                    ) {
                        System.out.println("error: argument type error : subroutine '" + pl.get(1) + "' in class '"
                                + pl.get(0) + "' cannot be used");
                        System.out.print("\n  required: ");
                        printExprList(list,0);
                        System.out.print("\n  found: ");
                        printExprList(pl,4);
                        System.out.println("\n  location: "+pl.get(3)+"\n  class "+ pl.get(2));
                        System.exit(0);
                    }
                }
            }
        }
    }

    // prints expression lists for solveParamCheckList
    private void  printExprList(List<String> list, int n){
        for (int j = n; j < list.size(); j++) {
            if (j > n){ System.out.print(", ");}
            System.out.print( list.get(j));
        }
    }

    // resolves object variable calls and adds them to file
    public void resolveObjectVarRefList(){

        for(List<Object> v: objectVarRefList){
            // find its symbol
            Symbol s = classSymbolTables.get(programSymbolTable.getSymbol((String)v.get(0)).getIndex()).getSymbol((String)v.get(1));
            int index = s.getIndex();
            String segment = s.getKind();
            // create a string for its file entry
            String fileIn = "push " + segment.toLowerCase() + " " + index;
            CodeGenerator.insertLine(fileIn, (File)v.get(2), (int)v.get(3)+1);
        }
    }

    //                              GETTERS & SETTERS

    public void setCurrentReturnType(String currentReturnType) {
        this.currentReturnType = currentReturnType;
    }

    public void setCurrentClass(String currentClass) {
        this.currentClass = currentClass;

        // add class to program's class symbol table
        Symbol symbol = new Symbol();
        // set the kind
        symbol.setKind("class");
        // set the index of the symbol
        symbol.setIndex(programSymbolTable.getCount("class"));
        //increment the count on the symbol table
        programSymbolTable.incrementCount("class");
        // set the symbol type
        symbol.setType(currentClass);
        // set the symbol name
        symbol.setName(currentClass);

        // make sure class of same name does not already exist within the program
        if (programSymbolTable.lookUp(currentClass)) {
            System.out.println("error: Class '" + currentClass +"' already exists");
            System.exit(0);
        }


        // add the symbol to the current symbol table
        programSymbolTable.insert(symbol);
    }

    public void setCurrentSubroutine(Symbol currentSubroutine) {
        this.currentSubroutine = currentSubroutine;
    }

    public void setLhsType(String lhsType) {
        this.lhsType = lhsType;
    }

    public void setIsArrayIndex(boolean isArrayIndex) {
        this.isArrayIndex = isArrayIndex;
    }

    public void setIsReturn(boolean isReturn) {
        this.isReturn = isReturn;
    }

    public void setIsAssignment(boolean isAssignment) {
        this.isAssignment = isAssignment;
    }

    public void setUnreachableReturn(boolean unreachableReturn){
        this.unreachableReturn = unreachableReturn;
    }

    public void setIsValueReturned(boolean isValueReturned){
        if (!unreachableReturn) {
            this.isValueReturned = isValueReturned;
        }
    }

    public void setisBooleanStmt(boolean isBooleanStmt) {
        this.isBooleanStmt = isBooleanStmt;
        lhsType = "-1";
    }

    public void setIsExpressionList(boolean isExpressionList) {
        this.isExpressionList = isExpressionList;
    }

    public String getCurrentClass() {
        return currentClass;
    }

    public String getCurrentSubroutine(){
        return currentSubroutine.getName();
    }

    public String getCurrentReturnType() {
        return currentReturnType;
    }

    // returns the type of an identifier if symbol with matching name exists within a class scope
    public String getIdType(String id){
        String s = null;
       if(idExistsInScope(id)) {
           for(SymbolTable st:symbolTableList) {
               if (st.lookUp(id)) {
                   s = st.getSymbol(id).getType();
               }
           }
       }
       return s;
    }

    // returns the type of an identifier if symbol with matching name exists within a class scope
    public String getIdKind(String id){
        String s = null;
        if(idExistsInScope(id)) {
            for(SymbolTable st:symbolTableList) {
                if (st.lookUp(id)) {
                    s = st.getSymbol(id).getKind();
                }
            }
        }
        return s;
    }

    // returns the type of an identifier if symbol with matching name exists within a class scope
    public int getIdIndex(String id){
        int s = 0;
        if(idExistsInScope(id)) {
            for(SymbolTable st:symbolTableList) {
                if (st.lookUp(id)) {
                    s = st.getSymbol(id).getIndex();
                }
            }
        }
        return s;
    }

    // returns total field variable in current class
    public int getClassFieldCount() {
        return symbolTableList.get(symbolTableList.size()-1).getCount("field");
    }

    // returns var count for current subroutine
    public int getSubrVarCount(){
        return symbolTableList.get(0).getCount("var");
    }

    public String getLhsType() {
        return lhsType;
    }

    // returns the current type to be matched
    public String getMatchType(){
        // array indices take precedence over a return
        if (isArrayIndex) {
            return  "int";
        } else if (isReturn && !isExpressionList) { // identifier type must match current function return type
            return currentReturnType;
        } else {
            return lhsType;
        }
    }

    public boolean getIsBooleanStmt() {
        return isBooleanStmt;
    }

    public boolean getIsReturn(){
        return isReturn;
    }

    public boolean getIsValueReturned(){
        return isValueReturned;
    }

    public boolean getUnreachableReturned() {
        return unreachableReturn;
    }

    public boolean getIsExpressionList() {
        return isExpressionList;
    }

    public boolean isArrayIndex() {
        return isArrayIndex;
    }
}