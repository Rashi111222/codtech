
import java.io.*;
import java.util.Scanner;

public class FileOperations {
    public static void main(String args[]){
        String fileName="sample.txt";
        int choice;
        do{
            System.out.println("File Menu: ");
            System.out.println("1. Write to file");
            System.out.println("2. Read file");
            System.out.println("3. Modify file contents");
            System.out.println("4. Exit");
            System.out.println("Enter your choice: ");
            Scanner sc=new Scanner(System.in);
            choice=sc.nextInt();
            sc.nextLine();

            switch(choice){
                case 1: System.out.print("Enter text to write: ");
                        String content=sc.nextLine();
                        writeFile(fileName, content);
                        break;

                case 2: readFile(fileName);
                        break;

                case 3: System.out.print("Enter word to replace: ");
                String oldWord = sc.nextLine();

                System.out.print("Enter new word: ");
                String newWord = sc.nextLine();

                modifyFile(fileName, oldWord, newWord);
                break;

                case 4:
                System.out.println("Exiting program...");
                break;

                default:
                System.out.println("Invalid choice. Try again.");
            }

        } while(choice != 4);
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
            System.out.println("Error while writing "+e.getMessage());
        }
    }

    /*Read function */
    public static void readFile(String fileName){
        try{
            BufferedReader reader=new BufferedReader(new FileReader(fileName));
            String line;

            System.out.println("Reading file content: ");

            while((line=reader.readLine())!=null){
                System.out.println(line);
            }
        }
        catch(IOException e){
            System.out.println("Error while reading file");
        }
    }

    /*Modify Function */
    public static void modifyFile(String fileName,String oldString,String newString)
    {
        try{
        BufferedReader reader=new BufferedReader(new FileReader(fileName));
        StringBuilder content=new StringBuilder();

        String line;

        while((line=reader.readLine())!=null){
            content.append(line.replace(oldString,newString)).append("\n");
        }
        reader.close();
        
        
        BufferedWriter writer=new BufferedWriter(new FileWriter(fileName));
        writer.write(content.toString());
        writer.close();

        System.out.println("File modified successfully");
    }
    catch(IOException e){
        System.out.println("Error while modifying file");
    }
    }
}
