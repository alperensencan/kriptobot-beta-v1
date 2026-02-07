package com.example.kriptobot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CoinService {

  /**
   * PROFESSIONAL TRADING SIGNALS
   * Cache: 10 minutes (rate-limit safe)
   * Uses: RSI, MACD, Trend Analysis, Volume
   */
  private static final Duration CACHE_TTL = Duration.ofSeconds(600); // 10 minutes
  private static final Duration CHART_CACHE_TTL = Duration.ofSeconds(1800); // 30 minutes

  private final ObjectMapper om = new ObjectMapper();
  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .build();

  private final Map<String, String> coins = new LinkedHashMap<>() {{
    // Top 50 most traded (120 coins = too much API calls)
    put("bitcoin", "BTC / USDT");
    put("ethereum", "ETH / USDT");
    put("tether", "USDT / USD");
    put("binancecoin", "BNB / USDT");
    put("solana", "SOL / USDT");
    put("ripple", "XRP / USDT");
    put("usd-coin", "USDC / USD");
    put("cardano", "ADA / USDT");
    put("dogecoin", "DOGE / USDT");
    put("tron", "TRX / USDT");
    put("avalanche-2", "AVAX / USDT");
    put("shiba-inu", "SHIB / USDT");
    put("polkadot", "DOT / USDT");
    put("chainlink", "LINK / USDT");
    put("matic-network", "MATIC / USDT");
    put("litecoin", "LTC / USDT");
    put("bitcoin-cash", "BCH / USDT");
    put("uniswap", "UNI / USDT");
    put("stellar", "XLM / USDT");
    put("cosmos", "ATOM / USDT");
    put("monero", "XMR / USDT");
    put("ethereum-classic", "ETC / USDT");
    put("filecoin", "FIL / USDT");
    put("internet-computer", "ICP / USDT");
    put("aptos", "APT / USDT");
    put("hedera-hashgraph", "HBAR / USDT");
    put("arbitrum", "ARB / USDT");
    put("optimism", "OP / USDT");
    put("near", "NEAR / USDT");
    put("vechain", "VET / USDT");
    put("algorand", "ALGO / USDT");
    put("aave", "AAVE / USDT");
    put("the-graph", "GRT / USDT");
    put("fantom", "FTM / USDT");
    put("sandbox", "SAND / USDT");
    put("decentraland", "MANA / USDT");
    put("tezos", "XTZ / USDT");
    put("maker", "MKR / USDT");
    put("enjincoin", "ENJ / USDT");
    put("pancakeswap-token", "CAKE / USDT");
    put("pepe", "PEPE / USDT");
    put("bonk", "BONK / USDT");
    put("floki", "FLOKI / USDT");
    put("sui", "SUI / USDT");
    put("sei-network", "SEI / USDT");
    put("render-token", "RNDR / USDT");
    put("kaspa", "KAS / USDT");
    put("fetch-ai", "FET / USDT");
    put("injective-protocol", "INJ / USDT");
    put("worldcoin-wld", "WLD / USDT");
  }};

  private volatile Instant lastFetch = Instant.EPOCH;
  private volatile Instant lastChartFetch = Instant.EPOCH;
  private volatile Map<String, MarketData> cache = new LinkedHashMap<>();
  private volatile Map<String, List<Double>> chartCache = new LinkedHashMap<>();

  public List<CoinDto> getPiyasa() {
    refreshIfNeeded();
    return buildDtosFromCache();
  }

  private void refreshIfNeeded() {
    Instant now = Instant.now();
    
    // Refresh basic data if needed
    if (Duration.between(lastFetch, now).compareTo(CACHE_TTL) >= 0 || cache.isEmpty()) {
      System.out.println("🔄 Fetching market data...");
      Map<String, MarketData> newData = fetchMarketData();
      if (!newData.isEmpty()) {
        cache = newData;
        lastFetch = now;
      }
    }

    // Refresh chart data if needed (less frequently)
    if (Duration.between(lastChartFetch, now).compareTo(CHART_CACHE_TTL) >= 0 || chartCache.isEmpty()) {
      System.out.println("📊 Fetching chart data for technical analysis...");
      chartCache = fetchChartData();
      lastChartFetch = now;
    }
  }

  private Map<String, MarketData> fetchMarketData() {
    try {
      String ids = String.join(",", coins.keySet());
      String url = "https://api.coingecko.com/api/v3/coins/markets" +
          "?vs_currency=usd" +
          "&ids=" + ids +
          "&order=market_cap_desc" +
          "&per_page=50" +
          "&page=1" +
          "&sparkline=false" +
          "&price_change_percentage=1h,24h,7d,30d";

      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(30))
          .header("User-Agent", "Mozilla/5.0")
          .header("Accept", "application/json")
          .GET()
          .build();

      HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
      
      if (res.statusCode() == 429) {
        System.err.println("❌ RATE LIMIT! Using cache.");
        return Map.of();
      }
      
      if (res.statusCode() != 200) {
        System.err.println("❌ HTTP " + res.statusCode());
        return Map.of();
      }

      JsonNode root = om.readTree(res.body());
      Map<String, MarketData> result = new LinkedHashMap<>();

      for (JsonNode coin : root) {
        String id = coin.path("id").asText();
        if (!coins.containsKey(id)) continue;

        MarketData data = new MarketData(
            bd(coin.get("current_price")),
            bd(coin.get("price_change_percentage_1h_in_currency")),
            bd(coin.get("price_change_percentage_24h_in_currency")),
            bd(coin.get("price_change_percentage_7d_in_currency")),
            bd(coin.get("price_change_percentage_30d_in_currency")),
            bd(coin.get("total_volume")),
            bd(coin.get("market_cap"))
        );
        result.put(id, data);
      }

      System.out.println("✅ Market data fetched: " + result.size() + " coins");
      return result;

    } catch (Exception e) {
      System.err.println("❌ Error fetching market data: " + e.getMessage());
      return Map.of();
    }
  }

  private Map<String, List<Double>> fetchChartData() {
    Map<String, List<Double>> charts = new LinkedHashMap<>();
    
    // Fetch chart data for top 10 coins only (to avoid rate limit)
    List<String> topCoins = List.of("bitcoin", "ethereum", "binancecoin", "solana", "ripple",
                                     "cardano", "dogecoin", "avalanche-2", "polkadot", "chainlink");
    
    for (String coinId : topCoins) {
      if (!coins.containsKey(coinId)) continue;
      
      try {
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId + "/market_chart" +
            "?vs_currency=usd" +
            "&days=30" +
            "&interval=daily";

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("User-Agent", "Mozilla/5.0")
            .GET()
            .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        
        if (res.statusCode() != 200) {
          continue;
        }

        JsonNode root = om.readTree(res.body());
        JsonNode prices = root.path("prices");
        
        List<Double> priceList = new ArrayList<>();
        for (JsonNode price : prices) {
          priceList.add(price.get(1).asDouble());
        }
        
        charts.put(coinId, priceList);
        
        // Sleep to avoid rate limit
        Thread.sleep(200);

      } catch (Exception e) {
        System.err.println("⚠️ Failed to fetch chart for " + coinId);
      }
    }

    System.out.println("📊 Chart data fetched for " + charts.size() + " coins");
    return charts;
  }

  private List<CoinDto> buildDtosFromCache() {
    List<CoinDto> result = new ArrayList<>();

    for (Map.Entry<String, String> entry : coins.entrySet()) {
      String id = entry.getKey();
      String symbol = entry.getValue();

      MarketData data = cache.get(id);
      if (data == null) {
        result.add(noData(symbol));
        continue;
      }

      // Get historical prices if available
      List<Double> prices = chartCache.get(id);
      
      // Calculate technical indicators
      TechnicalIndicators.SignalResult signal;
      
      if (prices != null && prices.size() >= 30) {
        // Full professional analysis
        double rsi = TechnicalIndicators.calculateRSI(prices, 14);
        double[] macd = TechnicalIndicators.calculateMACD(prices);
        int trendScore = TechnicalIndicators.analyzeTrend(
            data.change1h.doubleValue(),
            data.change24h.doubleValue(),
            data.change7d.doubleValue(),
            data.change30d.doubleValue()
        );
        
        double avgVolume = prices.stream().mapToDouble(Double::doubleValue).average().orElse(1);
        double volumeMultiplier = TechnicalIndicators.analyzeVolume(
            data.volume.doubleValue(),
            avgVolume
        );

        signal = TechnicalIndicators.generateSignal(
            rsi,
            macd,
            trendScore,
            volumeMultiplier,
            data.change24h.doubleValue()
        );
      } else {
        // Simplified analysis (no historical data)
        int trendScore = TechnicalIndicators.analyzeTrend(
            0, // No 1h data
            data.change24h.doubleValue(),
            data.change7d.doubleValue(),
            data.change30d.doubleValue()
        );
        
        // Estimate RSI from price changes
        double estimatedRSI = 50 + (data.change24h.doubleValue() * 2);
        estimatedRSI = Math.max(0, Math.min(100, estimatedRSI));
        
        signal = TechnicalIndicators.generateSignal(
            estimatedRSI,
            new double[]{0, 0, data.change24h.doubleValue() > 0 ? 0.5 : -0.5},
            trendScore,
            1.0,
            data.change24h.doubleValue()
        );
      }

      result.add(new CoinDto(
          symbol,
          "LIVE",
          fmtMoney(data.price),
          fmtPct(data.change24h),
          String.valueOf(signal.confidence),
          signal.signal,
          signal.color
      ));
    }

    return result;
  }

  private static CoinDto noData(String symbol) {
    return new CoinDto(symbol, "NO_DATA", "0", "0", "0", "NEUTRAL", "#474d57");
  }

  private static BigDecimal bd(JsonNode n) {
    if (n == null || n.isNull()) return BigDecimal.ZERO;
    try {
      return new BigDecimal(n.asText());
    } catch (Exception e) {
      return BigDecimal.ZERO;
    }
  }

  private static String fmtMoney(BigDecimal v) {
    if (v == null) return "0";
    int scale = v.compareTo(BigDecimal.valueOf(100)) >= 0 ? 2 : 4;
    return v.setScale(scale, RoundingMode.HALF_UP).toPlainString();
  }

  private static String fmtPct(BigDecimal v) {
    if (v == null) return "0";
    return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private record MarketData(
      BigDecimal price,
      BigDecimal change1h,
      BigDecimal change24h,
      BigDecimal change7d,
      BigDecimal change30d,
      BigDecimal volume,
      BigDecimal marketCap
  ) {}
}
