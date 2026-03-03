
import java.io.*;

public class FileOperations {
    public static void main(String args[]){
        String fileName="sample.txt";
        writeFile(fileName,"Hello!\nWelcome to Java Handling!");

        readFile(fileName);
    }

    /*Write Function */
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

    /*Read function */
    public static void readFile(String fileName){
        try{
            BufferedReader reader=new BufferedReader(new FileReader(fileName));
            String line;

            System.out.println("Reading file contentt: ");

            while((line=reader.readLine())!=null){
                System.out.println(line);
            }
        }
        catch(IOException e){
            System.out.println("Error while reading file");
        }
    }
}
