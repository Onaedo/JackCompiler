import java.util.ArrayList;
import java.util.List;

// has a list of tokens and creates new tokens
public class Tokeniser {

    private List<Token> tokens;
    public Tokeniser() {
        tokens = new ArrayList<Token>();
    }
    private String lexeme = "";

    // use lexeme to make tokens
    public void createToken( String lexeme){
        Token token = new Token(lexeme);
        tokens.add(token);
    }
    // return list of all tokens for the file
    public List<Token> getTokens () { return tokens; }

    public String getLexeme() {
        return lexeme;
    }

    // creates lexeme for a token
    public void createLexeme(char c, char cPrev, boolean isString) {
        String s = "";
        // add / to lexeme if it wasn't part of a comment and proceed below
        if (cPrev == '/' && c != '*' && !isString) {
            s += cPrev; // store the '/'
            if (!lexeme.isEmpty()) {
                createToken(lexeme); // create the preceding lexeme before creating a lexeme for '/'
            }
            lexeme = "";
            createToken(s);
            s = "";
        }

        if (isString) { // build string lexeme
            lexeme += c;
        } else if (c != '/') {
            s += c;
            if (Token.isSymbol(s)) { // make special symbols into their own token
                if (!lexeme.isEmpty()) {
                    createToken(lexeme);
                }
                lexeme = "";
                createToken(s);
            } else if (Character.isWhitespace(c)) { // make new token if white space encountered
                if (!lexeme.isEmpty()) {
                    createToken(lexeme);
                }
                lexeme = "";
            } else if (cPrev == '/') { // otherwise just add the character to the lexeme
                lexeme += c;
            } else { // otherwise just add the character to the lexeme
                lexeme += c;
            }

        }
    }
}