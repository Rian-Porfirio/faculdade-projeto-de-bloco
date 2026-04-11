package com.crud.selenium.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, DEFAULT_TIMEOUT);
        PageFactory.initElements(driver, this);
    }

    /** Aguarda elemento ser visível e clicável. */
    protected WebElement waitForClickable(By locator) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center'});", element
        );

        try { Thread.sleep(150); } catch (InterruptedException ignored) {}

        return element;
    }

    /** Aguarda elemento ser visível na página. */
    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Aguarda que a URL contenha o fragmento fornecido. */
    protected void waitForUrlContains(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
    }

    /** Aguarda ausência de elemento na página. */
    protected void waitForInvisibility(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /** Preenche campo limpo com texto. */
    protected void fillField(WebElement field, String value) {
        field.clear();
        if (value != null) field.sendKeys(value);
    }

    /** Retorna título da página. */
    public String getPageTitle() {
        return driver.getTitle();
    }

    /** Retorna URL atual. */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /** Verifica se o elemento existe sem lançar exceção. */
    protected boolean elementExists(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    /** Aguarda alerta de confirmação e aceita. */
    protected void acceptAlert() {
        wait.until(ExpectedConditions.alertIsPresent()).accept();
    }
}
