package stivka.net.slotchecker;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class SlotChecker {

    private static final File SLOTS_HASH_FILE = new File("previousSlotsHash.txt");

    @Value("${URL}")
    private String url;

    @Value("${CHROME_DRIVER_PATH}")
    private String chromeDriverPath;

    @PostConstruct
    public void checkForNewSlots() {

        try {
            // this is the separate executable between Selenium and the Chrome browser
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
            System.setProperty("webdriver.chrome.logfile", "./logs/chromedriver.log");

            ChromeOptions options = new ChromeOptions();
            // for an ideal match, supply the chrome version that goes with the chromedriver
            // options.setBinary("C:/Program Files/chrome-win64/chrome.exe");
            WebDriver driver = new ChromeDriver(options);

            driver.get(url);

            // Assuming there's only one iframe; if there are multiple iframes, you may need
            // to adjust this logic
            driver.switchTo().frame(0); // Switch to the first iframe

            Duration duration = Duration.ofSeconds(3);
            WebDriverWait wait = new WebDriverWait(driver, duration);
            WebElement firstLabel = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("label[for^='appointmentType-']")));

            List<String> allRowsContent = new ArrayList<>();

            while (true) {
                List<WebElement> labels = driver.findElements(By.cssSelector("label[for^='appointmentType-']"));

                for (WebElement label : labels) {
                    allRowsContent.add(label.getAttribute("textContent").trim());
                }

                WebElement calendarNextButton;
                try {
                    calendarNextButton = wait
                            .until(ExpectedConditions.visibilityOfElementLocated(By.className("calendar-next")));
                    calendarNextButton.click();
                    wait.until(ExpectedConditions
                            .presenceOfElementLocated(By.cssSelector("label[for^='appointmentType-']")));
                } catch (TimeoutException e) {
                    break; // If button is not found, break the loop
                }
            }

            Integer currentHash = computeHashForContent(allRowsContent);
            if (!currentHash.equals(getStoredHash())) {
                sendNotification();
                updatePreviousSlotsHash(currentHash);
            }

            driver.quit();

        } catch (Exception e) {
            e.printStackTrace();
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
        PushbulletNotifier notifier = new PushbulletNotifier();
        notifier.sendNotification("Slot Checker", "There's a change in slots!");
    }

    private void updatePreviousSlotsHash(Integer newHash) {
        try {
            Files.writeString(SLOTS_HASH_FILE.toPath(), Integer.toString(newHash));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
