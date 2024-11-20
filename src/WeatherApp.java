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
    private static final String API_KEY = "12830b72b3de8fc2a0871a45af97f927";

    public static JSONObject getWeatherData(String locationName) {
        JSONArray locationData = getLocationData(locationName);

        if (locationData == null || locationData.isEmpty()) {
            System.out.println("Error: Location not found.");
            return null;
        }

        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("lat");
        double longitude = (double) location.get("lon");

        String urlString = "https://api.openweathermap.org/data/2.5/forecast?" +
                "lat=" + latitude + "&lon=" + longitude +
                "&units=metric&appid=" + API_KEY;

        try {
                HttpURLConnection conn = fetchApiResponse(urlString);
                if (conn == null || conn.getResponseCode() != 200) {
                    System.out.println("Error: Could not connect to API.");
                return null;
            }

            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while (scanner.hasNext()) {
                resultJson.append(scanner.nextLine());
            }
            scanner.close();
            conn.disconnect();

            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(resultJson.toString());
            JSONArray list = (JSONArray) resultJsonObj.get("list");

            int index = findIndexOfCurrentTime(list);
            JSONObject forecastData = (JSONObject) list.get(index);

            JSONObject mainData = (JSONObject) forecastData.get("main");
            double temperature = (double) mainData.get("temp");

            JSONArray weatherArray = (JSONArray) forecastData.get("weather");
            JSONObject weatherObj = (JSONObject) weatherArray.get(0);
            long weatherCode = (long) weatherObj.get("id");
            String weatherCondition = convertWeatherCode(weatherCode);

            long humidity = (long) mainData.get("humidity");

            JSONObject windData = (JSONObject) forecastData.get("wind");
            double windspeed = (double) windData.get("speed");

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

    public static JSONArray getLocationData(String locationName) {
        locationName = locationName.replaceAll(" ", "+");
        String urlString = "http://api.openweathermap.org/geo/1.0/direct?q=" +
                locationName + "&limit=1&appid=" + API_KEY;

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);
            if (conn == null || conn.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to API.");
                return null;
            }

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

    private static String getCurrentTime() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
        return currentDateTime.format(formatter);
    }

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
