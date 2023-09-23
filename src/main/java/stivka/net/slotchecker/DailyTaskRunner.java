package stivka.net.slotchecker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyTaskRunner {

    @Autowired
    private SlotChecker slotChecker;

    @Scheduled(cron = "0 0 12 * * ?", zone="Europe/Tallinn")
    public void runDailyTask() {
        slotChecker.checkForNewSlots();
        System.out.println("Running daily scheduled task! " + System.currentTimeMillis());
    }
}
