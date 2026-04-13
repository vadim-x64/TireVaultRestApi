package course.project.ua.tirevault.Repositories;

import course.project.ua.tirevault.Entities.Models.WorkServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IWorkServiceCategoryRepository extends JpaRepository<WorkServiceCategory, Long> {
    List<WorkServiceCategory> findAllByOrderByIdAsc();
}