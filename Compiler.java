import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;

// compiler class. Handles extracting files from directory and passing them to Lexer and Parser
public class Compiler{
    Parser parser;

    public Compiler(String directory) {
        this.parser = new Parser(directory);
    }

    // runs the Parser. The parser runs the lexer
    public void compile(String directory) {
        parser.runParser(getFiles(directory));
    }

    // returns all of the jack files from within the given directory
    public File[] getFiles(String input) {
        File directory = new File(input);
        // make sure the directory exists and is not a file
        if(!directory.exists() || !directory.isDirectory()){
            System.out.println("Please enter an existing directory");
            System.exit(0);
        }

        // obtain list of files ending in '.jack'
        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File directory, String name) {
                return name.endsWith(".jack");
            }});

        // make sure there are files to compile
        if (files.length == 0) {
            System.out.println("Please enter a directory containing .jack files");
            System.exit(0);
        }


        return files;
    }

    public static void main( String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a single directory");
            System.exit(0);
        }
        Compiler compiler = new Compiler(args[0]);
        compiler.compile(args[0]);
    }
}