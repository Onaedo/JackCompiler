import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.io.File;



public class FileScanner {
    private File file;
    public Tokeniser tokeniser;

    public FileScanner(File file) {
        this.file = file;
        this.tokeniser = new Tokeniser();
    }

    // return true if string is being read, return false if string is no longer being read
    public boolean readingString(boolean isString) {
        if (isString == false) {
            return true;
        } else {
            return false;
        }
    }

    // return true only if comment has started
    public boolean isCommentStart(char cPrev, char c) {
        if ((cPrev == '/' && c == '/') || (cPrev == '/' && c == '*')) {
            return true;
        } else {
            return false;
        }
    }

    // return true only if comment has ended
    public boolean isCommentEnd(char cPrev, char c, boolean singleLine) {
        if (singleLine && c == '\n') {
            return true;
        } else if (cPrev == '*' && c == '/') {
            return true;
        } else {
            return false;
        }
    }

    // return true if single line comment, false otherwise
    public boolean isSingleLineComment(char c) {
        if (c == '/') {
            return true;
        } else {
            return false;
        }
    }

    // open file and read character by character
    public void readFile() {

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            char c = '0', cPrev = '0';
            int i;
            boolean isString = false, singleLine = false;
            try {
                while ((i = br.read()) != -1) { // iterate through char by char
                    c = (char) i;
                    if (c == '\"') { // create token for string (between a pair of quotes)
                        isString = this.readingString(isString);
                    }
                    if (isCommentStart(cPrev, c) && !isString) { // skip all lines until end of comment
                        // make a lexeme using chars before start of comment
                        tokeniser.createLexeme(' ', ' ', isString);

                        singleLine = isSingleLineComment(c);
                        while (!isCommentEnd(cPrev, c, singleLine) && i != -1) {
                            cPrev = c;
                            i = br.read();
                            c = (char) i;
                        }
                    } else {
                        if (c == '\"' && !isString) {
                            tokeniser.createLexeme(c, cPrev, true); // add character to lexeme
                        } else {
                            tokeniser.createLexeme(c, cPrev, isString); // add character to lexeme
                        }
                        cPrev = c;
                    }
                }
                tokeniser.createLexeme(' ', cPrev, isString); //create token using the last lexeme produced
                br.close();
            } catch (IOException e) {
                System.out.println("error");
            }
        } catch (IOException e) {
            System.out.println("ERROR: File not found: " + file);
        }
    }
}