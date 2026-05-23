package api_search_clients_maps.demo.client;

import api_search_clients_maps.demo.config.GoogleMapsProperties;
import api_search_clients_maps.demo.util.AddressParser;
import api_search_clients_maps.demo.util.AddressParser.ParsedAddress;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GoogleMapsPlaywrightClient {

	private static final Logger log = LoggerFactory.getLogger(GoogleMapsPlaywrightClient.class);

	private static final int VIEWPORT_WIDTH = 1400;
	private static final int VIEWPORT_HEIGHT = 900;
	private static final int PANEL_WAIT_MS = 4_000;

	private static final Pattern COORD_FROM_URL_AT = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
	private static final Pattern COORD_FROM_URL_3D4D = Pattern.compile("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)");

	private static final String[] COOKIE_BUTTON_TEXTS = {
			"Aceitar tudo", "Accept all", "Recusar tudo", "Reject all"
	};

	private static final String[] NAME_SELECTORS = {
			"h1.DUwDvf", "h1[class*='fontHeadline']", "h1"
	};

	private static final String[] CATEGORY_SELECTORS = {
			"button.DkEaL", "button[jsaction*='category']"
	};

	private static final String[] ADDRESS_SELECTORS = {
			"button[data-item-id='address']", "button[data-item-id^='address']"
	};

	private static final String[] PHONE_SELECTORS = {
			"button[data-item-id^='phone:tel']", "button[data-item-id*='phone']"
	};

	private static final String[] WEBSITE_SELECTORS = {
			"a[data-item-id='authority']",
			"a[aria-label*='Site']",
			"a[aria-label*='site']",
			"a[aria-label*='Website']",
			"a[href^='http']:has-text('Site')"
	};

	private final GoogleMapsProperties properties;

	public GoogleMapsPlaywrightClient(GoogleMapsProperties properties) {
		this.properties = properties;
	}

	public List<PlaceDetails> searchAll(
			Map<String, String> termToCategory,
			String location,
			Set<String> skipPlaceIds) {

		List<PlaceDetails> results = new ArrayList<>();
		Set<String> skip = skipPlaceIds != null ? skipPlaceIds : Set.of();

		try (Playwright playwright = Playwright.create()) {
			Browser browser = playwright.chromium().launch(
					new BrowserType.LaunchOptions().setHeadless(properties.isHeadless()));
			BrowserContext context = browser.newContext(new Browser.NewContextOptions()
					.setLocale("pt-BR")
					.setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT));
			blockHeavyResources(context);
			Page page = context.newPage();
			page.setDefaultTimeout(properties.getActionTimeoutMs());

			try {
				boolean dismissCookies = true;
				for (Map.Entry<String, String> entry : termToCategory.entrySet()) {
					String query = entry.getKey() + " perto de " + location;
					String defaultCategory = entry.getValue();
					results.addAll(scrapeSearchTerm(page, query, defaultCategory, skip, dismissCookies));
					dismissCookies = false;
					pause(properties.getPauseBetweenActionsMs());
				}
			}
			finally {
				context.close();
				browser.close();
			}
		}
		catch (Exception e) {
			log.error("Erro no Playwright: {}", e.getMessage());
		}

		return results;
	}

	private void blockHeavyResources(BrowserContext context) {
		context.route("**/*", route -> {
			String type = route.request().resourceType();
			if ("image".equals(type) || "font".equals(type) || "media".equals(type)) {
				route.abort();
			}
			else {
				route.resume();
			}
		});
	}

	private List<PlaceDetails> scrapeSearchTerm(
			Page page,
			String query,
			String defaultCategory,
			Set<String> skipPlaceIds,
			boolean firstSearch) {

		List<PlaceDetails> results = new ArrayList<>();
		Set<String> placeUrls = collectPlaceUrls(page, query, firstSearch);
		log.info("Busca '{}': {} links (pulando {} já no banco)", query, placeUrls.size(), skipPlaceIds.size());

		int processed = 0;
		for (String placeUrl : placeUrls) {
			if (processed >= properties.getMaxPlacesPerSearch()) {
				break;
			}
			String placeId = buildPlaceId(placeUrl);
			if (skipPlaceIds.contains(placeId)) {
				continue;
			}
			extractPlaceDetailsFromFeed(page, placeUrl, query, defaultCategory).ifPresent(details -> {
				results.add(details);
				skipPlaceIds.add(details.placeId());
			});
			processed++;
			pause(250);
		}
		return results;
	}

	private Set<String> collectPlaceUrls(Page page, String query, boolean dismissCookies) {
		Set<String> urls = new LinkedHashSet<>();
		String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
		String searchUrl = "https://www.google.com/maps/search/" + encoded + "?hl=pt-BR";

		page.navigate(searchUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
		pause(400);
		if (dismissCookies) {
			dismissCookiePopup(page);
		}

		try {
			page.waitForSelector("a.hfpxzc", new Page.WaitForSelectorOptions().setTimeout(12_000));
		}
		catch (Exception e) {
			log.warn("Lista de resultados não apareceu para '{}'", query);
			return urls;
		}

		int previousSize = 0;
		for (int scroll = 0; scroll < properties.getMaxScrollAttempts(); scroll++) {
			collectVisiblePlaceLinks(page, urls);
			if (urls.size() >= properties.getMaxPlacesPerSearch()) {
				break;
			}
			if (urls.size() == previousSize && scroll > 0) {
				break;
			}
			previousSize = urls.size();
			scrollResultsFeed(page);
			pause(500);
		}

		return urls;
	}

	private Optional<PlaceDetails> extractPlaceDetailsFromFeed(
			Page page, String placeUrl, String searchTerm, String defaultCategory) {

		if (!clickPlaceInFeed(page, placeUrl)) {
			return extractPlaceDetailsByNavigate(page, placeUrl, searchTerm, defaultCategory);
		}

		try {
			page.waitForSelector("h1", new Page.WaitForSelectorOptions().setTimeout(PANEL_WAIT_MS));
		}
		catch (Exception ignored) {
			// segue com o que carregou
		}
		pause(350);
		return buildPlaceDetails(page, placeUrl, searchTerm, defaultCategory);
	}

	private Optional<PlaceDetails> extractPlaceDetailsByNavigate(
			Page page, String placeUrl, String searchTerm, String defaultCategory) {

		page.navigate(placeUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
		try {
			page.waitForSelector("h1", new Page.WaitForSelectorOptions().setTimeout(PANEL_WAIT_MS));
		}
		catch (Exception ignored) {
			// segue
		}
		pause(350);
		return buildPlaceDetails(page, placeUrl, searchTerm, defaultCategory);
	}

	private Optional<PlaceDetails> buildPlaceDetails(
			Page page, String placeUrl, String searchTerm, String defaultCategory) {

		String nome = firstNonBlankText(page, NAME_SELECTORS).orElse(null);
		if (nome == null || nome.isBlank()) {
			return Optional.empty();
		}

		String website = firstWebsiteHref(page);
		String categoria = firstNonBlankText(page, CATEGORY_SELECTORS).orElse(defaultCategory);
		ParsedAddress address = AddressParser.parse(firstAddressRaw(page).orElse(""));
		String telefone = firstPhone(page).orElse(null);
		Coordinates coords = extractCoordinates(page.url(), placeUrl).orElse(null);

		String placeId = buildPlaceId(placeUrl);
		Double lat = coords != null ? coords.latitude() : null;
		Double lng = coords != null ? coords.longitude() : null;

		return Optional.of(new PlaceDetails(
				placeId,
				nome,
				address.endereco(),
				address.cidade(),
				address.estado(),
				telefone,
				website,
				categoria,
				lat,
				lng,
				placeUrl,
				searchTerm));
	}

	private boolean clickPlaceInFeed(Page page, String placeUrl) {
		Locator links = page.locator("a.hfpxzc[href*='/maps/place']");
		int count = links.count();
		for (int i = 0; i < count; i++) {
			Locator link = links.nth(i);
			String href = link.getAttribute("href");
			if (href != null && normalizeMapsUrl(href).equals(placeUrl)) {
				link.click();
				return true;
			}
		}
		return false;
	}

	private void collectVisiblePlaceLinks(Page page, Set<String> urls) {
		Locator links = page.locator("a.hfpxzc[href*='/maps/place']");
		int count = links.count();
		for (int i = 0; i < count; i++) {
			String href = links.nth(i).getAttribute("href");
			if (href != null && !href.isBlank()) {
				urls.add(normalizeMapsUrl(href));
			}
		}
	}

	private void scrollResultsFeed(Page page) {
		page.evaluate("""
				() => {
				  const feed = document.querySelector('div[role="feed"]');
				  if (feed) feed.scrollTop = feed.scrollHeight;
				}
				""");
	}

	private void dismissCookiePopup(Page page) {
		for (String text : COOKIE_BUTTON_TEXTS) {
			Locator button = page.locator("button:has-text('" + text + "')").first();
			if (button.count() > 0 && button.isVisible()) {
				button.click();
				pause(400);
				return;
			}
		}
	}

	private String firstWebsiteHref(Page page) {
		for (String selector : WEBSITE_SELECTORS) {
			Locator locator = page.locator(selector).first();
			if (locator.count() == 0) {
				continue;
			}
			String href = locator.getAttribute("href");
			if (isValidWebsite(href)) {
				return href;
			}
		}
		return null;
	}

	private Optional<String> firstNonBlankText(Page page, String[] selectors) {
		for (String selector : selectors) {
			Locator locator = page.locator(selector).first();
			if (locator.count() == 0) {
				continue;
			}
			String text = locator.innerText().trim();
			if (!text.isBlank()) {
				return Optional.of(text);
			}
		}
		return Optional.empty();
	}

	private Optional<String> firstAddressRaw(Page page) {
		for (String selector : ADDRESS_SELECTORS) {
			Locator locator = page.locator(selector).first();
			if (locator.count() == 0) {
				continue;
			}
			String text = locator.innerText();
			if (text == null || text.isBlank()) {
				text = locator.getAttribute("aria-label");
			}
			if (text != null && !text.isBlank()) {
				return Optional.of(cleanIconPrefix(text.trim()));
			}
		}
		return Optional.empty();
	}

	private Optional<String> firstPhone(Page page) {
		for (String selector : PHONE_SELECTORS) {
			Locator locator = page.locator(selector).first();
			if (locator.count() == 0) {
				continue;
			}
			String text = locator.innerText().trim();
			if (text.isBlank()) {
				text = locator.getAttribute("aria-label");
				if (text != null) {
					text = text.trim();
				}
			}
			if (text != null && !text.isBlank()) {
				return Optional.of(cleanIconPrefix(text));
			}
		}
		return Optional.empty();
	}

	private Optional<Coordinates> extractCoordinates(String currentUrl, String placeUrl) {
		for (String url : List.of(currentUrl, placeUrl)) {
			if (url == null) {
				continue;
			}
			Matcher at = COORD_FROM_URL_AT.matcher(url);
			if (at.find()) {
				return Optional.of(new Coordinates(
						Double.parseDouble(at.group(1)),
						Double.parseDouble(at.group(2))));
			}
			Matcher d3d4 = COORD_FROM_URL_3D4D.matcher(url);
			if (d3d4.find()) {
				return Optional.of(new Coordinates(
						Double.parseDouble(d3d4.group(1)),
						Double.parseDouble(d3d4.group(2))));
			}
		}
		return Optional.empty();
	}

	private static String buildPlaceId(String placeUrl) {
		try {
			String path = URI.create(placeUrl).getPath();
			if (path != null && path.length() > 128) {
				return path.substring(0, 128);
			}
			return path != null ? path : placeUrl.substring(0, Math.min(128, placeUrl.length()));
		}
		catch (Exception e) {
			return placeUrl.substring(0, Math.min(128, placeUrl.length()));
		}
	}

	private static String normalizeMapsUrl(String href) {
		if (href.startsWith("http")) {
			return href;
		}
		return "https://www.google.com" + href;
	}

	private static boolean isValidWebsite(String href) {
		if (href == null || href.isBlank()) {
			return false;
		}
		String lower = href.toLowerCase();
		return (lower.startsWith("http://") || lower.startsWith("https://"))
				&& !lower.contains("google.com/maps")
				&& !lower.contains("google.com/search");
	}

	private static String cleanIconPrefix(String text) {
		if (text.length() > 1 && !Character.isLetterOrDigit(text.charAt(0))) {
			return text.substring(1).trim();
		}
		return text;
	}

	private void pause(int ms) {
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private record Coordinates(double latitude, double longitude) {
	}

	public record PlaceDetails(
			String placeId,
			String nome,
			String endereco,
			String cidade,
			String estado,
			String telefone,
			String website,
			String categoria,
			Double latitude,
			Double longitude,
			String mapsUrl,
			String termoBusca) {
	}
}
