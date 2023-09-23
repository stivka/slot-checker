package stivka.net.slotchecker;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PushbulletNotifier {

    private static final String PUSHBULLET_API_URL = "https://api.pushbullet.com/v2/pushes";
    private static final String API_KEY = "o.gIUbauILUAmWxD8kwDj7tO45LMgIEcgb";
    
    public void sendNotification(String title, String body) {
        try {
            URL url = new URL(PUSHBULLET_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Headers
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Access-Token", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // JSON body
            String json = String.format("{\"active\":true,\"title\":\"%s\",\"body\":\"%s\",\"type\":\"note\"}", title, body);

            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("Response Code : " + responseCode);

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
