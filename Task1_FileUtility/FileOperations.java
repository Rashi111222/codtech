package Task1_FileUtility;
import java.io.*;

public class FileOperations {
    public static void main(String args[]){
        String fileName="sample.txt";
        writeFile(fileName,"Hello!\nWelcome to Java Handling!");
    }

    public static void writeFile(String fileName,String content){
        try{
            FileWriter writer= new FileWriter(fileName);
            writer.write(content);
            writer.close();
            System.out.println("File written successfully");
        }
        catch(IOException e){
            System.out.println("Error while writing ");
        }
    }
}
