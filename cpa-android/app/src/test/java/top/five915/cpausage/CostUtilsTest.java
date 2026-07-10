package top.five915.cpausage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CostUtilsTest {
    @Test
    public void calculatesInputOutputAndCacheCost() {
        assertEquals(5.5d, CostUtils.calculate(1_000_000, 500_000, 2_000_000, 2d, 5d, 0.5d), 0.000001d);
    }

    @Test
    public void emptySelectionIncludesEveryModel() {
        assertTrue(CostUtils.includesModel("", "gpt-5"));
        assertTrue(CostUtils.includesModel(null, "gpt-5"));
    }

    @Test
    public void modelSelectionOnlyIncludesExactModel() {
        assertTrue(CostUtils.includesModel("gpt-5", "gpt-5"));
        assertFalse(CostUtils.includesModel("gpt-5", "gpt-5-mini"));
    }
}
