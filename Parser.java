import java.util.List;
import java.util.ArrayList;
import java.io.File;

/* checks grammar of language
 ** terminals are consumed when encountered
 */
public class Parser {

    Lexer lexer;
    CodeGenerator codeGenerator;
    SemanticAnalyser semanticAnalyser;
    Token currentToken = null, peekToken = null;
    // lexemes for peek token and currentToken
    String currentTLexeme = null, peekTLexeme = null, directory = null;
    Token.TokenType currentTType = null, peekTType = null;
    // List of strings encountered at start of expression
    List<String> isExprStrings = List.of("-", "~", "(","true", "false", "null", "this");
    List<String> isStmtStrings = List.of("var", "let", "if", "while", "do", "return");
    List<String> isClassVarDeclarStrings = List.of("static", "field");
    List<String> isSubroutineDeclarStrings = List.of("constructor", "function", "method");
    List<String> isTypeStrings = List.of("int", "char", "boolean");

    /* To improve level of abstraction and number of calls to the semantic analyser, the declaration of function and
    ** class symbols
    ** symbolStrings[0] = kind, [1] = type, [2] = name
    */
    String[] symbolStrings = new String[3];
    // list for parameter types of a subroutine
    List<String> paramTypesList = null;
    // list of arguments from subroutine call to be checked against its paramTypesList later
    // {class, name, origin class, origin subroutine ,param1, param2,...}
    List<List<String>> expListList = new ArrayList<List<String>>();
    List<String> expList = null;

    public Parser(String directory){
        this.lexer = new Lexer();
        this.codeGenerator = new CodeGenerator();
        this.semanticAnalyser = new SemanticAnalyser();
        this.directory = directory;
    }

    public void runLexer(File file){
        // first get a list of the files from
        lexer.extractTokens(file);
    }

    public void runParser(File[] files) {
        for (int i=0; i < files.length ; i++) {

            if (i > 0) {
                semanticAnalyser.newClassSymbolTableList();
            }

            runLexer(files[i]);

            jackClass();
            codeGenerator.reset();
            codeGenerator.closeVMFile();
        }

        // run final stage of analysis now that all classes have been parsed
        semanticAnalyser.solveTypeCheckList();
        semanticAnalyser.solveVarCheckList();
        semanticAnalyser.solveSubrCheckList();
        semanticAnalyser.solveParamCheckList();
        semanticAnalyser.resolveObjectVarRefList();
    }

    private void loadNextToken() {
        currentToken = lexer.GetNextToken();
        currentTLexeme = currentToken.getLexeme();
        currentTType = currentToken.getType();

        peekToken = lexer.PeekNextToken();
        if (peekToken == null) {
            peekToken = new Token("9null");
        }
        peekTLexeme = peekToken.getLexeme();
        peekTType = peekToken.getType();
    }

    private void printErrorMsg(String actual, String expected) {
        System.out.println("error: expected " + expected + " got " + actual);
        System.out.println("  symbol:\t" + currentTLexeme);
        if (semanticAnalyser.getCurrentClass() != null){
            System.out.println("  location: subroutine " + semanticAnalyser.getCurrentSubroutine()
                    + "\n  class " + semanticAnalyser.getCurrentClass());
        }
        System.exit(0);
    }

    // returns true if terminal belonging to an expression is encountered, false if not
    private boolean isExpression (Token.TokenType type, String lexeme) {
        if (isExprStrings.contains(lexeme) == true || type == Token.TokenType.IDENTIFIER ||
            type == Token.TokenType.STRING_LIT || type == Token.TokenType.INTEGER_CONST) {
            return true;
        }
        return false;
    }

    // returns true if terminal belonging to a statement is encountered, false if not
    private boolean isStatement (String lexeme) {
        if (isStmtStrings.contains(lexeme) == true) {
            return true;
        }
        return false;
    }

    // returns true if terminal belonging to a class variable declaration is encountered, false if not
    private boolean isClassVarDeclar(String lexeme) {
        if (isClassVarDeclarStrings.contains(lexeme) == true) {
            return true;
        }
        return false;
    }

    // returns true if terminal belonging to subroutine declaration is encountered, false if not
    private boolean isSubroutineDeclar(String lexeme) {
        if (isSubroutineDeclarStrings.contains(lexeme) == true) {
            return true;
        }
        return false;
    }

    // returns true if terminal belonging to type declaration is encountered, false if not
    private boolean isType(Token.TokenType type, String lexeme) {
        if (isTypeStrings.contains(lexeme) == true || type == Token.TokenType.IDENTIFIER) {
            return true;
        }
        return false;
    }

    // returns true if token type is identifier
    private boolean isIdentifier(Token.TokenType type) {
        if (type == Token.TokenType.IDENTIFIER) {
            return true;
        }
        else {
            printErrorMsg(type.name(), Token.TokenType.IDENTIFIER.name());
            return false;
        }
    }

