
// represents a token
public class Token {
  //list contains types the 7 token types
  public enum TokenType {KEYWORD, SYMBOL, IDENTIFIER, CONSTANT, INTEGER_CONST, STRING_LIT, UNKNOWN}

  //the two parts of a token
  private String lexeme;
  private TokenType type;

  //constructor methods
  public Token(String lexeme) {
    this.lexeme = lexeme;

    if (isKeyword(lexeme)) {this.type = TokenType.KEYWORD;}
    else if (isSymbol(lexeme)) {this.type = TokenType.SYMBOL;}
    else if (isIdentifier(lexeme)) {this.type = TokenType.IDENTIFIER;}
    else if (isConstant(lexeme)) {this.type = TokenType.CONSTANT;}
    else if (isStringLiteral(lexeme)) {this.type = TokenType.STRING_LIT;}
    else if (isIntegerConstant(lexeme)) {this.type = TokenType.INTEGER_CONST;}
    else {this.type = TokenType.UNKNOWN;}
  }
  public Token() {
    lexeme = "";
  }

  public String getLexeme() {
    return lexeme;
  }
  public TokenType getType() {
    return type;
  }
  public void setLexeme( String lexemeString) {
    lexeme = lexemeString;
  }

  // set type of token depending on lexeme. Returns true if lexeme is of that type

  static public boolean isKeyword(String lexeme) {
    String[] keywords = new String[] { "class", "constructor", "method", "function", "int", "boolean", "char", "void",
            "var", "static", "field", "let", "do", "if", "else", "while", "return", "true",
            "false", "null", "this"};
    for (String keyword : keywords) {
      if (lexeme.compareTo(keyword) == 0) {
        return true;}
    }
    return false;
  }

  static public boolean isSymbol(String lexeme) {
    String[] symbols = new String[] { "(", ")", "[", "]", "{", "}", ",", ";", "=", ".", "+", "-", "*", "/", "&", "|",
            "~", "<", ">"};
    for (String symbol : symbols) {
      if (lexeme.compareTo(symbol) == 0) {
        return true;
      }
    }
    return false;
  }

  static public boolean isConstant(String lexeme) {
    // check if is boolean constant or null
    if ( lexeme.compareTo("true") == 0 || lexeme.compareTo("false") == 0 || lexeme.compareTo("null") == 0) {
      return true;
    }
    return false;
  }

  static public boolean isIntegerConstant(String lexeme){
    char initialChar = lexeme.charAt(0);

    if (initialChar >= '0' && initialChar <= '9') { // check if is integer
      for (int i = 1; i < lexeme.length(); i++) {
        if(!(lexeme.charAt(i) >= '0' && lexeme.charAt(i) <= '9')){
          return false;
        }
      }
    }
    return true;
  }

  static public boolean isStringLiteral(String lexeme){
    if( lexeme.charAt(0) == '\"') { // check if is string
      return true;
    }
    return false;
  }

  static public boolean isIdentifier(String lexeme) {
    char initialChar = lexeme.charAt(0);
    if( (initialChar >= 'a' && initialChar <= 'z') || (initialChar >= 'A' && initialChar <= 'Z')
            || initialChar == '_') {
      for (int i = 1; i < lexeme.length(); i++) {
        if(!((lexeme.charAt(i) >= 'a' && lexeme.charAt(i) <= 'z') || (lexeme.charAt(i) >= 'A'
                && lexeme.charAt(i) <= 'Z') || lexeme.charAt(i) == '_' || (lexeme.charAt(i) >= '0'
                && lexeme.charAt(i) <= '9'))){
          return false;
        }
      }
      return true;
    }
    return false;
  }

}
