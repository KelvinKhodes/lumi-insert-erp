package lumi.insert.app.utils.generator;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

@Component
public class NumberFormatter {

    @Value("${lumi.locale:en,US}")
    private String localeEnv;

    private static Currency instance;

    private static NumberFormat currencyInstance;

    @PostConstruct
    public void init() {
        String[] parts = localeEnv.split(",");
        Locale locale = Locale.of(parts[0].trim(), parts[1].trim());
        NumberFormatter.instance = Currency.getInstance(locale);
        NumberFormatter.currencyInstance = NumberFormat.getCurrencyInstance(locale);
    }

    public static BigDecimal normalizeBigDecimal(BigDecimal data, int scale){
        return data.setScale(scale, RoundingMode.HALF_UP);
    }

    public static String convertToCurrency(BigDecimal data){
        return currencyInstance.format(normalizeBigDecimal(data, instance.getDefaultFractionDigits()));
    }
}
