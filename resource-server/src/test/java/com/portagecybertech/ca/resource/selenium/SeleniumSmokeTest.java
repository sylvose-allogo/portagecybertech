package com.portagecybertech.ca.resource.selenium;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/**
 * 
 * Optional browser-oriented smoke tests for the demonstration UI and protected-resource flow. Some scenarios are disabled or require locally installed browser-driver tooling and external connectivity.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class SeleniumSmokeTest {

    private static final Logger logger = LogManager.getLogger(SeleniumSmokeTest.class);

    @LocalServerPort
    private int PORT_SPRING_BOOT_RANDOM;

    private static final String HOST_NAME = "http://localhost:";
    private static final String URI_RESOURCE_SERVER_ENDPOINT = "/api/hello";
    private static final String CLIENT_ID = "agent-client";
    private static final String GRANT_TYPE = "grant_type";
    private static final String SUBJECT = "subject";
    private static final String SCOPE = "scope";

    private static final String SUBJECT_GRANT = "urn:portagecybertech:oauth:grant-type:subject";
        private static final String USER_NAME = SeleniumSmokeTest.class.getSimpleName();
    private static final String API_READ = "api:read";

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.oauth.token-uri:http://localhost:9090/oauth2/token}")
    private String authorizationServerTokenUri;

    @Value("${app.oauth.client-secret:}")
    private String clientSecret;

    @Test
    /**
     * 
     * Performs the 'web driver container live test' operation required by this class.
     * @throws InterruptedException when the operation cannot complete its contract
     */
    void webDriverContainerLiveTest() throws InterruptedException {
        // Set the path to the ChromeDriver executable
        Path testResourceDirectory = Paths.get("src","test", "resources", "drivers");
        String driverPath = testResourceDirectory.toFile().getAbsolutePath();
        System.setProperty("webdriver.chrome.driver", driverPath + File.separator + "chromedriver.exe");
        System.setProperty("webdriver.chrome.args", "--disable-logging");
        System.setProperty("webdriver.chrome.silentOutput", "false");
        // Create a new WebDriver instance
        WebDriver webdriver = new ChromeDriver();
        // Open PortageCybertech homepage
        webdriver.get("https://www.portagecybertech.com/fr/");
        webdriver.manage().window().maximize();
        // Print the page title
        logger.debug("Page title is : " + webdriver.getTitle());
        // Let the user actually see something
        Thread.sleep(5000);
        // Close the browser
        webdriver.quit();
    }

    @Test
    @Disabled("Requires a locally installed ChromeDriver")
    /**
     * 
     * Performs the 'remote web driver live test' operation required by this class.
     */
    void remoteWebDriverLiveTest() {
        RemoteWebDriver remoteWebDriver = null;

        try {
            String proxyUrl = "http://localhost:8888";
            Proxy proxy = new Proxy();
            proxy.setAutodetect(false);
            proxy.setHttpProxy(proxyUrl);
            proxy.setProxyType(Proxy.ProxyType.MANUAL);

            ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.addArguments("--remote-debugging-port=1557");
            chromeOptions.addArguments("--start-maximized");
            chromeOptions.addArguments("--remote-debugging-pipe");
            chromeOptions.addArguments("--enable-unsafe-extension-debugging");
            chromeOptions.setCapability("browserVersion","154");
            chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
            chromeOptions.setCapability("platformName", Platform.WINDOWS);
            chromeOptions.setCapability(CapabilityType.PROXY, proxy);

            remoteWebDriver = new ChromeDriver(chromeOptions);
            remoteWebDriver.get("https://www.google.com");

            assertTrue(remoteWebDriver.findElement(By.tagName("title")).getText().equalsIgnoreCase(""));
            assertTrue(remoteWebDriver.findElement(By.tagName("body")).getText().contains("Connexion"));
            assertTrue(remoteWebDriver.findElement(By.tagName("body")).getText().contains("Google Store"));
            assertTrue(remoteWebDriver.findElement(By.tagName("body")).getText().contains("Canada"));

            remoteWebDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

            // Let the user actually see something
            Thread.sleep(5000);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);

        } finally {
            if (remoteWebDriver != null) {
                // Close the browser
                remoteWebDriver.quit();
            }
        }
    }

    @Test
    @Disabled("Requires a locally installed ChromeDriver")
        /**
         * 
         * Performs the 'chrome driver live test' operation required by this class.
         * @throws IOException when the operation cannot complete its contract
         * @throws InterruptedException when the operation cannot complete its contract
         */
        void chromeDriverLiveTest() throws IOException, InterruptedException {
        ChromeDriverService chromeDriverService = null;
        ChromeDriver chromeDriver = null;

        // Set the path to the ChromeDriver executable
        Path testResourceDirectory = Paths.get("src","test","resources","drivers");
        String driverPath = testResourceDirectory.toFile().getAbsolutePath();
        File file = new File(driverPath + File.separator + "chromedriver.exe");

        try {
            // Create a new ChromeDriverService instance
            chromeDriverService = new ChromeDriverService.Builder()
                    .withSilent(false)
                    .withVerbose(true)
                    .usingDriverExecutable(file)
                    .withTimeout(Duration.ofSeconds(30))
                    .build();

            // Ensure that the ChromeDriverService is not null before creating the ChromeDriver
            assertNotNull(chromeDriverService);

            // Start the ChromeDriverService
            chromeDriverService.start();

            // Get the new ChromeOptions instance
            ChromeOptions chromeOptions = getChromeOptions();

            // Create a new ChromeDriver instance
            chromeDriver = new ChromeDriver(chromeDriverService, chromeOptions);
            // Open Selenium homepage
            chromeDriver.get(HOST_NAME + PORT_SPRING_BOOT_RANDOM + URI_RESOURCE_SERVER_ENDPOINT);

            chromeDriver.manage().window().maximize();

            chromeDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

            String html = chromeDriver.getPageSource();
            // Printing result here
            logger.debug(html);

            // Print the page title
            logger.debug("Page title is : " + chromeDriver.getTitle());

            String jwt = obtainJwtToken();

            Object storedJwt = chromeDriver.executeScript("window.localStorage.setItem('jwt', arguments[0]); return window.localStorage.getItem('jwt');",
                    jwt);
            assertEquals(jwt, storedJwt);

            String resultJson = (String) chromeDriver.executeAsyncScript(
                    """
                    const token = window.localStorage.getItem('jwt');
                    const done = arguments[arguments.length - 1];
                    fetch('/api/hello', {
                        method: 'GET',
                        headers: {'Authorization': 'Bearer ' + token}
                    })
                    .then(async response => {
                        const body = await response.text();
                        done(JSON.stringify({status: response.status, body: body}));
                    })
                    .catch(error => {
                        done(JSON.stringify({status: 0, body: String(error)}));
                    });
                    """);

            JsonNode response = objectMapper.readTree(resultJson);
            assertEquals(200, response.get("status").asInt());
            assertTrue(response.get("body").asText().contains("Hello"));

            // Let the user actually see something
            Thread.sleep(5000);

        } finally {
            if (chromeDriver != null) {
                // Close the browser
                logger.debug("Closing ChromeDriver...");
                chromeDriver.quit();
                }

            if (chromeDriverService != null) {
                // Stop the ChromeDriverService
                logger.debug("Stopping ChromeDriverService...");
                chromeDriverService.close();
            }
        }
    }

    /**
     * 
     * Performs the 'get chrome options' operation and returns the corresponding result.
     * @return the result described above.
     */
    private ChromeOptions getChromeOptions() {
        HashMap<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service",false);
        prefs.put("profile.password_manager_enabled",false);
        prefs.put("download.prompt_for_download",false);

            // Create a new ChromeOptions instance
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("prefs", prefs);
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("start-maximized");
        chromeOptions.addArguments("enable-javascript");
        return chromeOptions;
    }

    /**
     * 
     * Performs the 'obtain jwt token' operation and returns the corresponding result.
     * @return the result described above.
     * @throws JsonProcessingException when the operation cannot complete its contract
     */
    private String obtainJwtToken() throws JsonProcessingException {
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("Configure app.oauth.client-secret for the external test client");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(CLIENT_ID, clientSecret);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(GRANT_TYPE, SUBJECT_GRANT);
        form.add(SUBJECT, USER_NAME);
        form.add(SCOPE, API_READ);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        String response = new RestTemplate().postForEntity(authorizationServerTokenUri, request, String.class).getBody();

        assertNotNull(response);
        JsonNode body = objectMapper.readTree(response);
        String accessToken = body.path("access_token").asText();
        assertTrue(accessToken != null && !accessToken.isBlank());
        return accessToken;
    }
}