package com.example.kriptobot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class CoinService {

  private static final Duration CACHE_TTL = Duration.ofMinutes(60);

  private final ObjectMapper om = new ObjectMapper();
  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .build();

  private volatile Instant lastFetch = Instant.EPOCH;
  private volatile List<CoinDto> cache = new ArrayList<>();
  private volatile boolean isFetching = false;

  public List<CoinDto> getPiyasa() {
    if (cache.isEmpty() && !isFetching) {
      fetchDataAsync();
    }
    return new ArrayList<>(cache);
  }

  @Scheduled(fixedRate = 3600000, initialDelay = 60000)
  public void scheduledRefresh() {
    System.out.println("Scheduled refresh triggered");
    fetchDataAsync();
  }

  private void fetchDataAsync() {
    if (isFetching) {
      System.out.println("Already fetching, skipping...");
      return;
    }
    
    Instant now = Instant.now();
    long cacheAge = Duration.between(lastFetch, now).toMinutes();
    
    if (cacheAge < 60 && !cache.isEmpty()) {
      System.out.println("Cache still fresh (" + cacheAge + " min), skipping");
      return;
    }

    isFetching = true;
    
    new Thread(() -> {
      try {
        System.out.println("=== STARTING DATA FETCH ===");
        System.out.println("Waiting 5 seconds to avoid rate limit...");
        Thread.sleep(5000);
        
        List<CoinDto> newData = fetchTopCoins();
        
        if (!newData.isEmpty()) {
          cache = newData;
          lastFetch = Instant.now();
          System.out.println("=== SUCCESS: " + newData.size() + " coins cached ===");
        } else {
          System.err.println("=== FETCH FAILED ===");
        }
      } catch (Exception e) {
        System.err.println("Exception in fetch: " + e.getMessage());
      } finally {
        isFetching = false;
      }
    }).start();
  }

  private List<CoinDto> fetchTopCoins() {
    try {
      String url = "https://api.coingecko.com/api/v3/coins/markets" +
          "?vs_currency=usd" +
          "&order=market_cap_desc" +
          "&per_page=250" +
          "&page=1" +
          "&sparkline=false" +
          "&price_change_percentage=24h";

      System.out.println("Calling API: /coins/markets");

      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(30))
          .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
          .header("Accept", "application/json")
          .GET()
          .build();

      HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
      
      System.out.println("HTTP Status: " + res.statusCode());
      
      if (res.statusCode() == 429) {
        System.err.println("RATE LIMIT DETECTED - Will retry in 1 hour");
        return List.of();
      }
      
      if (res.statusCode() != 200) {
        System.err.println("HTTP Error: " + res.statusCode());
        return List.of();
      }

      JsonNode root = om.readTree(res.body());
      List<CoinDto> results = new ArrayList<>();

      int count = 0;
      for (JsonNode coin : root) {
        try {
          String symbol = coin.path("symbol").asText().toUpperCase();
          
          BigDecimal price = bd(coin.get("current_price"));
          if (price.compareTo(BigDecimal.ZERO) == 0) continue;
          
          BigDecimal change24h = bd(coin.get("price_change_percentage_24h"));
          
          BigDecimal change7d = change24h.multiply(BigDecimal.valueOf(2.5));
          BigDecimal change30d = change24h.multiply(BigDecimal.valueOf(8));
          BigDecimal change1h = change24h.multiply(BigDecimal.valueOf(0.3));

          int trendScore = TechnicalIndicators.analyzeTrend(
              change1h.doubleValue(),
              change24h.doubleValue(),
              change7d.doubleValue(),
              change30d.doubleValue()
          );
          
          double estimatedRSI = 50 + change24h.doubleValue();
          estimatedRSI = Math.max(10, Math.min(90, estimatedRSI));
          
          double macdEst = change24h.doubleValue() > 0 ? 0.5 : -0.5;
          
          TechnicalIndicators.SignalResult signal = TechnicalIndicators.generateSignal(
              estimatedRSI,
              new double[]{macdEst, 0, macdEst},
              trendScore,
              1.0,
              change24h.doubleValue()
          );

          results.add(new CoinDto(
              symbol + " / USDT",
              "LIVE",
              fmtMoney(price),
              fmtPct(change24h),
              String.valueOf(signal.confidence),
              signal.signal,
              signal.color
          ));
          
          count++;
          
        } catch (Exception e) {
          System.err.println("Error parsing coin: " + e.getMessage());
        }
      }

      System.out.println("Successfully parsed " + count + " coins");
      return results;

    } catch (Exception e) {
      System.err.println("Fetch exception: " + e.getMessage());
      e.printStackTrace();
      return List.of();
    }
  }

  private static BigDecimal bd(JsonNode n) {
    if (n == null || n.isNull()) return BigDecimal.ZERO;
    try {
      if (n.isTextual()) {
        return new BigDecimal(n.asText());
      }
      return BigDecimal.valueOf(n.asDouble());
    } catch (Exception e) {
      return BigDecimal.ZERO;
    }
  }

  private static String fmtMoney(BigDecimal v) {
    if (v == null || v.compareTo(BigDecimal.ZERO) == 0) return "0";
    
    if (v.compareTo(BigDecimal.valueOf(1000)) >= 0) {
      return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    } else if (v.compareTo(BigDecimal.valueOf(1)) >= 0) {
      return v.setScale(4, RoundingMode.HALF_UP).toPlainString();
    } else if (v.compareTo(BigDecimal.valueOf(0.01)) >= 0) {
      return v.setScale(6, RoundingMode.HALF_UP).toPlainString();
    } else {
      return v.setScale(8, RoundingMode.HALF_UP).toPlainString();
    }
  }

  private static String fmtPct(BigDecimal v) {
    if (v == null) return "0";
    return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }
}
