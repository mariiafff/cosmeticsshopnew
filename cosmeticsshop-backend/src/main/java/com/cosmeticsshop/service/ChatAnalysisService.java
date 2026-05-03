package com.cosmeticsshop.service;

import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ChatAnalysisService {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");
    private static final DecimalFormat SUMMARY_DECIMAL_FORMAT = new DecimalFormat("0.00");

    static {
        SUMMARY_DECIMAL_FORMAT.setRoundingMode(RoundingMode.HALF_UP);
    }

    public String buildFinalAnswer(String question, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "No rows were returned for this question.";
        }

        Map<String, Object> firstRow = rows.get(0);
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);

        if (normalized.contains("membership") && firstRow.containsKey("membership_type") && firstRow.containsKey("avg_spend")) {
            return firstRow.get("membership_type") + " is the membership type with the highest average spend: "
                    + formatNumber(firstRow.get("avg_spend")) + ".";
        }

        if (normalized.contains("city") && firstRow.containsKey("city") && (firstRow.containsKey("total_customers") || firstRow.containsKey("customer_count"))) {
            Object customerCount = firstRow.containsKey("customer_count")
                    ? firstRow.get("customer_count")
                    : firstRow.get("total_customers");
            return firstRow.get("city") + " has the highest customer count with "
                    + formatNumber(customerCount) + " customers.";
        }

        if (normalized.contains("country") && firstRow.containsKey("country") && firstRow.containsKey("total_revenue")) {
            return firstRow.get("country") + " generates the most revenue: "
                    + formatNumber(firstRow.get("total_revenue")) + ".";
        }

        if (firstRow.containsKey("percentage_of_recent_orders")) {
            if (isTurkishQuestion(normalized)) {
                return buildTurkishLastOrderOfRecentOrdersPercentageAnswer(firstRow);
            }
            return "Your last order was "
                    + formatNumber(getRowValue(firstRow, "percentage_of_recent_orders"))
                    + "% of your recent orders total.";
        }

        if (firstRow.containsKey("percentage_of_total_spent")) {
            if (isTurkishQuestion(normalized)
                    && getRowValue(firstRow, "last_order_amount") != null
                    && getRowValue(firstRow, "total_spent") != null) {
                return buildTurkishLastOrderPercentageAnswer(firstRow);
            }
            return "Your last order was "
                    + formatNumber(getRowValue(firstRow, "percentage_of_total_spent"))
                    + "% of your total spending.";
        }

        if (isUserScopedQuestion(normalized) && firstRow.containsKey("different_product_count")) {
            return isTurkishQuestion(normalized)
                    ? "Son siparişinizde " + formatNumber(firstRow.get("different_product_count")) + " farklı ürün vardı."
                    : "Your last order had " + formatNumber(firstRow.get("different_product_count")) + " different products.";
        }

        // Most-frequent product result: product_name + total_quantity + order_count
        if (isUserScopedQuestion(normalized)
                && firstRow.containsKey("product_name")
                && firstRow.containsKey("total_quantity")
                && firstRow.containsKey("order_count")) {
            Object productName = firstRow.get("product_name");
            Object totalQty    = firstRow.get("total_quantity");
            Object orderCount  = firstRow.get("order_count");
            return isTurkishQuestion(normalized)
                    ? "Son alışverişlerinizde en sık aldığınız ürün " + productName
                            + " olup toplam " + formatNumber(totalQty)
                            + " adet, " + formatNumber(orderCount) + " farklı siparişte satın alınmıştır."
                    : "The most frequently purchased product is " + productName
                            + " with " + formatNumber(totalQty) + " units across "
                            + formatNumber(orderCount) + " orders.";
        }

        if (isUserScopedQuestion(normalized)
                && firstRow.containsKey("product_name")
                && (firstRow.containsKey("total_quantity") || firstRow.containsKey("total_spent"))) {
            return isTurkishQuestion(normalized)
                    ? "İsteğinize göre " + rows.size() + " ürün listelendi."
                    : "I found " + rows.size() + " products for your request.";
        }


        if (isUserScopedQuestion(normalized)
                && firstRow.containsKey("category_name")
                && (firstRow.containsKey("total_quantity") || firstRow.containsKey("total_spent") || firstRow.containsKey("product_count"))) {
            return isTurkishQuestion(normalized)
                    ? "İsteğinize göre kategori sonuçları listelendi."
                    : "I found the category results for your request.";
        }

        if (isUserScopedQuestion(normalized) && firstRow.containsKey("product_name")) {
            if (rows.size() > 1) {
                return buildLatestOrderItemsAnswer(firstRow, rows, isTurkishQuestion(normalized));
            }
            return buildLatestOrderItemsAnswer(firstRow, rows, isTurkishQuestion(normalized));
        }

        if (isUserScopedQuestion(normalized) && firstRow.containsKey("avg_order_value")) {
            boolean asksAvg = normalized.contains("ortalama") || normalized.contains("average") || normalized.contains("avg");
            if (asksAvg) {
                return isTurkishQuestion(normalized)
                        ? "Ortalama sipariş tutarınız: " + formatNumber(firstRow.get("avg_order_value")) + " TL."
                        : "Your average order value is " + formatNumber(firstRow.get("avg_order_value")) + ".";
            }
        }

        if (isUserScopedQuestion(normalized) && firstRow.containsKey("total_spent")) {
            boolean asksTotal = normalized.contains("toplam") || normalized.contains("total") || normalized.contains("harcadim");
            if (asksTotal || !firstRow.containsKey("avg_order_value")) {
                return isTurkishQuestion(normalized)
                        ? "Toplam harcamanız: " + formatNumber(firstRow.get("total_spent")) + " TL."
                        : "Your total spending is " + formatNumber(firstRow.get("total_spent")) + ".";
            }
        }

        if (isUserScopedQuestion(normalized) && firstRow.containsKey("total_amount")) {
            if (rows.size() > 1) {
                return isTurkishQuestion(normalized)
                        ? "İsteğinize göre son " + rows.size() + " siparişiniz aşağıda listelenmiştir."
                        : "Your last " + rows.size() + " orders are listed below as requested.";
            }
            return isTurkishQuestion(normalized)
                    ? "Son yaptığınız alışverişin tutarı: " + formatNumber(firstRow.get("total_amount")) + " TL."
                    : "The total amount of your latest purchase is " + formatNumber(firstRow.get("total_amount")) + ".";
        }

        if (normalized.contains("product") && firstRow.containsKey("product_name")) {
            return "Here are the products that best match your analytics request.";
        }

        if ((normalized.contains("mağaza") || normalized.contains("magaza") || normalized.contains("store"))
                && (normalized.contains("kategori") || normalized.contains("category"))
                && firstRow.containsKey("category_name")) {
            return isTurkishQuestion(normalized)
                    ? "Mağazanızdaki kategori bazlı satış dağılımı listelendi."
                    : "I have listed the category-based sales distribution for your store.";
        }

        if ((normalized.contains("mağaza") || normalized.contains("magaza") || normalized.contains("store"))
                && (normalized.contains("ürün") || normalized.contains("urun") || normalized.contains("product"))
                && firstRow.containsKey("product_name")) {
            
            if (normalized.contains("yorum") || normalized.contains("review") || normalized.contains("puan") || normalized.contains("rating")) {
                return isTurkishQuestion(normalized)
                        ? "Mağazanızdaki ürünler için yorum ve puan analizleri listelendi."
                        : "I have listed the review and rating analysis for your products.";
            }
            if (normalized.contains("en cok") || normalized.contains("en çok") || normalized.contains("en fazla") || normalized.contains("most sold") || normalized.contains("top selling")) {
                return isTurkishQuestion(normalized)
                        ? "Mağazanızda en çok satılan " + rows.size() + " ürün listelendi. İlk sırada " + firstRow.get("product_name") + " var."
                        : "I have listed your top " + rows.size() + " selling products. The top product is " + firstRow.get("product_name") + ".";
            }
            if (normalized.contains("en az") || normalized.contains("least sold") || normalized.contains("worst selling")) {
                return isTurkishQuestion(normalized)
                        ? "Mağazanızda en az satılan " + rows.size() + " ürün listelendi."
                        : "I have listed your " + rows.size() + " least sold products.";
            }

            if (rows.size() > 1) {
                return isTurkishQuestion(normalized)
                        ? "Mağazanızdaki ilgili ürünler listelendi."
                        : "I have listed the relevant products for your store.";
            }

            return isTurkishQuestion(normalized)
                    ? "Mağazanızdan alışveriş yapılan en son ürün: " + firstRow.get("product_name") + "."
                    : "The latest product sold from your store is: " + firstRow.get("product_name") + ".";
        }

        if ((normalized.contains("gelir") || normalized.contains("revenue") || normalized.contains("kazanc")) && firstRow.containsKey("total_revenue")) {
            return isTurkishQuestion(normalized)
                    ? "Mağazanızın toplam satış geliri: " + formatNumber(firstRow.get("total_revenue")) + " TL."
                    : "The total sales revenue of your store is " + formatNumber(firstRow.get("total_revenue")) + ".";
        }
        
        if ((normalized.contains("ortalama") || normalized.contains("average")) && firstRow.containsKey("average_order_value")) {
            return isTurkishQuestion(normalized)
                    ? "Mağazanızdaki ortalama sipariş tutarı: " + formatNumber(firstRow.get("average_order_value")) + " TL."
                    : "The average order value in your store is " + formatNumber(firstRow.get("average_order_value")) + ".";
        }

        return buildGenericAnswer(normalized, rows);
    }

    public VisualizationPayload buildVisualization(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return new VisualizationPayload("NONE", Map.of());
        }

        Map<String, Object> firstRow = rows.get(0);
        if (firstRow.size() < 2) {
            return new VisualizationPayload("TABLE", Map.of());
        }

        String labelKey = null;
        String valueKey = null;
        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            if (labelKey == null && entry.getValue() instanceof String) {
                labelKey = entry.getKey();
            } else if (valueKey == null && entry.getValue() instanceof Number) {
                valueKey = entry.getKey();
            }
        }

        if (labelKey == null || valueKey == null) {
            return new VisualizationPayload("TABLE", Map.of());
        }

        List<String> labels = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object label = row.get(labelKey);
            Object value = row.get(valueKey);
            if (label != null && value instanceof Number number) {
                labels.add(String.valueOf(label));
                values.add(number);
            }
        }

        if (labels.isEmpty()) {
            return new VisualizationPayload("TABLE", Map.of());
        }

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("values", values);
        chartData.put("type", "bar");
        return new VisualizationPayload("BAR", chartData);
    }

    public List<Map<String, Object>> sanitizeRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> sanitized = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> sanitizedRow = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String stringValue) {
                    sanitizedRow.put(entry.getKey(), escapeHtml(stringValue));
                } else {
                    sanitizedRow.put(entry.getKey(), value);
                }
            }
            sanitized.add(sanitizedRow);
        }
        return sanitized;
    }

    public String escapeHtml(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatNumber(Object value) {
        if (value instanceof Number number) {
            return DECIMAL_FORMAT.format(number.doubleValue());
        }
        return String.valueOf(value);
    }

    private String formatSummaryNumber(Object value) {
        if (value instanceof Number number) {
            return SUMMARY_DECIMAL_FORMAT.format(number.doubleValue());
        }
        return String.valueOf(value);
    }

    private String buildTurkishLastOrderPercentageAnswer(Map<String, Object> row) {
        Object totalSpent = getRowValue(row, "total_spent");
        if (totalSpent instanceof Number number && number.doubleValue() == 0) {
            return "Henüz harcama geçmişiniz olmadığı için oran hesaplanamıyor.";
        }

        return "Son alışverişiniz "
                + formatSummaryNumber(getRowValue(row, "last_order_amount"))
                + " TL tutarında. Toplam harcamanız "
                + formatSummaryNumber(totalSpent)
                + " TL olduğu için son alışverişiniz toplam harcamanızın %"
                + formatSummaryNumber(getRowValue(row, "percentage_of_total_spent"))
                + "’ini oluşturuyor.";
    }

    private String buildTurkishLastOrderOfRecentOrdersPercentageAnswer(Map<String, Object> row) {
        Object recentOrdersTotal = getRowValue(row, "recent_orders_total");
        if (recentOrdersTotal instanceof Number number && number.doubleValue() == 0) {
            return "Son alışverişlerinizin toplamı 0 olduğu için oran hesaplanamıyor.";
        }

        return "Son siparişinizdeki ürünlerin toplamı "
                + formatSummaryNumber(getRowValue(row, "last_order_amount"))
                + " TL. Son 10 alışverişinizin toplamı "
                + formatSummaryNumber(recentOrdersTotal)
                + " TL olduğu için bu ürünler son 10 alışverişinizin %"
                + formatSummaryNumber(getRowValue(row, "percentage_of_recent_orders"))
                + "’ini oluşturuyor.";
    }

    private Object getRowValue(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isTurkishQuestion(String normalized) {
        return normalized.contains("alışveriş")
                || normalized.contains("alisveris")
                || normalized.contains("harcama")
                || normalized.contains("harcaman")
                || normalized.contains("satın")
                || normalized.contains("satin")
                || normalized.contains("aldığım")
                || normalized.contains("aldigim")
                || normalized.contains("ürün")
                || normalized.contains("urun")
                || normalized.contains("kategori")
                || normalized.contains("sipariş")
                || normalized.contains("siparis")
                || normalized.contains("tutar")
                || normalized.contains("oran")
                || normalized.contains("nedir")
                || normalized.contains("son ");
    }

    private String buildLatestOrderItemsAnswer(Map<String, Object> firstRow, List<Map<String, Object>> rows, boolean turkish) {
        StringBuilder answer = new StringBuilder();
        if (firstRow.containsKey("total_amount")) {
            if (turkish) {
                answer.append("Son siparişinizin toplamı ")
                        .append(formatNumber(firstRow.get("total_amount")))
                        .append(" TL. İçerdiği ürünler:");
            } else {
                answer.append("Your latest order total is ")
                        .append(formatNumber(firstRow.get("total_amount")))
                        .append(". It includes:");
            }
        } else {
            answer.append(turkish ? "Son siparişinizdeki ürünler:" : "Your latest order includes:");
        }

        for (Map<String, Object> row : rows) {
            answer.append("\n- ")
                    .append(row.get("product_name"));
            if (row.containsKey("quantity")) {
                answer.append(" x").append(formatNumber(row.get("quantity")));
            }
            if (row.containsKey("line_total")) {
                answer.append(" = ").append(formatNumber(row.get("line_total")));
            } else if (row.containsKey("unit_price")) {
                answer.append(" @ ").append(formatNumber(row.get("unit_price")));
            }
        }

        return answer.toString();
    }

    private boolean isUserScopedQuestion(String normalized) {
        if (normalized.contains("mağaza") || normalized.contains("magaza") || normalized.contains("store")) {
            return false;
        }
        return normalized.contains("benim")
                || normalized.contains("aldığım")
                || normalized.contains("aldigim")
                || normalized.contains("alışveriş")
                || normalized.contains("alisveris")
                || normalized.contains("sipariş")
                || normalized.contains("siparis")
                || normalized.contains("my order")
                || normalized.contains("my purchase")
                || normalized.contains("my spending");
    }

    private String buildGenericAnswer(String normalized, List<Map<String, Object>> rows) {
        boolean turkish = isTurkishQuestion(normalized);
        int count = rows.size();
        Map<String, Object> firstRow = rows.get(0);

        // ── Entity-Specific List Summaries (High Priority) ────────────────────────
        
        // product list
        if (firstRow.containsKey("product_name")) {
            if (count == 1) {
                return turkish
                        ? "İsteğinize uygun en iyi ürün: " + firstRow.get("product_name") + "."
                        : "The top matching product is: " + firstRow.get("product_name") + ".";
            }
            return turkish
                    ? count + " ürün aşağıda listelenmiştir."
                    : count + " products are listed below.";
        }

        // category list
        if (firstRow.containsKey("category_name")) {
            if (count == 1) {
                return turkish
                        ? "Sonuç kategorisi: " + firstRow.get("category_name") + "."
                        : "The result category is: " + firstRow.get("category_name") + ".";
            }
            return turkish
                    ? count + " kategori sonucu aşağıda listelenmiştir."
                    : count + " category results are listed below.";
        }

        // ── Single-row numerical summaries (Lower Priority) ─────────────────────

        // revenue / total_revenue
        if (count == 1 && firstRow.containsKey("total_revenue")) {
            return turkish
                    ? "Toplam gelir " + formatSummaryNumber(firstRow.get("total_revenue")) + " olarak hesaplandı."
                    : "The total revenue is " + formatSummaryNumber(firstRow.get("total_revenue")) + ".";
        }

        // avg spend
        if (count == 1 && firstRow.containsKey("avg_spend")) {
            return turkish
                    ? "Ortalama harcama " + formatSummaryNumber(firstRow.get("avg_spend")) + " olarak bulundu."
                    : "The average spend is " + formatSummaryNumber(firstRow.get("avg_spend")) + ".";
        }

        // single numeric result (only one column)
        if (count == 1 && firstRow.size() == 1) {
            Object singleValue = firstRow.values().iterator().next();
            return turkish
                    ? "Sonuç: " + formatSummaryNumber(singleValue) + "."
                    : "The result is " + formatSummaryNumber(singleValue) + ".";
        }

        // ── Multi-row list summaries ─────────────────────────────────────────────

        // product list
        if (firstRow.containsKey("product_name")) {
            if (count == 1) {
                return turkish
                        ? "En iyi ürün: " + firstRow.get("product_name") + "."
                        : "The top product is: " + firstRow.get("product_name") + ".";
            }
            return turkish
                    ? "En iyi " + count + " ürün aşağıda listelenmiştir."
                    : "The top " + count + " products are listed below.";
        }

        // category list
        if (firstRow.containsKey("category_name")) {
            if (count == 1) {
                return turkish
                        ? "Sonuç kategorisi: " + firstRow.get("category_name") + "."
                        : "The result category is: " + firstRow.get("category_name") + ".";
            }
            return turkish
                    ? count + " kategori sonucu aşağıda listelenmiştir."
                    : count + " category results are listed below.";
        }

        // city list
        if (firstRow.containsKey("city")) {
            if (count == 1) {
                return turkish
                        ? "En yüksek müşteri sayısına sahip şehir: " + firstRow.get("city") + "."
                        : "The leading city is: " + firstRow.get("city") + ".";
            }
            return turkish
                    ? "En iyi " + count + " şehir aşağıda sıralanmıştır."
                    : "The top " + count + " cities are listed below.";
        }

        // country list
        if (firstRow.containsKey("country")) {
            if (count == 1) {
                return turkish
                        ? "En yüksek geliri üreten ülke: " + firstRow.get("country") + "."
                        : "The top revenue-generating country is: " + firstRow.get("country") + ".";
            }
            return turkish
                    ? "En iyi " + count + " ülke aşağıda listelenmiştir."
                    : "The top " + count + " countries are listed below.";
        }

        // ── Absolute fallback (still human-readable) ─────────────────────────────
        if (count == 1) {
            return turkish
                    ? "Sorgunuza göre 1 sonuç bulundu."
                    : "One result was found for your query.";
        }
        return turkish
                ? "Sorgunuza göre " + count + " sonuç listelendi."
                : count + " results were found for your query.";
    }

    public record VisualizationPayload(String visualizationType, Map<String, Object> chartData) {
    }
}
