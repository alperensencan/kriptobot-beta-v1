package com.example.kriptobot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Professional Technical Analysis Indicators
 * - RSI (Relative Strength Index)
 * - MACD (Moving Average Convergence Divergence)
 * - EMA (Exponential Moving Average)
 * - Trend Analysis
 */
public class TechnicalIndicators {

    /**
     * Calculate RSI (Relative Strength Index)
     * 0-30: Oversold (BUY signal)
     * 30-70: Neutral
     * 70-100: Overbought (SELL signal)
     */
    public static double calculateRSI(List<Double> prices, int period) {
        if (prices == null || prices.size() < period + 1) {
            return 50.0; // Default neutral
        }

        double avgGain = 0;
        double avgLoss = 0;

        // Calculate initial average gain/loss
        for (int i = 1; i <= period; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                avgGain += change;
            } else {
                avgLoss += Math.abs(change);
            }
        }

        avgGain /= period;
        avgLoss /= period;

        if (avgLoss == 0) {
            return 100.0; // All gains, maximum RSI
        }

        double rs = avgGain / avgLoss;
        double rsi = 100.0 - (100.0 / (1.0 + rs));

        return Math.round(rsi * 100.0) / 100.0;
    }

    /**
     * Calculate EMA (Exponential Moving Average)
     */
    public static double calculateEMA(List<Double> prices, int period) {
        if (prices == null || prices.isEmpty()) {
            return 0;
        }

        double multiplier = 2.0 / (period + 1);
        double ema = prices.get(0);

        for (int i = 1; i < prices.size(); i++) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }

        return ema;
    }

    /**
     * Calculate MACD
     * Returns: [MACD line, Signal line, Histogram]
     * Positive histogram = Bullish
     * Negative histogram = Bearish
     */
    public static double[] calculateMACD(List<Double> prices) {
        if (prices == null || prices.size() < 26) {
            return new double[]{0, 0, 0};
        }

        double ema12 = calculateEMA(prices, 12);
        double ema26 = calculateEMA(prices, 26);
        double macdLine = ema12 - ema26;

        // Signal line is 9-day EMA of MACD line
        // Simplified: we'll use a basic average for signal
        double signalLine = macdLine * 0.8; // Approximation

        double histogram = macdLine - signalLine;

        return new double[]{macdLine, signalLine, histogram};
    }

    /**
     * Analyze trend based on multiple timeframes
     * Returns trend strength: -100 to +100
     * Positive = Bullish, Negative = Bearish
     */
    public static int analyzeTrend(double change1h, double change24h, double change7d, double change30d) {
        int score = 0;

        // 1 hour trend (weight: 10)
        if (change1h > 2) score += 10;
        else if (change1h > 0.5) score += 5;
        else if (change1h < -2) score -= 10;
        else if (change1h < -0.5) score -= 5;

        // 24 hour trend (weight: 25)
        if (change24h > 5) score += 25;
        else if (change24h > 2) score += 15;
        else if (change24h > 0) score += 5;
        else if (change24h < -5) score -= 25;
        else if (change24h < -2) score -= 15;
        else score -= 5;

        // 7 day trend (weight: 35)
        if (change7d > 15) score += 35;
        else if (change7d > 5) score += 20;
        else if (change7d > 0) score += 10;
        else if (change7d < -15) score -= 35;
        else if (change7d < -5) score -= 20;
        else score -= 10;

        // 30 day trend (weight: 30)
        if (change30d > 30) score += 30;
        else if (change30d > 10) score += 20;
        else if (change30d > 0) score += 10;
        else if (change30d < -30) score -= 30;
        else if (change30d < -10) score -= 20;
        else score -= 10;

        return Math.max(-100, Math.min(100, score));
    }

    /**
     * Volume analysis
     * Compares current volume to average
     * Returns multiplier (e.g., 2.5 means 250% of average volume)
     */
    public static double analyzeVolume(double currentVolume, double avgVolume) {
        if (avgVolume == 0) return 1.0;
        return currentVolume / avgVolume;
    }

    /**
     * Generate professional trading signal
     * Based on RSI, MACD, Trend, and Volume
     */
    public static class SignalResult {
        public String signal;        // STRONG BUY, BUY, NEUTRAL, SELL, STRONG SELL
        public String color;         // Color code
        public int confidence;       // 0-100
        public String analysis;      // Detailed explanation
        public double rsi;           // RSI value
        public int trendScore;       // Trend strength
        public String recommendation; // Trading recommendation

        public SignalResult(String signal, String color, int confidence, String analysis,
                          double rsi, int trendScore, String recommendation) {
            this.signal = signal;
            this.color = color;
            this.confidence = confidence;
            this.analysis = analysis;
            this.rsi = rsi;
            this.trendScore = trendScore;
            this.recommendation = recommendation;
        }
    }

    /**
     * MAIN SIGNAL GENERATOR
     * Professional multi-indicator signal
     */
    public static SignalResult generateSignal(
            double rsi,
            double[] macd,
            int trendScore,
            double volumeMultiplier,
            double change24h
    ) {
        int signalScore = 0;
        StringBuilder analysis = new StringBuilder();

        // RSI Analysis (40% weight)
        if (rsi <= 30) {
            signalScore += 40;
            analysis.append("RSI oversold (").append(String.format("%.1f", rsi)).append("), ");
        } else if (rsi <= 40) {
            signalScore += 20;
            analysis.append("RSI low (").append(String.format("%.1f", rsi)).append("), ");
        } else if (rsi >= 70) {
            signalScore -= 40;
            analysis.append("RSI overbought (").append(String.format("%.1f", rsi)).append("), ");
        } else if (rsi >= 60) {
            signalScore -= 20;
            analysis.append("RSI high (").append(String.format("%.1f", rsi)).append("), ");
        } else {
            analysis.append("RSI neutral (").append(String.format("%.1f", rsi)).append("), ");
        }

        // MACD Analysis (30% weight)
        double macdHistogram = macd[2];
        if (macdHistogram > 0.5) {
            signalScore += 30;
            analysis.append("MACD bullish, ");
        } else if (macdHistogram > 0) {
            signalScore += 15;
            analysis.append("MACD weak bullish, ");
        } else if (macdHistogram < -0.5) {
            signalScore -= 30;
            analysis.append("MACD bearish, ");
        } else {
            signalScore -= 15;
            analysis.append("MACD weak bearish, ");
        }

        // Trend Analysis (20% weight)
        signalScore += (trendScore * 20) / 100;
        if (trendScore > 50) {
            analysis.append("strong uptrend, ");
        } else if (trendScore > 20) {
            analysis.append("uptrend, ");
        } else if (trendScore < -50) {
            analysis.append("strong downtrend, ");
        } else if (trendScore < -20) {
            analysis.append("downtrend, ");
        } else {
            analysis.append("sideways, ");
        }

        // Volume Analysis (10% weight)
        if (volumeMultiplier > 2.0) {
            signalScore += 10;
            analysis.append("high volume");
        } else if (volumeMultiplier > 1.5) {
            signalScore += 5;
            analysis.append("above avg volume");
        } else if (volumeMultiplier < 0.5) {
            signalScore -= 5;
            analysis.append("low volume");
        } else {
            analysis.append("normal volume");
        }

        // Generate final signal
        String signal;
        String color;
        String recommendation;
        int confidence = Math.min(100, Math.abs(signalScore));

        if (signalScore >= 60) {
            signal = "STRONG BUY";
            color = "#00ff41";
            recommendation = "Excellent entry point. Consider buying.";
        } else if (signalScore >= 30) {
            signal = "BUY";
            color = "#2ecc71";
            recommendation = "Good opportunity. Watch for confirmation.";
        } else if (signalScore >= -30) {
            signal = "NEUTRAL";
            color = "#95a5a6";
            recommendation = "Wait and observe. No clear signal.";
        } else if (signalScore >= -60) {
            signal = "SELL";
            color = "#ff6b6b";
            recommendation = "Consider taking profits or reducing position.";
        } else {
            signal = "STRONG SELL";
            color = "#ff0000";
            recommendation = "High risk. Consider exiting position.";
        }

        return new SignalResult(
            signal,
            color,
            confidence,
            analysis.toString(),
            rsi,
            trendScore,
            recommendation
        );
    }
}
