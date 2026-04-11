package com.crud.selenium.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;

public class ProductFormPage extends BasePage {

    @FindBy(id = "nameInput")
    private WebElement nameInput;

    @FindBy(id = "descriptionInput")
    private WebElement descriptionInput;

    @FindBy(id = "priceInput")
    private WebElement priceInput;

    @FindBy(id = "stockInput")
    private WebElement stockInput;

    @FindBy(id = "categoryInput")
    private WebElement categoryInput;

    @FindBy(id = "btnSubmit")
    private WebElement btnSubmit;

    @FindBy(id = "btnCancel")
    private WebElement btnCancel;

    @FindBy(id = "productForm")
    private WebElement productForm;

    @FindBy(id = "formErrorAlert")
    private WebElement formErrorAlert;

    @FindBy(id = "descCharCount")
    private WebElement descCharCount;

    public ProductFormPage(WebDriver driver) {
        super(driver);
        waitForVisible(By.id("productForm"));
    }

    // ---- Actions ----
    public ProductFormPage fillName(String name) {
        fillField(nameInput, name);
        return this;
    }

    public ProductFormPage fillDescription(String desc) {
        fillField(descriptionInput, desc);
        return this;
    }

    public ProductFormPage fillPrice(String price) {
        fillField(priceInput, price);
        return this;
    }

    public ProductFormPage fillStock(String stock) {
        fillField(stockInput, stock);
        return this;
    }

    public ProductFormPage fillCategory(String category) {
        fillField(categoryInput, category);
        return this;
    }

    /** Preenche todos os campos do formulário. */
    public ProductFormPage fillAll(String name, String desc, String price, String stock, String category) {
        return fillName(name).fillDescription(desc).fillPrice(price).fillStock(stock).fillCategory(category);
    }

    public ProductListPage submit() {
        WebElement btn = waitForClickable(By.id("btnSubmit"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", btn);
        btn.click();
        waitForUrlContains("/products");
        return new ProductListPage(driver);
    }

    // Submete e permanece na página (caso de validação com erros).
    public ProductFormPage submitExpectingErrors() {
        WebElement btn = waitForClickable(By.id("btnSubmit"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", btn);
        btn.click();
        waitForVisible(By.id("productForm"));
        return this;
    }

    public ProductListPage cancel() {
        waitForClickable(By.id("btnCancel")).click();
        return new ProductListPage(driver);
    }

    // ---- Queries ----
    public boolean hasFieldError(String fieldId) {
        return elementExists(By.id(fieldId + "Error"));
    }

    public String getDescCharCount() {
        return descCharCount.getText();
    }

    public String getNameValue() {
        return nameInput.getAttribute("value");
    }

    public boolean isOnFormPage() {
        return elementExists(By.id("productForm"));
    }

    public String getFormTitle() {
        return waitForVisible(By.cssSelector(".form-card-title")).getText();
    }
}
