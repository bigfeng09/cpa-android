package top.five915.cpausage;

final class QuotaUtils {
    private QuotaUtils() {}

    static double normalizeUsedPercent(Double usedPercent) {
        if (usedPercent == null || usedPercent.isNaN() || usedPercent.isInfinite()) return -1d;
        return Math.max(0d, Math.min(100d, usedPercent));
    }

    static double remainingPercent(Double usedPercent) {
        double used = normalizeUsedPercent(usedPercent);
        return used < 0d ? -1d : 100d - used;
    }
}
