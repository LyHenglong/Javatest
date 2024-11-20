import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class WeatherApp {
    // OpenWeatherMap API Key
    private static final String API_KEY = "12830b72b3de8fc2a0871a45af97f927";

    // Fetch weather data for a given location
    public static JSONObject getWeatherData(String locationName) {
        JSONArray locationData = getLocationData(locationName);

        // Ensure location data is valid
        if (locationData == null || locationData.isEmpty()) {
            System.out.println("Error: Location not found.");
            return null;
        }

        // Extract latitude and longitude
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = Double.parseDouble(location.get("lat").toString());
        double longitude = Double.parseDouble(location.get("lon").toString());

        // Build weather API URL
        String urlString = "https://api.openweathermap.org/data/2.5/forecast?" +
                "lat=" + latitude + "&lon=" + longitude +
                "&units=metric&appid=" + API_KEY;

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);

            // Check connection
            if (conn == null) {
                System.out.println("Error: Unable to establish a connection.");
                return null;
            }
            if (conn.getResponseCode() != 200) {
                System.out.println("Error: API response code " + conn.getResponseCode());
                return null;
            }

            // Parse response
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while (scanner.hasNext()) {
                resultJson.append(scanner.nextLine());
            }
            scanner.close();
            conn.disconnect();

            // Parse JSON response
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(resultJson.toString());
            JSONArray list = (JSONArray) resultJsonObj.get("list");

            // Find current hour's data
            int index = findIndexOfCurrentTime(list);
            JSONObject forecastData = (JSONObject) list.get(index);

            // Extract required data
            JSONObject mainData = (JSONObject) forecastData.get("main");
            double temperature = Double.parseDouble(mainData.get("temp").toString());

            JSONArray weatherArray = (JSONArray) forecastData.get("weather");
            JSONObject weatherObj = (JSONObject) weatherArray.get(0);
            long weatherCode = Long.parseLong(weatherObj.get("id").toString());
            String weatherCondition = convertWeatherCode(weatherCode);

            long humidity = Long.parseLong(mainData.get("humidity").toString());

            JSONObject windData = (JSONObject) forecastData.get("wind");
            double windspeed = Double.parseDouble(windData.get("speed").toString());

            // Build and return weather data object
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);

            return weatherData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Retrieve geographic coordinates for a given location
    public static JSONArray getLocationData(String locationName) {
        locationName = locationName.replaceAll(" ", "+");
        String urlString = "http://api.openweathermap.org/geo/1.0/direct?q=" +
                locationName + "&limit=1&appid=" + API_KEY;

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);

            if (conn == null) {
                System.out.println("Error: Unable to establish a connection.");
                return null;
            }
            if (conn.getResponseCode() != 200) {
                System.out.println("Error: API response code " + conn.getResponseCode());
                return null;
            }

            // Parse response
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while (scanner.hasNext()) {
                resultJson.append(scanner.nextLine());
            }
            scanner.close();
            conn.disconnect();

            JSONParser parser = new JSONParser();
            return (JSONArray) parser.parse(resultJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Establish HTTP connection and fetch response
    private static HttpURLConnection fetchApiResponse(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Find the index of the current hour's forecast
    private static int findIndexOfCurrentTime(JSONArray list) {
        String currentTime = getCurrentTime();
        for (int i = 0; i < list.size(); i++) {
            JSONObject forecast = (JSONObject) list.get(i);
            String time = (String) forecast.get("dt_txt");
            if (time.startsWith(currentTime.substring(0, 13))) {
                return i;
            }
        }
        return 0;
    }

    // Get the current time formatted as "yyyy-MM-dd HH:00"
    private static String getCurrentTime() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
        return currentDateTime.format(formatter);
    }

    // Convert weather code to human-readable condition
    private static String convertWeatherCode(long weatherCode) {
        if (weatherCode == 800) return "Clear";
        if (weatherCode >= 801 && weatherCode <= 804) return "Cloudy";
        if (weatherCode >= 500 && weatherCode <= 531) return "Rain";
        if (weatherCode >= 600 && weatherCode <= 622) return "Snow";
        if (weatherCode >= 200 && weatherCode <= 232) return "Thunderstorm";
        if (weatherCode >= 300 && weatherCode <= 321) return "Drizzle";
        if (weatherCode >= 700 && weatherCode <= 781) return "Atmospheric Conditions";
        return "Unknown";
    }
}
