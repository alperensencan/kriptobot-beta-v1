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

@Service
public class CoinService {

  private static final Duration CACHE_TTL = Duration.ofMinutes(20);
  private static final int BATCH_SIZE = 50; // Smaller batches!
  private static final int BATCH_DELAY_MS = 15000; // 15 seconds!

  private final ObjectMapper om = new ObjectMapper();
  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .build();

  // TOP 100 MOST POPULAR COINS ONLY
  private final Map<String, String> coins = new LinkedHashMap<>() {{
    put("bitcoin", "BTC");
    put("ethereum", "ETH");
    put("tether", "USDT");
    put("binancecoin", "BNB");
    put("solana", "SOL");
    put("ripple", "XRP");
    put("usd-coin", "USDC");
    put("cardano", "ADA");
    put("dogecoin", "DOGE");
    put("tron", "TRX");
    put("avalanche-2", "AVAX");
    put("shiba-inu", "SHIB");
    put("polkadot", "DOT");
    put("chainlink", "LINK");
    put("polygon", "MATIC");
    put("litecoin", "LTC");
    put("bitcoin-cash", "BCH");
    put("uniswap", "UNI");
    put("stellar", "XLM");
    put("cosmos", "ATOM");
    put("monero", "XMR");
    put("ethereum-classic", "ETC");
    put("filecoin", "FIL");
    put("internet-computer", "ICP");
    put("aptos", "APT");
    put("hedera-hashgraph", "HBAR");
    put("arbitrum", "ARB");
    put("optimism", "OP");
    put("near", "NEAR");
    put("vechain", "VET");
    put("algorand", "ALGO");
    put("aave", "AAVE");
    put("the-graph", "GRT");
    put("fantom", "FTM");
    put("the-sandbox", "SAND");
    put("decentraland", "MANA");
    put("tezos", "XTZ");
    put("maker", "MKR");
    put("enjincoin", "ENJ");
    put("pancakeswap-token", "CAKE");
    put("pepe", "PEPE");
    put("bonk", "BONK");
    put("floki", "FLOKI");
    put("sui", "SUI");
    put("sei-network", "SEI");
    put("render-token", "RNDR");
    put("kaspa", "KAS");
    put("fetch-ai", "FET");
    put("injective-protocol", "INJ");
    put("worldcoin-wld", "WLD");
    put("quant-network", "QNT");
    put("eos", "EOS");
    put("theta-token", "THETA");
    put("axie-infinity", "AXS");
    put("flow", "FLOW");
    put("multiversx-egld", "EGLD");
    put("bitcoin-cash-sv", "BSV");
    put("neo", "NEO");
    put("kucoin-shares", "KCS");
    put("iota", "MIOTA");
    put("zcash", "ZEC");
    put("curve-dao-token", "CRV");
    put("chiliz", "CHZ");
    put("1inch", "1INCH");
    put("thorchain", "RUNE");
    put("zilliqa", "ZIL");
    put("gala", "GALA");
    put("nexo", "NEXO");
    put("dash", "DASH");
    put("basic-attention-token", "BAT");
    put("compound-governance-token", "COMP");
    put("synthetix-network-token", "SNX");
    put("kusama", "KSM");
    put("waves", "WAVES");
    put("immutable-x", "IMX");
    put("mina-protocol", "MINA");
    put("gnosis", "GNO");
    put("lido-dao", "LDO");
    put("celo", "CELO");
    put("loopring", "LRC");
    put("helium", "HNT");
    put("convex-finance", "CVX");
    put("ecash", "XEC");
    put("qtum", "QTUM");
    put("ravencoin", "RVN");
    put("kava", "KAVA");
    put("arweave", "AR");
    put("oasis-network", "ROSE");
    put("sushi", "SUSHI");
    put("stacks", "STX");
    put("harmony", "ONE");
    put("ankr", "ANKR");
    put("terra-luna-2", "LUNA");
    put("osmosis", "OSMO");
    put("rocket-pool", "RPL");
    put("nervos-network", "CKB");
    put("woo-network", "WOO");
    put("blur", "BLUR");
    put("celestia", "TIA");
    put("pendle", "PENDLE");
  }};

  private volatile Instant lastFetch = Instant.EPOCH;
  private volatile Map<String, MarketData> cache = new LinkedHashMap<>();
  private volatile boolean isFetching = false;

  public List<CoinDto> getPiyasa() {
    refreshIfNeeded();
    return buildDtosFromCache();
  }

  private void refreshIfNeeded() {
    Instant now = Instant.now();
    long cacheAgeMinutes = Duration.between(lastFetch, now).toMinutes();
    
    boolean needsRefresh = cacheAgeMinutes >= 20 || cache.isEmpty();
    
    if (needsRefresh && !isFetching) {
      isFetching = true;
      System.out.println("🔄 Refresh needed (cache age: " + cacheAgeMinutes + " min)");
      
      new Thread(() -> {
        try {
          Map<String, MarketData> allData = fetchAllDataSafely();
          if (!allData.isEmpty()) {
            cache = allData;
            lastFetch = Instant.now();
            System.out.println("✅ CACHE UPDATED: " + allData.size() + "/" + coins.size() + " coins");
          } else {
            System.err.println("❌ Fetch failed completely");
          }
        } catch (Exception e) {
          System.err.println("❌ Exception: " + e.getMessage());
          e.printStackTrace();
        } finally {
          isFetching = false;
        }
      }).start();
    } else if (cache.isEmpty()) {
      System.out.println("⏳ Waiting for first fetch to complete...");
    }
  }

