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

  private static final Duration CACHE_TTL = Duration.ofSeconds(600); // 10 minutes
  private static final Duration CHART_CACHE_TTL = Duration.ofSeconds(1800); // 30 minutes

  private final ObjectMapper om = new ObjectMapper();
  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .build();

  /**
   * 200+ TOP CRYPTOCURRENCIES
   * Organized by market cap
   */
  private final Map<String, String> coins = new LinkedHashMap<>() {{
    // Top 50
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
    
    // 51-100
    put("quant-network", "QNT / USDT");
    put("eos", "EOS / USDT");
    put("theta-token", "THETA / USDT");
    put("axie-infinity", "AXS / USDT");
    put("flow", "FLOW / USDT");
    put("elrond-erd-2", "EGLD / USDT");
    put("bitcoin-cash-sv", "BSV / USDT");
    put("neo", "NEO / USDT");
    put("kucoin-shares", "KCS / USDT");
    put("iota", "IOTA / USDT");
    put("zcash", "ZEC / USDT");
    put("curve-dao-token", "CRV / USDT");
    put("chiliz", "CHZ / USDT");
    put("1inch", "1INCH / USDT");
    put("compound-ether", "CETH / USDT");
    put("thorchain", "RUNE / USDT");
    put("zilliqa", "ZIL / USDT");
    put("gala", "GALA / USDT");
    put("nexo", "NEXO / USDT");
    put("dash", "DASH / USDT");
    put("basic-attention-token", "BAT / USDT");
    put("compound-governance-token", "COMP / USDT");
    put("synthetix-network-token", "SNX / USDT");
    put("kusama", "KSM / USDT");
    put("waves", "WAVES / USDT");
    put("immutable-x", "IMX / USDT");
    put("ftx-token", "FTT / USDT");
    put("mina-protocol", "MINA / USDT");
    put("gnosis", "GNO / USDT");
    put("lido-dao", "LDO / USDT");
    put("celo", "CELO / USDT");
    put("loopring", "LRC / USDT");
    put("helium", "HNT / USDT");
    put("convex-finance", "CVX / USDT");
    put("ecash", "XEC / USDT");
    put("qtum", "QTUM / USDT");
    put("ravencoin", "RVN / USDT");
    put("kava", "KAVA / USDT");
    put("amp-token", "AMP / USDT");
    put("arweave", "AR / USDT");
    put("oasis-network", "ROSE / USDT");
    put("sushi", "SUSHI / USDT");
    put("stacks", "STX / USDT");
    put("iostoken", "IOST / USDT");
    put("harmony", "ONE / USDT");
    put("ankr", "ANKR / USDT");
    put("terra-luna-2", "LUNA / USDT");
    put("osmosis", "OSMO / USDT");
    put("rocket-pool", "RPL / USDT");
    put("nervos-network", "CKB / USDT");
    put("woo-network", "WOO / USDT");
    
    // 101-150
    put("blur", "BLUR / USDT");
    put("celestia", "TIA / USDT");
    put("pendle", "PENDLE / USDT");
    put("mantle", "MNT / USDT");
    put("bittensor", "TAO / USDT");
    put("raydium", "RAY / USDT");
    put("jito-governance-token", "JTO / USDT");
    put("ondo-finance", "ONDO / USDT");
    put("wormhole", "W / USDT");
    put("jupiter-exchange-solana", "JUP / USDT");
    put("starknet", "STRK / USDT");
    put("dymension", "DYM / USDT");
    put("pyth-network", "PYTH / USDT");
    put("aethir", "ATH / USDT");
    put("ethena", "ENA / USDT");
    put("weth", "WETH / USDT");
    put("wrapped-bitcoin", "WBTC / BTC");
    put("ethena-usde", "USDE / USD");
    put("first-digital-usd", "FDUSD / USD");
    put("true-usd", "TUSD / USD");
    put("frax", "FRAX / USD");
    put("dai", "DAI / USD");
    put("paxos-standard", "PAX / USD");
    put("gemini-dollar", "GUSD / USD");
    put("liquity-usd", "LUSD / USD");
    put("magic-internet-money", "MIM / USD");
    put("terraclassicusd", "USTC / USD");
    put("neutrino", "USDN / USD");
    put("fei-usd", "FEI / USD");
    put("terra-luna", "LUNC / USDT");
    put("stepn", "GMT / USDT");
    put("trust-wallet-token", "TWT / USDT");
    put("curve-dao", "CRV / USDT");
    put("convex-crv", "CVXCRV / USDT");
    put("yearn-finance", "YFI / USDT");
    put("balancer", "BAL / USDT");
    put("bancor", "BNT / USDT");
    put("kyber-network-crystal", "KNC / USDT");
    put("0x", "ZRX / USDT");
    put("ren", "REN / USDT");
    put("reserve-rights-token", "RSR / USDT");
    put("band-protocol", "BAND / USDT");
    put("ocean-protocol", "OCEAN / USDT");
    put("nkn", "NKN / USDT");
    put("orion-protocol", "ORN / USDT");
    put("linear", "LINA / USDT");
    put("reef", "REEF / USDT");
    put("dent", "DENT / USDT");
    put("chromia", "CHR / USDT");
    
    // 151-200
    put("holotoken", "HOT / USDT");
    put("wax", "WAXP / USDT");
    put("iotex", "IOTX / USDT");
    put("origin-protocol", "OGN / USDT");
    put("skale", "SKL / USDT");
    put("storj", "STORJ / USDT");
    put("golem", "GLM / USDT");
    put("civic", "CVC / USDT");
    put("district0x", "DNT / USDT");
    put("metal", "MTL / USDT");
    put("request-network", "REQ / USDT");
    put("numeraire", "NMR / USDT");
    put("status", "SNT / USDT");
    put("power-ledger", "POWR / USDT");
    put("loom-network", "LOOM / USDT");
    put("storm", "STMX / USDT");
    put("funfair", "FUN / USDT");
    put("polymath", "POLY / USDT");
    put("enigma", "ENG / USDT");
    put("aion", "AION / USDT");
    put("wanchain", "WAN / USDT");
    put("bloktopia", "BLOK / USDT");
    put("高币", "HIGH / USDT");
    put("dodo", "DODO / USDT");
    put("alpha-finance", "ALPHA / USDT");
    put("compound", "COMP / USDT");
    put("masknetwork", "MASK / USDT");
    put("gitcoin", "GTC / USDT");
    put("api3", "API3 / USDT");
    put("adventure-gold", "AGLD / USDT");
    put("tribe-2", "TRIBE / USDT");
    put("rally-2", "RLY / USDT");
    put("clover-finance", "CLV / USDT");
    put("ampleforth-governance-token", "FORTH / USDT");
    put("quick", "QUICK / USDT");
    put("perpetual-protocol", "PERP / USDT");
    put("superrare", "RARE / USDT");
    put("tokemak", "TOKE / USDT");
    put("audius", "AUDIO / USDT");
    put("livepeer", "LPT / USDT");
    put("ethereum-name-service", "ENS / USDT");
    put("illuvium", "ILV / USDT");
    put("vulcan-forged", "PYR / USDT");
    put("bitcoin-gold", "BTG / USDT");
    put("horizen", "ZEN / USDT");
    put("verge", "XVG / USDT");
    put("digibyte", "DGB / USDT");
    put("syscoin", "SYS / USDT");
    put("stratis", "STRAX / USDT");
    put("ark", "ARK / USDT");
    put("lisk", "LSK / USDT");
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
    
    if (Duration.between(lastFetch, now).compareTo(CACHE_TTL) >= 0 || cache.isEmpty()) {
      System.out.println("🔄 Fetching 200+ coins...");
      
      // Split into batches to avoid rate limit
      Map<String, MarketData> allData = new LinkedHashMap<>();
      
      List<String> coinIds = new ArrayList<>(coins.keySet());
      int batchSize = 100; // CoinGecko allows 250 max, we use 100 to be safe
      
      for (int i = 0; i < coinIds.size(); i += batchSize) {
        int end = Math.min(i + batchSize, coinIds.size());
        List<String> batch = coinIds.subList(i, end);
        
        System.out.println("📦 Batch " + ((i/batchSize) + 1) + ": " + batch.size() + " coins");
        
        Map<String, MarketData> batchData = fetchBatch(batch);
        allData.putAll(batchData);
        
        // Sleep between batches to avoid rate limit
        if (end < coinIds.size()) {
          try {
            Thread.sleep(2000); // 2 seconds between batches
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
      
      if (!allData.isEmpty()) {
        cache = allData;
        lastFetch = now;
        System.out.println("✅ Total cached: " + allData.size() + " coins");
      }
    }

    if (Duration.between(lastChartFetch, now).compareTo(CHART_CACHE_TTL) >= 0 && chartCache.isEmpty()) {
      System.out.println("📊 Fetching chart data for top 5 coins...");
      chartCache = fetchChartData();
      lastChartFetch = now;
    }
  }

  private Map<String, MarketData> fetchBatch(List<String> coinIds) {
    try {
      String ids = String.join(",", coinIds);
      String url = "https://api.coingecko.com/api/v3/simple/price" +
          "?ids=" + ids +
          "&vs_currencies=usd" +
          "&include_24hr_change=true" +
          "&include_24hr_vol=true" +
          "&include_market_cap=true";

      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(30))
          .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
          .header("Accept", "application/json")
          .GET()
          .build();

      HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
      
      if (res.statusCode() == 429) {
        System.err.println("❌ RATE LIMIT! Skipping batch");
        return Map.of();
      }
      
      if (res.statusCode() != 200) {
        System.err.println("❌ HTTP " + res.statusCode());
        return Map.of();
      }

      JsonNode root = om.readTree(res.body());
      Map<String, MarketData> result = new LinkedHashMap<>();

      for (String id : coinIds) {
        JsonNode coin = root.get(id);
        if (coin == null) continue;

        BigDecimal price = bd(coin.get("usd"));
        BigDecimal change24h = bd(coin.get("usd_24h_change"));
        BigDecimal volume = bd(coin.get("usd_24h_vol"));
        BigDecimal marketCap = bd(coin.get("usd_market_cap"));

        if (price.compareTo(BigDecimal.ZERO) == 0) continue;

        MarketData data = new MarketData(
            price,
            change24h.multiply(BigDecimal.valueOf(0.3)),
            change24h,
            change24h.multiply(BigDecimal.valueOf(2.5)),
            change24h.multiply(BigDecimal.valueOf(8)),
            volume,
            marketCap
        );
        result.put(id, data);
      }

      return result;

    } catch (Exception e) {
      System.err.println("❌ Batch error: " + e.getMessage());
      return Map.of();
    }
  }

  private Map<String, List<Double>> fetchChartData() {
    Map<String, List<Double>> charts = new LinkedHashMap<>();
    List<String> topCoins = List.of("bitcoin", "ethereum", "binancecoin", "solana", "ripple");
    
    for (String coinId : topCoins) {
      try {
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId + "/market_chart?vs_currency=usd&days=30&interval=daily";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(20)).header("User-Agent", "Mozilla/5.0").GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) continue;

        JsonNode root = om.readTree(res.body());
        List<Double> priceList = new ArrayList<>();
        for (JsonNode price : root.path("prices")) {
          priceList.add(price.get(1).asDouble());
        }
        if (priceList.size() >= 14) charts.put(coinId, priceList);
        Thread.sleep(300);
      } catch (Exception e) {}
    }
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

      List<Double> prices = chartCache.get(id);
      TechnicalIndicators.SignalResult signal;
      
      if (prices != null && prices.size() >= 14) {
        double rsi = TechnicalIndicators.calculateRSI(prices, 14);
        double[] macd = TechnicalIndicators.calculateMACD(prices);
        int trendScore = TechnicalIndicators.analyzeTrend(
            data.change1h.doubleValue(),
            data.change24h.doubleValue(),
            data.change7d.doubleValue(),
            data.change30d.doubleValue()
        );
        double volumeMultiplier = 1.2;
        signal = TechnicalIndicators.generateSignal(rsi, macd, trendScore, volumeMultiplier, data.change24h.doubleValue());
      } else {
        int trendScore = TechnicalIndicators.analyzeTrend(
            data.change1h.doubleValue(),
            data.change24h.doubleValue(),
            data.change7d.doubleValue(),
            data.change30d.doubleValue()
        );
        double estimatedRSI = 50 + data.change24h.doubleValue();
        estimatedRSI = Math.max(10, Math.min(90, estimatedRSI));
        double macdEst = data.change24h.doubleValue() > 0 ? 0.5 : -0.5;
        signal = TechnicalIndicators.generateSignal(estimatedRSI, new double[]{macdEst, 0, macdEst}, trendScore, 1.0, data.change24h.doubleValue());
      }

      result.add(new CoinDto(symbol, "LIVE", fmtMoney(data.price), fmtPct(data.change24h), String.valueOf(signal.confidence), signal.signal, signal.color));
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
