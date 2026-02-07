package com.example.kriptobot;

public class CoinDto {
    public String symbol;
    public String regime;
    public String price;
    public String change;
    public String confidence;
    public String signal;
    public String sigColor;

    public CoinDto() {}

    public CoinDto(String symbol, String regime, String price, String change,
                   String confidence, String signal, String sigColor) {
        this.symbol = symbol;
        this.regime = regime;
        this.price = price;
        this.change = change;
        this.confidence = confidence;
        this.signal = signal;
        this.sigColor = sigColor;
    }
}
