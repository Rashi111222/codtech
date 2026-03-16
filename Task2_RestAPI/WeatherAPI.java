import java.io.*;
import java.util.*;
import java.net.*;

public class WeatherAPI {
    public static void main(String args[])
    {
        Scanner sc=new Scanner(System.in);
        System.out.println("Enter city name: ");
        String city=sc.nextLine();

        String apiKey="3af5c95b01bd893312e8fa1f83ae712a";

        String urlString= "https://api.openweathermap.org/data/2.5/weather?q="
            + city + "&appid=" + apiKey + "&units=metric";

    }
}
