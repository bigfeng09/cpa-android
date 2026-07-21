package top.five915.cpausage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UrlUtilsTest {
    private static final String DEFAULT_BASE = "http://your-host:8318";
    private static final String DEFAULT_QUOTA = "https://your-domain.example/management.html#/quota";

    @Test
    public void normalizeBaseUrlAddsHttpAndRemovesTrailingSlash() {
        assertEquals("http://192.168.1.10:8318", UrlUtils.normalizeBaseUrl("192.168.1.10:8318/", DEFAULT_BASE));
    }

    @Test
    public void normalizeBaseUrlRemovesPageQueryAndFragment() {
        assertEquals("https://example.test", UrlUtils.normalizeBaseUrl("https://example.test/management.html?x=1#/quota", DEFAULT_BASE));
    }

    @Test
    public void usableBaseUrlRejectsBlankAndDocumentationPlaceholders() {
        assertFalse(UrlUtils.isUsableBaseUrl("  ", DEFAULT_BASE));
        assertFalse(UrlUtils.isUsableBaseUrl("http://your-host:8318", DEFAULT_BASE));
        assertFalse(UrlUtils.isUsableBaseUrl("https://your-domain.example", DEFAULT_BASE));
    }

    @Test
    public void usableBaseUrlAcceptsLanAndRealDomainHosts() {
        assertTrue(UrlUtils.isUsableBaseUrl("192.168.1.20:8318", DEFAULT_BASE));
        assertTrue(UrlUtils.isUsableBaseUrl("https://usage.example.com", DEFAULT_BASE));
    }

    @Test
    public void normalizeQuotaUrlAcceptsRootDomain() {
        assertEquals("https://example.test/management.html#/quota", UrlUtils.normalizeQuotaUrl("example.test", DEFAULT_QUOTA));
    }

    @Test
    public void normalizeQuotaUrlAcceptsManagementPage() {
        assertEquals("https://example.test/management.html#/quota", UrlUtils.normalizeQuotaUrl("https://example.test/management.html#/quota", DEFAULT_QUOTA));
    }

    @Test
    public void normalizeQuotaUrlAcceptsManagementApi() {
        assertEquals("https://example.test/management.html#/quota", UrlUtils.normalizeQuotaUrl("https://example.test/v0/management/api-call", DEFAULT_QUOTA));
    }

    @Test
    public void managementApiBaseAcceptsAllSupportedInputs() {
        assertEquals("https://example.test/v0/management", UrlUtils.managementApiBaseFromUrl("https://example.test", DEFAULT_QUOTA));
        assertEquals("https://example.test/v0/management", UrlUtils.managementApiBaseFromUrl("https://example.test/management.html#/quota", DEFAULT_QUOTA));
        assertEquals("https://example.test/v0/management", UrlUtils.managementApiBaseFromUrl("https://example.test/v0/management/api-call", DEFAULT_QUOTA));
    }

    @Test
    public void blankValuesUseDefaults() {
        assertEquals(DEFAULT_BASE, UrlUtils.normalizeBaseUrl("  ", DEFAULT_BASE));
        assertEquals(DEFAULT_QUOTA, UrlUtils.normalizeQuotaUrl(null, DEFAULT_QUOTA));
    }
}
