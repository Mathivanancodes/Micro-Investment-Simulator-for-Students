package com.microinvestment.simulator.service;

import com.microinvestment.simulator.model.Stock;
import com.microinvestment.simulator.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
public class MarketDataClient {

    private final RestTemplate restTemplate;
    private final StockRepository stockRepository;
    private final Random random = new Random();

    @Value("${market.data.api.key}")
    private String apiKey;

    @Value("${market.data.api.provider}")
    private String provider;

    // Static baseline prices for seeded stocks (used if not present in DB)
    private static final Map<String, BigDecimal> BASE_PRICES = new HashMap<>();
    static {
        BASE_PRICES.put("AAPL", new BigDecimal("185.00"));
        BASE_PRICES.put("MSFT", new BigDecimal("420.00"));
        BASE_PRICES.put("GOOGL", new BigDecimal("175.00"));
        BASE_PRICES.put("NVDA", new BigDecimal("120.00"));
        BASE_PRICES.put("AMZN", new BigDecimal("180.00"));
        BASE_PRICES.put("TSLA", new BigDecimal("170.00"));
        BASE_PRICES.put("DIS", new BigDecimal("100.00"));
        BASE_PRICES.put("WMT", new BigDecimal("65.00"));
        BASE_PRICES.put("JPM", new BigDecimal("195.00"));
        BASE_PRICES.put("BAC", new BigDecimal("38.00"));
        BASE_PRICES.put("V", new BigDecimal("270.00"));
        BASE_PRICES.put("KO", new BigDecimal("62.00"));
    }

    @Autowired
    public MarketDataClient(RestTemplateBuilder restTemplateBuilder, StockRepository stockRepository) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.stockRepository = stockRepository;
    }

    /**
     * Fetches the current stock price. Falls back to generating a realistic mock fluctuation
     * if the API key is not configured, is invalid, or if rate limits are exceeded.
     */
    public BigDecimal fetchCurrentPrice(String symbol) {
        if (isMockMode()) {
            return generateMockPrice(symbol);
        }

        try {
            String url = "https://api.twelvedata.com/price?symbol={symbol}&apikey={apikey}";
            log.info("Fetching real-time price for {} from Twelve Data API", symbol);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class, symbol, apiKey);
            Map<String, Object> body = response.getBody();

            if (body != null) {
                // Twelve Data returns {"status": "error", "message": "..."} for issues
                if (body.containsKey("status") && "error".equalsIgnoreCase(String.valueOf(body.get("status")))) {
                    String errorMsg = String.valueOf(body.get("message"));
                    log.warn("Twelve Data API error for symbol {}: {}. Falling back to mock price.", symbol, errorMsg);
                    return generateMockPrice(symbol);
                }

                if (body.containsKey("price")) {
                    String priceStr = String.valueOf(body.get("price"));
                    BigDecimal price = new BigDecimal(priceStr).setScale(2, RoundingMode.HALF_UP);
                    log.info("Successfully fetched price for {}: ${}", symbol, price);
                    return price;
                }
            }

            log.warn("Invalid response structure from Twelve Data for symbol {}. Falling back to mock price.", symbol);
            return generateMockPrice(symbol);

        } catch (Exception e) {
            log.error("Failed to fetch price for {} due to network/API error: {}. Falling back to mock price.", symbol, e.getMessage());
            return generateMockPrice(symbol);
        }
    }

    /**
     * Checks whether we should run in mock mode.
     */
    private boolean isMockMode() {
        return apiKey == null || 
               apiKey.trim().isEmpty() || 
               "YOUR_TWELVE_DATA_API_KEY_HERE".equalsIgnoreCase(apiKey.trim());
    }

    /**
     * Generates a mock stock price fluctuation of -1.5% to +1.5% based on the last known price.
     */
    private BigDecimal generateMockPrice(String symbol) {
        BigDecimal basePrice = BASE_PRICES.getOrDefault(symbol, new BigDecimal("100.00"));
        
        // Check if we have a last known price in the database
        Optional<Stock> existingStock = stockRepository.findBySymbol(symbol);
        if (existingStock.isPresent()) {
            basePrice = existingStock.get().getCurrentPrice();
        }

        // Generate a random fluctuation percentage between -1.5% and +1.5%
        // random.nextDouble() returns 0.0 to 1.0. 
        // (random.nextDouble() * 3.0) - 1.5 gives a range of -1.5 to +1.5.
        double fluctuationPct = (random.nextDouble() * 3.0) - 1.5;
        BigDecimal multiplier = BigDecimal.valueOf(1.0 + (fluctuationPct / 100.0));
        
        BigDecimal newPrice = basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        log.debug("Mocked price update for {}: ${} (fluctuation: {}%)", symbol, newPrice, String.format("%.2f", fluctuationPct));
        return newPrice;
    }
}
