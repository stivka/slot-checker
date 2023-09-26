package stivka.net.slotchecker;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class SlotChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlotChecker.class);

    private final Environment env;
    private final PushbulletNotifier notifier;

    private static final File SLOTS_HASH_FILE = new File("previousSlotsHash.txt");
    private String websiteUrl;
    private String remoteWebDriverPath = "http://selenium:4444/wd/hub";

    public SlotChecker(Environment env, PushbulletNotifier notifier) {
        this.env = env;
        this.notifier = notifier;
    }

    @PostConstruct
    public void init() {
        websiteUrl = env.getProperty("URL");
        checkForNewSlots();
    }

    public void checkForNewSlots() {

        LOGGER.info("Running checkForNewSlots! {}", System.currentTimeMillis());

        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
            WebDriver driver = new RemoteWebDriver(new URL(remoteWebDriverPath), options);

            LOGGER.info("Loading URL: {}", websiteUrl);
            // consider adding retries
            driver.get(websiteUrl);
            LOGGER.info("Successfully loaded URL: {}", websiteUrl);

            LOGGER.info("Switching to iframe");
            driver.switchTo().frame(0);
            LOGGER.info("Switched to iframe");

            Duration duration = Duration.ofSeconds(3);
            WebDriverWait wait = new WebDriverWait(driver, duration);

            List<String> allRowsContent = new ArrayList<>();

            while (true) {
                List<WebElement> labels = driver.findElements(By.cssSelector("label[for^='appointmentType-']"));
                for (WebElement label : labels) {
                    allRowsContent.add(label.getAttribute("textContent").trim());
                }

                LOGGER.info("Processed {} labels", allRowsContent.size());

                WebElement calendarNextButton;
                try {
                    calendarNextButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("calendar-next")));
                    LOGGER.info("Navigating to the next calendar page");
                    calendarNextButton.click();
                } catch (TimeoutException e) {
                    LOGGER.warn("No more calendar pages to navigate");
                    break;
                }
            }

            Integer currentHash = computeHashForContent(allRowsContent);
            if (!currentHash.equals(getStoredHash())) {
                LOGGER.info("Change detected in slots!");
                sendNotification();
                updatePreviousSlotsHash(currentHash);
            } else {
                LOGGER.info("No change detected in slots");
            }

            driver.quit();

        } catch (Exception e) {
            LOGGER.error("Error while checking for new slots", e);
        }
    }

    private Integer computeHashForContent(List<String> content) {
        StringBuilder sb = new StringBuilder();
        for (String row : content) {
            sb.append(row);
        }
        return sb.toString().hashCode();
    }

    private int getStoredHash() {
        try {
            String content = new String(Files.readAllBytes(SLOTS_HASH_FILE.toPath()), StandardCharsets.UTF_8);
            return Integer.parseInt(content);
        } catch (IOException e) {
            return 0;
        }
    }

    private void sendNotification() {
        notifier.sendNotification("Slot Checker", "There's a change in slots!");
    }

    private void updatePreviousSlotsHash(Integer newHash) {
        try {
            Files.writeString(SLOTS_HASH_FILE.toPath(), Integer.toString(newHash));
        } catch (IOException e) {
            LOGGER.error("Error while updating the previous slots hash", e);
        }
    }
}
