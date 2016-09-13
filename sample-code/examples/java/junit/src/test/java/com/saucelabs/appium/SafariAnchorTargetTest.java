package com.saucelabs.appium;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.ios.IOSDriver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;

import static org.junit.Assert.*;

/**
 *
 * Test that demonstrates the issue where links with target="_blank" do not open iOS Safari
 * Tested iOS simulators: 
 *   iPhone 6 (8.1)
 *   iPhone 6 (8.4)
 *   iPhone 6 (9.2)
 *   iPhone 6 (9.3)
 *     
 * Physical iOS device untested
 * Android untested
 *
 * To prove that this test is trying to do the right thing, if you remove the target='_blank' from pageBody,
 * it can be observed that the test will pass and the current page will navigate to the URL. This is specifically
 * occurring on links with a target, either named or _blank.
 *
 * This test is based on SafariTest.java
 *
 * @author Matt Buland
 */
public class SafariAnchorTargetTest {
	HttpServer httpServer;
	
	public void createAndStartWebServer() throws IOException {
		ServerBootstrap bootstrapper = ServerBootstrap.bootstrap();
		bootstrapper.setListenerPort(8080);
		bootstrapper.registerHandler("/link_testing", new HttpRequestHandler() {
			
			@Override
			public void handle(HttpRequest request, HttpResponse response, HttpContext context)
					throws HttpException, IOException {
				String pageBody = 
					"<html><body>" +
						"<a id='newTabLink' target='_blank' href='www.google.com'>Click me</a>" +
					"</body></html>";
				BasicHttpEntity returnEntity = new BasicHttpEntity();
				StringInputStream sis = new StringInputStream(pageBody);
				returnEntity.setContent(sis);
				returnEntity.setContentLength(pageBody.length());
				returnEntity.setContentType("HTML");
				response.setEntity(returnEntity);
			}
		});
		httpServer = bootstrapper.create();
		httpServer.start();
	}

    private WebDriver driver;

    /**
     * Instantiates the {@link #driver} instance by using DesiredCapabilities which specify the
     * 'iPhone Simulator' device and 'safari' app.
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        createAndStartWebServer();

        DesiredCapabilities capabilities = DesiredCapabilities.iphone();
        capabilities.setCapability("deviceName", "iPhone 6");
        capabilities.setCapability("platformName", "iOS");
        capabilities.setCapability("platformVersion", "8.1");
        capabilities.setCapability("browserName", "safari");
        driver = new IOSDriver<WebElement>(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);
    }

    /**
     * Navigates to http://saucelabs.com/test/guinea-pig and interacts with the browser.
     *
     * @throws Exception
     */
    @Test
    public void runTest() throws Exception {
        driver.get("http://localhost:8080/link_testing");
        Thread.sleep(1000);
        
        By linkLocator = By.cssSelector("#newTabLink");
        
        WebDriverWait waiter = new WebDriverWait(driver, 10);
        waiter.until(ExpectedConditions.visibilityOfElementLocated(linkLocator));
        WebElement linkElement = waiter.until(ExpectedConditions.elementToBeClickable(linkLocator));
        linkElement.click();
        
        // Check that a new tab has opened
        waitForOpenedUrl("www.google.com", waiter);
    }
    
    private void waitForOpenedUrl(String expectedUrl, WebDriverWait waiter) {
    	waiter.until(ExpectedConditions.urlContains(expectedUrl));
    }

    /**
     * Closes the {@link #driver} instance.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        driver.quit();

    	httpServer.stop();
    	httpServer = null;
    }
}
