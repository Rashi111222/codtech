import java.io.*;
import java.net.*;

public class AgeAPI{
    public static void main(String args[]){

        try{
            URL url=new URL("https://api.agify.io/?name=rashi");

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