  private Map<String, MarketData> fetchAllDataSafely() {
    Map<String, MarketData> allData = new LinkedHashMap<>();
    List<String> coinIds = new ArrayList<>(coins.keySet());
    
    int totalBatches = (coinIds.size() + BATCH_SIZE - 1) / BATCH_SIZE;
    System.out.println("📊 Total batches: " + totalBatches + " (batch size: " + BATCH_SIZE + ")");
    
    for (int i = 0; i < coinIds.size(); i += BATCH_SIZE) {
      int end = Math.min(i + BATCH_SIZE, coinIds.size());
      List<String> batch = coinIds.subList(i, end);
      int batchNum = (i / BATCH_SIZE) + 1;
      
      System.out.println("\n📦 Batch " + batchNum + "/" + totalBatches + ": " + batch.size() + " coins");
      
      int retries = 0;
      Map<String, MarketData> batchData = null;
      
      while (retries < 3 && (batchData == null || batchData.isEmpty())) {
        if (retries > 0) {
          System.out.println("   🔄 Retry " + retries + "/3...");
        }
        
        batchData = fetchBatchWithRetry(batch);
        
        if (batchData != null && !batchData.isEmpty()) {
          allData.putAll(batchData);
          System.out.println("   ✅ Got " + batchData.size() + "/" + batch.size() + " coins");
          break;
        }
        
        retries++;
        if (retries < 3) {
          try {
            System.out.println("   ⏳ Waiting 30s before retry...");
            Thread.sleep(30000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return allData;
          }
        }
      }
      
      if (batchData == null || batchData.isEmpty()) {
        System.err.println("   ❌ Batch failed after 3 retries, skipping");
      }
      
      // Wait between batches
      if (end < coinIds.size()) {
        try {
          System.out.println("   ⏳ Waiting " + (BATCH_DELAY_MS/1000) + "s before next batch...");
          Thread.sleep(BATCH_DELAY_MS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
    
    System.out.println("\n📊 FINAL: " + allData.size() + "/" + coins.size() + " coins successfully cached");
    return allData;
  }

  private Map<String, MarketData> fetchBatchWithRetry(List<String> coinIds) {
    try {
      String ids = String.join(",", coinIds);
      String url = "https://api.coingecko.com/api/v3/simple/price" +
          "?ids=" + ids +
          "&vs_currencies=usd" +
          "&include_24hr_change=true";

      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(30))
          .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
          .header("Accept", "application/json")
          .GET()
          .build();

      HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
      
      System.out.println("   📡 Response: " + res.statusCode());
      
      if (res.statusCode() == 429) {
        System.err.println("   ⚠️  RATE LIMIT!");
        return Map.of();
      }
      
      if (res.statusCode() != 200) {
        System.err.println("   ❌ HTTP " + res.statusCode() + ": " + res.body().substring(0, Math.min(200, res.body().length())));
        return Map.of();
      }

      JsonNode root = om.readTree(res.body());
      Map<String, MarketData> result = new LinkedHashMap<>();

      for (String id : coinIds) {
        JsonNode coin = root.get(id);
        if (coin == null) {
          System.err.println("   ⚠️  Missing: " + id);
          continue;
        }

        BigDecimal price = bd(coin.get("usd"));
        if (price.compareTo(BigDecimal.ZERO) == 0) {
          System.err.println("   ⚠️  Zero price: " + id);
          continue;
        }

        BigDecimal change24h = bd(coin.get("usd_24h_change"));

        MarketData data = new MarketData(
            price,
            change24h.multiply(BigDecimal.valueOf(0.3)),
            change24h,
            change24h.multiply(BigDecimal.valueOf(2.5)),
            change24h.multiply(BigDecimal.valueOf(8)),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        result.put(id, data);
      }

      return result;

    } catch (Exception e) {
      System.err.println("   ❌ Exception: " + e.getMessage());
      return Map.of();
    }
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

      int trendScore = TechnicalIndicators.analyzeTrend(
          data.change1h.doubleValue(),
          data.change24h.doubleValue(),
          data.change7d.doubleValue(),
          data.change30d.doubleValue()
      );
      
      double estimatedRSI = 50 + data.change24h.doubleValue();
      estimatedRSI = Math.max(10, Math.min(90, estimatedRSI));
      double macdEst = data.change24h.doubleValue() > 0 ? 0.5 : -0.5;
      
      TechnicalIndicators.SignalResult signal = TechnicalIndicators.generateSignal(
          estimatedRSI, 
          new double[]{macdEst, 0, macdEst}, 
          trendScore, 
          1.0, 
          data.change24h.doubleValue()
      );

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
      try {
        return BigDecimal.valueOf(n.asDouble());
      } catch (Exception ex) {
        return BigDecimal.ZERO;
      }
    }
  }

  private static String fmtMoney(BigDecimal v) {
    if (v == null || v.compareTo(BigDecimal.ZERO) == 0) return "0";
    int scale = v.compareTo(BigDecimal.valueOf(100)) >= 0 ? 2 : 4;
    return v.setScale(scale, RoundingMode.HALF_UP).toPlainString();
  }

  private static String fmtPct(BigDecimal v) {
    if (v == null) return "0";
    return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private record MarketData(BigDecimal price, BigDecimal change1h, BigDecimal change24h, BigDecimal change7d, BigDecimal change30d, BigDecimal volume, BigDecimal marketCap) {}
}
