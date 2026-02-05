package com.example.kriptobot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

@Service
public class TechnicalAnalysisService {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public static class SignalResult {
        public final String signal;     // BUY / SELL / NEUTRAL
        public final String color;      // UI renk
        public final String regime;     // TREND / RANGE / NO_DATA
        public final int confidence;    // 0-100

        public SignalResult(String signal, String color) {
            this(signal, color, "UNKNOWN", 0);
        }

        public SignalResult(String signal, String color, String regime, int confidence) {
            this.signal = signal;
            this.color = color;
            this.regime = regime;
            this.confidence = confidence;
        }
    }

    /**
     * Basit, rate-limit yemeyen sinyal:
     * - Binance 24h change yüzdesine göre BUY/SELL/NEUTRAL verir.
     * - TA gibi davranır ama tek istek ile çalışır.
     *
     * NOT: Render'da Binance bazen 451 dönebiliyor (bölgesel / WAF).
     * O durumda CoinService zaten fallback'e düşüyor.
     */
    public SignalResult getSignal(String symbol) {
        try {
            // tek symbol 24h endpoint (az istek)
            String url = String.format(Locale.US,
                    "https://api.binance.com/api/v3/ticker/24hr?symbol=%s", symbol);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() != 200) {
                return new SignalResult("NEUTRAL", "#474d57", "NO_DATA", 0);
            }

            JsonNode n = mapper.readTree(res.body());
            double ch = n.path("priceChangePercent").asDouble(0);

            // basit eşikler (istersen değiştirirsin)
            if (ch >= 2.0) return new SignalResult("BUY", "#1db954", "TREND", 70);
            if (ch <= -2.0) return new SignalResult("SELL", "#ff4d4d", "TREND", 70);
            return new SignalResult("NEUTRAL", "#474d57", "RANGE", 50);

        } catch (Exception e) {
            return new SignalResult("NEUTRAL", "#474d57", "NO_DATA", 0);
        }
    }
}
