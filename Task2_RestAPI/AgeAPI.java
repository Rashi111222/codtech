import java.io.*;
import java.net.*;
import java.util.Scanner;

public class AgeAPI{
    public static void main(String args[]){
        Scanner sc=new Scanner(System.in);

        try{
            String name;
            System.out.println("Enter name: ");
            name=sc.nextLine();

            String apiURL="https://api.agify.io/?name="+name;
            URL url=new URL(apiURL);

            HttpURLConnection conn= (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;

            while((line=reader.readLine())!=null){
                System.out.println(line);
            }
            reader.close();
        }
        catch(Exception e){
            System.out.println("Error occured");
        }
    }
}