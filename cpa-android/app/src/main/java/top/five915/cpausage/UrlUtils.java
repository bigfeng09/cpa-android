package top.five915.cpausage;

import java.net.URI;
import java.util.Locale;

final class UrlUtils {
    private static final String MANAGEMENT_PAGE = "/management.html";
    private static final String MANAGEMENT_API = "/v0/management";

    private UrlUtils() {
    }

    static String normalizeBaseUrl(String raw, String defaultBaseUrl) {
        String value = trimOrDefault(raw, defaultBaseUrl);
        value = stripUrlHashAndQuery(value);
        if (value.endsWith(MANAGEMENT_PAGE)) {
            value = value.substring(0, value.length() - MANAGEMENT_PAGE.length());
        }
        if (!hasHttpScheme(value)) value = "http://" + value;
        return stripTrailingSlashes(value);
    }

    static boolean isUsableBaseUrl(String raw, String defaultBaseUrl) {
        if (raw == null || raw.trim().length() == 0) return false;
        try {
            URI uri = URI.create(normalizeBaseUrl(raw, defaultBaseUrl));
            String host = uri.getHost();
            if (host == null || host.length() == 0) return false;
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return !"your-host".equals(normalizedHost) && !normalizedHost.endsWith(".example");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static String normalizeQuotaUrl(String raw, String defaultQuotaUrl) {
        String value = trimOrDefault(raw, defaultQuotaUrl);
        if (!hasHttpScheme(value)) value = "https://" + value;
        value = stripUrlHashAndQuery(value);
        int apiIndex = value.indexOf(MANAGEMENT_API);
        if (apiIndex >= 0) value = value.substring(0, apiIndex);
        int pageIndex = value.indexOf(MANAGEMENT_PAGE);
        if (pageIndex >= 0) value = value.substring(0, pageIndex);
        return stripTrailingSlashes(value) + MANAGEMENT_PAGE + "#/quota";
    }

    static String managementApiBaseFromUrl(String raw, String defaultQuotaUrl) {
        String value = trimOrDefault(raw, defaultQuotaUrl);
        if (!hasHttpScheme(value)) value = "https://" + value;
        value = stripUrlHashAndQuery(value);
        int apiIndex = value.indexOf(MANAGEMENT_API);
        if (apiIndex >= 0) {
            return stripTrailingSlashes(value.substring(0, apiIndex + MANAGEMENT_API.length()));
        }
        int pageIndex = value.indexOf(MANAGEMENT_PAGE);
        if (pageIndex >= 0) value = value.substring(0, pageIndex);
        return stripTrailingSlashes(value) + MANAGEMENT_API;
    }

    static String stripUrlHashAndQuery(String value) {
        String out = value == null ? "" : value;
        int hash = out.indexOf('#');
        if (hash >= 0) out = out.substring(0, hash);
        int query = out.indexOf('?');
        if (query >= 0) out = out.substring(0, query);
        return out;
    }

    private static String trimOrDefault(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        return value.length() == 0 ? fallback : value;
    }

    private static boolean hasHttpScheme(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private static String stripTrailingSlashes(String value) {
        String out = value;
        while (out.endsWith("/")) out = out.substring(0, out.length() - 1);
        return out;
    }
}
