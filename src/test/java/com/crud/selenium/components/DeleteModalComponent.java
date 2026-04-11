package com.crud.selenium.components;

import com.crud.selenium.pages.BasePage;
import com.crud.selenium.pages.ProductListPage;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class DeleteModalComponent extends BasePage {

    @FindBy(id = "deleteModal")
    private WebElement modalElement;

    @FindBy(id = "deleteProductName")
    private WebElement productNameLabel;

    @FindBy(id = "btnConfirmDelete")
    private WebElement btnConfirmDelete;

    @FindBy(id = "btnCancelDelete")
    private WebElement btnCancelDelete;

    public DeleteModalComponent(WebDriver driver) {
        super(driver);
        waitForVisible(By.id("deleteModal"));
        wait.until(ExpectedConditions.visibilityOf(modalElement));
    }

    // ---- Queries ----
    public boolean isVisible() {
        return modalElement.isDisplayed();
    }

    public String getProductName() {
        return waitForVisible(By.id("deleteProductName")).getText();
    }

    // ---- Actions ----
    public ProductListPage confirmDelete() {
        waitForClickable(By.id("btnConfirmDelete")).click();
        waitForUrlContains("/products");
        return new ProductListPage(driver);
    }

    public ProductListPage cancelDelete() {
        waitForClickable(By.id("btnCancelDelete")).click();
        waitForInvisibility(By.id("deleteModal"));
        return new ProductListPage(driver);
    }
}
