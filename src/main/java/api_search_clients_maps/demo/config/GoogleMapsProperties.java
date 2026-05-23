package api_search_clients_maps.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.maps")
public class GoogleMapsProperties {

	private String apiKey = "";
	private double defaultLatitude = -23.5505;
	private double defaultLongitude = -46.6333;
	private int defaultRadiusMeters = 5000;
	private String searchLocation = "São Paulo, SP";
	private int maxPlacesPerSearch = 20;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public double getDefaultLatitude() {
		return defaultLatitude;
	}

	public void setDefaultLatitude(double defaultLatitude) {
		this.defaultLatitude = defaultLatitude;
	}

	public double getDefaultLongitude() {
		return defaultLongitude;
	}

	public void setDefaultLongitude(double defaultLongitude) {
		this.defaultLongitude = defaultLongitude;
	}

	public int getDefaultRadiusMeters() {
		return defaultRadiusMeters;
	}

	public void setDefaultRadiusMeters(int defaultRadiusMeters) {
		this.defaultRadiusMeters = defaultRadiusMeters;
	}

	public String getSearchLocation() {
		return searchLocation;
	}

	public void setSearchLocation(String searchLocation) {
		this.searchLocation = searchLocation;
	}

	public int getMaxPlacesPerSearch() {
		return maxPlacesPerSearch;
	}

	public void setMaxPlacesPerSearch(int maxPlacesPerSearch) {
		this.maxPlacesPerSearch = maxPlacesPerSearch;
	}

}