    // returns true if token lexeme matches string to be compared to
    private boolean lexemeIsEqual(String lexeme, String string) {
        if (lexeme.compareTo(string) == 0) {
            return true;
        } else {
            printErrorMsg(lexeme, string);
            return false;
        }
    }

    // class grammar

    private void jackClass() {
        finishedVarDeclars = false;
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "class");
        loadNextToken();
        isIdentifier(currentTType);
        semanticAnalyser.setCurrentClass(currentTLexeme);
        // create vm file for current class
        codeGenerator.createVMFile(currentTLexeme, directory);
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "{");
        if (isClassVarDeclar(peekTLexeme) || isSubroutineDeclar(peekTLexeme)){
            while (isClassVarDeclar(peekTLexeme) || isSubroutineDeclar(peekTLexeme)) {
                memberDeclar();
            }
        }
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "}");
    }

    private void memberDeclar() {
        if (isClassVarDeclar(peekTLexeme)) {
            if (!finishedVarDeclars) {
                classVarDeclar();
            } else {
                System.out.println("error: all class variables must be declared before subroutines");
                System.out.println("location: class "+semanticAnalyser.getCurrentClass());
                System.exit(0);
            }
            codeGenerator.printToFile();
        } else if (isSubroutineDeclar(peekTLexeme)) {
            finishedVarDeclars = true;
            subroutineDeclar();
            codeGenerator.printToFile();
        } else {
            printErrorMsg(peekTLexeme, "class variable declaration or subroutine declaration");
        }
    }

    boolean finishedVarDeclars = false;
    private void classVarDeclar() {
        loadNextToken();
        if (currentTLexeme.compareTo("static") == 0) {
            symbolStrings[0] = "static";
        } else if (currentTLexeme.compareTo("field") == 0){
            symbolStrings[0] = "field";
        }else{
            printErrorMsg(currentTLexeme, "static or field");
        }
        type();
        symbolStrings[1] = currentTLexeme; // add the token type to the lexeme
        loadNextToken();
        isIdentifier(currentTType);
        symbolStrings[2] = currentTLexeme;
        // create new symbol for class variable
        semanticAnalyser.newSymbol(symbolStrings);
        if (peekTLexeme.compareTo(",") == 0) {
            while (peekTLexeme.compareTo(",") == 0) {
                loadNextToken(); // consume the ','
                loadNextToken();
                isIdentifier(currentTType);
                symbolStrings[2] = currentTLexeme;
                semanticAnalyser.newSymbol(symbolStrings);  // create new symbol for class variable
            }
        }
        loadNextToken();
        lexemeIsEqual(currentTLexeme, ";");
    }

    private void type() {
        loadNextToken();
        if (currentTLexeme.compareTo("int") == 0){
            ;
        } else if (currentTLexeme.compareTo("char") == 0) {
            ;
        } else if (currentTLexeme.compareTo("boolean") == 0) {
            ;
        } else if (currentTType == Token.TokenType.IDENTIFIER) {
            semanticAnalyser.addToTypeCheckList(currentTLexeme); // check if this type exists
        } else {
            printErrorMsg(currentTLexeme, "'int', 'char', 'boolean' or IDENTIFIER");
        }
    }


    private void subroutineDeclar() {
        loadNextToken();
        boolean isMethod = false;
        boolean isConstructor = false;
        if (currentTLexeme.compareTo("constructor") == 0 || currentTLexeme.compareTo("function") == 0 ||
            currentTLexeme.compareTo("method") == 0) {
            if (currentTLexeme.compareTo("constructor") == 0){
                isConstructor = true;
            } else if (currentTLexeme.compareTo("method") == 0){
                isMethod = true;
            }
        } else {
            printErrorMsg(currentTLexeme, "'constructor', 'function' or 'method'");
        }

        if (peekTLexeme.compareTo("void") == 0 ) {
            loadNextToken(); // consume the void'
        } else {
           type();
        }
        semanticAnalyser.setCurrentReturnType(currentTLexeme); // update current return type
        loadNextToken();
        isIdentifier(currentTType);

        String funcName = currentTLexeme;

        // add current subroutine to symbol table
        symbolStrings[0] = "subr";
        symbolStrings[1] = semanticAnalyser.getCurrentReturnType();
        symbolStrings[2] = currentTLexeme;
        semanticAnalyser.newSymbol(symbolStrings);

        loadNextToken();
        semanticAnalyser.newSymbolTable(); // create symbol table for current scope
        if (isMethod) {
            // add object of method to symbol table
            symbolStrings[0] = "arg";
            symbolStrings[1] = semanticAnalyser.getCurrentClass();
            symbolStrings[2] = "this";
            semanticAnalyser.newSymbol(symbolStrings);
        }

        lexemeIsEqual(currentTLexeme, "(");
        paramList();
        loadNextToken();
        lexemeIsEqual(currentTLexeme, ")");

        // allocate memory for new object if constructor has been called
        if (isConstructor){
            codeGenerator.writePush("constant",Integer.toString(semanticAnalyser.getClassFieldCount()));
            codeGenerator.writeFunction("Memory", "alloc", 1);
            codeGenerator.writePop("pointer", "0");
        } else if (isMethod) {
            codeGenerator.writePush("argument", "0");
            codeGenerator.writePop("pointer", "0");
        }

        subroutineBody();
        codeGenerator.writeFuncDeclar(semanticAnalyser.getCurrentClass(), funcName, semanticAnalyser.getSubrVarCount());
        // delete symbol table as it is no longer needed
        semanticAnalyser.removeSymbolTable();
    }

    private void paramList() {
        symbolStrings[0] = "arg"; // the parameters will appear in the table as being of kind 'argument'
        if (isType(peekTType, peekTLexeme)) {
            type();
            symbolStrings[1] = currentTLexeme;
            // create new list object
            paramTypesList = new ArrayList<String>();
            paramTypesList.add(currentTLexeme); // add the parameter type to the list
            loadNextToken();
            isIdentifier(currentTType);
            symbolStrings[2] = currentTLexeme;
            semanticAnalyser.newSymbol(symbolStrings); // add parameter to symbol table
            semanticAnalyser.markVarInitialised(currentTLexeme); // parameters initialised already in calling function
            if (peekTLexeme.compareTo(",") == 0) {
                while (peekTLexeme.compareTo(",") == 0) {
                    loadNextToken(); // consume the ','
                    type();
                    symbolStrings[1] = currentTLexeme;
                    paramTypesList.add(currentTLexeme); // add the parameter type to the list
                    loadNextToken();
                    isIdentifier(currentTType);
                    symbolStrings[2] = currentTLexeme;
                    semanticAnalyser.newSymbol(symbolStrings); // add parameter to symbol table
                    semanticAnalyser.markVarInitialised(currentTLexeme);
                }
            }
            // add the parameter types list to the subroutine symbol
            semanticAnalyser.addParamTypesList(paramTypesList);
            paramTypesList = null;
        }
    }

    private void subroutineBody() {

        semanticAnalyser.setIsValueReturned(false);
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "{");
        if (isStatement(peekTLexeme)) {
            while (isStatement(peekTLexeme)) {
                statement();
            }
        }
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "}");
        // check if a value has been returned
        if(!semanticAnalyser.getIsValueReturned() && semanticAnalyser.getCurrentSubroutine().compareTo("new") != 0
                && semanticAnalyser.getCurrentReturnType().compareTo("void")!=0){
            System.out.println("warning: expecting return value: control may reach the end of non void function");
            System.out.println("  location: subroutine " + semanticAnalyser.getCurrentSubroutine() +"\n  class "
                    + semanticAnalyser.getCurrentClass());
        }
    }

    // statement grammar

    private void statement() {

        if (peekTLexeme.compareTo("var") == 0){
            varDeclarStmt();
        }
        else if (peekTLexeme.compareTo("let") == 0){
            letStmt();
        }
        else if (peekTLexeme.compareTo("if") == 0){
            ifStmt();
        }
        else if (peekTLexeme.compareTo("while") == 0){
            whileStmt();
        }
        else if (peekTLexeme.compareTo("do") == 0){
            doStmt();
        }
        else if (peekTLexeme.compareTo("return") == 0){
            returnStmt();
        }
        else {
            System.out.println("ERROR: Illegal start to statement: " + peekTLexeme);
            System.exit(0);
        }
    }

    private void varDeclarStmt() {
        // check if this code is reachable by checking whether value has been returned
        semanticAnalyser.checkIfValueReturned();
        // set symbol kind to var
        symbolStrings[0] = "var";
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "var");
        type();
        symbolStrings[1] = currentTLexeme;
        loadNextToken();
        isIdentifier(currentTType);
        symbolStrings[2] = currentTLexeme;
        // create new symbol for the variable
        semanticAnalyser.newSymbol(symbolStrings);
        if (peekTLexeme.compareTo(",") == 0) {
            while (peekTLexeme.compareTo(",") == 0) {
                loadNextToken(); // consume the ','
                loadNextToken();
                isIdentifier(currentTType);
                symbolStrings[2] = currentTLexeme;
                semanticAnalyser.newSymbol(symbolStrings); // create new symbol for the variable
            }
        }
        loadNextToken();
        lexemeIsEqual(currentTLexeme, ";");
    }

    private void letStmt() {
        // check if this code is reachable by checking whether value has been returned
        semanticAnalyser.checkIfValueReturned();
        semanticAnalyser.setIsAssignment(true);
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "let");
        loadNextToken();
        isIdentifier(currentTType);
        semanticAnalyser.checkVarDeclared(null, currentTLexeme, true); // check if variable has been declared
        semanticAnalyser.markVarInitialised(currentTLexeme); // set that variable has been initialised
        String id = currentTLexeme; // variable being assigned
        if (semanticAnalyser.getIdType(currentTLexeme).compareTo("boolean") == 0) {
            semanticAnalyser.setisBooleanStmt(true);// if it is boolean then the expression must also be boolean
        }
        String arrayName = null;
        if (peekTLexeme.compareTo("[") == 0){
            arrayName = currentTLexeme;
            loadNextToken(); // consume the '['
            semanticAnalyser.setIsArrayIndex(true);
            expression();
            loadNextToken();
            lexemeIsEqual(currentTLexeme, "]");
            pushIdentifier(arrayName);
            codeGenerator.writeArithmetic("add"); // add the array base address and index
            id = null;
            semanticAnalyser.setIsArrayIndex(false);
        } else { // type can't be array if array index is being accessed
            semanticAnalyser.setLhsType(semanticAnalyser.getIdType(currentTLexeme)); // set the lhs type
            if(semanticAnalyser.getIsBooleanStmt()){
                semanticAnalyser.setLhsType("-1");
            }
        }
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "=");
        expression();
        loadNextToken();
        lexemeIsEqual(currentTLexeme, ";");
        if (id != null) {
            popIdentifier(id);
        } else {
            codeGenerator.writePop("temp", "0"); // tempoarily store result
            codeGenerator.writePop("pointer", "1");
            codeGenerator.writePush("temp", "0");
            codeGenerator.writePop("that", "0");
        }
        // reset lhs type
        semanticAnalyser.setLhsType("-1");
        semanticAnalyser.setIsAssignment(false);
        semanticAnalyser.setisBooleanStmt(false);
    }

    private void ifStmt() {
        int thisLabelNum = codeGenerator.getLabelNum(true);
        // check if this code is reachable by checking whether value has been returned
        semanticAnalyser.checkIfValueReturned();
        semanticAnalyser.setUnreachableReturn(true); // returns might be unreachable

        semanticAnalyser.setLhsType("-1");
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "if");
        loadNextToken();
        codeGenerator.incrementLabelNum(true);
        lexemeIsEqual(currentTLexeme, "(");
        semanticAnalyser.setisBooleanStmt(true);// expression for if statement must be boolean
        expression();
        loadNextToken();
        lexemeIsEqual(currentTLexeme, ")");
        codeGenerator.writeIf("IF_TRUE", thisLabelNum);
        codeGenerator.writeGoto("IF_FALSE",thisLabelNum);
        codeGenerator.writeLabel("IF_TRUE", thisLabelNum);
        semanticAnalyser.setisBooleanStmt(false);
        semanticAnalyser.setLhsType("-1");
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "{");
        if (isStatement(peekTLexeme)) {
            while(isStatement(peekTLexeme)) {
                statement();
            }
        }
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "}");

        semanticAnalyser.setUnreachableReturn(false); //returns are no longer unreachable

        if (peekTLexeme.compareTo("else") == 0) {
            codeGenerator.writeGoto("IF_END", thisLabelNum);
            codeGenerator.writeLabel("IF_FALSE",thisLabelNum);
            loadNextToken(); // eat the 'else'
            loadNextToken();
            lexemeIsEqual(currentTLexeme, "{");
            if (isStatement(peekTLexeme)) {
                while(isStatement(peekTLexeme)) {
                    statement();
                }
            }
            loadNextToken();
            lexemeIsEqual(currentTLexeme, "}");
            codeGenerator.writeLabel("IF_END", thisLabelNum);
        } else {
            codeGenerator.writeLabel("IF_FALSE", thisLabelNum);
        }
    }

    private void whileStmt() {
        int thisLabelNum = codeGenerator.getLabelNum(false);
        codeGenerator.incrementLabelNum(false);
        // check if this code is reachable by checking whether value has been returned
        semanticAnalyser.checkIfValueReturned();
        semanticAnalyser.setUnreachableReturn(true); // returns might not be reached within the loop
        semanticAnalyser.setLhsType("-1");
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "while");
        codeGenerator.writeLabel("WHILE_EXP", thisLabelNum);
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "(");
        semanticAnalyser.setisBooleanStmt(true);// expression for while statement must be boolean
        expression();
        loadNextToken();
        lexemeIsEqual(currentTLexeme, ")");
        codeGenerator.writeArithmetic("not"); // negate expression
        codeGenerator.writeIf("WHILE_END", thisLabelNum);
        semanticAnalyser.setisBooleanStmt(false);// expression for if statement must be boolean
        semanticAnalyser.setLhsType("-1");
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "{");

        if (isStatement(peekTLexeme)) {
            while (isStatement(peekTLexeme)){
                statement();
            }
        }
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "}");
        codeGenerator.writeGoto("WHILE_EXP", thisLabelNum);
        codeGenerator.writeLabel("WHILE_END", thisLabelNum);
        semanticAnalyser.setUnreachableReturn(false); // returns can now be reached again
    }

    private void doStmt() {
        // check if this code is reachable by checking whether value has been returned
        semanticAnalyser.checkIfValueReturned();
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "do");
        subroutineCall();
        loadNextToken();
        lexemeIsEqual(currentTLexeme, ";");
        codeGenerator.writePop("temp","0");
    }

    private void subroutineCall() {
        String className = null, funcName = null;
        pushExpList();
        // check if this code is reachable by checking whether value has been returned
        semanticAnalyser.checkIfValueReturned();
        loadNextToken();
        isIdentifier(currentTType);
        boolean pushedObject = false;
        // if call is to subroutine in another class
        if (peekTLexeme.compareTo(".") == 0) {
            // find out if call is being made direct to class or by object
            if(semanticAnalyser.idExistsInScope(currentTLexeme)){
                className = semanticAnalyser.getIdType(currentTLexeme);
                pushIdentifier(currentTLexeme); // push the identifier
                pushedObject = true;
            } else {
                className = currentTLexeme;
            }
            loadNextToken(); // consume '.'
            loadNextToken();
            isIdentifier(currentTType);
            funcName = currentTLexeme;
            // check if subroutine exists now or later
            semanticAnalyser.checkSubrDeclared(className,currentTLexeme);
        } else { // if call is to subroutine within this class
            // check if subroutine exists now or later
            codeGenerator.writePush("pointer", "0"); // is a method so push the object
            className = semanticAnalyser.getCurrentClass();
            funcName = currentTLexeme;
            semanticAnalyser.checkSubrDeclared(semanticAnalyser.getCurrentClass(),currentTLexeme);
            pushedObject = true;
        }
        expList.add(className); // add class of subroutine to the list
        expList.add(funcName); // add name of subroutine to the list
        expList.add(semanticAnalyser.getCurrentClass()); // add class of source of query to the list
        expList.add(semanticAnalyser.getCurrentSubroutine());
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "(");
        expressionList();
        loadNextToken();
        lexemeIsEqual(currentTLexeme, ")");
        semanticAnalyser.addToParamCheckList(expList);
        if (!pushedObject) {
            codeGenerator.writeFunction(className, funcName, expList.size() - 4);
        } else {
            codeGenerator.writeFunction(className, funcName, expList.size() - 3);
        }
        popExpList();
    }

    private void expressionList() {
        if(isExpression(peekTType, peekTLexeme)) {
            semanticAnalyser.setLhsType("-1");
            semanticAnalyser.setIsExpressionList(true);
            expression();
            if(expList!=null) {
                if (semanticAnalyser.getLhsType().compareTo("-1") !=0) {
                    expList.add(semanticAnalyser.getLhsType()); // add type of expression to expression list
                }
                else if (currentTType == Token.TokenType.INTEGER_CONST){
                        expList.add("int");
                } else if (currentTType == Token.TokenType.STRING_LIT){
                    expList.add("String");
                }
                else if(currentTLexeme.compareTo("true") == 0 || currentTLexeme.compareTo("false") == 0){
                    expList.add("boolean");
                }
                else if(currentTLexeme.compareTo("null") == 0){
                    expList.add("null");
                }
            }
            if(peekTLexeme.compareTo(",") == 0) {
                while (peekTLexeme.compareTo(",") == 0) {
                    semanticAnalyser.setIsExpressionList(true);
                    loadNextToken(); // consume ','
                    semanticAnalyser.setLhsType("-1");
                    expression();
                    if(expList!=null) {
                        if (semanticAnalyser.getLhsType().compareTo("-1") !=0) {
                            expList.add(semanticAnalyser.getLhsType()); // add type of expression to expression list
                        } else if (currentTType == Token.TokenType.INTEGER_CONST){
                                expList.add("int");
                        } else if (currentTType == Token.TokenType.STRING_LIT){
                            expList.add("String");
                        }
                        else if(currentTLexeme.compareTo("true") == 0 || currentTLexeme.compareTo("false") == 0){
                            expList.add("boolean");
                        }
                        else if(currentTLexeme.compareTo("null") == 0){
                            expList.add("null");
                        }
                    }
                }
            }
        }
        semanticAnalyser.setIsExpressionList(false);
    }

    private void returnStmt(){
        // value has been returned so set valueReturned to true
        semanticAnalyser.setIsValueReturned(true);
        loadNextToken();
        lexemeIsEqual(currentTLexeme, "return");
        semanticAnalyser.setIsReturn(true);
        semanticAnalyser.setLhsType("-1");
        if (isExpression(peekTType, peekTLexeme)) {
            // make sure the current return type is not void
            semanticAnalyser.checkReturnVoid(peekTLexeme);
            expression();
        } else {
            codeGenerator.writePush("constant","0");
        }
        loadNextToken();
        semanticAnalyser.setIsReturn(false);
        semanticAnalyser.setLhsType("-1");
        lexemeIsEqual(currentTLexeme, ";");
        codeGenerator.writeReturn();
    }

    // expression grammar
    boolean metOperator = false;
    private void expression() {
        metOperator = false;
        relationalExpr();
        if (peekTLexeme.compareTo("&") == 0 || peekTLexeme.compareTo("|") == 0) {
            metOperator = true;
            while (peekTLexeme.compareTo("&") == 0 || peekTLexeme.compareTo("|") == 0) {
                // aray indices are of type int. Comparators make the expression boolean
                if(semanticAnalyser.isArrayIndex()){
                    System.out.println("error: incompatible types: array index must be of type int ");
                    System.out.println("  symbol: " + peekTLexeme);
                    System.out.println("  location: subroutine " + semanticAnalyser.getCurrentSubroutine()
                            +"\n  class " + semanticAnalyser.getCurrentClass());
                    System.exit(0);
                }
                semanticAnalyser.setLhsType("-1"); // allow for comparison of different types in boolean statements
                loadNextToken(); // consume the '&' or '|'
                String op = currentTLexeme;
                relationalExpr();
                if (op.compareTo("&") == 0){
                    codeGenerator.writeArithmetic("and");
                } else if (op.compareTo("|") == 0) {
                    codeGenerator.writeArithmetic("or");
                }
            }
            // ensure that the expression is boolean if there are no comparators
        } else if (semanticAnalyser.getIsBooleanStmt() && metOperator == false) {
            String type = semanticAnalyser.getIdType(currentTLexeme);
            boolean isCorrectType = false;
            if (type != null){
                isCorrectType = (type.compareTo("boolean") == 0);
            } else if (currentTLexeme.compareTo("true") == 0 || currentTLexeme.compareTo("false") == 0) {
                isCorrectType = true;
                type = currentTType.name();
            } else {
                type = currentTType.name();
            }
            if(!isCorrectType) {
                System.out.println("error: incompatible types: '" + type.toLowerCase()
                        +"' cannot be converted to boolean ");
                System.out.println("  symbol: " + currentTLexeme);
                System.out.println("  location: subroutine " + semanticAnalyser.getCurrentSubroutine() +"\n  class "
                        + semanticAnalyser.getCurrentClass());
                System.exit(0);
            }
        }
    }

    private void relationalExpr() {
        arithmeticExpr();
        if (peekTLexeme.compareTo("=") == 0 || peekTLexeme.compareTo(">") == 0 || peekTLexeme.compareTo("<") == 0) {
            metOperator = true;
            while (peekTLexeme.compareTo("=") == 0 || peekTLexeme.compareTo(">") == 0
                    || peekTLexeme.compareTo("<") == 0) {
                // aray indices are of type int. Comparators make the expression boolean
                if(semanticAnalyser.isArrayIndex()){
                    System.out.println("error: incompatible types: array index must be of type int ");
                    System.out.println("  symbol: " + peekTLexeme);
                    System.out.println("  location: subroutine " + semanticAnalyser.getCurrentSubroutine()
                            +"\n  class " + semanticAnalyser.getCurrentClass());
                    System.exit(0);
                }
                semanticAnalyser.setLhsType("-1"); // allow for comparation of different types in boolean statements
                loadNextToken(); // consume the '=', '>' or '<'
                String op = currentTLexeme;
                arithmeticExpr();
                if (op.compareTo("=") == 0) {
                    codeGenerator.writeArithmetic("eq");
                } else if (op.compareTo(">") == 0) {
                    codeGenerator.writeArithmetic("gt");
                } else if (op.compareTo("<") == 0) {
                    codeGenerator.writeArithmetic("lt");
                }
            }
        }
    }

    private void arithmeticExpr() {
        term();
        if(peekTLexeme.compareTo("+") == 0 || peekTLexeme.compareTo("-") == 0) {
            while (peekTLexeme.compareTo("+") == 0 || peekTLexeme.compareTo("-") == 0) {
                loadNextToken(); // consume the '+' or '-'
                String op = currentTLexeme;
                term();
                if (op.compareTo("+") == 0){
                    codeGenerator.writeArithmetic("add");
                } else if (op.compareTo("-") == 0) {
                    codeGenerator.writeArithmetic("sub");
                }
            }
        }
    }

    private void term() {
        factor();
        if (peekTLexeme.compareTo("*") == 0 || peekTLexeme.compareTo("/") == 0) {
            while (peekTLexeme.compareTo("*") == 0 || peekTLexeme.compareTo("/") == 0) {
                loadNextToken(); // consume the '*' or '/'
                String op = currentTLexeme;
                factor();
                if(op.compareTo("*") == 0) {
                    codeGenerator.writeFunction("Math","multiply",2);
                } else if (op.compareTo("/") == 0) {
                    codeGenerator.writeFunction("Math","divide",2);
                }
            }
        }
    }

    private void factor() {
        String op = null;
        if (peekTLexeme.compareTo("-") == 0 || peekTLexeme.compareTo("~") == 0) {
            loadNextToken(); // consume the '-' or '~'
            op = currentTLexeme;
        }
        operand();
        if(op != null){
            if (op.compareTo("-") == 0){
                codeGenerator.writeArithmetic("neg");
            } else if (op.compareTo("~") == 0) {
                codeGenerator.writeArithmetic("not");
            }
        }
    }

    private void operand() {
        boolean inScope = false;
        loadNextToken();
        String arrayName = null;
        String funcName = null;
        String className = null;
        pushExpList();

        if (currentTType == Token.TokenType.INTEGER_CONST){
            // if current match type is not set, or not equal to current type, print error message
            semanticAnalyser.checkOperandTypes("int", currentTLexeme);
            codeGenerator.writePush("constant",currentTLexeme);
        }
        else if(currentTType == Token.TokenType.IDENTIFIER){
            inScope = false;
            int Parser = 0;
            boolean pushedObject = false;

            if(peekTLexeme.compareTo(".") == 0){
                // find out if call is being made direct to class or by object. The class of the object is className.
                if(semanticAnalyser.idExistsInScope(currentTLexeme)){
                    className = semanticAnalyser.getIdType(currentTLexeme);
                    pushIdentifier(currentTLexeme);
                    pushedObject = true;
                } else {
                    className = currentTLexeme;
                }
                loadNextToken(); // consume the '.'
                loadNextToken();
                isIdentifier(currentTType);
                // given that it is a variable not in scope, check for it's existence in given class & check its type
                // accessing fields in other classes is illegal so error will be issued later
                if (peekTLexeme.compareTo("(") != 0) {
                    codeGenerator.writePop("pointer", "0");
                    List<Object> objectVarRef = new ArrayList<Object>();
                    objectVarRef.add(className);
                    objectVarRef.add(currentTLexeme);
                    codeGenerator.addObjectVarDetails(objectVarRef);
                    semanticAnalyser.addToObjectVarRefList(objectVarRef);

                    semanticAnalyser.checkVarDeclared(className, currentTLexeme, false);
                    if (peekTLexeme.compareTo("[") != 0){ // only check type if it's not an array
                        semanticAnalyser.checkType(currentTLexeme, className, true);
                    }
                } else { // given that it is a subroutine not in scope, check for its existence in given class
                    funcName = currentTLexeme;

                }
            } else if(peekTLexeme.compareTo("(") != 0){ // case if a variable in current scope
                // given that it is a variable, check if the operand has been declared within scope
                semanticAnalyser.checkVarDeclared(null,currentTLexeme,true);
                if (peekTLexeme.compareTo("[") != 0){ // only check type and initialisation if it's not an array
                    pushIdentifier(currentTLexeme);
                    semanticAnalyser.checkType(currentTLexeme, null, true);
                    // type can't be array in this case
                    // set lhs type if not already set
                    if (semanticAnalyser.getLhsType().compareTo("-1") == 0 && !semanticAnalyser.isArrayIndex()) {
                        semanticAnalyser.setLhsType(semanticAnalyser.getIdType(currentTLexeme));
                    }
                    semanticAnalyser.checkInitialised(currentTLexeme);
                } else {
                    arrayName = currentTLexeme;
                }
            } else {
                inScope = true; // it is a subroutine within current class
                funcName = currentTLexeme;
                className= semanticAnalyser.getCurrentClass();
            }

            if(peekTLexeme.compareTo("[") == 0){
                loadNextToken(); // consume the '['
                semanticAnalyser.setIsArrayIndex(true);
                expression();
                if (arrayName!=null) {
                    pushIdentifier(arrayName);
                }
                loadNextToken();
                lexemeIsEqual(currentTLexeme, "]");
                codeGenerator.writeArithmetic("add"); // add the array base address and index
                codeGenerator.writePop("pointer","1");
                codeGenerator.writePush("that","0");
                semanticAnalyser.setIsArrayIndex(false);
            } else if(peekTLexeme.compareTo("(") == 0){
                expList.add(className);
                expList.add(funcName);
                expList.add(semanticAnalyser.getCurrentClass()); // add class from which call originates
                expList.add(semanticAnalyser.getCurrentSubroutine());
                semanticAnalyser.checkSubrDeclared(className,funcName);
                semanticAnalyser.checkType(funcName,className ,false);

                // if subroutine is declared
                if (inScope) { // check if subroutine exists in current scope now or later & check its type
                    if (semanticAnalyser.getLhsType().compareTo("-1") == 0) { // set lhs type if not already set
                        semanticAnalyser.setLhsType(semanticAnalyser.getIdType(currentTLexeme));
                    }
                    inScope = false;
                } else {
                    if (semanticAnalyser.getLhsType().compareTo("-1") == 0) { // set lhs type if not already set
                        semanticAnalyser.setLhsType("-2"); // unknown function return type
                    }
                }
                loadNextToken(); // consume the '('
                expressionList();
                semanticAnalyser.addToParamCheckList(expList);
                if (!pushedObject) {
                    codeGenerator.writeFunction(className, funcName, expList.size() - 4);
                } else {
                    codeGenerator.writeFunction(className, funcName, expList.size() - 3);
                }
                loadNextToken();
                lexemeIsEqual(currentTLexeme, ")");
            }
        }
        else if(currentTLexeme.compareTo("(") == 0){
            expression();
            loadNextToken();
            lexemeIsEqual(currentTLexeme, ")");
        }
        else if (currentTType == Token.TokenType.STRING_LIT){
            semanticAnalyser.checkOperandTypes("String", currentTLexeme);
            // process string into suitable vm format
            codeGenerator.writePush("constant", Integer.toString(currentTLexeme.length()-2));
            codeGenerator.writeFunction("String", "new", 1);
            for (int i = 1; i < currentTLexeme.length()-1; i++){
                codeGenerator.writePush("constant", Integer.toString(Character.codePointAt(currentTLexeme, i)));
                codeGenerator.writeFunction("String", "appendChar", 2);
            }
        }
        else if(currentTLexeme.compareTo("true") == 0){
            semanticAnalyser.checkOperandTypes("boolean", currentTLexeme);
            codeGenerator.writePush("constant","1");
            codeGenerator.writeArithmetic("neg");
        }
        else if(currentTLexeme.compareTo("false") == 0){
            semanticAnalyser.checkOperandTypes("boolean", currentTLexeme);
            codeGenerator.writePush("constant","0");
        }
        else if(currentTLexeme.compareTo("null") == 0){
            String matchType = semanticAnalyser.getMatchType();
            codeGenerator.writePush("constant","0");
        }
        else if(currentTLexeme.compareTo("this") == 0){
            if (semanticAnalyser.getLhsType().compareTo("-1") == 0) {
                semanticAnalyser.setLhsType(semanticAnalyser.getCurrentClass());
            }
            codeGenerator.writePush("pointer", "0");
        }
        else if (currentTType == Token.TokenType.UNKNOWN){
            System.out.println("ERROR: unknown type encountered '"+currentTLexeme+"'");
            System.exit(0);
        }

        popExpList();
    }

    // pushes identifiers to stack, using their index and segment location
    public void pushIdentifier(String id){
        if (semanticAnalyser.getIdKind(id).compareTo("ARGUMENT") == 0) {
            codeGenerator.writePush("argument", Integer.toString(semanticAnalyser.getIdIndex(id)));
        } else if (semanticAnalyser.getIdKind(id).compareTo("VARIABLE") == 0) {
            codeGenerator.writePush("local", Integer.toString(semanticAnalyser.getIdIndex(id)));
        } else if (semanticAnalyser.getIdKind(id).compareTo("STATIC") == 0) {
            codeGenerator.writePush("static", Integer.toString(semanticAnalyser.getIdIndex(id)));
        } else if (semanticAnalyser.getIdKind(id).compareTo("FIELD") == 0) {
            codeGenerator.writePush("this", Integer.toString(semanticAnalyser.getIdIndex(id)));
        }
    }

    // pops identifiers to stack, using their index and segment location
    public void popIdentifier(String id){
        if (semanticAnalyser.getIdKind(id).compareTo("ARGUMENT") == 0) {
            codeGenerator.writePop("argument", Integer.toString(semanticAnalyser.getIdIndex(id)));
        } else if (semanticAnalyser.getIdKind(id).compareTo("VARIABLE") == 0) {
            codeGenerator.writePop("local", Integer.toString(semanticAnalyser.getIdIndex(id)));
        } else if (semanticAnalyser.getIdKind(id).compareTo("STATIC") == 0) {
            codeGenerator.writePop("static", Integer.toString(semanticAnalyser.getIdIndex(id)));
        } else if (semanticAnalyser.getIdKind(id).compareTo("FIELD") == 0) {
            codeGenerator.writePop("this", Integer.toString(semanticAnalyser.getIdIndex(id)));
        }
    }

    // creates new expression list for a subroutine expression list
    public void pushExpList(){
        expList = new ArrayList<String>();
        expListList.add(0, expList);
    }

    // removes newest expression list from list expression lists list
    public void popExpList(){

        expListList.remove(0);
        if (expListList.size() > 0) {
            expList = expListList.get(0);
        } else{
            expList = null;
        }
    }
}