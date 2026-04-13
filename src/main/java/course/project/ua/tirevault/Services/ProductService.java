package course.project.ua.tirevault.Services;

import course.project.ua.tirevault.Entities.Models.Product;
import course.project.ua.tirevault.Entities.Models.ProductCategory;
import course.project.ua.tirevault.Entities.Models.Vehicle;
import course.project.ua.tirevault.Repositories.IProductCategoryRepository;
import course.project.ua.tirevault.Repositories.IProductRepository;
import course.project.ua.tirevault.Repositories.IVehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductCategoryRepository categoryRepository;

    @Autowired
    private IVehicleRepository vehicleRepository;

    public List<ProductCategory> getAllCategories() {
        return categoryRepository.findAllByOrderByIdAsc();
    }

    public Optional<ProductCategory> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderByIdAsc();
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdOrderByIdAsc(categoryId);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public void saveCategory(ProductCategory category) {
        categoryRepository.save(category);
    }

    public void saveProduct(Product product) {
        productRepository.save(product);
    }

    public void deleteProductById(Long id) {
        productRepository.deleteById(id);
    }

    public void deleteCategoryById(Long id) {
        categoryRepository.deleteById(id);
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAllByOrderByBrandAscModelAscYearAsc();
    }

    public List<Product> getFilteredProducts(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Boolean availability, String brand, String vmodel, Integer year, String modification) {
        return productRepository.findFiltered(categoryId, minPrice, maxPrice, availability, brand, vmodel, year, modification);
    }
}