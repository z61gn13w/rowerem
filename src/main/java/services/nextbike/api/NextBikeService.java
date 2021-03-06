package services.nextbike.api;

import com.google.gson.*;
import services.JSONTransformer;
import services.nextbike.api.structure.*;
import travel.RouteFromClient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * NextBike API proxy class.
 */
public class NextBikeService {

    /**
     * JSON data URL.
     */
    private String url = "https://nextbike.net/maps/nextbike-official.json";
    /**
     * Root of the data.
     */
    private Root root;

    /**
     * Construct NetBike proxy. Pass JSON data to Root object.
     * @throws IOException Input-Output Exception
     */
    public NextBikeService() throws IOException {

        System.out.println("Start of building NextBikeAPI");

        JSONTransformer jsonTransformer = new JSONTransformer();

        InputStream is = new URL(url).openStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        String jsonText = jsonTransformer.readAll(rd);

        Gson gson = new GsonBuilder().serializeNulls().create();
        root = gson.fromJson(jsonText, Root.class);

        System.out.println("End of building NextBikeAPI");
    }

    public NextBikeService(String filename) throws IOException {
        JSONTransformer jsonTransformer = new JSONTransformer();

        InputStream is = new FileInputStream(filename);
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        String jsonText = jsonTransformer.readAll(rd);

        Gson gson = new GsonBuilder().serializeNulls().create();
        root = gson.fromJson(jsonText, Root.class);
    }
    /**
     * Set city the route is located in. Search in database for name of country and name of city given.
     * @param country_name Name of the Country user is in.
     * @param city_name Name of the City user is in.
     * @return Found city.
     */
    public City findCity(String country_name, String city_name) {
        for(Country country : getRoot().countries)
            if (country.getCountry_shortname().equals(country_name)) {
                for (City city : country.getCities())
                    if (city.getName().equals(city_name)) {
                        return city;
                    }
            }
        return null;
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     * @param lat1 Start point latitude.
     * @param lat2 End point latitude.
     * @param lon1 Start point longitude.
     * @param lon2 End point longitude.
     * @param el1 Start altitude in meters.
     * @param el2 End altitude in meters.
     * @return Distance in Meters.
     */
    public double distance(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    /**
     * Finds the closest Station near the Place passed as argument.
     * @param city City in which we're looking for a station.
     * @param place Place to find the closest station to.
     * @return Closest station to passed Place.
     */
    public Station findClosest(City city, Place place) {
        if(city.getStations().isEmpty()) return null;
        Station closestStation = city.getStations().get(0);
        for(Station station : city.getStations()) {
            if(distance(place.getLat(), station.getLat(), place.getLng(), station.getLng(), 0.0, 0.0)
                    < distance(place.getLat(), closestStation.getLat(), place.getLng(), closestStation.getLng(), 0.0, 0.0)) {
                closestStation = station;
            }
        }
        return closestStation;
    }

    /**
     * Find the Stations closest to the origin and destination of passed RouteFromClient.
     * @param route Processed RouteFromClient.
     */
    public void findStations(RouteFromClient route) {
        route.setCity(findCity("PL","Poznań"));
        if(route.getCity().getStations().isEmpty()) return;
        route.setStartStation(route.getCity().getStations().get(0));
        route.setEndStation(route.getCity().getStations().get(0));
        for(Station station : route.getCity().getStations()) {
            if(distance(route.getOriginLat(), station.getLat(), route.getOriginLng(), station.getLng(), 0.0, 0.0)
                    < distance(route.getOriginLat(), route.getStartStation().getLat(), route.getOriginLng(), route.getStartStation().getLng(), 0.0, 0.0)) {
                route.setStartStation(station); //TODO tutaj mółgbym użyć funkcji find closest ale optymalniej jest z jedną pętlą niż z dwoma.
            }
            if(distance(route.getDestinationLat(), station.getLat(), route.getDestinationLng(), station.getLng(), 0.0, 0.0)
                    < distance(route.getDestinationLat(), route.getEndStation().getLat(), route.getDestinationLng(), route.getEndStation().getLng(), 0.0, 0.0)) {
                route.setEndStation(station);
            }
        }
    }

    public Root getRoot() {
        return root;
    }
}
