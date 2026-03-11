import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * SalahTimes
 */
public class SalahTimes {
    public static void main(String[] args)
            // IOException and InterruptedException required for HttpClient.send()
            throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        IO.println("Welcome to the SalahTimes app");
        IO.print("What city do you live in? ");
        String userCity = scanner.next();
        // create Location Object
        Location location = new Location(userCity);
        // use method to geocode city
        location.geolocate();
    }
}

/**
 * Holds user's geographic data
 */
class Location {
    public double latitude;
    public double longitude;
    public String cityName;

    public Location(String city){
        IO.println("Making a new Location with: " + city);
        this.cityName = city;
    }

    public Location(double lat, double lon, String city){
        this.latitude = lat;
        this.longitude = lon;
        this.cityName = city;
    }

    // geocode city name
    // IOException and InterruptedException required for HttpClient.send()
    public void geolocate() throws IOException, InterruptedException {
        // encoding for safe URL usage
        String encodedCity = URLEncoder.encode(this.cityName, StandardCharsets.UTF_8);

        // prepare API request
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedCity
                + "&count=1";

        // create HttpClient
        try (HttpClient client = HttpClient.newHttpClient()) {

            // build Http GET request for API
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            // Send request and get response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


            // assign results
            if (response.statusCode() == 200) {
                //DEBUG
                IO.println("DEBUG: response code == 200");
                IO.println(response.body());
            } else {
                IO.println("ERR: Bad STATUS CODE");
            }
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCityName() {
        return cityName;
    }

    @Override
    public String toString() {
        return "Latitude: " + getLatitude() + " Longitude: " + getLongitude();
    }
}

/**
 * Applies prayer time formulas to solar data
 */
class PrayerTimesCalculator {

}

/**
 * Computes sun position and solar events
 */
class SolarCalculator {

}

/**
 * Stores the 5 calculated prayer times
 */
class PrayerSchedule {

}
