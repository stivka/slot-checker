package stivka.net.slotchecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SlotCheckerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SlotCheckerApplication.class, args);
		
	}

}
