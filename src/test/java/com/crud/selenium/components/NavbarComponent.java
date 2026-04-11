package com.crud.selenium.components;

import com.crud.selenium.pages.BasePage;
import com.crud.selenium.pages.ProductFormPage;
import com.crud.selenium.pages.ProductListPage;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;

public class NavbarComponent extends BasePage {

    @FindBy(id = "mainNavbar")
    private WebElement navbar;

    @FindBy(css = "#mainNavbar .brand-name")
    private WebElement brandName;

    @FindBy(css = "#mainNavbar .btn-accent")
    private WebElement btnNewProduct;

    public NavbarComponent(WebDriver driver) {
        super(driver);
    }

    // ---- Queries ----
    public boolean isVisible() {
        return elementExists(By.id("mainNavbar"));
    }

    public String getBrandName() {
        return waitForVisible(By.cssSelector("#mainNavbar .brand-name")).getText();
    }

    // ---- Actions ----
    public ProductListPage clickBrand() {
        waitForClickable(By.cssSelector("#mainNavbar .navbar-brand")).click();
        return new ProductListPage(driver);
    }

    public ProductFormPage clickNewProductButton() {
        waitForClickable(By.cssSelector("#mainNavbar .btn-accent")).click();
        return new ProductFormPage(driver);
    }

    public ProductListPage clickProductsLink() {
        waitForClickable(By.cssSelector("#mainNavbar .nav-link")).click();
        return new ProductListPage(driver);
    }
}
