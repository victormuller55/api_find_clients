package api_search_clients_maps.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "establishments", uniqueConstraints = @UniqueConstraint(columnNames = "google_place_id"))
public class Establishment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "google_place_id", nullable = false, length = 128)
	private String googlePlaceId;

	@Column(nullable = false)
	private String nome;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String endereco;

	@Column(nullable = false, length = 128, columnDefinition = "varchar(128) not null default ''")
	private String cidade = "";

	@Column(nullable = false, length = 64, columnDefinition = "varchar(64) not null default ''")
	private String estado = "";

	@Column(length = 64)
	private String telefone;

	@Column(length = 512)
	private String website;

	@Column(nullable = false, length = 128)
	private String categoria;

	@Column
	private Double latitude;

	@Column
	private Double longitude;

	@Column(name = "maps_url", length = 1024)
	private String mapsUrl;

	@Column(name = "termo_busca", length = 256)
	private String termoBusca;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	public Establishment() {
	}

	public Establishment(
			String googlePlaceId,
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
		this.googlePlaceId = googlePlaceId;
		this.nome = nome;
		this.endereco = endereco;
		this.cidade = cidade;
		this.estado = estado;
		this.telefone = telefone;
		this.website = website;
		this.categoria = categoria;
		this.latitude = latitude;
		this.longitude = longitude;
		this.mapsUrl = mapsUrl;
		this.termoBusca = termoBusca;
	}

	public Long getId() {
		return id;
	}

	public String getGooglePlaceId() {
		return googlePlaceId;
	}

	public String getNome() {
		return nome;
	}

	public String getEndereco() {
		return endereco;
	}

	public String getCidade() {
		return cidade;
	}

	public String getEstado() {
		return estado;
	}

	public String getTelefone() {
		return telefone;
	}

	public String getWebsite() {
		return website;
	}

	public String getCategoria() {
		return categoria;
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public String getMapsUrl() {
		return mapsUrl;
	}

	public String getTermoBusca() {
		return termoBusca;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
