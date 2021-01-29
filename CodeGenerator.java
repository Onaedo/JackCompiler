import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.lang.IllegalArgumentException;
import java.util.ArrayList;
import java.util.List;

public class CodeGenerator {

    private int whileLabelNum = 0, ifLabelNum = 0; // keeps track of highest label number for vm
    private File file;
    private PrintWriter pw;
    private List <String> fileInput = new ArrayList <String>();
    private int lineCount = 0;

    public int getLabelNum(boolean isIf) {
        if (isIf){
            return ifLabelNum;
        } else {
            return whileLabelNum;
        }
    }
    public void incrementLabelNum(boolean isIf){

        if (isIf){
            ifLabelNum++;
        } else {
            whileLabelNum++;
        }
    }

    public void reset(){
        whileLabelNum = 0;
        ifLabelNum = 0;
        lineCount = 0;
        fileInput.clear();
    }
    // creates a .vm file for current class
    public void createVMFile(String name, String directory){
        // create file
        try {
            file = new File(directory + "/" + name + ".vm");
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("error: file could not be created");
        }
        // create reader for file
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        } catch (IOException e) {
            System.out.println("error: error writing to file");
        }
    }

    // closes current file
    public void closeVMFile(){pw.close();}

    // prints fileInput list into file
    public void printToFile(){
        if (fileInput.size() > 0) {
            for (String s : fileInput) {
                pw.println(s);
            }
        }
        fileInput.clear();

    }

    // writes push command
    public void writePush(String seg, String value){
        fileInput.add("push "+ seg +" "+ value);
        lineCount++;
    }

    // writes pop command
    public void writePop(String seg, String value){
        fileInput.add("pop " +seg+" "+ value);
        lineCount++;
    }

    // writes arithmetic command
    public void writeArithmetic(String command){
        fileInput.add(command);
        lineCount++;
    }

    // writes label command
    public void writeLabel(String s,int n){
        fileInput.add("label "+ s + n);
        lineCount++;
    }

    // writes goto command
    public void writeGoto(String s,int n){
        fileInput.add("goto " + s + n);
        lineCount++;
    }

    // writes if-goto command
    public void writeIf(String s, int n){
        fileInput.add("if-goto " + s + n);
        lineCount++;
    }

    // writes function command
    public void writeFunction(String className, String name, int argsNum) {
        fileInput.add("call " +className+"."+ name + " " + argsNum);
        lineCount++;
    }

    // writes function declaration
    public void writeFuncDeclar(String className, String name, int paramNumber) {
        fileInput.add(0,"function " + className + "." + name + " " + paramNumber);
        lineCount++;
    }

    // writes return command
    public void writeReturn(){
        fileInput.add("return");
        lineCount++;
    }

    // returns details needed for to resolve a class variable call
    public void addObjectVarDetails(List<Object> list){
        list.add(file);
        list.add(lineCount);
    }

    // inserts line into specified position in specified file
    public static void insertLine(String line, File file, int index){
        // read file to array list, insert line, print back to file
        List<String> fileContent = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while (br.ready()) {
                fileContent.add(br.readLine());
            }
            fileContent.add(index, line);
            br.close();

        } catch (FileNotFoundException e){
            System.out.println("error: could not find file");
        } catch (IOException e){
            System.out.println("error: could not read file");
        }
        // now write the contents back into file
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            if (fileContent.size() > 0) {
                for (String s : fileContent) {
                    pw.println(s);
                }
            }
            pw.close();
        } catch (FileNotFoundException e){
            System.out.println("error: could not find file");
        }catch (IOException e) {
            System.out.println("error: error writing to file");
        }
    }


}