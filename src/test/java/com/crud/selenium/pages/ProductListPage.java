package com.crud.selenium.pages;

import com.crud.selenium.components.DeleteModalComponent;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

public class ProductListPage extends BasePage {

    // ---- Page elements ----
    @FindBy(id = "btnNewProduct")
    private WebElement btnNewProduct;

    @FindBy(id = "searchInput")
    private WebElement searchInput;

    @FindBy(id = "categorySelect")
    private WebElement categorySelect;

    @FindBy(id = "btnSearch")
    private WebElement btnSearch;

    @FindBy(id = "btnApplyFilter")
    private WebElement btnApplyFilter;

    @FindBy(id = "btnClearFilter")
    private WebElement btnClearFilter;

    @FindBy(id = "productsTable")
    private WebElement productsTable;

    @FindBy(id = "emptyState")
    private WebElement emptyState;

    @FindBy(id = "alertSuccess")
    private WebElement alertSuccess;

    @FindBy(id = "alertError")
    private WebElement alertError;

    public ProductListPage(WebDriver driver) {
        super(driver);
    }

    // ---- Navigation ----
    public void open(String baseUrl) {
        driver.get(baseUrl + "/products");
        waitForVisible(By.tagName("main"));
    }

    // ---- Actions ----
    public ProductFormPage clickNewProduct() {
        waitForClickable(By.id("btnNewProduct")).click();
        return new ProductFormPage(driver);
    }

    public void searchBy(String term) {
        fillField(searchInput, term);
        btnSearch.click();
        // Aguarda HTMX swap
        waitForVisible(By.id("productTableWrapper"));
    }

    public void clearFilters() {
        waitForClickable(By.id("btnClearFilter")).click();
        waitForVisible(By.tagName("main"));
    }

    public ProductFormPage clickEditButton(int rowIndex) {
        List<WebElement> editBtns = driver.findElements(By.cssSelector("a.btn-action.btn-outline-primary"));
        editBtns.get(rowIndex).click();
        return new ProductFormPage(driver);
    }

    public DeleteModalComponent clickDeleteButton(int rowIndex) {
        List<WebElement> deleteBtns = driver.findElements(By.cssSelector("button.btn-delete"));
        deleteBtns.get(rowIndex).click();
        return new DeleteModalComponent(driver);
    }

    // ---- Queries ----
    public boolean isTableVisible() {
        return elementExists(By.id("productsTable"));
    }

    public boolean isEmptyStateVisible() {
        return elementExists(By.id("emptyState"));
    }

    public int getProductRowCount() {
        if (!isTableVisible()) return 0;
        return driver.findElements(By.cssSelector("#productsTable tbody tr")).size();
    }

    public String getProductNameAt(int rowIndex) {
        return driver.findElements(By.cssSelector(".product-name")).get(rowIndex).getText();
    }

    public boolean isSuccessAlertVisible() {
        return elementExists(By.id("alertSuccess"));
    }


    public String getSuccessAlertText() {
        return waitForVisible(By.id("alertSuccess")).getText();
    }

}
