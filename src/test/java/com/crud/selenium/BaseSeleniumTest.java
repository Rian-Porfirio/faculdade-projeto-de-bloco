package com.crud.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

public abstract class BaseSeleniumTest {

    protected WebDriver driver;
    protected static final String BASE_URL = "http://localhost:8080";

    @BeforeAll
    static void setUpClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUpDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1366,768",
            "--disable-extensions",
            "--disable-popup-blocking"
        );
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
    }

    @AfterEach
    void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }
}
