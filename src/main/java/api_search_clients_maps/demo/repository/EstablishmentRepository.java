package api_search_clients_maps.demo.repository;

import api_search_clients_maps.demo.model.Establishment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface EstablishmentRepository extends JpaRepository<Establishment, Long> {

	boolean existsByGooglePlaceId(String googlePlaceId);

	@Query("SELECT e.googlePlaceId FROM Establishment e")
	Set<String> findAllGooglePlaceIds();

	@Query("""
			SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
			FROM Establishment e
			WHERE LOWER(TRIM(e.nome)) = LOWER(TRIM(:nome))
			AND LOWER(TRIM(e.endereco)) = LOWER(TRIM(:endereco))
			AND LOWER(TRIM(e.cidade)) = LOWER(TRIM(:cidade))
			AND LOWER(TRIM(e.estado)) = LOWER(TRIM(:estado))
			""")
	boolean existsByNomeAndEnderecoCompleto(
			@Param("nome") String nome,
			@Param("endereco") String endereco,
			@Param("cidade") String cidade,
			@Param("estado") String estado);
}
