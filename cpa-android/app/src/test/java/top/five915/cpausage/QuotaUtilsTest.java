package top.five915.cpausage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QuotaUtilsTest {
    @Test
    public void oneMeansOnePercentUsedInsteadOfFullQuota() {
        assertEquals(1d, QuotaUtils.normalizeUsedPercent(1d), 0.001d);
        assertEquals(99d, QuotaUtils.remainingPercent(1d), 0.001d);
    }

    @Test
    public void keepsZeroAndOneHundredPercentBoundaries() {
        assertEquals(100d, QuotaUtils.remainingPercent(0d), 0.001d);
        assertEquals(0d, QuotaUtils.remainingPercent(100d), 0.001d);
    }

    @Test
    public void clampsOutOfRangeValuesAndKeepsUnknownValuesUnknown() {
        assertEquals(100d, QuotaUtils.normalizeUsedPercent(120d), 0.001d);
        assertEquals(0d, QuotaUtils.normalizeUsedPercent(-20d), 0.001d);
        assertEquals(-1d, QuotaUtils.remainingPercent(null), 0.001d);
        assertEquals(-1d, QuotaUtils.remainingPercent(Double.NaN), 0.001d);
    }
}
