package api_search_clients_maps.demo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressParserTest {

	@Test
	void parsesMultilineAddress() {
		var parsed = AddressParser.parse("R. da Consolação, 327 - Consolação\nSão Paulo - SP\n01301-000");
		assertEquals("R. da Consolação, 327 - Consolação", parsed.endereco());
		assertEquals("São Paulo", parsed.cidade());
		assertEquals("SP", parsed.estado());
	}

	@Test
	void parsesCommaSeparatedAddress() {
		var parsed = AddressParser.parse("R. Maj. Sertório, 82 - República, São Paulo - SP, 01219-011");
		assertEquals("R. Maj. Sertório, 82 - República", parsed.endereco());
		assertEquals("São Paulo", parsed.cidade());
		assertEquals("SP", parsed.estado());
	}
}
