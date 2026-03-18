import java.io.*;
import java.util.*;
import java.net.*;

public class WeatherAPI {
    public static void main(String args[])
    {
        Scanner sc=new Scanner(System.in);

        try{
            System.out.println("Enter city name: ");
        String city=sc.nextLine();

        String apiKey="3af5c95b01bd893312e8fa1f83ae712a";

        String urlString= "https://api.openweathermap.org/data/2.5/weather?q="
            + city + "&appid=" + apiKey + "&units=metric";
        
        URL url = new URL(urlString);

        HttpURLConnection conn =
            (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            BufferedReader reader =
            new BufferedReader(
            new InputStreamReader(conn.getInputStream()));

            String line;
            StringBuilder response =new StringBuilder();

            while((line=reader.readLine())!= null){
                response.append(line);
            }

            reader.close();

            System.out.println("\nWeather API response:");
            System.out.println(response.toString());
        }

        catch(Exception e){
            System.out.println("Error: "+e.getMessage());
        }
        
        sc.close();

    }
}
