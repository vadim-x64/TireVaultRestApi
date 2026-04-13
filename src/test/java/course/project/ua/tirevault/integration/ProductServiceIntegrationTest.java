package course.project.ua.tirevault.integration;

import course.project.ua.tirevault.Entities.Models.*;
import course.project.ua.tirevault.Repositories.*;
import course.project.ua.tirevault.Services.ProductService;
import course.project.ua.tirevault.Services.WorkServiceManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceIntegrationTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private WorkServiceManager workServiceManager;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductCategoryRepository productCategoryRepository;

    @Autowired
    private IWorkServiceRepository workServiceRepository;

    @Autowired
    private IWorkServiceCategoryRepository workServiceCategoryRepository;

    private ProductCategory productCategory;

    private WorkServiceCategory serviceCategory;

    @BeforeEach
    void setUp() {
        productCategory = new ProductCategory();
        productCategory.setName("Масла");
        productCategory = productCategoryRepository.save(productCategory);

        Product product = new Product();
        product.setName("Моторна олива 5W-30");
        product.setArticle("OIL-001");
        product.setAvailability(true);
        product.setQuantity(5);
        product.setPrice(new BigDecimal("300.00"));
        product.setCategory(productCategory);
        productRepository.save(product);

        serviceCategory = new WorkServiceCategory();
        serviceCategory.setName("Діагностика");
        serviceCategory = workServiceCategoryRepository.save(serviceCategory);

        WorkService ws = new WorkService();
        ws.setName("Комп'ютерна діагностика");
        ws.setPrice(new BigDecimal("500.00"));
        ws.setCategory(serviceCategory);
        workServiceRepository.save(ws);
    }

    @Test
    void getAllProducts_shouldReturnProducts() {
        assertFalse(productService.getAllProducts().isEmpty());
    }

    @Test
    void getProductsByCategory_shouldReturnFilteredProducts() {
        List<Product> products = productService.getProductsByCategory(productCategory.getId());
        assertFalse(products.isEmpty());
        assertEquals("Масла", products.get(0).getCategory().getName());
    }

    @Test
    void getProductById_shouldReturnEmpty_whenNotFound() {
        Optional<Product> product = productService.getProductById(999999L);
        assertTrue(product.isEmpty());
    }

    @Test
    void getAllWorkServices_shouldReturnServices() {
        assertFalse(workServiceManager.getAllWorkServices().isEmpty());
    }

    @Test
    void getWorkServicesByCategory_shouldReturnFiltered() {
        List<WorkService> services = workServiceManager.getWorkServicesByCategory(serviceCategory.getId());
        assertFalse(services.isEmpty());
    }

    @Test
    void getWorkServiceById_shouldReturnEmpty_whenNotFound() {
        Optional<WorkService> ws = workServiceManager.getWorkServiceById(999999L);
        assertTrue(ws.isEmpty());
    }
}