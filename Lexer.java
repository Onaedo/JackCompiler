import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.io.File;



public class Lexer {
  // List of tokens
  public List<Token> tokens;
  public Lexer() {}
  // return list of all tokens for the file
  public List<Token> getTokens () { return tokens; }


  // methods

  // removes token from token list at specified index i
  public void removeToken(int i){
    tokens.remove(i);
  }

  public void extractTokens(File file) {
    // first reset all lists, then read file
    tokens = new ArrayList<Token>();
    FileScanner fs = new FileScanner( file );
    fs.readFile();
    this.tokens = fs.tokeniser.getTokens();
  }


  public Token GetNextToken(){
    if(tokens.size() == 0) {
      return null;
    }
    Token token = tokens.get(0);
    this.removeToken(0);
    return token;
  }
  public Token PeekNextToken(){
    if(tokens.size() <= 1) {
      return null;
    }
    Token token = tokens.get(0);

    return token;
  }
}
