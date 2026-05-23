package api_search_clients_maps.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.maps")
public class GoogleMapsProperties {

	private double defaultLatitude = -23.5505;
	private double defaultLongitude = -46.6333;
	private int defaultRadiusMeters = 5000;
	private String searchLocation = "São Paulo, SP";
	private int maxPlacesPerSearch = 8;
	private int maxScrollAttempts = 3;
	private int actionTimeoutMs = 12_000;
	private int pauseBetweenActionsMs = 300;
	private boolean headless = true;

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

	public int getMaxScrollAttempts() {
		return maxScrollAttempts;
	}

	public void setMaxScrollAttempts(int maxScrollAttempts) {
		this.maxScrollAttempts = maxScrollAttempts;
	}

	public int getActionTimeoutMs() {
		return actionTimeoutMs;
	}

	public void setActionTimeoutMs(int actionTimeoutMs) {
		this.actionTimeoutMs = actionTimeoutMs;
	}

	public int getPauseBetweenActionsMs() {
		return pauseBetweenActionsMs;
	}

	public void setPauseBetweenActionsMs(int pauseBetweenActionsMs) {
		this.pauseBetweenActionsMs = pauseBetweenActionsMs;
	}

	public boolean isHeadless() {
		return headless;
	}

	public void setHeadless(boolean headless) {
		this.headless = headless;
	}
}
