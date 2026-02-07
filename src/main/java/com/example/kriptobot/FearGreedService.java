package com.example.kriptobot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

@Service
public class FearGreedService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10); // 10 dakika cache
    private static final String API_URL = "https://api.alternative.me/fng/?limit=1";

    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile Instant lastFetch = Instant.EPOCH;
    private volatile FearGreedData cache = null;

    public FearGreedData getFearGreed() {
        Instant now = Instant.now();
        
        // Cache varsa ve fresh ise direkt dön
        if (cache != null && Duration.between(lastFetch, now).compareTo(CACHE_TTL) < 0) {
            return cache;
        }

        // Yeni veri çek
        FearGreedData data = fetchFromApi();
        if (data != null) {
            cache = data;
            lastFetch = now;
            return data;
        }

        // API başarısız, eski cache'i dön veya default
        return cache != null ? cache : getDefaultData();
    }

    private FearGreedData fetchFromApi() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            
            if (res.statusCode() != 200) {
                System.err.println("Fear & Greed API HTTP " + res.statusCode());
                return null;
            }

            JsonNode root = om.readTree(res.body());
            JsonNode data = root.path("data").get(0);
            
            if (data == null) return null;

            int value = data.path("value").asInt(50);
            String classification = data.path("value_classification").asText("Neutral");
            long timestamp = data.path("timestamp").asLong(System.currentTimeMillis() / 1000);

            System.out.println("Fear & Greed SUCCESS: " + value + " (" + classification + ")");
            
            return new FearGreedData(value, classification, timestamp);

        } catch (Exception e) {
            System.err.println("Fear & Greed ERROR: " + e.getMessage());
            return null;
        }
    }

    private FearGreedData getDefaultData() {
        return new FearGreedData(50, "Neutral", System.currentTimeMillis() / 1000);
    }

    public static class FearGreedData {
        public int value;           // 0-100
        public String classification; // Extreme Fear, Fear, Neutral, Greed, Extreme Greed
        public long timestamp;
        public String sentiment;     // Emoji ve açıklama
        public String color;         // Renk kodu

        public FearGreedData(int value, String classification, long timestamp) {
            this.value = value;
            this.classification = classification;
            this.timestamp = timestamp;
            this.sentiment = getSentiment(value);
            this.color = getColor(value);
        }

        private String getSentiment(int val) {
            if (val <= 25) return "😱 Aşırı Korku - Alım Fırsatı!";
            if (val <= 45) return "😰 Korku - Dikkatli Alım";
            if (val <= 55) return "😐 Nötr - Bekle & Gör";
            if (val <= 75) return "😊 Açgözlülük - Kâr Al?";
            return "🤑 Aşırı Açgözlülük - Balon Riski!";
        }

        private String getColor(int val) {
            if (val <= 25) return "#ff4d4f";      // Kırmızı
            if (val <= 45) return "#ff9800";      // Turuncu
            if (val <= 55) return "#95a5a6";      // Gri
            if (val <= 75) return "#8bc34a";      // Açık yeşil
            return "#00ff41";                     // Parlak yeşil
        }
    }
}
