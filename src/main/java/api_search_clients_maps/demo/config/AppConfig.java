package api_search_clients_maps.demo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GoogleMapsProperties.class)
public class AppConfig {

	@Bean
	public RestClient googlePlacesRestClient() {
		return RestClient.builder()
				.baseUrl("https://places.googleapis.com/v1")
				.build();
	}
}
