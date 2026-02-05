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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CoinService {

  /**
   * CoinGecko ücretsiz uç noktası. Rate-limit'e takılmamak için:
   * - Sunucuda cache (TTL 90 saniye - daha uzun)
   * - Frontend'de daha seyrek polling
   */
  private static final Duration CACHE_TTL = Duration.ofSeconds(90);

  private final ObjectMapper om = new ObjectMapper();
  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  /**
   * Ekranda göstermek istediğin coinler.
   * key: CoinGecko "id" (endpointte kullanılacak)
   * value: UI'da yazılacak isim
   */
  private final Map<String, String> coins = new LinkedHashMap<>() {{
    // Top 20
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
    
    // 21-40
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
    put("quant-network", "QNT / USDT");
    put("aave", "AAVE / USDT");
    put("the-graph", "GRT / USDT");
    put("fantom", "FTM / USDT");
    put("eos", "EOS / USDT");
    put("theta-token", "THETA / USDT");
    put("axie-infinity", "AXS / USDT");
    put("flow", "FLOW / USDT");
    put("elrond-erd-2", "EGLD / USDT");
    
    // 41-60
    put("sandbox", "SAND / USDT");
    put("decentraland", "MANA / USDT");
    put("tezos", "XTZ / USDT");
    put("maker", "MKR / USDT");
    put("bitcoin-cash-sv", "BSV / USDT");
    put("neo", "NEO / USDT");
    put("kucoin-shares", "KCS / USDT");
    put("iota", "IOTA / USDT");
    put("zcash", "ZEC / USDT");
    put("curve-dao-token", "CRV / USDT");
    put("chiliz", "CHZ / USDT");
    put("enjincoin", "ENJ / USDT");
    put("1inch", "1INCH / USDT");
    put("pancakeswap-token", "CAKE / USDT");
    put("compound-ether", "CETH / USDT");
    put("thorchain", "RUNE / USDT");
    put("zilliqa", "ZIL / USDT");
    put("gala", "GALA / USDT");
    put("nexo", "NEXO / USDT");
    put("dash", "DASH / USDT");
    
    // 61-80
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
    
    // 81-100
    put("oasis-network", "ROSE / USDT");
    put("sushi", "SUSHI / USDT");
    put("stacks", "STX / USDT");
    put("iostoken", "IOST / USDT");
    put("harmony", "ONE / USDT");
    put("ankr", "ANKR / USDT");
    put("injective-protocol", "INJ / USDT");
    put("terra-luna-2", "LUNA / USDT");
    put("osmosis", "OSMO / USDT");
    put("rocket-pool", "RPL / USDT");
    put("nervos-network", "CKB / USDT");
    put("woo-network", "WOO / USDT");
    put("pepe", "PEPE / USDT");
    put("bonk", "BONK / USDT");
    put("floki", "FLOKI / USDT");
    put("blur", "BLUR / USDT");
    put("sui", "SUI / USDT");
    put("sei-network", "SEI / USDT");
    put("celestia", "TIA / USDT");
    put("pendle", "PENDLE / USDT");
    
    // 101-120 - Yeni ve Popüler
    put("render-token", "RNDR / USDT");
    put("kaspa", "KAS / USDT");
    put("mantle", "MNT / USDT");
    put("bittensor", "TAO / USDT");
    put("fetch-ai", "FET / USDT");
    put("raydium", "RAY / USDT");
    put("jito-governance-token", "JTO / USDT");
    put("ondo-finance", "ONDO / USDT");
    put("wormhole", "W / USDT");
    put("jupiter-exchange-solana", "JUP / USDT");
    put("worldcoin-wld", "WLD / USDT");
    put("starknet", "STRK / USDT");
    put("dymension", "DYM / USDT");
    put("pyth-network", "PYTH / USDT");
    put("aethir", "ATH / USDT");
    put("ethena", "ENA / USDT");
    put("weth", "WETH / USDT");
    put("wrapped-bitcoin", "WBTC / BTC");
    put("ethena-usde", "USDE / USD");
    put("first-digital-usd", "FDUSD / USD");
  }};

  private volatile Instant lastFetch = Instant.EPOCH;
  private volatile Map<String, MarketPoint> cache = new LinkedHashMap<>();

  public List<CoinDto> getPiyasa() {
    refreshIfNeeded();
    return buildDtosFromCache();
  }

  private void refreshIfNeeded() {
    Instant now = Instant.now();
    if (Duration.between(lastFetch, now).compareTo(CACHE_TTL) < 0 && !cache.isEmpty()) return;

    // CoinGecko'dan çek
    Map<String, MarketPoint> cg = fetchFromCoingecko();
    if (!cg.isEmpty()) {
      cache = cg;
      lastFetch = now;
      return;
    }

    // CoinGecko başarısız oldu, cache'i bozma
    lastFetch = now;
  }

  private List<CoinDto> buildDtosFromCache() {
    List<CoinDto> out = new ArrayList<>();
    for (Map.Entry<String, String> e : coins.entrySet()) {
      String id = e.getKey();
      String symbol = e.getValue();

      MarketPoint p = cache.get(id);
      if (p == null) {
        out.add(noData(symbol));
        continue;
      }

      BigDecimal change = p.change24hPct;
      Signal s = signalFromChange(change);
      BigDecimal confidence = change.abs().multiply(BigDecimal.valueOf(20))
          .min(BigDecimal.valueOf(100));

      out.add(new CoinDto(
          symbol,
          "LIVE",
          fmtMoney(p.priceUsd),
          fmtPct(change),
          confidence.setScale(0, RoundingMode.HALF_UP).toPlainString(),
          s.name,
          s.color
      ));
    }
    return out;
  }

  private static CoinDto noData(String symbol) {
    return new CoinDto(symbol, "NO_DATA", "0", "0", "0", "NEUTRAL", "#474d57");
  }

  private Map<String, MarketPoint> fetchFromCoingecko() {
    try {
      String ids = String.join(",", coins.keySet());
      String url = "https://api.coingecko.com/api/v3/simple/price" +
          "?ids=" + ids +
          "&vs_currencies=usd" +
          "&include_24hr_change=true" +
          "&precision=full";

      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(30))
          .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
          .header("Accept", "application/json")
          .GET()
          .build();

      HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() != 200) {
        System.err.println("CoinGecko HTTP " + res.statusCode() + ": " + res.body());
        return Map.of();
      }

      JsonNode root = om.readTree(res.body());
      Map<String, MarketPoint> out = new LinkedHashMap<>();

      for (String id : coins.keySet()) {
        JsonNode node = root.get(id);
        if (node == null) continue;
        BigDecimal price = bd(node.get("usd"));
        BigDecimal chg = bd(node.get("usd_24h_change"));
        if (price == null || chg == null) continue;
        out.put(id, new MarketPoint(price, chg));
      }
      
      System.out.println("CoinGecko SUCCESS: " + out.size() + " coins fetched");
      return out;
    } catch (Exception e) {
      System.err.println("CoinGecko ERROR: " + e.getMessage());
      return Map.of();
    }
  }

  private static BigDecimal bd(JsonNode n) {
    if (n == null || n.isNull()) return null;
    try {
      return new BigDecimal(n.asText());
    } catch (Exception e) {
      return null;
    }
  }

  private static String fmtMoney(BigDecimal v) {
    if (v == null) return "0";
    // Büyük coinlerde 2, küçüklerde 4 ondalık
    int scale = v.compareTo(BigDecimal.valueOf(100)) >= 0 ? 2 : 4;
    return v.setScale(scale, RoundingMode.HALF_UP).toPlainString();
  }

  private static String fmtPct(BigDecimal v) {
    if (v == null) return "0";
    return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private static Signal signalFromChange(BigDecimal chg) {
    if (chg == null) return Signal.NEUTRAL;
    // Basit sinyal: +/- 2% üstü BUY/SELL
    if (chg.compareTo(BigDecimal.valueOf(2)) >= 0) return Signal.BUY;
    if (chg.compareTo(BigDecimal.valueOf(-2)) <= 0) return Signal.SELL;
    return Signal.NEUTRAL;
  }

  private record MarketPoint(BigDecimal priceUsd, BigDecimal change24hPct) {}

  private enum Signal {
    BUY("BUY", "#2ecc71"),
    SELL("SELL", "#ff4d4f"),
    NEUTRAL("NEUTRAL", "#474d57");

    final String name;
    final String color;

    Signal(String name, String color) {
      this.name = name;
      this.color = color;
    }
  }
}
