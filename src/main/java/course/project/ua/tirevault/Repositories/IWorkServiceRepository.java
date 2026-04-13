package course.project.ua.tirevault.Repositories;

import course.project.ua.tirevault.Entities.Models.WorkService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IWorkServiceRepository extends JpaRepository<WorkService, Long> {
    List<WorkService> findAllByOrderByIdAsc();
    List<WorkService> findByCategoryIdOrderByIdAsc(Long categoryId);

    @Query("""
        SELECT s FROM WorkService s
        WHERE LOWER(s.name)        LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(s.description) LIKE LOWER(CONCAT('%', :q, '%'))
           OR CAST(s.price AS string) LIKE CONCAT('%', :q, '%')
        ORDER BY s.name
        """)
    List<WorkService> searchByKeyword(@Param("q") String q);
}