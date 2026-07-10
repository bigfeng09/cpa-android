package top.five915.cpausage;

final class CostUtils {
    private static final double TOKENS_PER_MILLION = 1_000_000d;

    private CostUtils() {
    }

    static double calculate(long inputTokens, long outputTokens, long cachedTokens,
                            double inputPer1m, double outputPer1m, double cachePer1m) {
        return inputTokens / TOKENS_PER_MILLION * inputPer1m
                + outputTokens / TOKENS_PER_MILLION * outputPer1m
                + cachedTokens / TOKENS_PER_MILLION * cachePer1m;
    }

    static boolean includesModel(String selectedModel, String eventModel) {
        return selectedModel == null || selectedModel.length() == 0 || selectedModel.equals(eventModel);
    }
}
