package course.project.ua.tirevault.Repositories;

import course.project.ua.tirevault.Entities.Models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface IProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByIdAsc();
    List<Product> findByCategoryIdOrderByIdAsc(Long categoryId);

    @Query("SELECT DISTINCT p FROM Product p " +
            "WHERE (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:availability IS NULL OR p.availability = :availability) " +
            "AND (:brand IS NULL OR EXISTS (" +
            "    SELECT v FROM p.vehicles v " +
            "    WHERE v.brand = :brand " +
            "    AND (:vmodel IS NULL OR v.model = :vmodel) " +
            "    AND (:year IS NULL OR v.year = :year) " +
            "    AND (:modification IS NULL OR v.modification = :modification)" +
            ")) ORDER BY p.id ASC")
    List<Product> findFiltered(
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("availability") Boolean availability,
            @Param("brand") String brand,
            @Param("vmodel") String vmodel,
            @Param("year") Integer year,
            @Param("modification") String modification);

    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.name)        LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%'))
           OR EXISTS (SELECT v FROM p.vehicles v WHERE LOWER(v.brand) LIKE LOWER(CONCAT('%', :q, '%')))
           OR CAST(p.price AS string) LIKE CONCAT('%', :q, '%')
        ORDER BY p.name
        """)
    List<Product> searchByKeyword(@Param("q") String q);
}