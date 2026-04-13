package course.project.ua.tirevault.Repositories;

import course.project.ua.tirevault.Entities.Models.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IVehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findAllByOrderByBrandAscModelAscYearAsc();
}