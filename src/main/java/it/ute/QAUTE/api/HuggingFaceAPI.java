package it.ute.QAUTE.api;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HuggingFaceAPI {
    public static void main(String[] args) throws Exception {

        String spaceApiUrl = "https://nhansohoccode-vn-toxic-classifier.hf.space/api/predict";

        String text = "Mày là đồ vô học";

        // Tạo kết nối đến Space
        URL url = new URL(spaceApiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Sử dụng payload đúng định dạng của Gradio API: {"data": ["văn bản..."]}
        String payload = "{\"data\": [\"" + text.replace("\"", "\\\"") + "\"]}";

        System.out.println("Đang gọi đến URL: " + spaceApiUrl);
        System.out.println("Đang gửi Payload: " + payload);

        // Gửi yêu cầu
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        // Lấy mã phản hồi HTTP
        int responseCode = conn.getResponseCode();
        System.out.println("HTTP Response Code: " + responseCode);

        // Đọc phản hồi từ server
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {

            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println("Kết quả trả về: " + response.toString());
        }
    }
}
