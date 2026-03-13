import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import com.google.gson.Gson;

/**
 * SalahTimes
 */
public class SalahTimes {
    static void main(String[] args)
            // IOException and InterruptedException required for HttpClient.send()
            throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        IO.println("Welcome to the SalahTimes app");
        IO.print("What city do you live in? ");
        String userCity = scanner.nextLine();
        // create Location Object
        Location location = new Location(userCity);
        // use method to geocode city
        location.geolocate();

        //IO.println(location.toString());

        PrayerTimesCalculator prayerTimesCalculator = new PrayerTimesCalculator(location);

        prayerTimesCalculator.calculateSolarPositions();
    }
}

/**
 * Holds user's geographic data
 */
class Location {
    private static class Result {
        double latitude;
        double longitude;
        String timezone;
    }

    private static class Response {
        List<Result> results;
    }

    public double latitude;
    public double longitude;
    public String timezone;
    public String cityName;

    public Location(String city){
        IO.println("Your city: " + city);
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

        Gson gson = new Gson();

        // encoding for safe URL usage
        String encodedCity = URLEncoder.encode(this.cityName, StandardCharsets.UTF_8);

        // prepare API request
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedCity
                + "&count=1";

        // create HttpClient
        try (HttpClient client = HttpClient.newHttpClient()) {
            IO.println("Getting coordinates of " + this.cityName + "...");

            // build Http GET request for API
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            // Send request and get response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


            // assign results
            if (response.statusCode() == 200) {
                Response responseJson = gson.fromJson(response.body(), Response.class);

                this.latitude = responseJson.results.getFirst().latitude;
                this.longitude = responseJson.results.getFirst().longitude;
                this.timezone = responseJson.results.getFirst().timezone;
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

    public String getTimezone() {
        return timezone;
    }

    public String getCityName() {
        return cityName;
    }

    @Override
    public String toString() {
        return "Latitude: " + getLatitude() +
                "\nLongitude: " + getLongitude() +
                "\nTimezone: " + getTimezone();
    }
}

/**
 * Applies prayer time formulas to solar data
 */
class PrayerTimesCalculator {
    public double fajrAngle = -15.0;
    public double ishaAngle = -15.0;
    public Location location;
    public SolarCalculator solarCalc;

    public PrayerTimesCalculator(Location location){
        this.location = location;
        IO.println("Made a PrayerTimesCalculator with location:\n" + this.location);
        this.solarCalc = new SolarCalculator(this.location);
    }

    public void calculateSolarPositions(){
        double sunriseTime = this.solarCalc.getSunrise();
        IO.println("sunrise: " + sunriseTime);
    }

    @Override
    public String toString() {
        return "Prayer Times Calculator\n" +
                "fajrAngle: " + this.fajrAngle +
                "\nishaAngle: " + this.ishaAngle +
                "\nlocation: " + this.location +
                "\nSolarCalculator: " + this.solarCalc;
    }
}

/**
 * Computes sun position and solar events
 */
class SolarCalculator {
    public Location location;
    public Calendar localDate = new GregorianCalendar();
    public int dayOfYear;
    public int hour;

    public SolarCalculator(Location location){
        this.location = location;
        this.localDate.setTime(new Date());
        this.dayOfYear = localDate.get(Calendar.DAY_OF_YEAR);
        this.hour = localDate.get(Calendar.HOUR);
    }

    public double getSolarZenith(){
        double y = fractionalYear(this.localDate);
        double eqtime = eqTime(y);
        double declination = decl(y);
        double timeOffset = timeOffset(eqtime, this.location, this.location.timezone);

        return 0.0;
    }

    public double getSunrise() {
        double y = fractionalYear(this.localDate);
        double eqtime = eqTime(y);
        double declination = decl(y);
        double latRadian = Math.toRadians(this.location.getLatitude());

        double hourAngle = Math.acos(Math.cos(Math.toRadians(90.833))
                / (Math.cos(latRadian) * Math.cos(declination))
                - (Math.tan(latRadian) * Math.tan(declination)));

        IO.println("DEBUG: hourAngle = " + hourAngle);

        // convert to degrees for timeOfSunrise calculation
        hourAngle = Math.toDegrees(hourAngle);

        // in minutes
        double utcTimeOfSunrise = 720 - 4 * (this.location.getLongitude() + hourAngle) - eqtime;

        return utcTimeOfSunrise / 60;
    }

    private double fractionalYear(Calendar localDate){
        /*
         *   fractional year calculation, in radians
         *   numerator with hour variable is cast to double to match return type
         */
        double fractionalYear = ((2 * Math.PI) / 365) * (dayOfYear - 1 + ((double) (hour - 12) / 24));
        IO.println("DEBUG: fractional year = " + fractionalYear);
        return fractionalYear;
    }

    public double eqTime(double y) {
        /*
         *   equation of time in minutes
         */
        double eqTime =  229.18 *
                (0.000075 +
                        (0.001868 * Math.cos(y)) -
                        (0.032077 * Math.sin(y)) -
                        (0.014615 * Math.cos(2 * y)) -
                                (0.040849 * Math.sin(2 * y)));
        IO.println("DEBUG: equation of time = " + eqTime);
        return eqTime;
    }

    public double decl(double y) {
        /*
         *   solar declination angle in radians
         */
        double decl = 0.006918 -
                (0.399912 * Math.cos(y)) +
                (0.070257 * Math.sin(y)) -
                (0.006758 * Math.cos(2 * y)) +
                (0.000907 * Math.sin(2 * y)) -
                (0.002697 * Math.cos(3 * y)) +
                (0.00148 * Math.sin(3 * y));
        IO.println("DEBUG: declination = " + decl);
        return decl;
    }

    public double timeOffset(double eqTime, Location location, String timezone){
        // convert timezone name to number offset
        ZoneId zone = ZoneId.of(timezone);
        double offset = (double) ZonedDateTime.now(zone).getOffset().getTotalSeconds() / 3600;

        double timeOffset = eqTime + 4 * location.longitude - 60 * offset;
        IO.println("DEBUG: time_offset = " + timeOffset);
        return timeOffset;
    }

    public double trueSolarTime(){ return 0.0; }

    public String toString() {
        return "\nlocation: " + this.location +
        "\ndate: " + this.localDate +
        "\nday: " + this.dayOfYear +
        "\nhour: " + this.hour;
    }
}

/**
 * Stores the 5 calculated prayer times
 */
class PrayerSchedule {

}
