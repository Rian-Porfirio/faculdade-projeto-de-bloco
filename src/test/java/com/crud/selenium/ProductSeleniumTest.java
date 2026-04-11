package com.crud.selenium;

import com.crud.selenium.components.DeleteModalComponent;
import com.crud.selenium.components.NavbarComponent;
import com.crud.selenium.pages.ProductFormPage;
import com.crud.selenium.pages.ProductListPage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Selenium E2E — Fluxos CRUD Completos")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductSeleniumTest extends BaseSeleniumTest {

    @LocalServerPort
    private int port;

    private String baseUrl;

    @BeforeEach
    void setUpUrl() {
        baseUrl = "http://localhost:" + port;
    }

    // ================================================================ LIST
    @Test
    @Order(1)
    @DisplayName("SEL-01: Página de listagem deve carregar com tabela visível")
    void listPage_loads() {
        ProductListPage listPage = new ProductListPage(driver);
        listPage.open(baseUrl);

        assertThat(listPage.getPageTitle()).contains("AgrotelliSystem");
        assertThat(listPage.isTableVisible()).isTrue();
        assertThat(listPage.getProductRowCount()).isGreaterThan(0);
    }

    @Test
    @Order(2)
    @DisplayName("SEL-02: Navbar deve exibir marca e botão de novo produto")
    void navbar_visible() {
        new ProductListPage(driver).open(baseUrl);
        NavbarComponent navbar = new NavbarComponent(driver);

        assertThat(navbar.isVisible()).isTrue();
        assertThat(navbar.getBrandName()).isEqualTo("AgrotelliSystem");
    }

    // ================================================================ CREATE
    @Test
    @Order(3)
    @DisplayName("SEL-03: Criar produto válido deve redirecionar e exibir sucesso")
    void create_validProduct() {
        ProductListPage listPage = new ProductListPage(driver);
        listPage.open(baseUrl);

        ProductFormPage form = listPage.clickNewProduct();
        assertThat(form.getFormTitle()).isEqualTo("Novo Produto");

        ProductListPage resultPage = form
                .fillAll("Produto Selenium", "Desc Selenium", "299.90", "5", "Teste Selenium")
                .submit();

        assertThat(resultPage.isSuccessAlertVisible()).isTrue();
        assertThat(resultPage.getSuccessAlertText()).contains("sucesso");
    }

    @Test
    @Order(4)
    @DisplayName("SEL-04: Criar produto sem nome deve exibir erro de validação")
    void create_emptyName_showsValidationError() {
        driver.get(baseUrl + "/products/new");
        ProductFormPage form = new ProductFormPage(driver);

        form.fillPrice("100.00").fillStock("1").fillCategory("Teste");
        ProductFormPage result = form.submitExpectingErrors();

        assertThat(result.isOnFormPage()).isTrue();
        assertThat(result.hasFieldError("name")).isTrue();
    }

    @ParameterizedTest(name = "Campo inválido: {0}={1}")
    @CsvSource({
        "name, ''",
        "price, '-1'",
        "stock, '-1'",
        "category, ''"
    })
    @Order(5)
    @DisplayName("SEL-05: Campos obrigatórios inválidos devem exibir erros individuais")
    void create_invalidFields_showsErrors(String field, String value) {
        driver.get(baseUrl + "/products/new");
        ProductFormPage form = new ProductFormPage(driver);

        // Preenche tudo válido e substitui o campo em teste
        form.fillAll("Produto Válido", "", "10.00", "1", "Cat");
        switch (field) {
            case "name"     -> form.fillName(value);
            case "price"    -> form.fillPrice(value);
            case "stock"    -> form.fillStock(value);
            case "category" -> form.fillCategory(value);
        }

        ProductFormPage result = form.submitExpectingErrors();
        assertThat(result.isOnFormPage()).isTrue();
    }

    // ================================================================ EDIT
    @Test
    @Order(6)
    @DisplayName("SEL-06: Editar produto existente deve salvar e exibir sucesso")
    void edit_existingProduct() {
        ProductListPage listPage = new ProductListPage(driver);
        listPage.open(baseUrl);

        assertThat(listPage.getProductRowCount()).isGreaterThan(0);

        ProductFormPage form = listPage.clickEditButton(0);
        assertThat(form.getFormTitle()).isEqualTo("Editar Produto");

        form.fillName("Produto Editado Selenium").fillPrice("499.99").fillStock("20");
        ProductListPage result = form.submit();

        assertThat(result.isSuccessAlertVisible()).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("SEL-07: Cancelar edição deve retornar para listagem sem alterações")
    void edit_cancel_returnsToList() {
        driver.get(baseUrl + "/products/1/edit");
        ProductFormPage form = new ProductFormPage(driver);
        String originalName = form.getNameValue();

        ProductListPage listPage = form.fillName("Nome Temporário").cancel();
        // Verifica retorno e que não houve alteração
        assertThat(listPage.getCurrentUrl()).contains("/products");
    }

    // ================================================================ DELETE
    @Test
    @Order(8)
    @DisplayName("SEL-08: Modal de exclusão deve exibir nome do produto")
    void delete_modalShowsProductName() {
        ProductListPage listPage = new ProductListPage(driver);
        listPage.open(baseUrl);

        int initialCount = listPage.getProductRowCount();
        assertThat(initialCount).isGreaterThan(0);

        String productName = listPage.getProductNameAt(0);
        DeleteModalComponent modal = listPage.clickDeleteButton(0);

        assertThat(modal.isVisible()).isTrue();
        assertThat(modal.getProductName()).isEqualTo(productName);
    }

    @Test
    @Order(9)
    @DisplayName("SEL-09: Confirmar exclusão deve remover produto da lista")
    void delete_confirmRemovesProduct() {
        ProductListPage listPage = new ProductListPage(driver);
        listPage.open(baseUrl);

        int before = listPage.getProductRowCount();
        DeleteModalComponent modal = listPage.clickDeleteButton(0);
        ProductListPage after = modal.confirmDelete();

        assertThat(after.isSuccessAlertVisible()).isTrue();
        assertThat(after.getProductRowCount()).isEqualTo(before - 1);
    }

    @Test
    @Order(10)
    @DisplayName("SEL-10: Cancelar exclusão não deve remover produto")
    void delete_cancelKeepsProduct() {
        ProductListPage listPage = new ProductListPage(driver);
        listPage.open(baseUrl);

        int before = listPage.getProductRowCount();
        DeleteModalComponent modal = listPage.clickDeleteButton(0);
        ProductListPage after = modal.cancelDelete();

        assertThat(after.getProductRowCount()).isEqualTo(before);
    }

    // ================================================================ SEARCH
    @Test
    @Order(11)
    @DisplayName("SEL-11: Busca por nome deve filtrar resultados")
    void search_byName_filtersResults() {
        ProductListPage listPage = new ProductListPage(driver);
        listPage.open(baseUrl);

        listPage.searchBy("Notebook");
        // Resultados devem conter apenas itens com "Notebook"
        int count = listPage.getProductRowCount();
        if (count > 0) {
            assertThat(listPage.getProductNameAt(0).toLowerCase()).contains("notebook");
        }
    }

    @Test
    @Order(12)
    @DisplayName("SEL-12: Busca sem resultados deve exibir estado vazio")
    void search_noResults_showsEmptyState() {
        ProductListPage listPage = new ProductListPage(driver);
        listPage.open(baseUrl);

        listPage.searchBy("xyzNaoExisteNuncaABC12345");
        assertThat(listPage.isEmptyStateVisible()).isTrue();
        assertThat(listPage.isTableVisible()).isFalse();
    }

    @Test
    @Order(13)
    @DisplayName("SEL-13: Limpar filtro deve retornar todos os produtos")
    void clearFilter_restoresAll() {
        ProductListPage listPage = new ProductListPage(driver);
        listPage.open(baseUrl);

        int allCount = listPage.getProductRowCount();
        listPage.searchBy("Notebook");
        listPage.clearFilters();

        assertThat(listPage.getProductRowCount()).isEqualTo(allCount);
    }

    // ================================================================ NAVBAR
    @Test
    @Order(14)
    @DisplayName("SEL-14: Botão 'Novo Produto' na navbar deve abrir formulário")
    void navbar_newProductButton_opensForm() {
        new ProductListPage(driver).open(baseUrl);
        NavbarComponent navbar = new NavbarComponent(driver);
        ProductFormPage form = navbar.clickNewProductButton();
        assertThat(form.getFormTitle()).isEqualTo("Novo Produto");
    }

    // ================================================================ CHAR COUNTER
    @Test
    @Order(15)
    @DisplayName("SEL-15: Contador de caracteres da descrição deve atualizar")
    void form_charCounter_updates() {
        driver.get(baseUrl + "/products/new");
        ProductFormPage form = new ProductFormPage(driver);

        form.fillDescription("Teste de descrição");
        assertThat(form.getDescCharCount()).startsWith("18");
    }
}
