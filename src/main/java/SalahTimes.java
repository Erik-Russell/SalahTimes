import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import com.google.gson.Gson;

/**
 * SalahTimes
 */
public class SalahTimes {
    static void main()
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
        PrayerSchedule prayerSchedule = prayerTimesCalculator.calculateAll();

        IO.println(prayerSchedule);


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
        //IO.println("Made a PrayerTimesCalculator with location:\n" + this.location);
        this.solarCalc = new SolarCalculator(this.location);

    }

    public PrayerSchedule calculateAll(){
        return new PrayerSchedule(
                calcFajr(),
                calcDhuhr(),
                calcAsr(),
                calcMaghrib(),
                calcIsha()
        );
    }

    public LocalTime calcFajr(){
        return this.solarCalc.getSunTimeOf(105, false);
    }
    public LocalTime calcDhuhr(){
        return this.solarCalc.getSolarNoon();
    }
    public LocalTime calcAsr(){
        return this.solarCalc.getAsr();
    }
    public LocalTime calcMaghrib(){
        return this.solarCalc.getSunTimeOf(90.833, true);
    }
    public LocalTime calcIsha(){
        return this.solarCalc.getSunTimeOf(105, true);
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

    // convert timezone name to number offset
    public double getTimeZoneDouble(String timezone){
        ZoneId zone = ZoneId.of(timezone);
        return (double) ZonedDateTime.now(zone).getOffset().getTotalSeconds() / 3600;
    }

    public LocalTime minutesToTime(double solarMinutes){
        int hours = (int) solarMinutes / 60;
        int minutes = (int) (solarMinutes % 60);
        int seconds = (int) ((solarMinutes * 60) % 60);

        return LocalTime.of(hours,minutes,seconds);
    }

    public LocalTime getSolarNoon() {
        double offset = getTimeZoneDouble(this.location.timezone) * 60;
        double longitude = this.location.longitude;
        double equationOfTime = eqTime(fractionalYear());

        double timeOfNoon = 720 - 4 * longitude - equationOfTime;

        return minutesToTime(timeOfNoon + offset);
    }

    public LocalTime getSunTimeOf(double degreesFromZenith, boolean isAfternoon) {
        double y = fractionalYear();
        double equationOfTime = eqTime(y);
        double declination = decl(y);
        double latRadian = Math.toRadians(this.location.getLatitude());
        double offset = getTimeZoneDouble(this.location.timezone) * 60; //hour to minutes

        double hourAngle = Math.acos(Math.cos(Math.toRadians(degreesFromZenith))
                / (Math.cos(latRadian) * Math.cos(declination))
                - (Math.tan(latRadian) * Math.tan(declination)));

        // convert to degrees for timeOf calculation
        hourAngle = Math.toDegrees(hourAngle);

        if (isAfternoon){
            hourAngle = -hourAngle;
        }

        // in minutes
        double utcTimeOf = 720 - 4 * (this.location.getLongitude() + hourAngle) - equationOfTime;

        return minutesToTime(utcTimeOf + offset);
    }

    public LocalTime getAsr(){
        double y = fractionalYear();
        double declination = decl(y);
        double latRadian = Math.toRadians(this.location.getLatitude());

        double zenithNoon = Math.abs(Math.toDegrees(latRadian) - Math.toDegrees(declination));
        double alphaNoon = Math.toRadians(90.0 - zenithNoon);
        double alphaAsr = Math.atan(1.0 / (1.0 + (1.0 / Math.tan(alphaNoon))));

        double cosHa = (Math.sin(alphaAsr) - Math.sin(latRadian) * Math.sin(declination))
                / (Math.cos(latRadian) * Math.cos(declination));

        double timeFromNoon = Math.toDegrees(Math.acos(cosHa)) / 15.0;
        long totalSeconds = Math.round(timeFromNoon * 3600);
        return getSolarNoon().plusSeconds(totalSeconds);
    }

    private double fractionalYear(){
        /*
         *   fractional year calculation, in radians
         *   numerator with hour variable is cast to double to match return type
         */
        return ((2 * Math.PI) / 365) * (dayOfYear - 1 + ((double) (hour - 12) / 24));
    }

    public double eqTime(double y) {
        /*
         *   equation of time in minutes
         */
        return 229.18 *
                (0.000075 +
                        (0.001868 * Math.cos(y)) -
                        (0.032077 * Math.sin(y)) -
                        (0.014615 * Math.cos(2 * y)) -
                                (0.040849 * Math.sin(2 * y)));
    }

    public double decl(double y) {
        /*
         *   solar declination angle in radians
         */
        return 0.006918 -
                (0.399912 * Math.cos(y)) +
                (0.070257 * Math.sin(y)) -
                (0.006758 * Math.cos(2 * y)) +
                (0.000907 * Math.sin(2 * y)) -
                (0.002697 * Math.cos(3 * y)) +
                (0.00148 * Math.sin(3 * y));
    }

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
    private final Map<String, LocalTime> prayers;

    public PrayerSchedule(LocalTime fajr, LocalTime dhuhr, LocalTime asr,
                          LocalTime maghrib, LocalTime isha){
        prayers = new LinkedHashMap<>();
        prayers.put("Fajr", fajr);
        prayers.put("Dhuhr", dhuhr);
        prayers.put("Asr", asr);
        prayers.put("Maghrib", maghrib);
        prayers.put("Isha", isha);
    }

    public LocalTime getPrayer(String prayer){
        return prayers.get(prayer);
    }

    public Map<String, LocalTime> getAllPrayers(){
        return prayers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Prayer Schedule\n");
        sb.append("---------------\n");
        for (Map.Entry<String, LocalTime> entry : prayers.entrySet()) {
            sb.append(String.format("%-10s %s%n", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }
}
