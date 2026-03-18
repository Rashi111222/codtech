import java.io.*;
import java.util.*;
import java.net.*;
import org.json.JSONObject;

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

            JSONObject json =new JSONObject(response.toString());
            String cityName= json.getString("name");

            double temp=json.getJSONObject("main").getDouble("temp");

            int humidity=json.getJSONObject("main").getInt("humidity");

            String weather= json.getJSONArray("weather")
                                .getJSONObject(0)
                                .getString("description");

            System.out.println("\nWeather Details:");
System.out.println("City: " + cityName);
System.out.println("Temperature: " + temp + " °C");
System.out.println("Humidity: " + humidity + "%");
System.out.println("Weather: " + weather);
        }

        catch(Exception e){
            System.out.println("Error: "+e.getMessage());
        }
        
        sc.close();

    }
}
