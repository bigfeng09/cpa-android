package top.five915.cpausage;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String PREFS = "cpa_usage_prefs";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_QUOTA_URL = "quota_url";
    private static final String KEY_PRICE_TABLE = "price_table";
    private static final String KEY_MANAGEMENT_KEY = "management_key";
    private static final String KEY_LOGIN_PASSWORD = "login_password";
    private static final String KEY_MANAGEMENT_KEY_ENCRYPTED = "management_key_encrypted";
    private static final String KEY_LOGIN_PASSWORD_ENCRYPTED = "login_password_encrypted";
    private static final String KEY_SELECTED_RANGE = "selected_range";
    private static final String KEY_CUSTOM_START_DATE = "custom_start_date";
    private static final String KEY_CUSTOM_END_DATE = "custom_end_date";
    private static final String DEFAULT_BASE_URL = "http://your-host:8318";
    private static final String DEFAULT_QUOTA_URL = "https://your-domain.example/management.html#/quota";

    private static final int RANGE_4H = 0;
    private static final int RANGE_8H = 1;
    private static final int RANGE_24H = 2;
    private static final int RANGE_7D = 3;
    private static final int RANGE_30D = 4;
    private static final int RANGE_ALL = 5;
    private static final int RANGE_CUSTOM = 6;
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String[] RANGE_MENU_LABELS = new String[]{"4h", "8h", "24h", "7天", "30天", "全部", "自定义日期"};
    private static final int[] RANGE_MENU_VALUES = new int[]{RANGE_4H, RANGE_8H, RANGE_24H, RANGE_7D, RANGE_30D, RANGE_ALL, RANGE_CUSTOM};

    private static final int BG = Color.rgb(245, 247, 251);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(18, 27, 45);
    private static final int MUTED = Color.rgb(102, 112, 133);
    private static final int BORDER = Color.rgb(225, 230, 239);
    private static final int BLUE = Color.rgb(23, 107, 255);
    private static final int GREEN = Color.rgb(24, 164, 103);
    private static final int RED = Color.rgb(218, 45, 65);
    private static final int ORANGE = Color.rgb(214, 129, 35);
    private static final int SOFT_BLUE = Color.rgb(235, 242, 255);
    private static final int SOFT_GREEN = Color.rgb(232, 247, 239);
    private static final int SOFT_RED = Color.rgb(255, 238, 240);
    private static final int SOFT_ORANGE = Color.rgb(255, 245, 229);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DecimalFormat oneDecimal = new DecimalFormat("0.0");
    private final DecimalFormat twoDecimal = new DecimalFormat("0.00");
    private final DecimalFormat fourDecimal = new DecimalFormat("0.0000");

    private SharedPreferences prefs;
    private SecureStringStore secureStore;
    private String baseUrl = DEFAULT_BASE_URL;
    private String quotaUrl = DEFAULT_QUOTA_URL;
    private String managementKey = "";
    private String loginPassword = "";
    private final Map<String, PriceRule> priceTable = new HashMap<>();
    private final List<CodexQuotaAccount> codexQuotaAccounts = new ArrayList<>();
    private final List<ApiKeyItem> apiKeyItems = new ArrayList<>();
    private boolean quotaLoading = false;
    private String quotaStatus = "未刷新";
    private String quotaDetail = "输入管理 Key 后可读取每个 Codex 账号的实时额度。";
    private boolean apiKeysLoading = false;
    private String apiKeysStatus = "未刷新";
    private String apiKeysDetail = "点击刷新读取 API 密钥。";
    private boolean oauthLoading = false;
    private boolean oauthPolling = false;
    private String oauthStatus = "未开始";
    private String oauthDetail = "点击开始 Codex 登录后生成授权链接。";
    private String oauthAuthUrl = "";
    private String oauthState = "";
    private String oauthCallbackUrl = "";
    private int oauthPollAttempts = 0;
    private boolean quotaPageRendered = false;
    private int quotaScrollY = 0;
    private boolean focusOAuthAfterRender = false;
    private DataSnapshot allSnapshot = new DataSnapshot();
    private DataSnapshot viewSnapshot = new DataSnapshot();
    private boolean loading = false;
    private int selectedTab = 0;
    private int selectedRange = RANGE_4H;
    private String customStartDate = "";
    private String customEndDate = "";
    private int resourceTab = 0;
    private int logTab = 0;
    private boolean errorsOnly = false;
    private String selectedModelFilter = "";

    private LinearLayout root;
    private LinearLayout header;
    private LinearLayout content;
    private LinearLayout bottomNav;
    private TextView headerMeta;
    private TextView statusPill;
    private TextView[] navItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        secureStore = new SecureStringStore(prefs);
        baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        quotaUrl = prefs.getString(KEY_QUOTA_URL, DEFAULT_QUOTA_URL);
        quotaUrl = correctedQuotaUrl(quotaUrl);
        managementKey = readSecret(KEY_MANAGEMENT_KEY_ENCRYPTED, KEY_MANAGEMENT_KEY);
        loginPassword = readSecret(KEY_LOGIN_PASSWORD_ENCRYPTED, KEY_LOGIN_PASSWORD);
        selectedRange = Math.max(RANGE_4H, Math.min(RANGE_CUSTOM, prefs.getInt(KEY_SELECTED_RANGE, RANGE_4H)));
        customStartDate = prefs.getString(KEY_CUSTOM_START_DATE, "");
        customEndDate = prefs.getString(KEY_CUSTOM_END_DATE, "");
        if (managementKey.length() == 0) {
            managementKey = loginPassword;
            if (managementKey.length() > 0) saveSecret(KEY_MANAGEMENT_KEY_ENCRYPTED, KEY_MANAGEMENT_KEY, managementKey);
        }
        loadLocalPrices();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);

        if (prefs.contains(KEY_BASE_URL)) {
            showApp();
            refreshAll();
        } else {
            showLoginScreen();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void showLoginScreen() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = vertical();
        panel.setPadding(dp(22), dp(42), dp(22), dp(24));
        scroll.addView(panel, new ScrollView.LayoutParams(-1, -2));
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        panel.addView(text("CPA Usage 登录", 28, TEXT, Typeface.BOLD));

        LinearLayout card = card();
        LinearLayout.LayoutParams cardLp = matchWrapWithBottom(0);
        cardLp.setMargins(0, dp(22), 0, 0);
        panel.addView(card, cardLp);
        card.addView(text("服务地址", 16, TEXT, Typeface.BOLD));
        addMuted(card, "示例：http://your-host:8318 或 https://your-domain.example");
        EditText addressInput = input(prefs.contains(KEY_BASE_URL) ? baseUrl : "");
        addressInput.setHint("http://your-host:8318 或 https://your-domain.example");
        LinearLayout.LayoutParams addressLp = new LinearLayout.LayoutParams(-1, dp(48));
        addressLp.setMargins(0, dp(10), 0, 0);
        card.addView(addressInput, addressLp);

        card.addView(text("密码 / 管理 Key", 16, TEXT, Typeface.BOLD));
        EditText passwordInput = passwordInput(loginPassword);
        LinearLayout.LayoutParams passwordLp = new LinearLayout.LayoutParams(-1, dp(48));
        passwordLp.setMargins(0, dp(10), 0, 0);
        card.addView(passwordInput, passwordLp);

        TextView status = text("未登录", 13, MUTED, Typeface.NORMAL);
        status.setPadding(0, dp(12), 0, dp(4));
        card.addView(status);

        LinearLayout shortcuts = horizontal();
        shortcuts.setPadding(0, dp(12), 0, 0);
        card.addView(shortcuts, new LinearLayout.LayoutParams(-1, dp(54)));
        Button httpBtn = secondaryButton("HTTP 示例");
        shortcuts.addView(httpBtn, weightLp(1));
        httpBtn.setOnClickListener(v -> addressInput.setText("http://your-host:8318"));
        Button httpsBtn = secondaryButton("HTTPS 示例");
        LinearLayout.LayoutParams httpsLp = weightLp(1);
        httpsLp.setMargins(dp(10), 0, 0, 0);
        shortcuts.addView(httpsBtn, httpsLp);
        httpsBtn.setOnClickListener(v -> addressInput.setText("https://your-domain.example"));

        Button testBtn = secondaryButton("测试地址");
        LinearLayout.LayoutParams testLp = new LinearLayout.LayoutParams(-1, dp(46));
        testLp.setMargins(0, dp(10), 0, 0);
        card.addView(testBtn, testLp);
        testBtn.setOnClickListener(v -> {
            hideKeyboard(addressInput);
            String url = normalizeBaseUrl(addressInput.getText().toString());
            String pass = passwordInput.getText().toString().trim();
            testBtn.setEnabled(false);
            status.setText("正在测试 /api/v1/status...");
            executor.execute(() -> {
                try {
                    JSONObject obj = new JSONObject(getWithPassword(url, "/api/v1/status", pass, 8000, 15000));
                    boolean running = obj.optBoolean("running", true);
                    String lastRun = obj.optString("last_run_at", "");
                    runOnUiThread(() -> status.setText(running ? "连接成功，服务在线。最近同步：" + shortTime(lastRun) : "连接成功，但服务状态不是 running。"));
                } catch (Exception e) {
                    runOnUiThread(() -> status.setText("连接失败：" + cleanError(e)));
                } finally {
                    runOnUiThread(() -> testBtn.setEnabled(true));
                }
            });
        });

        Button loginBtn = primaryButton("登录并进入");
        LinearLayout.LayoutParams loginLp = new LinearLayout.LayoutParams(-1, dp(50));
        loginLp.setMargins(0, dp(14), 0, 0);
        card.addView(loginBtn, loginLp);
        loginBtn.setOnClickListener(v -> {
            hideKeyboard(passwordInput);
            baseUrl = normalizeBaseUrl(addressInput.getText().toString());
            loginPassword = passwordInput.getText().toString().trim();
            managementKey = loginPassword;
            quotaUrl = correctedQuotaUrl(quotaUrl);
            boolean passwordSaved = saveSecret(KEY_LOGIN_PASSWORD_ENCRYPTED, KEY_LOGIN_PASSWORD, loginPassword);
            boolean keySaved = saveSecret(KEY_MANAGEMENT_KEY_ENCRYPTED, KEY_MANAGEMENT_KEY, managementKey);
            prefs.edit()
                    .putString(KEY_BASE_URL, baseUrl)
                    .putString(KEY_QUOTA_URL, quotaUrl)
                    .apply();
            if (!passwordSaved || !keySaved) toast("凭据未能加密保存，仅本次会话可用");
            showApp();
            refreshAll();
        });

        setContentView(frame);
    }

    private void showConnectScreen() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = vertical();
        panel.setPadding(dp(22), dp(42), dp(22), dp(24));
        scroll.addView(panel, new ScrollView.LayoutParams(-1, -2));
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        panel.addView(text("CPA Usage", 30, TEXT, Typeface.BOLD));
        TextView subtitle = text("连接 Usage Keeper，查看不同时间范围下的请求、模型、凭证、日志和配额。", 15, MUTED, Typeface.NORMAL);
        subtitle.setPadding(0, dp(8), 0, dp(22));
        panel.addView(subtitle);

        LinearLayout card = card();
        panel.addView(card, matchWrapWithBottom(0));
        card.addView(text("统计服务地址", 16, TEXT, Typeface.BOLD));
        addMuted(card, "支持 http 局域网地址，也支持 https 反代链接。默认使用当前局域网站点。");

        EditText input = input(baseUrl == null || baseUrl.length() == 0 ? DEFAULT_BASE_URL : baseUrl);
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(-1, dp(48));
        inputLp.setMargins(0, dp(10), 0, 0);
        card.addView(input, inputLp);

        TextView status = text("未连接", 13, MUTED, Typeface.NORMAL);
        status.setPadding(0, dp(12), 0, dp(4));
        card.addView(status);

        LinearLayout actions = horizontal();
        actions.setPadding(0, dp(12), 0, 0);
        card.addView(actions, new LinearLayout.LayoutParams(-1, dp(58)));

        Button defaultBtn = secondaryButton("局域网默认");
        actions.addView(defaultBtn, weightLp(1));
        defaultBtn.setOnClickListener(v -> input.setText(DEFAULT_BASE_URL));

        Button httpsBtn = secondaryButton("HTTPS 示例");
        LinearLayout.LayoutParams httpsLp = weightLp(1);
        httpsLp.setMargins(dp(10), 0, 0, 0);
        actions.addView(httpsBtn, httpsLp);
        httpsBtn.setOnClickListener(v -> input.setText("https://your-domain.example"));

        Button testBtn = secondaryButton("测试连接");
        LinearLayout.LayoutParams testLp = new LinearLayout.LayoutParams(-1, dp(46));
        testLp.setMargins(0, dp(10), 0, 0);
        card.addView(testBtn, testLp);
        testBtn.setOnClickListener(v -> {
            hideKeyboard(input);
            String url = normalizeBaseUrl(input.getText().toString());
            testBtn.setEnabled(false);
            status.setText("正在测试 /api/v1/status...");
            executor.execute(() -> {
                try {
                    JSONObject obj = new JSONObject(get(url, "/api/v1/status", 8000, 15000));
                    boolean running = obj.optBoolean("running", true);
                    String lastRun = obj.optString("last_run_at", "");
                    runOnUiThread(() -> status.setText(running ? "连接成功，服务在线。最近同步：" + shortTime(lastRun) : "连接成功，但服务状态不是 running。"));
                } catch (Exception e) {
                    runOnUiThread(() -> status.setText("连接失败：" + cleanError(e)));
                } finally {
                    runOnUiThread(() -> testBtn.setEnabled(true));
                }
            });
        });

        Button enterBtn = primaryButton("进入仪表盘");
        LinearLayout.LayoutParams enterLp = new LinearLayout.LayoutParams(-1, dp(50));
        enterLp.setMargins(0, dp(14), 0, 0);
        card.addView(enterBtn, enterLp);
        enterBtn.setOnClickListener(v -> {
            hideKeyboard(input);
            baseUrl = normalizeBaseUrl(input.getText().toString());
            prefs.edit().putString(KEY_BASE_URL, baseUrl).putString(KEY_QUOTA_URL, quotaUrl).apply();
            showApp();
            refreshAll();
        });

        LinearLayout quotaCard = card();
        LinearLayout.LayoutParams quotaLp = matchWrapWithBottom(0);
        quotaLp.setMargins(0, dp(14), 0, 0);
        panel.addView(quotaCard, quotaLp);
        quotaCard.addView(text("配额管理页", 16, TEXT, Typeface.BOLD));
        addMuted(quotaCard, "默认接入管理中心配额页面，进入 App 后可在“配额”标签展示和刷新。 ");
        EditText quotaInput = input(quotaUrl);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(-1, dp(48));
        qLp.setMargins(0, dp(10), 0, 0);
        quotaCard.addView(quotaInput, qLp);
        quotaInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                quotaUrl = correctedQuotaUrl(quotaInput.getText().toString());
                prefs.edit().putString(KEY_QUOTA_URL, quotaUrl).apply();
            }
        });

        setContentView(frame);
    }

    private void showApp() {
        root = vertical();
        root.setBackgroundColor(BG);
        setContentView(root);

        header = vertical();
        header.setPadding(dp(18), dp(26), dp(18), dp(10));
        header.setBackgroundColor(BG);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleRow = horizontal();
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleCol = vertical();
        titleRow.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1));
        titleCol.addView(text("CPA Usage", 22, TEXT, Typeface.BOLD));
        headerMeta = text(baseUrl, 12, MUTED, Typeface.NORMAL);
        headerMeta.setPadding(0, dp(4), 0, 0);
        titleCol.addView(headerMeta);

        Button refresh = secondaryButton("刷新");
        titleRow.addView(refresh, new LinearLayout.LayoutParams(dp(78), dp(40)));
        refresh.setOnClickListener(v -> {
            if (selectedTab == 4) {
                refreshQuotaAccounts();
                refreshApiKeys();
            }
            else refreshAll();
        });

        LinearLayout statusRow = horizontal();
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(0, dp(10), 0, 0);
        header.addView(statusRow, new LinearLayout.LayoutParams(-1, -2));
        statusPill = pill("未刷新", MUTED, Color.rgb(239, 242, 247));
        statusRow.addView(statusPill);
        TextView last = text("  等待数据", 12, MUTED, Typeface.NORMAL);
        last.setId(2001);
        statusRow.addView(last, new LinearLayout.LayoutParams(0, -2, 1));

        content = vertical();
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        bottomNav = horizontal();
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(6), dp(6), dp(6), dp(8));
        bottomNav.setBackgroundColor(Color.WHITE);
        root.addView(bottomNav, new LinearLayout.LayoutParams(-1, dp(66)));

        String[] labels = new String[]{"首页", "统计", "模型", "日志", "账号", "设置"};
        navItems = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView item = text(labels[i], 13, MUTED, Typeface.BOLD);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(v -> {
                selectedTab = index;
                render();
            });
            navItems[i] = item;
            bottomNav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }
        render();
    }

    private void refreshAll() {
        if (loading) return;
        if (!isNetworkLikelyAvailable()) toast("当前网络不可用");
        loading = true;
        setStatus("刷新中", BLUE, SOFT_BLUE, "正在读取 Usage Keeper 数据");
        render();

        executor.execute(() -> {
            DataSnapshot next = new DataSnapshot();
            next.baseUrl = baseUrl;
            StringBuilder errors = new StringBuilder();
            try { parseSession(next, new JSONObject(get(baseUrl, "/api/v1/auth/session", 8000, 15000))); } catch (Exception e) { appendError(errors, "会话", e); }
            try { parseStatus(next, new JSONObject(get(baseUrl, "/api/v1/status", 8000, 15000))); } catch (Exception e) { appendError(errors, "状态", e); }
            try { parseModelNames(next, new JSONObject(get(baseUrl, "/api/v1/models/used", 8000, 15000))); } catch (Exception e) { appendError(errors, "模型", e); }
            try { parsePricing(new JSONObject(get(baseUrl, "/api/v1/pricing", 8000, 15000))); } catch (Exception e) { appendError(errors, "价格", e); }
            try { parseUsage(next, new JSONObject(get(baseUrl, "/api/v1/usage", 10000, 90000))); } catch (Exception e) { appendError(errors, "统计", e); }
            applyCost(next);
            if (errors.length() > 0) next.error = errors.toString().trim();
            next.loadedAtMillis = System.currentTimeMillis();
            mainHandler.post(() -> {
                allSnapshot = next;
                viewSnapshot = applyRange(next, selectedRange);
                applyCost(viewSnapshot);
                loading = false;
                render();
            });
        });
    }

    private void render() {
        if (content == null) return;
        content.removeAllViews();
        updateNav();
        updateHeader();

        if (selectedTab == 4) {
            renderQuotaPage();
            return;
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = vertical();
        body.setPadding(dp(14), 0, dp(14), dp(18));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout old = content;
        content = body;

        if (selectedTab != 5) renderRangeSelector();
        if (selectedTab == 5) {
            renderSettings();
        } else if (loading && !viewSnapshot.hasAnyData()) renderLoading();
        else {
            if (viewSnapshot.error != null && viewSnapshot.error.length() > 0) renderErrorBanner();
            if (selectedTab == 0) renderDashboard();
            else if (selectedTab == 1) renderStats();
            else if (selectedTab == 2) renderResources();
            else renderLogs();
        }
        content = old;
    }

    private void renderRangeSelector() {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), 0, dp(2), dp(8));
        content.addView(row, matchWrapWithBottom(dp(4)));
        row.addView(text("时间范围", 13, MUTED, Typeface.BOLD), new LinearLayout.LayoutParams(dp(78), dp(48)));
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, RANGE_MENU_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        final boolean[] initialized = new boolean[]{false};
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!initialized[0]) { initialized[0] = true; return; }
                selectRange(RANGE_MENU_VALUES[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinner.setSelection(rangeMenuIndex(selectedRange), false);
        spinner.post(() -> initialized[0] = true);
        row.addView(spinner, new LinearLayout.LayoutParams(0, dp(48), 1));
        if (selectedRange == RANGE_CUSTOM) renderCustomDateControls();
        TextView note = text("当前范围：" + rangeLabel(selectedRange) + "。请求数、Token、模型、凭证、日志都会按范围重新统计。", 12, MUTED, Typeface.NORMAL);
        note.setPadding(dp(4), 0, dp(4), dp(10));
        content.addView(note);
    }

    private int rangeMenuIndex(int range) {
        for (int i = 0; i < RANGE_MENU_VALUES.length; i++) if (RANGE_MENU_VALUES[i] == range) return i;
        return 0;
    }

    private void addRangeSegment(String[] labels, int[] values) {
        LinearLayout segment = horizontal();
        segment.setPadding(dp(3), dp(3), dp(3), dp(3));
        segment.setBackground(round(Color.rgb(232, 236, 244), 8));
        content.addView(segment, matchWrapWithBottom(dp(8)));
        for (int i = 0; i < labels.length; i++) {
            final int value = values[i];
            TextView item = text(labels[i], 13, value == selectedRange ? BLUE : MUTED, Typeface.BOLD);
            item.setGravity(Gravity.CENTER);
            item.setBackground(round(value == selectedRange ? Color.WHITE : Color.TRANSPARENT, 7));
            item.setOnClickListener(v -> selectRange(value));
            segment.addView(item, new LinearLayout.LayoutParams(0, dp(40), 1));
        }
    }

    private void renderCustomDateControls() {
        ensureCustomDates();
        LinearLayout row = horizontal();
        row.setPadding(0, dp(2), 0, dp(10));
        content.addView(row, new LinearLayout.LayoutParams(-1, dp(56)));
        Button start = secondaryButton("开始 " + customStartDate);
        row.addView(start, weightLp(1));
        start.setOnClickListener(v -> showCustomDatePicker(true));
        Button end = secondaryButton("结束 " + customEndDate);
        LinearLayout.LayoutParams endLp = weightLp(1);
        endLp.setMargins(dp(10), 0, 0, 0);
        row.addView(end, endLp);
        end.setOnClickListener(v -> showCustomDatePicker(false));
    }

    private void selectRange(int range) {
        selectedRange = range;
        if (selectedRange == RANGE_CUSTOM) ensureCustomDates();
        saveRangePrefs();
        viewSnapshot = applyRange(allSnapshot, selectedRange);
        applyCost(viewSnapshot);
        render();
        if (range == RANGE_CUSTOM) mainHandler.post(() -> showCustomDatePicker(true));
    }

    private void renderQuotaPage() {
        final int restoreY = quotaPageRendered ? quotaScrollY : 0;
        final boolean focusOAuth = focusOAuthAfterRender;
        focusOAuthAfterRender = false;
        quotaPageRendered = true;
        content.setPadding(0, 0, 0, 0);
        ScrollView scroll = new ScrollView(this);
        if (focusOAuth || restoreY > 0) scroll.setVisibility(View.INVISIBLE);
        scroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> quotaScrollY = scrollY);
        LinearLayout box = vertical();
        box.setPadding(dp(12), 0, dp(12), dp(18));
        scroll.addView(box, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        if (quotaLoading && codexQuotaAccounts.isEmpty()) {
            LinearLayout loadingCard = card();
            box.addView(loadingCard, matchWrapWithBottom(dp(10)));
            loadingCard.addView(new ProgressBar(this), new LinearLayout.LayoutParams(-1, dp(44)));
            addEmpty(loadingCard, "正在读取账号和额度...");
        } else if (codexQuotaAccounts.isEmpty()) {
            LinearLayout empty = card();
            box.addView(empty, matchWrapWithBottom(dp(10)));
            empty.addView(text("还没有账号数据", 15, TEXT, Typeface.BOLD));
            addMuted(empty, "点击“刷新全部凭证”后，会读取 /v0/management/auth-files，只展示已启用账号，并按账号显示 5 小时限额、周限额、重置次数和到期时间。 ");
        } else {
            for (CodexQuotaAccount account : codexQuotaAccounts) addQuotaAccountCard(box, account);
        }

        LinearLayout actionsCard = card();
        box.addView(actionsCard, matchWrapWithBottom(dp(10)));
        LinearLayout titleRow = horizontal();
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        actionsCard.addView(titleRow);
        titleRow.addView(text("账号操作", 18, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        titleRow.addView(pill(codexQuotaAccounts.size() + " 个启用账号", BLUE, SOFT_BLUE));
        addKeyValue(actionsCard, "刷新状态", quotaLoading ? "正在刷新..." : quotaStatus);
        addKeyValue(actionsCard, "刷新结果", cleanText(quotaDetail, 120));

        LinearLayout actions = horizontal();
        actions.setPadding(0, dp(10), 0, 0);
        actionsCard.addView(actions, new LinearLayout.LayoutParams(-1, dp(48)));
        Button reload = primaryButton("刷新全部凭证");
        actions.addView(reload, weightLp(1));
        reload.setOnClickListener(v -> {
            refreshQuotaAccounts();
        });
        Button reset = secondaryButton("清空结果");
        LinearLayout.LayoutParams resetLp = weightLp(1);
        resetLp.setMargins(dp(10), 0, 0, 0);
        actions.addView(reset, resetLp);
        reset.setOnClickListener(v -> {
            quotaStatus = "未刷新";
            quotaDetail = "输入管理 Key 后点击刷新全部凭证。";
            codexQuotaAccounts.clear();
            render();
        });

        renderApiKeysCard(box);
        LinearLayout oauthCard = renderOAuthCard(box);
        scroll.post(() -> {
            int targetY = focusOAuth ? Math.max(0, oauthCard.getTop() - dp(10)) : restoreY;
            scroll.scrollTo(0, targetY);
            quotaScrollY = targetY;
            scroll.setVisibility(View.VISIBLE);
        });
    }

    private void renderApiKeysCard(LinearLayout box) {
        LinearLayout card = card();
        box.addView(card, matchWrapWithBottom(dp(10)));
        LinearLayout titleRow = horizontal();
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(titleRow);
        titleRow.addView(text("API 密钥", 18, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        titleRow.addView(pill(apiKeyItems.size() + " 个", BLUE, SOFT_BLUE));
        addKeyValue(card, "读取状态", apiKeysLoading ? "正在刷新..." : apiKeysStatus);
        addKeyValue(card, "结果", cleanText(apiKeysDetail, 120));

        LinearLayout actionRow = horizontal();
        actionRow.setPadding(0, dp(10), 0, 0);
        card.addView(actionRow, new LinearLayout.LayoutParams(-1, dp(48)));
        Button refresh = primaryButton("刷新 API 密钥");
        actionRow.addView(refresh, weightLp(1));
        refresh.setEnabled(!apiKeysLoading);
        refresh.setOnClickListener(v -> refreshApiKeys());
        Button generate = secondaryButton("生成");
        LinearLayout.LayoutParams generateLp = weightLp(1);
        generateLp.setMargins(dp(10), 0, 0, 0);
        actionRow.addView(generate, generateLp);

        EditText keyInput = input("");
        keyInput.setHint("输入新 API 密钥，或点击生成");
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(-1, dp(48));
        inputLp.setMargins(0, dp(10), 0, 0);
        card.addView(keyInput, inputLp);
        generate.setOnClickListener(v -> keyInput.setText(generateApiKeyValue()));

        Button add = secondaryButton("添加 API 密钥");
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(-1, dp(46));
        addLp.setMargins(0, dp(10), 0, 0);
        card.addView(add, addLp);
        add.setEnabled(!apiKeysLoading);
        add.setOnClickListener(v -> {
            hideKeyboard(keyInput);
            addApiKey(keyInput.getText().toString());
        });

        if (apiKeyItems.isEmpty()) {
            addEmpty(card, "暂无 API 密钥");
            return;
        }
        for (ApiKeyItem item : apiKeyItems) addApiKeyRow(card, item);
    }

    private void addApiKeyRow(LinearLayout parent, ApiKeyItem item) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, 0);
        parent.addView(row);
        LinearLayout info = vertical();
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        info.addView(text(nonEmpty(item.name, "API Key " + (item.index + 1)), 14, TEXT, Typeface.BOLD));
        TextView preview = text(maskApiKey(item.value), 12, MUTED, Typeface.NORMAL);
        preview.setPadding(0, dp(3), 0, 0);
        info.addView(preview);
        Button delete = secondaryButton("删除");
        row.addView(delete, new LinearLayout.LayoutParams(dp(82), dp(42)));
        delete.setEnabled(!apiKeysLoading);
        delete.setOnClickListener(v -> deleteApiKey(item));
    }

    private LinearLayout renderOAuthCard(LinearLayout box) {
        LinearLayout card = card();
        box.addView(card, matchWrapWithBottom(dp(10)));
        LinearLayout titleRow = horizontal();
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(titleRow);
        TextView icon = pill("↗", Color.WHITE, Color.rgb(114, 129, 255));
        titleRow.addView(icon);
        TextView title = text("  Codex OAuth", 18, TEXT, Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button start = primaryButton(oauthLoading || oauthPolling ? "正在登录" : "开始 Codex 登录");
        titleRow.addView(start, new LinearLayout.LayoutParams(dp(142), dp(48)));
        start.setEnabled(!oauthLoading);
        start.setOnClickListener(v -> startCodexOAuth());

        addMuted(card, "通过 OAuth 流程登录 Codex 服务，自动获取并保存认证文件。 ");
        addKeyValue(card, "状态", oauthStatus);
        if (oauthDetail.length() > 0) addKeyValue(card, "结果", cleanText(oauthDetail, 120));

        LinearLayout authBox = card();
        LinearLayout.LayoutParams authLp = matchWrapWithBottom(dp(10));
        authLp.setMargins(0, dp(12), 0, 0);
        card.addView(authBox, authLp);
        authBox.addView(text("授权链接:", 14, MUTED, Typeface.NORMAL));
        TextView url = text(oauthAuthUrl.length() > 0 ? oauthAuthUrl : "点击开始 Codex 登录后显示授权链接", 14, oauthAuthUrl.length() > 0 ? TEXT : MUTED, Typeface.BOLD);
        url.setPadding(0, dp(8), 0, 0);
        authBox.addView(url);
        LinearLayout linkActions = horizontal();
        linkActions.setPadding(0, dp(12), 0, 0);
        authBox.addView(linkActions, new LinearLayout.LayoutParams(-1, dp(52)));
        Button copy = secondaryButton("复制链接");
        copy.setTextSize(13);
        copy.setPadding(dp(6), 0, dp(6), 0);
        linkActions.addView(copy, weightLp(1));
        copy.setEnabled(oauthAuthUrl.length() > 0);
        copy.setOnClickListener(v -> copyToClipboard("Codex OAuth", oauthAuthUrl));
        Button open = secondaryButton("打开链接");
        open.setTextSize(13);
        open.setPadding(dp(6), 0, dp(6), 0);
        LinearLayout.LayoutParams openLp = weightLp(1);
        openLp.setMargins(dp(10), 0, 0, 0);
        linkActions.addView(open, openLp);
        open.setEnabled(oauthAuthUrl.length() > 0);
        open.setOnClickListener(v -> openUrl(oauthAuthUrl));

        TextView callbackLabel = text("回调 URL", 16, TEXT, Typeface.BOLD);
        callbackLabel.setPadding(0, dp(4), 0, 0);
        card.addView(callbackLabel);
        EditText callback = input(oauthCallbackUrl);
        callback.setHint("http://localhost:1455/auth/callback?code=...&state=...");
        LinearLayout.LayoutParams callbackLp = new LinearLayout.LayoutParams(-1, dp(52));
        callbackLp.setMargins(0, dp(8), 0, 0);
        card.addView(callback, callbackLp);
        addMuted(card, "远程浏览器模式：当授权跳转到 localhost 后，复制完整 URL 并提交到这里。 ");
        LinearLayout bottom = horizontal();
        bottom.setPadding(0, dp(10), 0, 0);
        card.addView(bottom, new LinearLayout.LayoutParams(-1, dp(48)));
        Button submit = secondaryButton("提交回调 URL");
        bottom.addView(submit, weightLp(1));
        submit.setEnabled(!oauthLoading && oauthState.length() > 0);
        submit.setOnClickListener(v -> {
            hideKeyboard(callback);
            oauthCallbackUrl = callback.getText().toString().trim();
            submitOAuthCallback(oauthCallbackUrl);
        });
        Button refresh = secondaryButton("刷新账号");
        LinearLayout.LayoutParams refreshLp = weightLp(1);
        refreshLp.setMargins(dp(10), 0, 0, 0);
        bottom.addView(refresh, refreshLp);
        refresh.setOnClickListener(v -> refreshQuotaAccounts());
        return card;
    }

    private void refreshQuotaAccounts() {
        if (quotaLoading) return;
        if (managementKey == null || managementKey.trim().length() == 0) {
            quotaStatus = "缺少管理 Key";
            quotaDetail = "请输入管理 Key 后再刷新。网页端如果能登录，使用同一个 Key。";
            render();
            return;
        }
        quotaUrl = correctedQuotaUrl(quotaUrl);
        prefs.edit().putString(KEY_QUOTA_URL, quotaUrl).apply();
        quotaLoading = true;
        quotaStatus = "正在刷新";
        quotaDetail = "正在读取 /v0/management/auth-files...";
        codexQuotaAccounts.clear();
        render();
        executor.execute(() -> {
            String resultStatus = "刷新失败";
            String resultDetail;
            List<CodexQuotaAccount> accounts = new ArrayList<>();
            try {
                String apiBase = null;
                String authFilesBody = null;
                Exception lastError = null;
                for (String candidate : managementApiBaseCandidates()) {
                    try {
                        authFilesBody = request("GET", candidate + "/auth-files", null, managementHeaders(), 10000, 20000);
                        apiBase = candidate;
                        break;
                    } catch (Exception e) {
                        lastError = e;
                    }
                }
                if (apiBase == null || authFilesBody == null) throw lastError == null ? new IOException("无法连接配额管理接口") : lastError;
                JSONArray files = extractArray(authFilesBody);
                int total = files.length();
                int codex = 0;
                int disabled = 0;
                int success = 0;
                for (int i = 0; i < files.length(); i++) {
                    JSONObject file = files.optJSONObject(i);
                    if (file == null) continue;
                    String name = file.optString("name", "");
                    String type = detectAuthType(file, name);
                    if (!"codex".equals(type)) continue;
                    if (!isQuotaAccountEnabled(file)) {
                        disabled++;
                        continue;
                    }
                    CodexQuotaAccount account = quotaAccountFromFile(file);
                    if (account.authIndex.length() == 0) continue;
                    codex++;
                    if (refreshCodexQuotaInWorker(apiBase, account)) success++;
                    accounts.add(account);
                }
                resultStatus = "刷新完成";
                String hidden = disabled > 0 ? "。已隐藏停用账号：" + disabled : "";
                resultDetail = codex == 0
                        ? "未发现已启用且带 auth_index 的 Codex OAuth 凭证。auth-files 总数：" + total + hidden + "。已使用：" + apiBase
                        : "已启用 Codex " + success + "/" + codex + " 个账号刷新成功。auth-files 总数：" + total + hidden + "。已使用：" + apiBase;
            } catch (Exception e) {
                resultDetail = cleanError(e);
            }
            String finalStatus = resultStatus;
            String finalDetail = resultDetail;
            mainHandler.post(() -> {
                quotaLoading = false;
                quotaStatus = finalStatus;
                quotaDetail = finalDetail;
                codexQuotaAccounts.clear();
                codexQuotaAccounts.addAll(accounts);
                render();
            });
        });
    }

    private void refreshQuotaNative() {
        if (quotaLoading) return;
        if (managementKey == null || managementKey.trim().length() == 0) {
            quotaStatus = "缺少管理 Key";
            quotaDetail = "请输入管理 Key 后再刷新。网页端如果能登录，使用同一个 Key。";
            render();
            return;
        }
        quotaLoading = true;
        quotaStatus = "正在刷新";
        quotaDetail = "正在读取 /v0/management/auth-files...";
        codexQuotaAccounts.clear();
        render();
        executor.execute(() -> {
            String resultStatus = "刷新失败";
            String resultDetail;
            List<CodexQuotaAccount> accounts = new ArrayList<>();
            try {
                String apiBase = managementApiBase();
                String authFilesBody = request("GET", apiBase + "/auth-files", null, managementHeaders(), 10000, 20000);
                JSONArray files = extractArray(authFilesBody);
                int total = files.length();
                int codex = 0;
                int disabled = 0;
                int success = 0;
                for (int i = 0; i < files.length(); i++) {
                    JSONObject file = files.optJSONObject(i);
                    if (file == null) continue;
                    String name = file.optString("name", "");
                    String type = detectAuthType(file, name);
                    if (!"codex".equals(type)) continue;
                    if (!isQuotaAccountEnabled(file)) {
                        disabled++;
                        continue;
                    }
                    CodexQuotaAccount account = quotaAccountFromFile(file);
                    if (account.authIndex.length() == 0) continue;
                    codex++;
                    if (refreshCodexQuotaInWorker(apiBase, account)) success++;
                    accounts.add(account);
                }
                resultStatus = "刷新完成";
                String hidden = disabled > 0 ? "。已隐藏停用账号：" + disabled : "";
                resultDetail = codex == 0
                        ? "未发现已启用且带 auth_index 的 Codex OAuth 凭证。auth-files 总数：" + total + hidden
                        : "已启用 Codex " + success + "/" + codex + " 个账号刷新成功。auth-files 总数：" + total + hidden;
            } catch (Exception e) {
                resultDetail = cleanError(e);
            }
            String finalStatus = resultStatus;
            String finalDetail = resultDetail;
            mainHandler.post(() -> {
                quotaLoading = false;
                quotaStatus = finalStatus;
                quotaDetail = finalDetail;
                codexQuotaAccounts.clear();
                codexQuotaAccounts.addAll(accounts);
                render();
            });
        });
    }

    private void refreshApiKeys() {
        if (apiKeysLoading) return;
        if (managementKey == null || managementKey.trim().length() == 0) {
            apiKeysStatus = "缺少管理 Key";
            apiKeysDetail = "请先在设置页保存 CLI Proxy API 的管理 Key。";
            render();
            return;
        }
        apiKeysLoading = true;
        apiKeysStatus = "正在刷新";
        apiKeysDetail = "正在读取 /v0/management/api-keys...";
        render();
        executor.execute(() -> {
            String status = "刷新失败";
            String detail;
            List<ApiKeyItem> keys = new ArrayList<>();
            try {
                String apiBase = null;
                String body = null;
                Exception lastError = null;
                for (String candidate : managementApiBaseCandidates()) {
                    try {
                        body = request("GET", candidate + "/api-keys", null, managementHeaders(), 10000, 20000);
                        apiBase = candidate;
                        break;
                    } catch (Exception e) {
                        lastError = e;
                    }
                }
                if (apiBase == null || body == null) throw lastError == null ? new IOException("无法连接 API Key 接口") : lastError;
                keys.addAll(parseApiKeys(body));
                status = "刷新完成";
                detail = "已读取 " + keys.size() + " 个 API 密钥。已使用：" + apiBase;
            } catch (Exception e) {
                detail = cleanError(e);
            }
            String finalStatus = status;
            String finalDetail = detail;
            mainHandler.post(() -> {
                apiKeysLoading = false;
                apiKeysStatus = finalStatus;
                apiKeysDetail = finalDetail;
                apiKeyItems.clear();
                apiKeyItems.addAll(keys);
                render();
            });
        });
    }

    private void addApiKey(String rawKey) {
        String key = rawKey == null ? "" : rawKey.trim();
        if (key.length() == 0) {
            apiKeysStatus = "添加失败";
            apiKeysDetail = "API 密钥不能为空。";
            render();
            return;
        }
        if (apiKeysLoading) return;
        apiKeysLoading = true;
        apiKeysStatus = "正在添加";
        apiKeysDetail = "正在保存 API 密钥列表...";
        render();
        executor.execute(() -> {
            String status = "添加失败";
            String detail;
            List<ApiKeyItem> keys = new ArrayList<>();
            try {
                String apiBase = firstWorkingApiKeyBase();
                keys.addAll(loadApiKeysFromWorker(apiBase));
                boolean exists = false;
                for (ApiKeyItem item : keys) if (key.equals(item.value)) exists = true;
                if (!exists) {
                    ApiKeyItem item = new ApiKeyItem();
                    item.index = keys.size();
                    item.name = "API Key " + (item.index + 1);
                    item.value = key;
                    keys.add(item);
                }
                saveApiKeyList(apiBase, apiKeyValues(keys));
                keys.clear();
                keys.addAll(loadApiKeysFromWorker(apiBase));
                status = "添加完成";
                detail = exists ? "该 API 密钥已存在，列表已刷新。" : "已添加 API 密钥。";
            } catch (Exception e) {
                detail = cleanError(e);
            }
            String finalStatus = status;
            String finalDetail = detail;
            mainHandler.post(() -> {
                apiKeysLoading = false;
                apiKeysStatus = finalStatus;
                apiKeysDetail = finalDetail;
                apiKeyItems.clear();
                apiKeyItems.addAll(keys);
                render();
            });
        });
    }

    private void deleteApiKey(ApiKeyItem item) {
        if (item == null || apiKeysLoading) return;
        apiKeysLoading = true;
        apiKeysStatus = "正在删除";
        apiKeysDetail = "正在删除 " + maskApiKey(item.value) + "...";
        render();
        executor.execute(() -> {
            String status = "删除失败";
            String detail;
            List<ApiKeyItem> keys = new ArrayList<>();
            try {
                String apiBase = firstWorkingApiKeyBase();
                keys.addAll(loadApiKeysFromWorker(apiBase));
                int index = findApiKeyIndex(keys, item);
                if (index < 0) throw new IOException("未找到要删除的 API 密钥");
                try {
                    request("DELETE", apiBase + "/api-keys?index=" + index, null, managementHeaders(), 10000, 20000);
                } catch (Exception deleteError) {
                    keys.remove(index);
                    saveApiKeyList(apiBase, apiKeyValues(keys));
                }
                keys.clear();
                keys.addAll(loadApiKeysFromWorker(apiBase));
                status = "删除完成";
                detail = "已删除 API 密钥。";
            } catch (Exception e) {
                detail = cleanError(e);
            }
            String finalStatus = status;
            String finalDetail = detail;
            mainHandler.post(() -> {
                apiKeysLoading = false;
                apiKeysStatus = finalStatus;
                apiKeysDetail = finalDetail;
                apiKeyItems.clear();
                apiKeyItems.addAll(keys);
                render();
            });
        });
    }

    private String firstWorkingApiKeyBase() throws Exception {
        Exception lastError = null;
        for (String candidate : managementApiBaseCandidates()) {
            try {
                request("GET", candidate + "/api-keys", null, managementHeaders(), 10000, 20000);
                return candidate;
            } catch (Exception e) {
                lastError = e;
            }
        }
        throw lastError == null ? new IOException("无法连接 API Key 接口") : lastError;
    }

    private List<ApiKeyItem> loadApiKeysFromWorker(String apiBase) throws Exception {
        return parseApiKeys(request("GET", apiBase + "/api-keys", null, managementHeaders(), 10000, 20000));
    }

    private void saveApiKeyList(String apiBase, List<String> values) throws Exception {
        JSONArray arr = new JSONArray();
        for (String value : values) arr.put(value);
        try {
            request("PUT", apiBase + "/api-keys", arr.toString(), managementHeaders(), 10000, 20000);
        } catch (Exception arrayError) {
            JSONObject payload = new JSONObject();
            payload.put("api-keys", arr);
            request("PUT", apiBase + "/api-keys", payload.toString(), managementHeaders(), 10000, 20000);
        }
    }

    private List<ApiKeyItem> parseApiKeys(String body) throws JSONException {
        List<ApiKeyItem> out = new ArrayList<>();
        Object value = new JSONTokener(body).nextValue();
        JSONArray arr = null;
        if (value instanceof JSONArray) arr = (JSONArray) value;
        else if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            arr = obj.optJSONArray("api-keys");
            if (arr == null) arr = obj.optJSONArray("apiKeys");
            if (arr == null) arr = obj.optJSONArray("api_keys");
            if (arr == null) arr = obj.optJSONArray("data");
        }
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            ApiKeyItem item = new ApiKeyItem();
            item.index = i;
            Object raw = arr.opt(i);
            if (raw instanceof JSONObject) {
                JSONObject obj = (JSONObject) raw;
                item.id = firstNonEmpty(obj.optString("id", ""), obj.optString("key", ""), obj.optString("value", ""));
                item.name = firstNonEmpty(obj.optString("name", ""), obj.optString("label", ""), "API Key " + (i + 1));
                item.value = firstNonEmpty(obj.optString("api-key", ""), obj.optString("apiKey", ""), obj.optString("value", ""), obj.optString("key", ""), item.id);
                item.createdAt = firstNonEmpty(obj.optString("created_at", ""), obj.optString("createdAt", ""));
            } else {
                item.value = raw == null || JSONObject.NULL.equals(raw) ? "" : String.valueOf(raw);
                item.id = item.value;
                item.name = "API Key " + (i + 1);
            }
            if (item.value.length() > 0) out.add(item);
        }
        return out;
    }

    private List<String> apiKeyValues(List<ApiKeyItem> keys) {
        List<String> out = new ArrayList<>();
        for (ApiKeyItem item : keys) if (item.value != null && item.value.trim().length() > 0) out.add(item.value.trim());
        return out;
    }

    private int findApiKeyIndex(List<ApiKeyItem> keys, ApiKeyItem target) {
        if (target.index >= 0 && target.index < keys.size()) return target.index;
        for (int i = 0; i < keys.size(); i++) {
            ApiKeyItem item = keys.get(i);
            if (target.value != null && target.value.equals(item.value)) return i;
            if (target.id != null && target.id.length() > 0 && target.id.equals(item.id)) return i;
        }
        return -1;
    }

    private String generateApiKeyValue() {
        return "sk-" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String maskApiKey(String value) {
        if (value == null || value.length() == 0) return "未命名密钥";
        String raw = value.trim();
        if (raw.length() <= 12) return raw;
        return raw.substring(0, 6) + "..." + raw.substring(raw.length() - 4);
    }

    private void startCodexOAuth() {
        if (oauthLoading) return;
        focusOAuthAfterRender = true;
        if (managementKey == null || managementKey.trim().length() == 0) {
            oauthStatus = "缺少管理 Key";
            oauthDetail = "请先在设置页保存 CLI Proxy API 的管理 Key。";
            render();
            return;
        }
        oauthLoading = true;
        oauthPolling = false;
        oauthStatus = "正在启动";
        oauthDetail = "正在请求 Codex 授权链接...";
        oauthAuthUrl = "";
        oauthState = "";
        oauthCallbackUrl = "";
        oauthPollAttempts = 0;
        render();
        executor.execute(() -> {
            String status = "启动失败";
            String detail;
            String authUrl = "";
            String state = "";
            try {
                Exception lastError = null;
                for (String candidate : managementApiBaseCandidates()) {
                    try {
                        JSONObject obj = new JSONObject(request("GET", candidate + "/codex-auth-url?is_webui=true", null, managementHeaders(), 10000, 20000));
                        authUrl = firstNonEmpty(obj.optString("url", ""), obj.optString("auth_url", ""), obj.optString("authUrl", ""), obj.optString("authorize_url", ""), obj.optString("authorizeUrl", ""));
                        state = firstNonEmpty(obj.optString("state", ""), queryParam(authUrl, "state"));
                        if (state.length() == 0) throw new IOException("授权响应缺少 state");
                        status = "等待认证";
                        detail = authUrl.length() > 0 ? "授权链接已生成，请复制或打开链接完成登录。" : "授权已启动，请在管理端继续完成登录。";
                        String finalStatus = status;
                        String finalDetail = detail;
                        String finalAuthUrl = authUrl;
                        String finalState = state;
                        mainHandler.post(() -> {
                            oauthLoading = false;
                            oauthPolling = true;
                            oauthStatus = finalStatus;
                            oauthDetail = finalDetail;
                            oauthAuthUrl = finalAuthUrl;
                            oauthState = finalState;
                            focusOAuthAfterRender = true;
                            render();
                            pollOAuthStatus();
                        });
                        return;
                    } catch (Exception e) {
                        lastError = e;
                    }
                }
                throw lastError == null ? new IOException("无法连接 Codex OAuth 接口") : lastError;
            } catch (Exception e) {
                detail = cleanError(e);
            }
            String finalStatus = status;
            String finalDetail = detail;
            mainHandler.post(() -> {
                oauthLoading = false;
                oauthPolling = false;
                oauthStatus = finalStatus;
                oauthDetail = finalDetail;
                focusOAuthAfterRender = true;
                render();
            });
        });
    }

    private void submitOAuthCallback(String callbackUrl) {
        focusOAuthAfterRender = true;
        String value = callbackUrl == null ? "" : callbackUrl.trim();
        if (value.length() == 0) {
            oauthStatus = "回调缺失";
            oauthDetail = "请粘贴完整回调 URL 后再提交。";
            render();
            return;
        }
        if (oauthState.length() == 0) {
            oauthStatus = "缺少 state";
            oauthDetail = "请先点击开始 Codex 登录生成本次 OAuth state。";
            render();
            return;
        }
        if (oauthLoading) return;
        oauthLoading = true;
        oauthStatus = "正在提交回调";
        oauthDetail = "正在提交回调 URL...";
        render();
        executor.execute(() -> {
            String status = "提交失败";
            String detail;
            try {
                JSONObject payload = new JSONObject();
                payload.put("provider", "codex");
                payload.put("redirect_url", normalizeOAuthCallback(value));
                Exception lastError = null;
                for (String candidate : managementApiBaseCandidates()) {
                    try {
                        request("POST", candidate + "/oauth-callback", payload.toString(), managementHeaders(), 10000, 20000);
                        status = "回调已提交";
                        detail = "回调 URL 已提交，等待认证完成。";
                        String finalStatus = status;
                        String finalDetail = detail;
                        mainHandler.post(() -> {
                            oauthLoading = false;
                            oauthPolling = true;
                            oauthStatus = finalStatus;
                            oauthDetail = finalDetail;
                            focusOAuthAfterRender = true;
                            render();
                            pollOAuthStatus();
                        });
                        return;
                    } catch (Exception e) {
                        lastError = e;
                    }
                }
                throw lastError == null ? new IOException("无法连接 OAuth 回调接口") : lastError;
            } catch (Exception e) {
                detail = cleanError(e);
            }
            String finalStatus = status;
            String finalDetail = detail;
            mainHandler.post(() -> {
                oauthLoading = false;
                oauthStatus = finalStatus;
                oauthDetail = finalDetail;
                focusOAuthAfterRender = true;
                render();
            });
        });
    }

    private void pollOAuthStatus() {
        if (!oauthPolling || oauthState.length() == 0 || oauthPollAttempts > 80) return;
        oauthPollAttempts++;
        String state = oauthState;
        executor.execute(() -> {
            String status = oauthStatus;
            String detail = oauthDetail;
            boolean done = false;
            try {
                Exception lastError = null;
                for (String candidate : managementApiBaseCandidates()) {
                    try {
                        JSONObject obj = new JSONObject(request("GET", candidate + "/get-auth-status?state=" + Uri.encode(state), null, managementHeaders(), 8000, 12000));
                        String raw = firstNonEmpty(obj.optString("status", ""), obj.optString("state", ""));
                        if ("ok".equalsIgnoreCase(raw) || "success".equalsIgnoreCase(raw) || obj.optBoolean("success", false)) {
                            status = "认证成功";
                            detail = "Codex OAuth 认证成功，正在刷新账号。";
                            done = true;
                        } else if ("error".equalsIgnoreCase(raw) || obj.has("error")) {
                            status = "认证失败";
                            detail = firstNonEmpty(obj.optString("error", ""), obj.optString("message", ""), "OAuth 认证失败");
                            done = true;
                        } else {
                            status = "等待认证";
                            detail = "等待认证中...";
                        }
                        break;
                    } catch (Exception e) {
                        lastError = e;
                    }
                }
                if (lastError != null && "等待认证".equals(status)) detail = cleanError(lastError);
            } catch (Exception e) {
                status = "检查失败";
                detail = cleanError(e);
                done = true;
            }
            boolean finalDone = done;
            String finalStatus = status;
            String finalDetail = detail;
            mainHandler.post(() -> {
                oauthStatus = finalStatus;
                oauthDetail = finalDetail;
                if (finalDone) {
                    oauthPolling = false;
                    oauthLoading = false;
                    focusOAuthAfterRender = true;
                    render();
                    if ("认证成功".equals(finalStatus)) refreshQuotaAccounts();
                } else {
                    mainHandler.postDelayed(this::pollOAuthStatus, 3000);
                }
            });
        });
    }

    private String normalizeOAuthCallback(String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;
        if (raw.startsWith("code=") || raw.contains("&state=")) return "http://localhost:1455/auth/callback?" + raw;
        return raw;
    }

    private String queryParam(String url, String key) {
        if (url == null || url.length() == 0 || key == null || key.length() == 0) return "";
        try {
            Uri uri = Uri.parse(url);
            String value = uri.getQueryParameter(key);
            return value == null ? "" : value;
        } catch (Exception ignored) {
            return "";
        }
    }

    private void copyToClipboard(String label, String value) {
        if (value == null || value.length() == 0) return;
        try {
            ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager != null) manager.setPrimaryClip(ClipData.newPlainText(label, value));
            toast("已复制链接");
        } catch (Exception e) {
            toast("复制失败：" + cleanError(e));
        }
    }

    private void openUrl(String url) {
        if (url == null || url.length() == 0) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast("无法打开链接：" + cleanError(e));
        }
    }

    private String managementWebBase() {
        String value = quotaUrl == null || quotaUrl.length() == 0 ? DEFAULT_QUOTA_URL : correctedQuotaUrl(quotaUrl);
        int index = value.indexOf("/management.html");
        if (index >= 0) value = value.substring(0, index);
        int hash = value.indexOf('#');
        if (hash >= 0) value = value.substring(0, hash);
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private void addQuotaAccountCard(LinearLayout parent, CodexQuotaAccount account) {
        LinearLayout card = card();
        parent.addView(card, matchWrapWithBottom(dp(10)));

        LinearLayout top = horizontal();
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top);
        LinearLayout title = vertical();
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        title.addView(text(account.name, 16, TEXT, Typeface.BOLD));
        TextView sub = text(account.authIndex.length() == 0 ? "Codex OAuth" : "账号 " + maskIdentifier(account.authIndex), 12, MUTED, Typeface.NORMAL);
        sub.setPadding(0, dp(3), 0, 0);
        title.addView(sub);
        top.addView(pill(account.loading ? "刷新中" : account.resetting ? "重置中" : account.error.length() > 0 ? "异常" : "Codex", account.error.length() > 0 ? RED : BLUE, account.error.length() > 0 ? SOFT_RED : SOFT_BLUE));

        addKeyValue(card, "套餐", nonEmpty(displayPlan(account.planType), "未知"));
        addKeyValue(card, "续期时间", nonEmpty(formatSubscriptionTime(account.subscriptionActiveUntil), "未知"));
        addKeyValue(card, "主动重置次数", account.resetCreditsKnown ? String.valueOf(account.resetCreditsAvailableCount) : "未知");

        if (!account.resetCreditExpiries.isEmpty()) {
            TextView exp = text("重置额度有效期：" + joinExpiries(account.resetCreditExpiries), 12, MUTED, Typeface.NORMAL);
            exp.setPadding(0, dp(7), 0, 0);
            card.addView(exp);
        }
        if (account.resetCreditsError.length() > 0) addQuotaMessage(card, account.resetCreditsError, ORANGE, SOFT_ORANGE);
        if (account.error.length() > 0) addQuotaMessage(card, account.error, RED, SOFT_RED);
        if (account.lastActionMessage.length() > 0) addQuotaMessage(card, account.lastActionMessage, GREEN, SOFT_GREEN);

        if (account.windows.isEmpty()) {
            TextView empty = text(nonEmpty(account.rawSummary, "未返回可解析的额度窗口"), 12, MUTED, Typeface.NORMAL);
            empty.setPadding(0, dp(10), 0, 0);
            card.addView(empty);
        } else {
            for (QuotaWindow window : account.windows) addQuotaWindow(card, window);
        }

        LinearLayout actions = horizontal();
        actions.setPadding(0, dp(12), 0, 0);
        card.addView(actions, new LinearLayout.LayoutParams(-1, dp(48)));
        Button reset = secondaryButton("重置额度");
        actions.addView(reset, weightLp(1));
        reset.setEnabled(!quotaLoading && !account.loading && !account.resetting);
        reset.setOnClickListener(v -> {
            resetCodexQuota(account.authIndex);
        });
        Button refresh = primaryButton("刷新额度");
        LinearLayout.LayoutParams refreshLp = weightLp(1);
        refreshLp.setMargins(dp(10), 0, 0, 0);
        actions.addView(refresh, refreshLp);
        refresh.setEnabled(!quotaLoading && !account.loading && !account.resetting);
        refresh.setOnClickListener(v -> {
            refreshCodexQuota(account.authIndex);
        });
    }

    private void addQuotaWindow(LinearLayout parent, QuotaWindow window) {
        LinearLayout block = vertical();
        block.setPadding(0, dp(12), 0, 0);
        parent.addView(block);
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        block.addView(row);
        row.addView(text(window.label, 13, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        String remain = window.remainingPercent < 0 ? "剩余 --" : "剩余 " + Math.round(window.remainingPercent) + "%";
        row.addView(text(remain, 12, window.remainingPercent >= 50 ? GREEN : window.remainingPercent >= 20 ? ORANGE : RED, Typeface.BOLD));
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress((int) Math.max(0, Math.min(100, Math.round(window.remainingPercent < 0 ? 0 : window.remainingPercent))));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, dp(10));
        barLp.setMargins(0, dp(6), 0, 0);
        block.addView(bar, barLp);
        TextView meta = text("已用 " + (window.usedPercent < 0 ? "--" : Math.round(window.usedPercent) + "%") + " · 重置 " + nonEmpty(window.resetLabel, "未知"), 12, MUTED, Typeface.NORMAL);
        meta.setPadding(0, dp(4), 0, 0);
        block.addView(meta);
    }

    private void addQuotaMessage(LinearLayout parent, String message, int color, int bg) {
        TextView view = text(cleanText(message, 220), 12, color, Typeface.BOLD);
        view.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(10), 0, 0);
        view.setBackground(round(bg, 8));
        parent.addView(view, lp);
    }

    private void updateHeader() {
        if (headerMeta != null) headerMeta.setText(selectedTab == 4 ? quotaUrl : baseUrl);
        if (selectedTab == 4) {
            setStatus("账号", BLUE, SOFT_BLUE, "CLI Proxy API 账号额度");
            return;
        }
        if (selectedTab == 5) {
            setStatus("设置", BLUE, SOFT_BLUE, "CPA Usage Keeper 与 CLI Proxy API");
            return;
        }
        if (loading) {
            setStatus("刷新中", BLUE, SOFT_BLUE, "正在读取 Usage Keeper 数据");
        } else if (viewSnapshot.online) {
            setStatus("在线", GREEN, SOFT_GREEN, "范围 " + rangeLabel(selectedRange) + " · 最近同步：" + nonEmpty(shortTime(viewSnapshot.lastRunAt), "未知") + " · 本机刷新：" + viewSnapshot.loadedAtLabel());
        } else if (viewSnapshot.hasAnyData()) {
            setStatus("部分可用", ORANGE, SOFT_ORANGE, "范围 " + rangeLabel(selectedRange) + " · 本机刷新：" + viewSnapshot.loadedAtLabel());
        } else if (viewSnapshot.error != null && viewSnapshot.error.length() > 0) {
            setStatus("异常", RED, SOFT_RED, cleanText(viewSnapshot.error, 42));
        } else {
            setStatus("未刷新", MUTED, Color.rgb(239, 242, 247), "等待数据");
        }
    }

    private void renderLoading() {
        LinearLayout card = card();
        content.addView(card, matchWrapWithBottom(dp(12)));
        card.addView(new ProgressBar(this), new LinearLayout.LayoutParams(-1, dp(48)));
        TextView title = text("正在连接 Usage Keeper", 17, TEXT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(title);
        TextView hint = text("首次读取 /api/v1/usage 可能需要几秒，完成后会按当前范围重新计算。", 13, MUTED, Typeface.NORMAL);
        hint.setGravity(Gravity.CENTER_HORIZONTAL);
        hint.setPadding(0, dp(8), 0, 0);
        card.addView(hint);
    }

    private void renderErrorBanner() {
        LinearLayout card = card();
        card.setBackground(roundStroke(SOFT_RED, Color.rgb(255, 205, 211), 8));
        content.addView(card, matchWrapWithBottom(dp(12)));
        card.addView(text("接口异常", 15, RED, Typeface.BOLD));
        TextView msg = text(viewSnapshot.error, 12, RED, Typeface.NORMAL);
        msg.setPadding(0, dp(6), 0, 0);
        card.addView(msg);
    }

    private void renderDashboard() {
        LinearLayout top = card();
        content.addView(top, matchWrapWithBottom(dp(12)));
        LinearLayout row = horizontal();
        top.addView(row, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout left = vertical();
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        left.addView(text("运行概览", 18, TEXT, Typeface.BOLD));
        addMuted(left, viewSnapshot.authenticated ? "会话有效 · 当前数据范围：" + rangeLabel(selectedRange) : "会话未确认 · 请检查认证配置");
        row.addView(pill(viewSnapshot.syncRunning ? "同步中" : "同步空闲", viewSnapshot.syncRunning ? ORANGE : GREEN, viewSnapshot.syncRunning ? SOFT_ORANGE : SOFT_GREEN));

        addMetricGrid(content, new Metric[]{
                new Metric("请求数", formatCount(viewSnapshot.totalRequests), "成功 " + formatCount(viewSnapshot.successCount)),
                new Metric("成功率", percent(viewSnapshot.successRate()), "失败 " + formatCount(viewSnapshot.failureCount)),
                new Metric("Token", formatCount(viewSnapshot.totalTokens), inputOutputLabel(viewSnapshot)),
                new Metric("平均耗时", viewSnapshot.avgLatencyMs > 0 ? formatLatency(viewSnapshot.avgLatencyMs) : "未汇总", "P95 " + (viewSnapshot.p95LatencyMs > 0 ? formatLatency(viewSnapshot.p95LatencyMs) : "未知"))
        });

        LinearLayout service = card();
        content.addView(service, matchWrapWithBottom(dp(12)));
        service.addView(text("服务状态", 16, TEXT, Typeface.BOLD));
        addKeyValue(service, "统计地址", baseUrl);
        addKeyValue(service, "范围", rangeLabel(selectedRange));
        addKeyValue(service, "服务运行", viewSnapshot.online ? "running" : "unknown/offline");
        addKeyValue(service, "模型数量", String.valueOf(viewSnapshot.modelCount()));
        addKeyValue(service, "凭证数量", String.valueOf(viewSnapshot.credentials.size()));

        LinearLayout health = card();
        content.addView(health, matchWrapWithBottom(dp(12)));
        LinearLayout healthTop = horizontal();
        healthTop.setGravity(Gravity.CENTER_VERTICAL);
        health.addView(healthTop);
        healthTop.addView(pill("稳定性", MUTED, Color.rgb(247, 244, 238)));
        TextView range = text(rangeLabel(selectedRange), 12, MUTED, Typeface.NORMAL);
        range.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams rangeLp = new LinearLayout.LayoutParams(0, -2, 1);
        rangeLp.setMargins(dp(8), 0, dp(8), 0);
        healthTop.addView(range, rangeLp);
        int healthColor = viewSnapshot.successRate() >= 0.98 ? GREEN : viewSnapshot.successRate() >= 0.9 ? ORANGE : RED;
        healthTop.addView(pill(percent(viewSnapshot.successRate()), healthColor, healthColor == GREEN ? SOFT_GREEN : healthColor == ORANGE ? SOFT_ORANGE : SOFT_RED));
        TextView healthTitle = text("请求健康时间线", 18, TEXT, Typeface.BOLD);
        healthTitle.setPadding(0, dp(8), 0, 0);
        health.addView(healthTitle);
        addMuted(health, "用紧凑的可靠性条带展示当前范围的请求结果。 ");
        addHealthHeatmap(health, healthCells(viewSnapshot));
    }

    private void renderStats() {
        addSectionTitle("成本", "当前范围：" + rangeLabel(selectedRange));
        LinearLayout trend = card();
        content.addView(trend, matchWrapWithBottom(dp(12)));
        trend.addView(text("成本趋势", 16, TEXT, Typeface.BOLD));
        addLineChart(trend, "成本", costTrendPoints(viewSnapshot), "$", false);

        addMetricGrid(content, new Metric[]{
                new Metric("成本估算", viewSnapshot.costLabel(), "按手机端价格表实时计算"),
                new Metric("Token 总量", formatCount(viewSnapshot.totalTokens), inputOutputLabel(viewSnapshot)),
                new Metric("请求总量", formatCount(viewSnapshot.totalRequests), "失败 " + formatCount(viewSnapshot.failureCount)),
                new Metric("成功率", percent(viewSnapshot.successRate()), "成功 " + formatCount(viewSnapshot.successCount))
        });

        renderPriceEditor();

        LinearLayout token = card();
        content.addView(token, matchWrapWithBottom(dp(12)));
        token.addView(text("Token 结构", 16, TEXT, Typeface.BOLD));
        long max = Math.max(1, Math.max(viewSnapshot.inputTokens, Math.max(viewSnapshot.outputTokens, Math.max(viewSnapshot.reasoningTokens, viewSnapshot.cachedTokens))));
        addProgress(token, "输入", viewSnapshot.inputTokens, max);
        addProgress(token, "输出", viewSnapshot.outputTokens, max);
        addProgress(token, "Reasoning", viewSnapshot.reasoningTokens, max);
        addProgress(token, "Cached", viewSnapshot.cachedTokens, max);
    }

    private void renderPriceEditor() {
        LinearLayout card = card();
        content.addView(card, matchWrapWithBottom(dp(12)));
        card.addView(text("价格配置", 16, TEXT, Typeface.BOLD));
        addMuted(card, "单位：美元 / 1M token。启动或刷新时会读取网页端 /api/v1/pricing；手机端修改后本地保存并立即重算成本。 ");

        List<String> models = new ArrayList<>();
        for (String model : viewSnapshot.models.keySet()) if (!models.contains(model)) models.add(model);
        for (String model : priceTable.keySet()) if (!models.contains(model)) models.add(model);
        Collections.sort(models);
        if (models.isEmpty()) {
            addEmpty(card, "暂无模型或价格数据");
            return;
        }

        int count = 0;
        for (String model : models) {
            PriceRule existing = priceForModel(model);
            PriceRule rule = existing == null ? new PriceRule() : existing.copyFor(model);
            rule.model = model;
            LinearLayout item = card();
            LinearLayout.LayoutParams itemLp = matchWrapWithBottom(dp(10));
            itemLp.setMargins(0, dp(10), 0, 0);
            card.addView(item, itemLp);
            item.addView(text(model, 14, TEXT, Typeface.BOLD));
            addKeyValue(item, "当前范围成本", modelCostLabel(model));

            LinearLayout row = horizontal();
            row.setPadding(0, dp(8), 0, 0);
            item.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
            EditText input = numberInput(rule.inputPer1m);
            EditText output = numberInput(rule.outputPer1m);
            EditText cache = numberInput(rule.cachePer1m);
            row.addView(input, weightLp(1));
            LinearLayout.LayoutParams outLp = weightLp(1); outLp.setMargins(dp(6), 0, 0, 0); row.addView(output, outLp);
            LinearLayout.LayoutParams cacheLp = weightLp(1); cacheLp.setMargins(dp(6), 0, 0, 0); row.addView(cache, cacheLp);
            addMuted(item, "左到右：输入价 / 输出价 / 缓存价");

            Button save = secondaryButton("保存此模型价格");
            LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(42));
            saveLp.setMargins(0, dp(8), 0, 0);
            item.addView(save, saveLp);
            save.setOnClickListener(v -> {
                PriceRule updated = new PriceRule();
                updated.model = model;
                updated.inputPer1m = parseDouble(input.getText().toString());
                updated.outputPer1m = parseDouble(output.getText().toString());
                updated.cachePer1m = parseDouble(cache.getText().toString());
                priceTable.put(model, updated);
                saveLocalPrices();
                applyCost(allSnapshot);
                viewSnapshot = applyRange(allSnapshot, selectedRange);
                applyCost(viewSnapshot);
                toast("已保存价格并重算成本");
                render();
            });

            count++;
            if (count >= 30) {
                addMuted(card, "仅显示前 30 个模型，更多模型可后续加搜索。 ");
                break;
            }
        }
    }

    private String modelCostLabel(String model) {
        ModelSummary summary = viewSnapshot.models.get(model);
        if (summary == null || summary.estimatedCost <= 0) return "未匹配价格或无用量";
        return "$" + twoDecimal.format(summary.estimatedCost);
    }

    private void renderResources() {
        addSegment(new String[]{"模型统计", "凭证状态"}, resourceTab, index -> { resourceTab = index; render(); });
        if (resourceTab == 0) renderModels(); else renderCredentials();
    }

    private void renderModels() {
        addSectionTitle("模型用量", "按当前范围内请求量排序");
        if (viewSnapshot.models.isEmpty()) { addEmptyCard("当前范围暂无模型数据"); return; }
        List<ModelSummary> models = new ArrayList<>(viewSnapshot.models.values());
        Collections.sort(models, (a, b) -> Long.compare(b.totalRequests, a.totalRequests));
        if (selectedModelFilter.length() == 0 || !viewSnapshot.models.containsKey(selectedModelFilter)) selectedModelFilter = models.get(0).name;

        LinearLayout selector = card();
        content.addView(selector, matchWrapWithBottom(dp(12)));
        selector.addView(text("Token 趋势", 16, TEXT, Typeface.BOLD));
        Spinner spinner = new Spinner(this);
        List<String> names = new ArrayList<>();
        for (ModelSummary model : models) names.add(model.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int selectedIndex = Math.max(0, names.indexOf(selectedModelFilter));
        final boolean[] initialized = new boolean[]{false};
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!initialized[0]) { initialized[0] = true; return; }
                selectedModelFilter = names.get(position);
                render();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinner.setSelection(selectedIndex, false);
        spinner.post(() -> initialized[0] = true);
        LinearLayout.LayoutParams spinnerLp = new LinearLayout.LayoutParams(-1, dp(48));
        spinnerLp.setMargins(0, dp(10), 0, 0);
        selector.addView(spinner, spinnerLp);
        addLineChart(selector, selectedModelFilter, tokenTrendPoints(viewSnapshot, selectedModelFilter), "token", false);

        int max = Math.min(60, models.size());
        for (int i = 0; i < max; i++) {
            ModelSummary m = models.get(i);
            LinearLayout card = card();
            content.addView(card, matchWrapWithBottom(dp(10)));
            LinearLayout row = horizontal();
            card.addView(row);
            row.addView(text(m.name, 16, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(pill(percent(m.successRate()), m.successRate() >= 0.9 ? GREEN : ORANGE, m.successRate() >= 0.9 ? SOFT_GREEN : SOFT_ORANGE));
            addKeyValue(card, "请求", formatCount(m.totalRequests));
            addKeyValue(card, "Token", formatCount(m.totalTokens));
            addKeyValue(card, "输入/输出", formatCount(m.inputTokens) + " / " + formatCount(m.outputTokens));
            addKeyValue(card, "平均耗时", m.avgLatencyMs() > 0 ? formatLatency(m.avgLatencyMs()) : "未汇总");
            addKeyValue(card, "最近使用", nonEmpty(shortTime(m.lastUsedAt), "未知"));
        }
    }

    private void renderCredentials() {
        addSectionTitle("凭证状态", "按当前范围估算健康状态，完整 Key 不会展示");
        if (viewSnapshot.credentials.isEmpty()) { addEmptyCard("当前范围暂无凭证数据"); return; }
        List<CredentialSummary> creds = new ArrayList<>(viewSnapshot.credentials.values());
        Collections.sort(creds, (a, b) -> Long.compare(b.totalRequests, a.totalRequests));
        for (CredentialSummary c : creds) {
            LinearLayout card = card();
            content.addView(card, matchWrapWithBottom(dp(10)));
            LinearLayout row = horizontal();
            card.addView(row);
            row.addView(text(nonEmpty(c.displayName, c.id), 16, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
            int color = c.healthColor();
            row.addView(pill(c.healthLabel(), color, color == GREEN ? SOFT_GREEN : color == RED ? SOFT_RED : SOFT_ORANGE));
            addKeyValue(card, "请求", formatCount(c.totalRequests));
            addKeyValue(card, "成功率", percent(c.successRate()));
            addKeyValue(card, "Token", formatCount(c.totalTokens));
            addKeyValue(card, "失败", formatCount(c.failureCount));
        }
    }

    private void renderLogsAndSettings() {
        renderLogs();
    }

    private void renderLogs() {
        LinearLayout filters = horizontal();
        content.addView(filters, matchWrapWithBottom(dp(12)));
        Button all = errorsOnly ? secondaryButton("全部") : primaryButton("全部");
        filters.addView(all, weightLp(1));
        all.setOnClickListener(v -> { errorsOnly = false; render(); });
        Button err = errorsOnly ? primaryButton("仅错误") : secondaryButton("仅错误");
        LinearLayout.LayoutParams errLp = weightLp(1); errLp.setMargins(dp(10), 0, 0, 0);
        filters.addView(err, errLp);
        err.setOnClickListener(v -> { errorsOnly = true; render(); });

        LinearLayout card = card();
        content.addView(card, matchWrapWithBottom(dp(12)));
        card.addView(text(errorsOnly ? "范围内错误日志" : "范围内最近事件", 16, TEXT, Typeface.BOLD));
        int added = 0;
        for (LogEntry log : viewSnapshot.logs) {
            if (errorsOnly && !log.failed) continue;
            addLogRow(card, log, false);
            if (++added >= 120) break;
        }
        if (added == 0) addEmpty(card, errorsOnly ? "当前范围暂无错误日志" : "当前范围暂无日志");
    }

    private void logoutToLogin() {
        loginPassword = "";
        managementKey = "";
        allSnapshot = new DataSnapshot();
        viewSnapshot = new DataSnapshot();
        codexQuotaAccounts.clear();
        quotaStatus = "未刷新";
        quotaDetail = "输入管理 Key 后可读取每个 Codex 账号的实时额度。";
        clearSecret(KEY_LOGIN_PASSWORD_ENCRYPTED, KEY_LOGIN_PASSWORD);
        clearSecret(KEY_MANAGEMENT_KEY_ENCRYPTED, KEY_MANAGEMENT_KEY);
        showLoginScreen();
    }

    private void renderSettings() {
        LinearLayout card = card();
        content.addView(card, matchWrapWithBottom(dp(12)));
        card.addView(text("CPA Usage Keeper设置", 16, TEXT, Typeface.BOLD));
        EditText input = input(baseUrl);
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(-1, dp(48)); inputLp.setMargins(0, dp(12), 0, dp(12));
        card.addView(input, inputLp);
        Button save = primaryButton("保存统计地址并刷新");
        card.addView(save, new LinearLayout.LayoutParams(-1, dp(48)));
        save.setOnClickListener(v -> { hideKeyboard(input); baseUrl = normalizeBaseUrl(input.getText().toString()); prefs.edit().putString(KEY_BASE_URL, baseUrl).apply(); refreshAll(); });

        LinearLayout quota = card();
        content.addView(quota, matchWrapWithBottom(dp(12)));
        quota.addView(text("CLI Proxy API设置", 16, TEXT, Typeface.BOLD));
        EditText q = input(quotaUrl);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(-1, dp(48)); qLp.setMargins(0, dp(12), 0, dp(12));
        quota.addView(q, qLp);
        Button saveQ = secondaryButton("保存配额页地址");
        quota.addView(saveQ, new LinearLayout.LayoutParams(-1, dp(48)));
        saveQ.setOnClickListener(v -> { hideKeyboard(q); quotaUrl = correctedQuotaUrl(q.getText().toString()); prefs.edit().putString(KEY_QUOTA_URL, quotaUrl).apply(); toast("已保存配额页地址"); });

        EditText key = input(managementKey);
        key.setHint("管理 Key / Management Key");
        LinearLayout.LayoutParams keyLp = new LinearLayout.LayoutParams(-1, dp(48)); keyLp.setMargins(0, dp(12), 0, dp(12));
        quota.addView(key, keyLp);
        Button saveKey = secondaryButton("保存管理 Key");
        quota.addView(saveKey, new LinearLayout.LayoutParams(-1, dp(48)));
        saveKey.setOnClickListener(v -> { hideKeyboard(key); managementKey = key.getText().toString().trim(); toast(saveSecret(KEY_MANAGEMENT_KEY_ENCRYPTED, KEY_MANAGEMENT_KEY, managementKey) ? "已保存管理 Key" : "管理 Key 未能加密保存，仅本次会话可用"); });

        Button relogin = primaryButton("返回登录页");
        LinearLayout.LayoutParams reloginLp = new LinearLayout.LayoutParams(-1, dp(48)); reloginLp.setMargins(0, dp(12), 0, 0);
        quota.addView(relogin, reloginLp);
        relogin.setOnClickListener(v -> showLoginScreen());
    }

    private void parseSession(DataSnapshot target, JSONObject obj) { target.authenticated = obj.optBoolean("authenticated", false); }
    private void parseStatus(DataSnapshot target, JSONObject obj) { target.online = obj.optBoolean("running", false); target.syncRunning = obj.optBoolean("sync_running", false); target.lastRunAt = obj.optString("last_run_at", ""); target.statusLoaded = true; }
    private void parseModelNames(DataSnapshot target, JSONObject obj) { JSONArray arr = obj.optJSONArray("models"); if (arr == null) return; for (int i = 0; i < arr.length(); i++) { String name = arr.optString(i, ""); if (name.length() > 0 && !target.models.containsKey(name)) target.models.put(name, new ModelSummary(name)); } }

    private void parseUsage(DataSnapshot target, JSONObject root) throws JSONException {
        JSONObject usage = root.optJSONObject("usage"); if (usage == null) usage = root;
        target.totalRequests = optLongAny(usage, "total_requests", "totalRequests", "request_total", "requests");
        target.successCount = optLongAny(usage, "success_count", "successCount", "success_requests", "successes");
        target.failureCount = optLongAny(usage, "failure_count", "failureCount", "failed_requests", "failures");
        target.totalTokens = optLongAny(usage, "total_tokens", "totalTokens", "tokens_total", "tokens");
        target.estimatedCost = optDoubleAny(usage, "estimated_cost", "estimatedCost", "total_cost", "cost");
        target.currency = usage.optString("currency", "");
        JSONObject apis = usage.optJSONObject("apis");
        if (apis != null) parseApis(target, apis);
        target.usageLoaded = true;
        target.recalculateFromEventsIfNeeded();
    }

    private void parsePricing(JSONObject root) throws JSONException {
        JSONArray pricing = root.optJSONArray("pricing");
        if (pricing == null) pricing = root.optJSONArray("prices");
        if (pricing == null) return;
        for (int i = 0; i < pricing.length(); i++) {
            JSONObject item = pricing.optJSONObject(i);
            if (item == null) continue;
            String model = item.optString("model", "").trim();
            if (model.length() == 0) continue;
            PriceRule rule = new PriceRule();
            rule.model = model;
            rule.inputPer1m = optDoubleAny(item, "prompt_price_per_1m", "input_price_per_1m", "inputPer1m", "promptPricePer1m");
            rule.outputPer1m = optDoubleAny(item, "completion_price_per_1m", "output_price_per_1m", "outputPer1m", "completionPricePer1m");
            rule.cachePer1m = optDoubleAny(item, "cache_price_per_1m", "cached_price_per_1m", "cachePer1m", "cachedPricePer1m");
            priceTable.put(model, rule);
        }
        saveLocalPrices();
    }

    private void applyCost(DataSnapshot snapshot) {
        if (snapshot == null) return;
        double total = 0d;
        for (ModelSummary model : snapshot.models.values()) {
            PriceRule rule = priceForModel(model.name);
            if (rule == null) continue;
            double cost = model.inputTokens / 1_000_000d * rule.inputPer1m
                    + model.outputTokens / 1_000_000d * rule.outputPer1m
                    + model.cachedTokens / 1_000_000d * rule.cachePer1m;
            model.estimatedCost = cost;
            total += cost;
        }
        snapshot.estimatedCost = total;
        snapshot.currency = "USD";
    }

    private PriceRule priceForModel(String modelName) {
        if (modelName == null) return null;
        PriceRule direct = priceTable.get(modelName);
        if (direct != null) return direct;
        PriceRule best = null;
        int bestLen = -1;
        for (Map.Entry<String, PriceRule> entry : priceTable.entrySet()) {
            String key = entry.getKey();
            if (key.length() > bestLen && (modelName.equals(key) || modelName.startsWith(key) || key.startsWith(modelName))) {
                best = entry.getValue();
                bestLen = key.length();
            }
        }
        return best;
    }

    private void loadLocalPrices() {
        priceTable.clear();
        String raw = prefs == null ? "" : prefs.getString(KEY_PRICE_TABLE, "");
        if (raw == null || raw.length() == 0) return;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) continue;
                PriceRule rule = new PriceRule();
                rule.model = obj.optString("model", "");
                rule.inputPer1m = obj.optDouble("input", 0d);
                rule.outputPer1m = obj.optDouble("output", 0d);
                rule.cachePer1m = obj.optDouble("cache", 0d);
                if (rule.model.length() > 0) priceTable.put(rule.model, rule);
            }
        } catch (Exception ignored) {
        }
    }

    private void saveLocalPrices() {
        if (prefs == null) return;
        JSONArray arr = new JSONArray();
        List<String> keys = new ArrayList<>(priceTable.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            PriceRule rule = priceTable.get(key);
            if (rule == null) continue;
            JSONObject obj = new JSONObject();
            try {
                obj.put("model", rule.model);
                obj.put("input", rule.inputPer1m);
                obj.put("output", rule.outputPer1m);
                obj.put("cache", rule.cachePer1m);
                arr.put(obj);
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_PRICE_TABLE, arr.toString()).apply();
    }

    private void parseApis(DataSnapshot target, JSONObject apis) throws JSONException {
        Iterator<String> apiKeys = apis.keys();
        while (apiKeys.hasNext()) {
            String apiId = apiKeys.next();
            JSONObject api = apis.optJSONObject(apiId); if (api == null) continue;
            CredentialSummary cred = target.credentials.get(apiId);
            if (cred == null) { cred = new CredentialSummary(); cred.id = apiId; target.credentials.put(apiId, cred); }
            cred.displayName = nonEmpty(api.optString("display_name", ""), apiId);
            JSONObject models = api.optJSONObject("models"); if (models == null) continue;
            Iterator<String> modelNames = models.keys();
            while (modelNames.hasNext()) {
                String modelName = modelNames.next();
                JSONObject obj = models.optJSONObject(modelName); if (obj == null) continue;
                ModelSummary summary = target.models.get(modelName); if (summary == null) { summary = new ModelSummary(modelName); target.models.put(modelName, summary); }
                JSONArray details = obj.optJSONArray("details");
                if (details == null) {
                    addAggregate(target, summary, cred, obj, "");
                    continue;
                }
                for (int i = 0; i < details.length(); i++) {
                    JSONObject detail = details.optJSONObject(i); if (detail == null) continue;
                    UsageEvent event = new UsageEvent();
                    event.timestamp = detail.optString("timestamp", "");
                    event.epochMs = parseIsoMillis(event.timestamp);
                    event.model = modelName;
                    event.credentialId = apiId;
                    event.credentialName = cred.displayName;
                    event.failed = detail.optBoolean("failed", false);
                    event.latencyMs = optLongAny(detail, "latency_ms", "latencyMs", "duration_ms");
                    JSONObject tokensObj = detail.optJSONObject("tokens");
                    if (tokensObj != null) {
                        event.inputTokens = optLongAny(tokensObj, "input_tokens", "inputTokens", "prompt_tokens");
                        event.outputTokens = optLongAny(tokensObj, "output_tokens", "outputTokens", "completion_tokens");
                        event.reasoningTokens = optLongAny(tokensObj, "reasoning_tokens", "reasoningTokens");
                        event.cachedTokens = optLongAny(tokensObj, "cached_tokens", "cachedTokens");
                        event.totalTokens = optLongAny(tokensObj, "total_tokens", "totalTokens");
                    }
                    if (event.totalTokens == 0) event.totalTokens = event.inputTokens + event.outputTokens + event.reasoningTokens;
                    target.events.add(event);
                }
            }
        }
    }

    private void addAggregate(DataSnapshot target, ModelSummary summary, CredentialSummary cred, JSONObject obj, String time) {
        long requests = optLongAny(obj, "total_requests", "totalRequests", "requests");
        long successes = optLongAny(obj, "success_count", "successCount", "successes");
        long failures = optLongAny(obj, "failure_count", "failureCount", "failures");
        long tokens = optLongAny(obj, "total_tokens", "totalTokens", "tokens");
        summary.totalRequests += requests; summary.successCount += successes; summary.failureCount += failures; summary.totalTokens += tokens;
        cred.totalRequests += requests; cred.successCount += successes; cred.failureCount += failures; cred.totalTokens += tokens;
    }

    private DataSnapshot applyRange(DataSnapshot source, int range) {
        if (range == RANGE_ALL || source.events.isEmpty()) return source.copySummary();
        long start = rangeStartMillis(range);
        long end = rangeEndExclusiveMillis(range);
        DataSnapshot out = source.emptyCopyMeta();
        for (UsageEvent e : source.events) {
            if (e.epochMs > 0 && e.epochMs < start) continue;
            if (end > 0 && e.epochMs > 0 && e.epochMs >= end) continue;
            out.addEvent(e);
        }
        out.finalizeComputed();
        return out;
    }

    private long rangeStartMillis(int range) {
        long now = System.currentTimeMillis();
        if (range == RANGE_4H) return now - 4L * 60L * 60L * 1000L;
        if (range == RANGE_8H) return now - 8L * 60L * 60L * 1000L;
        if (range == RANGE_24H) return now - 24L * 60L * 60L * 1000L;
        if (range == RANGE_7D) return now - 7L * 24L * 60L * 60L * 1000L;
        if (range == RANGE_30D) return now - 30L * 24L * 60L * 60L * 1000L;
        if (range == RANGE_CUSTOM) {
            ensureCustomDates();
            return parseLocalDateStartMillis(customStartDate);
        }
        return 0L;
    }

    private long rangeEndExclusiveMillis(int range) {
        if (range != RANGE_CUSTOM) return 0L;
        ensureCustomDates();
        long end = parseLocalDateStartMillis(customEndDate);
        if (end <= 0) return 0L;
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(end);
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        return calendar.getTimeInMillis();
    }

    private String get(String base, String path, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        String full = normalizeBaseUrl(base) + path;
        HttpURLConnection conn = (HttpURLConnection) new URL(full).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "CPAUsageAndroid/0.2");
        if (loginPassword != null && loginPassword.trim().length() > 0) conn.setRequestProperty("Authorization", "Bearer " + loginPassword.trim());
        int code = conn.getResponseCode();
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String body = readAll(stream);
        conn.disconnect();
        if (code < 200 || code >= 300) throw new IOException("HTTP " + code + " " + cleanText(body, 160));
        return body;
    }

    private String getWithPassword(String base, String path, String password, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        String full = normalizeBaseUrl(base) + path;
        HttpURLConnection conn = (HttpURLConnection) new URL(full).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "CPAUsageAndroid/0.3");
        if (password != null && password.trim().length() > 0) conn.setRequestProperty("Authorization", "Bearer " + password.trim());
        int code = conn.getResponseCode();
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String body = readAll(stream);
        conn.disconnect();
        if (code < 200 || code >= 300) throw new IOException("HTTP " + code + " " + cleanText(body, 160));
        return body;
    }

    private String request(String method, String fullUrl, String body, Map<String, String> headers, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "CPAUsageAndroid/0.3");
        if (headers != null) for (Map.Entry<String, String> entry : headers.entrySet()) conn.setRequestProperty(entry.getKey(), entry.getValue());
        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes("UTF-8"));
            out.close();
        }
        int code = conn.getResponseCode();
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String response = readAll(stream);
        conn.disconnect();
        if (code < 200 || code >= 300) throw new IOException("HTTP " + code + " " + cleanText(response, 180));
        return response;
    }

    private Map<String, String> managementHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + managementKey.trim());
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private String managementApiBase() {
        String value = quotaUrl == null || quotaUrl.length() == 0 ? DEFAULT_QUOTA_URL : quotaUrl;
        int index = value.indexOf("/management.html");
        if (index >= 0) value = value.substring(0, index);
        int hash = value.indexOf('#');
        if (hash >= 0) value = value.substring(0, hash);
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value + "/v0/management";
    }

    private List<String> managementApiBaseCandidates() {
        List<String> out = new ArrayList<>();
        addManagementCandidate(out, managementApiBaseFromUrl(quotaUrl));
        addManagementCandidate(out, managementApiBaseFromUrl(correctedQuotaUrl(quotaUrl)));
        addManagementCandidate(out, managementApiBaseFromUrl(DEFAULT_QUOTA_URL));
        return out;
    }

    private void addManagementCandidate(List<String> out, String value) {
        if (value == null || value.length() == 0 || out.contains(value)) return;
        out.add(value);
    }

    private String managementApiBaseFromUrl(String raw) {
        String value = normalizeQuotaUrl(raw);
        int index = value.indexOf("/management.html");
        if (index >= 0) value = value.substring(0, index);
        int hash = value.indexOf('#');
        if (hash >= 0) value = value.substring(0, hash);
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value + "/v0/management";
    }

    private String correctedQuotaUrl(String raw) {
        String value = normalizeQuotaUrl(raw);
        return value;
    }

    private JSONArray extractArray(String body) throws JSONException {
        Object value = new JSONTokener(body).nextValue();
        if (value instanceof JSONArray) return (JSONArray) value;
        if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            JSONArray arr = obj.optJSONArray("files");
            if (arr != null) return arr;
            arr = obj.optJSONArray("data");
            if (arr != null) return arr;
            arr = obj.optJSONArray("authFiles");
            if (arr != null) return arr;
        }
        return new JSONArray();
    }

    private String detectAuthType(JSONObject file, String name) {
        String raw = (file.optString("type", "") + " " + file.optString("provider", "") + " " + file.optString("channel", "") + " " + name).toLowerCase(Locale.US);
        if (raw.contains("codex") || raw.contains("chatgpt") || raw.contains("openai")) return "codex";
        if (raw.contains("claude") || raw.contains("anthropic")) return "claude";
        if (raw.contains("kimi")) return "kimi";
        if (raw.contains("xai") || raw.contains("grok")) return "xai";
        if (raw.contains("antigravity")) return "antigravity";
        return raw.trim();
    }

    private boolean isQuotaAccountEnabled(JSONObject file) {
        Boolean disabled = optBooleanField(file, "disabled", "is_disabled", "isDisabled", "paused", "is_paused", "isPaused");
        if (disabled != null && disabled) return false;
        Boolean deleted = optBooleanField(file, "deleted", "is_deleted", "isDeleted", "removed", "is_removed", "isRemoved");
        if (deleted != null && deleted) return false;

        Boolean enabled = optBooleanField(file, "enabled", "is_enabled", "isEnabled", "active", "is_active", "isActive");
        if (enabled != null) return enabled;

        JSONObject metadata = optObjectAny(file, "metadata", "meta", "attributes");
        if (metadata != null) {
            disabled = optBooleanField(metadata, "disabled", "is_disabled", "isDisabled", "paused", "is_paused", "isPaused");
            if (disabled != null && disabled) return false;
            enabled = optBooleanField(metadata, "enabled", "is_enabled", "isEnabled", "active", "is_active", "isActive");
            if (enabled != null) return enabled;
        }

        String status = firstNonEmpty(
                file.optString("status", ""),
                file.optString("state", ""),
                file.optString("enabled_status", ""),
                metadata == null ? "" : metadata.optString("status", ""),
                metadata == null ? "" : metadata.optString("state", "")
        ).toLowerCase(Locale.US).trim();
        if (status.length() == 0) return true;
        if ("disabled".equals(status) || "inactive".equals(status) || "off".equals(status) || "paused".equals(status) || "stopped".equals(status) || "deleted".equals(status) || "removed".equals(status)) return false;
        return true;
    }

    private Boolean optBooleanField(JSONObject obj, String... keys) {
        if (obj == null || keys == null) return null;
        for (String key : keys) {
            if (!obj.has(key) || obj.isNull(key)) continue;
            Object value = obj.opt(key);
            if (value instanceof Boolean) return (Boolean) value;
            if (value instanceof Number) return ((Number) value).doubleValue() != 0d;
            String raw = String.valueOf(value).trim().toLowerCase(Locale.US);
            if (raw.length() == 0) continue;
            if ("true".equals(raw) || "1".equals(raw) || "yes".equals(raw) || "y".equals(raw) || "on".equals(raw) || "enabled".equals(raw) || "active".equals(raw)) return true;
            if ("false".equals(raw) || "0".equals(raw) || "no".equals(raw) || "n".equals(raw) || "off".equals(raw) || "disabled".equals(raw) || "inactive".equals(raw) || "paused".equals(raw) || "stopped".equals(raw)) return false;
        }
        return null;
    }

    private CodexQuotaAccount quotaAccountFromFile(JSONObject file) {
        CodexQuotaAccount account = new CodexQuotaAccount();
        account.rawFile = file;
        account.authIndex = firstNonEmpty(file.optString("auth_index", ""), file.optString("authIndex", ""));
        account.name = firstNonEmpty(
                file.optString("name", ""),
                file.optString("display_name", ""),
                file.optString("displayName", ""),
                file.optString("filename", ""),
                file.optString("file", ""),
                file.optString("path", ""),
                account.authIndex.length() > 0 ? "Codex " + account.authIndex : "Codex 账号"
        );
        account.planType = firstNonEmpty(extractPlanType(file), file.optString("plan", ""));
        account.subscriptionActiveUntil = extractSubscriptionActiveUntil(file);
        account.name = safeQuotaAccountName(file, account.authIndex);
        return account;
    }

    private void refreshCodexQuota(String authIndex) {
        CodexQuotaAccount account = findQuotaAccount(authIndex);
        if (account == null || quotaLoading) return;
        if (managementKey == null || managementKey.trim().length() == 0) {
            account.error = "请输入管理 Key 后再刷新。";
            render();
            return;
        }
        account.loading = true;
        account.error = "";
        account.lastActionMessage = "";
        quotaStatus = "正在刷新单账号";
        render();
        executor.execute(() -> {
            boolean ok = false;
            try {
                ok = refreshCodexQuotaInWorker(managementApiBase(), account);
            } catch (Exception e) {
                account.error = cleanError(e);
            }
            boolean finalOk = ok;
            mainHandler.post(() -> {
                account.loading = false;
                quotaStatus = finalOk ? "单账号刷新完成" : "单账号刷新失败";
                quotaDetail = account.name + (finalOk ? " 刷新成功" : " 刷新失败");
                render();
            });
        });
    }

    private void resetCodexQuota(String authIndex) {
        CodexQuotaAccount account = findQuotaAccount(authIndex);
        if (account == null || quotaLoading) return;
        if (managementKey == null || managementKey.trim().length() == 0) {
            account.error = "请输入管理 Key 后再重置。";
            render();
            return;
        }
        account.resetting = true;
        account.error = "";
        account.lastActionMessage = "正在提交额度重置...";
        quotaStatus = "正在重置单账号";
        render();
        executor.execute(() -> {
            boolean ok = false;
            try {
                apiCall(managementApiBase(), account.authIndex, "POST", "https://chatgpt.com/backend-api/wham/rate-limit-reset-credits/consume", codexHeaders(account.rawFile, false), new JSONObject().put("redeem_request_id", UUID.randomUUID().toString()).toString(), 15000, 45000);
                account.lastActionMessage = "重置请求已提交，已重新读取当前额度。";
                ok = refreshCodexQuotaInWorker(managementApiBase(), account);
            } catch (Exception e) {
                account.error = cleanError(e);
                account.lastActionMessage = "";
            }
            boolean finalOk = ok;
            mainHandler.post(() -> {
                account.resetting = false;
                quotaStatus = finalOk ? "单账号重置完成" : "单账号重置失败";
                quotaDetail = account.name + (finalOk ? " 重置后刷新成功" : " 重置失败");
                render();
            });
        });
    }

    private CodexQuotaAccount findQuotaAccount(String authIndex) {
        if (authIndex == null) return null;
        for (CodexQuotaAccount account : codexQuotaAccounts) if (authIndex.equals(account.authIndex)) return account;
        return null;
    }

    private boolean refreshCodexQuotaInWorker(String apiBase, CodexQuotaAccount account) {
        try {
            account.loading = true;
            account.error = "";
            account.rawSummary = "";
            ApiCallResponse usage = apiCall(apiBase, account.authIndex, "GET", "https://chatgpt.com/backend-api/wham/usage", codexHeaders(account.rawFile, false), null, 15000, 45000);
            parseCodexUsage(account, usage.body);
            try {
                ApiCallResponse reset = apiCall(apiBase, account.authIndex, "GET", "https://chatgpt.com/backend-api/wham/rate-limit-reset-credits", codexHeaders(account.rawFile, true), null, 10000, 25000);
                parseResetCredits(account, reset.body, "");
            } catch (Exception resetError) {
                parseResetCredits(account, null, cleanError(resetError));
            }
            return account.error.length() == 0;
        } catch (Exception e) {
            account.error = cleanError(e);
            account.windows.clear();
            return false;
        } finally {
            account.loading = false;
        }
    }

    private ApiCallResponse apiCall(String apiBase, String authIndex, String method, String url, JSONObject headers, String data, int connectTimeoutMs, int readTimeoutMs) throws IOException, JSONException {
        JSONObject payload = new JSONObject();
        payload.put("authIndex", authIndex);
        payload.put("method", method);
        payload.put("url", url);
        payload.put("header", headers);
        if (data != null) payload.put("data", data);
        String body = request("POST", apiBase + "/api-call", payload.toString(), managementHeaders(), connectTimeoutMs, readTimeoutMs);
        JSONObject response = new JSONObject(body);
        int statusCode = response.optInt("statusCode", response.optInt("status_code", response.optInt("status", 0)));
        String responseBody = response.optString("body", response.optString("bodyText", ""));
        if (responseBody.length() == 0) responseBody = body;
        if (statusCode < 200 || statusCode >= 300) throw new IOException("HTTP " + statusCode + " " + cleanText(responseBody, 180));
        return new ApiCallResponse(statusCode, responseBody);
    }

    private JSONObject codexHeaders(JSONObject file, boolean resetCreditsList) throws JSONException {
        JSONObject header = new JSONObject();
        header.put("Authorization", "Bearer $TOKEN$");
        header.put("Content-Type", "application/json");
        header.put("User-Agent", "codex_cli_rs/0.76.0 (Debian 13.0.0; x86_64) WindowsTerminal");
        if (resetCreditsList) {
            header.put("Accept", "application/json");
            header.put("OpenAI-Beta", "codex-1");
            header.put("Originator", "Codex Desktop");
        }
        String accountId = extractChatgptAccountId(file);
        if (accountId.length() > 0) header.put("Chatgpt-Account-Id", accountId);
        return header;
    }

    private void parseCodexUsage(CodexQuotaAccount account, String body) throws JSONException {
        JSONObject obj = asObject(body);
        if (obj == null) {
            account.rawSummary = "Quota API returned a non-JSON response";
            account.error = "额度接口返回空数据";
            return;
        }
        account.rawSummary = "Quota response loaded without parsed windows";
        account.planType = firstNonEmpty(
                obj.optString("plan_type", ""),
                obj.optString("planType", ""),
                account.planType
        );
        account.subscriptionActiveUntil = firstNonEmpty(extractSubscriptionActiveUntil(obj), account.subscriptionActiveUntil);
        JSONObject embeddedReset = optObjectAny(obj, "rate_limit_reset_credits", "rateLimitResetCredits");
        if (embeddedReset != null) parseResetCredits(account, embeddedReset.toString(), "");
        account.windows.clear();
        JSONObject rate = optObjectAny(obj, "rate_limit", "rateLimit");
        addCodexRateWindows(account.windows, rate, false, "");
        JSONObject review = optObjectAny(obj, "code_review_rate_limit", "codeReviewRateLimit");
        addCodexRateWindows(account.windows, review, true, "Code Review ");
        JSONArray additional = optArrayAny(obj, "additional_rate_limits", "additionalRateLimits");
        if (additional != null) {
            for (int i = 0; i < additional.length(); i++) {
                JSONObject item = additional.optJSONObject(i);
                if (item == null) continue;
                String name = firstNonEmpty(item.optString("limit_name", ""), item.optString("limitName", ""), item.optString("metered_feature", ""), item.optString("meteredFeature", ""), "附加额度 " + (i + 1));
                addCodexRateWindows(account.windows, optObjectAny(item, "rate_limit", "rateLimit"), false, name + " ");
            }
        }
        if (account.windows.isEmpty()) account.rawSummary = summarizeQuotaJson(body);
    }

    private void addCodexRateWindows(List<QuotaWindow> out, JSONObject rate, boolean codeReview, String prefix) {
        if (rate == null) return;
        JSONObject primary = optObjectAny(rate, "primary_window", "primaryWindow");
        JSONObject secondary = optObjectAny(rate, "secondary_window", "secondaryWindow");
        boolean limitReached = optBooleanAny(rate, "limit_reached", "limitReached");
        Boolean allowed = optNullableBooleanAny(rate, "allowed");
        if (primary != null) out.add(parseQuotaWindow(primary, codeReview ? "Code Review 5 小时限额" : nonEmpty(prefix, "") + "5 小时限额", "five-hour", limitReached, allowed));
        if (secondary != null) {
            long seconds = optLongAny(secondary, "limit_window_seconds", "limitWindowSeconds");
            String label = seconds >= 2419200L ? "月限额" : "周限额";
            if (codeReview) label = "Code Review " + label;
            else label = nonEmpty(prefix, "") + label;
            out.add(parseQuotaWindow(secondary, label, seconds >= 2419200L ? "monthly" : "weekly", limitReached, allowed));
        }
    }

    private QuotaWindow parseQuotaWindow(JSONObject obj, String label, String id, boolean limitReached, Boolean allowed) {
        QuotaWindow window = new QuotaWindow();
        window.id = id;
        window.label = label;
        Double used = optNullableDoubleAny(obj, "used_percent", "usedPercent", "utilization");
        if (used == null && (limitReached || (allowed != null && !allowed))) used = 100d;
        if (used != null && used > 0d && used <= 1d) used = used * 100d;
        window.usedPercent = used == null ? -1d : Math.max(0d, Math.min(100d, used));
        window.remainingPercent = window.usedPercent < 0 ? -1d : Math.max(0d, Math.min(100d, 100d - window.usedPercent));
        window.resetLabel = resetLabel(obj);
        return window;
    }

    private void parseResetCredits(CodexQuotaAccount account, String body, String error) throws JSONException {
        account.resetCreditsError = error == null ? "" : error;
        if (body == null || body.length() == 0) return;
        JSONObject obj = asObject(body);
        if (obj == null) {
            account.resetCreditsError = "重置次数接口返回格式无法解析";
            return;
        }
        long count = optLongAny(obj, "available_count", "availableCount");
        JSONArray credits = obj.optJSONArray("credits");
        List<String> expiries = new ArrayList<>();
        if (credits != null) {
            for (int i = 0; i < credits.length(); i++) {
                JSONObject credit = credits.optJSONObject(i);
                if (credit == null) continue;
                String type = firstNonEmpty(credit.optString("reset_type", ""), credit.optString("resetType", ""));
                String status = credit.optString("status", "");
                String expires = firstNonEmpty(credit.optString("expires_at", ""), credit.optString("expiresAt", ""));
                if ((type.length() == 0 || "codex_rate_limits".equals(type)) && (status.length() == 0 || "available".equals(status))) {
                    if (expires.length() > 0) expiries.add(formatFlexibleTime(expires));
                }
            }
            if (count == 0 && credits.length() > 0) count = expiries.size();
        }
        account.resetCreditsAvailableCount = count;
        account.resetCreditsKnown = true;
        account.resetCreditExpiries.clear();
        account.resetCreditExpiries.addAll(expiries);
    }

    private JSONObject asObject(String body) throws JSONException {
        if (body == null || body.trim().length() == 0) return null;
        Object value = new JSONTokener(body).nextValue();
        if (value instanceof JSONObject) return (JSONObject) value;
        return null;
    }

    private JSONObject optObjectAny(JSONObject obj, String... keys) {
        if (obj == null) return null;
        for (String key : keys) {
            Object value = obj.opt(key);
            if (value instanceof JSONObject) return (JSONObject) value;
            if (value instanceof String) {
                String raw = ((String) value).trim();
                if (raw.startsWith("{") && raw.endsWith("}")) {
                    try { return new JSONObject(raw); } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }

    private JSONArray optArrayAny(JSONObject obj, String... keys) {
        if (obj == null) return null;
        for (String key : keys) {
            JSONArray out = obj.optJSONArray(key);
            if (out != null) return out;
        }
        return null;
    }

    private boolean optBooleanAny(JSONObject obj, String... keys) {
        Boolean value = optNullableBooleanAny(obj, keys);
        return value != null && value;
    }

    private Boolean optNullableBooleanAny(JSONObject obj, String... keys) {
        if (obj == null) return null;
        for (String key : keys) {
            if (!obj.has(key) || obj.isNull(key)) continue;
            Object value = obj.opt(key);
            if (value instanceof Boolean) return (Boolean) value;
            String raw = String.valueOf(value).trim().toLowerCase(Locale.US);
            if ("true".equals(raw) || "1".equals(raw) || "yes".equals(raw)) return true;
            if ("false".equals(raw) || "0".equals(raw) || "no".equals(raw)) return false;
        }
        return null;
    }

    private String resetLabel(JSONObject obj) {
        String resetAt = firstNonEmpty(obj.optString("reset_at", ""), obj.optString("resetAt", ""), obj.optString("reset_time", ""), obj.optString("resetTime", ""));
        String formatted = formatFlexibleTime(resetAt);
        if (formatted.length() > 0) return formatted;
        long resetAfter = optLongAny(obj, "reset_after_seconds", "resetAfterSeconds", "reset_in", "resetIn", "ttl");
        if (resetAfter > 0) return "约 " + durationLabel(resetAfter) + " 后";
        return "-";
    }

    private String durationLabel(long seconds) {
        long minutes = Math.max(1L, (long) Math.ceil(seconds / 60d));
        long days = minutes / 1440L;
        long hours = (minutes % 1440L) / 60L;
        long mins = minutes % 60L;
        if (days > 0) return days + "天" + (hours > 0 ? hours + "小时" : "");
        if (hours > 0) return hours + "小时" + (mins > 0 ? mins + "分钟" : "");
        return mins + "分钟";
    }

    private String formatFlexibleTime(String value) {
        if (value == null || value.trim().length() == 0) return "";
        String raw = value.trim();
        long millis = parseFlexibleTimeMillis(raw);
        if (millis > 0) return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(millis));
        try {
            String normalized = raw.replace('T', ' ');
            if (normalized.matches("^\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}.*")) return normalized;
            if (normalized.length() >= 16) return normalized.substring(5, 16);
        } catch (Exception ignored) {}
        return raw;
    }

    private String formatSubscriptionTime(String value) {
        if (value == null || value.trim().length() == 0) return "";
        String raw = value.trim();
        long millis = parseFlexibleTimeMillis(raw);
        if (millis > 0) return new SimpleDateFormat("yyyy/M/d HH:mm:ss", Locale.CHINA).format(new Date(millis));
        String normalized = raw.replace('T', ' ').trim();
        if (normalized.matches("^\\d{4}-\\d{1,2}-\\d{1,2}.*")) normalized = normalized.replace('-', '/');
        return normalized;
    }

    private long parseFlexibleTimeMillis(String value) {
        if (value == null || value.trim().length() == 0) return 0L;
        String raw = value.trim();
        try {
            double numeric = Double.parseDouble(raw);
            long millis = numeric > 10000000000d ? (long) numeric : (long) (numeric * 1000d);
            if (millis > 0) return millis;
        } catch (Exception ignored) {}
        long iso = parseIsoMillis(raw);
        if (iso > 0) return iso;
        String[] patterns = new String[]{
                "yyyy/M/d HH:mm:ss",
                "yyyy/M/d HH:mm",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy/MM/dd HH:mm",
                "yyyy-M-d HH:mm:ss",
                "yyyy-M-d HH:mm",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.CHINA);
                fmt.setLenient(false);
                Date date = fmt.parse(raw);
                if (date != null) return date.getTime();
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    private String displayPlan(String plan) {
        String raw = plan == null ? "" : plan.trim();
        String lower = raw.toLowerCase(Locale.US).replace('_', '-');
        if ("plus".equals(lower)) return "Plus";
        if ("team".equals(lower)) return "Team";
        if ("free".equals(lower)) return "Free";
        if ("pro".equals(lower)) return "Pro";
        if ("prolite".equals(lower) || "pro-lite".equals(lower)) return "Pro Lite";
        return raw;
    }

    private String joinExpiries(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size() && i < 3; i++) {
            if (i > 0) sb.append("，");
            sb.append(values.get(i));
        }
        if (values.size() > 3) sb.append(" 等 ").append(values.size()).append(" 个");
        return sb.toString();
    }

    private String extractPlanType(JSONObject file) {
        String direct = firstNonEmpty(file.optString("plan_type", ""), file.optString("planType", ""));
        if (direct.length() > 0) return direct;
        String fromNested = firstNonEmpty(
                nestedString(file, "metadata", "plan_type"),
                nestedString(file, "metadata", "planType"),
                nestedString(file, "attributes", "plan_type"),
                nestedString(file, "attributes", "planType")
        );
        if (fromNested.length() > 0) return fromNested;
        JSONObject token = jwtPayload(firstNonEmpty(file.optString("id_token", ""), nestedString(file, "metadata", "id_token"), nestedString(file, "attributes", "id_token")));
        if (token != null) return firstNonEmpty(token.optString("plan_type", ""), token.optString("planType", ""));
        return "";
    }

    private String extractSubscriptionActiveUntil(JSONObject file) {
        String recursive = findSubscriptionTimeValue(file, 0);
        if (recursive.length() > 0) return recursive;
        JSONObject metadata = optObjectAny(file, "metadata", "meta");
        JSONObject attributes = optObjectAny(file, "attributes");
        JSONObject subscription = optObjectAny(file, "subscription", "billing", "plan", "account");
        JSONObject metadataSubscription = optObjectAny(metadata, "subscription", "billing", "plan", "account");
        JSONObject attributesSubscription = optObjectAny(attributes, "subscription", "billing", "plan", "account");
        String direct = firstNonEmpty(
                file.optString("chatgpt_subscription_active_until", ""),
                file.optString("chatgptSubscriptionActiveUntil", ""),
                file.optString("subscription_active_until", ""),
                file.optString("subscriptionActiveUntil", ""),
                file.optString("active_until", ""),
                file.optString("activeUntil", ""),
                file.optString("billing_period_end", ""),
                file.optString("billingPeriodEnd", ""),
                file.optString("renewal", ""),
                file.optString("renewal_at", ""),
                file.optString("renewalAt", ""),
                file.optString("renew_at", ""),
                file.optString("renewAt", ""),
                file.optString("expires_at", ""),
                file.optString("expiresAt", ""),
                file.optString("expires", ""),
                file.optString("expiry", ""),
                subscriptionTimeValue(subscription),
                subscriptionTimeValue(metadata),
                subscriptionTimeValue(metadataSubscription),
                subscriptionTimeValue(attributes),
                subscriptionTimeValue(attributesSubscription)
        );
        if (direct.length() > 0) return direct;
        JSONObject token = jwtPayload(firstNonEmpty(
                file.optString("id_token", ""),
                metadata == null ? "" : metadata.optString("id_token", ""),
                metadata == null ? "" : metadata.optString("idToken", ""),
                attributes == null ? "" : attributes.optString("id_token", ""),
                attributes == null ? "" : attributes.optString("idToken", "")
        ));
        if (token != null) return findSubscriptionTimeValue(token, 0);
        return "";
    }

    private String subscriptionTimeValue(JSONObject obj) {
        if (obj == null) return "";
        return firstNonEmpty(
                obj.optString("chatgpt_subscription_active_until", ""),
                obj.optString("chatgptSubscriptionActiveUntil", ""),
                obj.optString("subscription_active_until", ""),
                obj.optString("subscriptionActiveUntil", ""),
                obj.optString("active_until", ""),
                obj.optString("activeUntil", ""),
                obj.optString("billing_period_end", ""),
                obj.optString("billingPeriodEnd", ""),
                obj.optString("renewal", ""),
                obj.optString("renewal_at", ""),
                obj.optString("renewalAt", ""),
                obj.optString("renew_at", ""),
                obj.optString("renewAt", ""),
                obj.optString("renewal_date", ""),
                obj.optString("renewalDate", ""),
                obj.optString("expires_at", ""),
                obj.optString("expiresAt", ""),
                obj.optString("expires_label", ""),
                obj.optString("expiresLabel", ""),
                obj.optString("expires", ""),
                obj.optString("expiry", ""),
                obj.optString("current_period_end", ""),
                obj.optString("currentPeriodEnd", ""),
                obj.optString("period_end", ""),
                obj.optString("periodEnd", ""),
                obj.optString("next_billing_at", ""),
                obj.optString("nextBillingAt", ""),
                obj.optString("paid_until", ""),
                obj.optString("paidUntil", ""),
                obj.optString("valid_until", ""),
                obj.optString("validUntil", "")
        );
    }

    private String findSubscriptionTimeValue(Object value, int depth) {
        if (value == null || JSONObject.NULL.equals(value) || depth > 5) return "";
        if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            String direct = subscriptionTimeValue(obj);
            if (direct.length() > 0) return direct;
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String lower = key == null ? "" : key.toLowerCase(Locale.US);
                Object child = obj.opt(key);
                boolean likely = lower.contains("subscription") || lower.contains("billing") || lower.contains("renew") || lower.contains("plan") || lower.contains("account") || lower.contains("metadata") || lower.contains("attribute") || lower.contains("token") || lower.contains("payload") || lower.contains("profile") || lower.contains("organization");
                if (!likely) continue;
                String found = findSubscriptionTimeValue(child, depth + 1);
                if (found.length() > 0) return found;
            }
        } else if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            for (int i = 0; i < arr.length(); i++) {
                String found = findSubscriptionTimeValue(arr.opt(i), depth + 1);
                if (found.length() > 0) return found;
            }
        } else if (value instanceof String) {
            String raw = ((String) value).trim();
            JSONObject token = jwtPayload(raw);
            if (token != null) {
                String found = findSubscriptionTimeValue(token, depth + 1);
                if (found.length() > 0) return found;
            }
            if (raw.startsWith("{") && raw.endsWith("}")) {
                try { return findSubscriptionTimeValue(new JSONObject(raw), depth + 1); } catch (Exception ignored) {}
            } else if (raw.startsWith("[") && raw.endsWith("]")) {
                try { return findSubscriptionTimeValue(new JSONArray(raw), depth + 1); } catch (Exception ignored) {}
            }
        }
        return "";
    }

    private String extractChatgptAccountId(JSONObject file) {
        String direct = firstNonEmpty(
                file.optString("chatgpt_account_id", ""),
                file.optString("chatgptAccountId", ""),
                nestedString(file, "metadata", "chatgpt_account_id"),
                nestedString(file, "metadata", "chatgptAccountId"),
                nestedString(file, "attributes", "chatgpt_account_id"),
                nestedString(file, "attributes", "chatgptAccountId")
        );
        if (direct.length() > 0) return direct;
        JSONObject token = jwtPayload(firstNonEmpty(file.optString("id_token", ""), nestedString(file, "metadata", "id_token"), nestedString(file, "attributes", "id_token")));
        if (token == null) return "";
        JSONObject auth = token.optJSONObject("https://api.openai.com/auth");
        if (auth != null) return firstNonEmpty(auth.optString("chatgpt_account_id", ""), auth.optString("chatgptAccountId", ""));
        return firstNonEmpty(token.optString("chatgpt_account_id", ""), token.optString("chatgptAccountId", ""));
    }

    private String nestedString(JSONObject obj, String objectKey, String valueKey) {
        JSONObject nested = obj == null ? null : obj.optJSONObject(objectKey);
        if (nested == null) return "";
        Object value = nested.opt(valueKey);
        return value == null || JSONObject.NULL.equals(value) ? "" : String.valueOf(value).trim();
    }

    private JSONObject jwtPayload(String token) {
        if (token == null || token.trim().length() == 0) return null;
        try {
            String[] parts = token.trim().split("\\.");
            if (parts.length < 2) return null;
            byte[] bytes = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return new JSONObject(new String(bytes, "UTF-8"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String summarizeQuotaJson(String body) {
        try {
            JSONObject obj = new JSONObject(body);
            String plan = firstNonEmpty(obj.optString("plan_type", ""), obj.optString("planType", ""), obj.optString("plan", ""));
            JSONArray windows = obj.optJSONArray("windows");
            if (windows == null) windows = obj.optJSONArray("usage_windows");
            StringBuilder sb = new StringBuilder();
            if (plan.length() > 0) sb.append("Plan ").append(plan).append("；");
            if (windows != null && windows.length() > 0) {
                for (int i = 0; i < Math.min(3, windows.length()); i++) {
                    JSONObject w = windows.optJSONObject(i);
                    if (w == null) continue;
                    long used = optLongAny(w, "used", "usage", "used_tokens", "usedTokens");
                    long limit = optLongAny(w, "limit", "quota", "limit_tokens", "limitTokens");
                    String name = firstNonEmpty(w.optString("name", ""), w.optString("window", ""), w.optString("type", "窗口"));
                    sb.append(name).append(" ");
                    if (limit > 0) sb.append(formatCount(used)).append("/").append(formatCount(limit));
                    else sb.append(cleanText(w.toString(), 60));
                    if (i < windows.length() - 1) sb.append("；");
                }
            }
            if (sb.length() == 0) return cleanText(body, 180);
            return sb.toString();
        } catch (Exception e) {
            return cleanText(body, 180);
        }
    }

    private List<TrendPoint> healthTrendPoints(DataSnapshot snapshot) {
        Map<Long, long[]> buckets = new HashMap<>();
        long[] bounds = trendBounds(snapshot);
        long bucketMs = trendBucketMillis(bounds[0], bounds[1]);
        for (UsageEvent e : snapshot.events) {
            if (e.epochMs <= 0) continue;
            long key = bucketKey(e.epochMs, bounds[0], bucketMs);
            long[] counts = buckets.get(key);
            if (counts == null) { counts = new long[2]; buckets.put(key, counts); }
            counts[0]++;
            if (!e.failed) counts[1]++;
        }
        List<Long> keys = new ArrayList<>(buckets.keySet());
        Collections.sort(keys);
        List<TrendPoint> out = new ArrayList<>();
        for (Long key : keys) {
            long[] counts = buckets.get(key);
            if (counts == null || counts[0] <= 0) continue;
            double successRate = counts[1] * 100d / counts[0];
            out.add(new TrendPoint(bucketLabel(key, bucketMs), successRate, counts[0]));
        }
        return out;
    }

    private List<HealthCell> healthCells(DataSnapshot snapshot) {
        long[] bounds = trendBounds(snapshot);
        long start = bounds[0];
        long end = bounds[1];
        int cellCount = 168;
        long bucketMs = Math.max(60_000L, (long) Math.ceil((end - start) / (double) cellCount));
        Map<Integer, long[]> counts = new HashMap<>();
        for (UsageEvent e : snapshot.events) {
            if (e.epochMs <= 0 || e.epochMs < start || e.epochMs >= end) continue;
            int index = (int) Math.min(cellCount - 1, Math.max(0, (e.epochMs - start) / bucketMs));
            long[] item = counts.get(index);
            if (item == null) { item = new long[2]; counts.put(index, item); }
            item[0]++;
            if (e.failed) item[1]++;
        }
        List<HealthCell> out = new ArrayList<>();
        for (int i = 0; i < cellCount; i++) {
            long cellStart = start + i * bucketMs;
            long cellEnd = Math.min(end, cellStart + bucketMs);
            long[] item = counts.get(i);
            long total = item == null ? 0 : item[0];
            long failed = item == null ? 0 : item[1];
            out.add(new HealthCell(cellStart, cellEnd, total, failed));
        }
        return out;
    }

    private List<TrendPoint> costTrendPoints(DataSnapshot snapshot) {
        Map<Long, Double> buckets = new HashMap<>();
        long[] bounds = trendBounds(snapshot);
        long bucketMs = trendBucketMillis(bounds[0], bounds[1]);
        for (UsageEvent e : snapshot.events) {
            if (e.epochMs <= 0) continue;
            long key = bucketKey(e.epochMs, bounds[0], bucketMs);
            Double old = buckets.get(key);
            buckets.put(key, (old == null ? 0d : old) + eventCost(e));
        }
        return trendPointsFromBuckets(buckets, bucketMs);
    }

    private List<TrendPoint> tokenTrendPoints(DataSnapshot snapshot, String model) {
        Map<Long, Double> buckets = new HashMap<>();
        long[] bounds = trendBounds(snapshot);
        long bucketMs = trendBucketMillis(bounds[0], bounds[1]);
        for (UsageEvent e : snapshot.events) {
            if (e.epochMs <= 0) continue;
            if (model != null && model.length() > 0 && !model.equals(e.model)) continue;
            long key = bucketKey(e.epochMs, bounds[0], bucketMs);
            Double old = buckets.get(key);
            buckets.put(key, (old == null ? 0d : old) + e.totalTokens);
        }
        return trendPointsFromBuckets(buckets, bucketMs);
    }

    private List<TrendPoint> trendPointsFromBuckets(Map<Long, Double> buckets, long bucketMs) {
        List<Long> keys = new ArrayList<>(buckets.keySet());
        Collections.sort(keys);
        List<TrendPoint> out = new ArrayList<>();
        for (Long key : keys) {
            Double value = buckets.get(key);
            if (value == null) continue;
            out.add(new TrendPoint(bucketLabel(key, bucketMs), value, 0));
        }
        return out;
    }

    private long[] trendBounds(DataSnapshot snapshot) {
        long now = System.currentTimeMillis();
        long start;
        long end;
        if (selectedRange == RANGE_ALL) {
            start = Long.MAX_VALUE;
            end = 0L;
            for (UsageEvent e : snapshot.events) {
                if (e.epochMs <= 0) continue;
                start = Math.min(start, e.epochMs);
                end = Math.max(end, e.epochMs);
            }
            if (start == Long.MAX_VALUE || end <= 0L) {
                start = now - 24L * 60L * 60L * 1000L;
                end = now;
            } else {
                end += 1L;
            }
        } else {
            start = rangeStartMillis(selectedRange);
            end = rangeEndExclusiveMillis(selectedRange);
            if (end <= 0L) end = now;
        }
        if (start <= 0L || start >= end) start = end - 60L * 60L * 1000L;
        return new long[]{start, end};
    }

    private long trendBucketMillis(long start, long end) {
        long span = Math.max(1L, end - start);
        long hour = 60L * 60L * 1000L;
        long day = 24L * hour;
        if (span <= 6L * hour) return 30L * 60L * 1000L;
        if (span <= 12L * hour) return hour;
        if (span <= 36L * hour) return 2L * hour;
        if (span <= 8L * day) return day;
        if (span <= 45L * day) return day;
        long roughly = span / 30L;
        long days = Math.max(1L, (long) Math.ceil(roughly / (double) day));
        return days * day;
    }

    private long bucketKey(long epochMs, long start, long bucketMs) {
        long offset = Math.max(0L, epochMs - start);
        return start + (offset / bucketMs) * bucketMs;
    }

    private String bucketLabel(long millis, long bucketMs) {
        long day = 24L * 60L * 60L * 1000L;
        String pattern = bucketMs < day ? "MM-dd HH:mm" : "MM-dd";
        return new SimpleDateFormat(pattern, Locale.CHINA).format(new Date(millis));
    }

    private double eventCost(UsageEvent e) {
        PriceRule rule = priceForModel(e.model);
        if (rule == null) return 0d;
        return e.inputTokens / 1_000_000d * rule.inputPer1m
                + e.outputTokens / 1_000_000d * rule.outputPer1m
                + e.cachedTokens / 1_000_000d * rule.cachePer1m;
    }

    private void addLineChart(LinearLayout parent, String label, List<TrendPoint> points, String unit, boolean percentUnit) {
        if (points == null || points.isEmpty()) {
            addEmpty(parent, "当前范围没有可用趋势数据");
            return;
        }
        TrendPoint latest = points.get(points.size() - 1);
        TrendPoint maxPoint = points.get(0);
        for (TrendPoint point : points) if (point.value > maxPoint.value) maxPoint = point;
        addKeyValue(parent, "最新", formatTrendValue(latest.value, unit, percentUnit));
        addKeyValue(parent, "峰值", formatTrendValue(maxPoint.value, unit, percentUnit));
        LineChartView chart = new LineChartView(this, points, percentUnit);
        LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(-1, dp(150));
        chartLp.setMargins(0, dp(10), 0, 0);
        parent.addView(chart, chartLp);
        LinearLayout labels = horizontal();
        labels.setPadding(0, dp(4), 0, 0);
        parent.addView(labels);
        labels.addView(text(points.get(0).label, 11, MUTED, Typeface.NORMAL), new LinearLayout.LayoutParams(0, -2, 1));
        TextView right = text(latest.label, 11, MUTED, Typeface.NORMAL);
        right.setGravity(Gravity.RIGHT);
        labels.addView(right, new LinearLayout.LayoutParams(0, -2, 1));
        if (label != null && label.length() > 0) {
            TextView caption = text(label, 12, MUTED, Typeface.NORMAL);
            caption.setPadding(0, dp(3), 0, 0);
            parent.addView(caption);
        }
    }

    private void addHealthHeatmap(LinearLayout parent, List<HealthCell> cells) {
        if (cells == null || cells.isEmpty()) {
            addEmpty(parent, "当前范围没有可用时间线数据");
            return;
        }
        HealthHeatmapView chart = new HealthHeatmapView(this, cells);
        LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(-1, dp(104));
        chartLp.setMargins(0, dp(10), 0, 0);
        parent.addView(chart, chartLp);
        LinearLayout legend = horizontal();
        legend.setGravity(Gravity.CENTER);
        legend.setPadding(0, dp(4), 0, 0);
        parent.addView(legend, new LinearLayout.LayoutParams(-1, -2));
        legend.addView(text("最早  ", 11, MUTED, Typeface.NORMAL));
        addLegendSquare(legend, Color.rgb(224, 226, 230));
        addLegendSquare(legend, GREEN);
        addLegendSquare(legend, Color.rgb(250, 204, 21));
        addLegendSquare(legend, ORANGE);
        addLegendSquare(legend, RED);
        legend.addView(text("  最新", 11, MUTED, Typeface.NORMAL));
    }

    private void addLegendSquare(LinearLayout parent, int color) {
        TextView square = text("", 1, color, Typeface.NORMAL);
        square.setBackground(round(color, 2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(10), dp(10));
        lp.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(square, lp);
    }

    private String formatTrendValue(double value, String unit, boolean percentUnit) {
        if (percentUnit) return oneDecimal.format(value) + "%";
        if ("$".equals(unit)) return "$" + fourDecimal.format(value);
        if ("token".equals(unit)) return formatCount(Math.round(value));
        return twoDecimal.format(value);
    }

    private String readAll(InputStream stream) throws IOException { if (stream == null) return ""; ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[16 * 1024]; int n; while ((n = stream.read(buffer)) >= 0) out.write(buffer, 0, n); stream.close(); return out.toString("UTF-8"); }

    private void setStatus(String label, int color, int bg, String meta) { if (statusPill != null) { statusPill.setText(label); statusPill.setTextColor(color); statusPill.setBackground(round(bg, 16)); } if (header != null) { TextView last = header.findViewById(2001); if (last != null) last.setText("  " + meta); } }
    private void updateNav() { if (navItems == null) return; for (int i = 0; i < navItems.length; i++) { boolean on = i == selectedTab; navItems[i].setTextColor(on ? BLUE : MUTED); navItems[i].setBackground(round(on ? SOFT_BLUE : Color.TRANSPARENT, 8)); } }
    private void addMetricGrid(LinearLayout parent, Metric[] metrics) { for (int i = 0; i < metrics.length; i += 2) { LinearLayout row = horizontal(); parent.addView(row, matchWrapWithBottom(dp(10))); addMetricCard(row, metrics[i], 0); if (i + 1 < metrics.length) addMetricCard(row, metrics[i + 1], dp(10)); } }
    private void addMetricCard(LinearLayout row, Metric metric, int leftMargin) { LinearLayout card = card(); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1); lp.setMargins(leftMargin, 0, 0, 0); row.addView(card, lp); card.addView(text(metric.label, 12, MUTED, Typeface.NORMAL)); TextView value = text(metric.value, 23, TEXT, Typeface.BOLD); value.setPadding(0, dp(8), 0, dp(4)); card.addView(value); card.addView(text(metric.sub, 12, MUTED, Typeface.NORMAL)); }
    private void addSectionTitle(String title, String subtitle) { LinearLayout box = vertical(); box.setPadding(dp(4), dp(2), dp(4), dp(12)); content.addView(box); box.addView(text(title, 20, TEXT, Typeface.BOLD)); TextView sub = text(subtitle, 12, MUTED, Typeface.NORMAL); sub.setPadding(0, dp(4), 0, 0); box.addView(sub); }
    private void addKeyValue(LinearLayout parent, String key, String value) { LinearLayout row = horizontal(); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(9), 0, 0); parent.addView(row); row.addView(text(key, 13, MUTED, Typeface.NORMAL), new LinearLayout.LayoutParams(0, -2, 1)); TextView v = text(value == null ? "" : value, 13, TEXT, Typeface.BOLD); v.setGravity(Gravity.RIGHT); row.addView(v, new LinearLayout.LayoutParams(0, -2, 2)); }
    private void addProgress(LinearLayout parent, String label, long value, long max) { LinearLayout row = horizontal(); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(10), 0, 0); parent.addView(row); row.addView(text(label, 12, MUTED, Typeface.NORMAL), new LinearLayout.LayoutParams(dp(92), -2)); ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); bar.setMax(100); bar.setProgress((int) Math.max(1, Math.min(100, value * 100 / Math.max(1, max)))); row.addView(bar, new LinearLayout.LayoutParams(0, dp(12), 1)); TextView val = text(formatCount(value), 12, TEXT, Typeface.BOLD); val.setGravity(Gravity.RIGHT); row.addView(val, new LinearLayout.LayoutParams(dp(76), -2)); }
    private void addLogRow(LinearLayout parent, LogEntry log, boolean compact) { LinearLayout row = vertical(); row.setPadding(0, dp(12), 0, 0); parent.addView(row); LinearLayout top = horizontal(); top.setGravity(Gravity.CENTER_VERTICAL); row.addView(top); top.addView(pill(log.failed ? "ERROR" : "OK", log.failed ? RED : GREEN, log.failed ? SOFT_RED : SOFT_GREEN)); top.addView(text("  " + nonEmpty(shortTime(log.timestamp), "未知时间"), 12, MUTED, Typeface.NORMAL), new LinearLayout.LayoutParams(0, -2, 1)); if (log.latencyMs > 0) top.addView(text(formatLatency(log.latencyMs), 12, MUTED, Typeface.BOLD)); TextView msg = text(log.model + " · " + log.message, 14, TEXT, Typeface.BOLD); msg.setPadding(0, dp(6), 0, 0); row.addView(msg); if (!compact) { TextView meta = text(nonEmpty(log.credential, "未知凭证"), 12, MUTED, Typeface.NORMAL); meta.setPadding(0, dp(4), 0, 0); row.addView(meta); } }
    private void addSegment(String[] labels, int selected, SegmentCallback cb) { LinearLayout segment = horizontal(); segment.setPadding(dp(3), dp(3), dp(3), dp(3)); segment.setBackground(round(Color.rgb(232, 236, 244), 8)); content.addView(segment, matchWrapWithBottom(dp(12))); for (int i = 0; i < labels.length; i++) { final int index = i; TextView item = text(labels[i], 13, i == selected ? BLUE : MUTED, Typeface.BOLD); item.setGravity(Gravity.CENTER); item.setBackground(round(i == selected ? Color.WHITE : Color.TRANSPARENT, 7)); item.setOnClickListener(v -> cb.onSelect(index)); segment.addView(item, new LinearLayout.LayoutParams(0, dp(40), 1)); } }
    private void addEmptyCard(String value) { LinearLayout c = card(); content.addView(c, matchWrapWithBottom(dp(12))); addEmpty(c, value); }
    private void addEmpty(LinearLayout parent, String value) { TextView empty = text(value, 13, MUTED, Typeface.NORMAL); empty.setGravity(Gravity.CENTER_HORIZONTAL); empty.setPadding(0, dp(16), 0, dp(8)); parent.addView(empty); }
    private void addMuted(LinearLayout parent, String value) { TextView view = text(value, 12, MUTED, Typeface.NORMAL); view.setPadding(0, dp(6), 0, 0); parent.addView(view); }
    private LinearLayout card() { LinearLayout card = vertical(); card.setPadding(dp(14), dp(14), dp(14), dp(14)); card.setBackground(roundStroke(CARD, BORDER, 8)); return card; }
    private LinearLayout horizontal() { LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); return row; }
    private LinearLayout vertical() { LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); return col; }
    private EditText input(String value) { EditText input = new EditText(this); input.setSingleLine(true); input.setText(value); input.setTextSize(15); input.setTextColor(TEXT); input.setHintTextColor(MUTED); input.setInputType(InputType.TYPE_TEXT_VARIATION_URI); input.setPadding(dp(12), 0, dp(12), 0); input.setBackground(roundStroke(Color.WHITE, BORDER, 8)); return input; }
    private EditText passwordInput(String value) { EditText input = input(value); input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); return input; }
    private EditText numberInput(double value) { EditText input = input(value <= 0 ? "" : twoDecimal.format(value)); input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); input.setTextSize(13); return input; }
    private TextView text(String value, float sp, int color, int style) { TextView tv = new TextView(this); tv.setText(value == null ? "" : value); tv.setTextSize(sp); tv.setTextColor(color); tv.setTypeface(Typeface.DEFAULT, style); tv.setIncludeFontPadding(true); return tv; }
    private TextView pill(String value, int color, int bg) { TextView tv = text(value, 12, color, Typeface.BOLD); tv.setGravity(Gravity.CENTER); tv.setPadding(dp(10), dp(5), dp(10), dp(5)); tv.setBackground(round(bg, 16)); return tv; }
    private Button primaryButton(String value) { Button b = new Button(this); b.setText(value); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(Color.WHITE); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackground(round(BLUE, 8)); return b; }
    private Button secondaryButton(String value) { Button b = new Button(this); b.setText(value); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(TEXT); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackground(roundStroke(Color.WHITE, BORDER, 8)); return b; }
    private GradientDrawable round(int color, int radiusDp) { GradientDrawable gd = new GradientDrawable(); gd.setColor(color); gd.setCornerRadius(dp(radiusDp)); return gd; }
    private GradientDrawable roundStroke(int color, int stroke, int radiusDp) { GradientDrawable gd = round(color, radiusDp); gd.setStroke(dp(1), stroke); return gd; }
    private LinearLayout.LayoutParams weightLp(float weight) { return new LinearLayout.LayoutParams(0, -1, weight); }
    private LinearLayout.LayoutParams matchWrapWithBottom(int bottom) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, bottom); return lp; }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private void hideKeyboard(View view) { try { ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0); } catch (Exception ignored) {} }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }
    private boolean isNetworkLikelyAvailable() { try { ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE); NetworkInfo info = cm == null ? null : cm.getActiveNetworkInfo(); return info != null && info.isConnected(); } catch (Exception e) { return true; } }
    private String normalizeBaseUrl(String raw) { String value = raw == null ? "" : raw.trim(); if (value.length() == 0) value = DEFAULT_BASE_URL; if (value.endsWith("/management.html")) value = value.substring(0, value.length() - "/management.html".length()); int hash = value.indexOf('#'); if (hash >= 0) value = value.substring(0, hash); if (!value.startsWith("http://") && !value.startsWith("https://")) value = "http://" + value; while (value.endsWith("/")) value = value.substring(0, value.length() - 1); return value; }
    private String normalizeQuotaUrl(String raw) { String value = raw == null ? "" : raw.trim(); if (value.length() == 0) return DEFAULT_QUOTA_URL; if (!value.startsWith("http://") && !value.startsWith("https://")) value = "https://" + value; if (!value.contains("management.html")) { while (value.endsWith("/")) value = value.substring(0, value.length() - 1); value += "/management.html#/quota"; } else if (!value.contains("#")) value += "#/quota"; return value; }
    private String readSecret(String encryptedKey, String legacyPlainKey) { return secureStore == null ? "" : secureStore.read(encryptedKey, legacyPlainKey); }
    private boolean saveSecret(String encryptedKey, String legacyPlainKey, String value) { return secureStore != null && secureStore.write(encryptedKey, legacyPlainKey, value); }
    private void clearSecret(String encryptedKey, String legacyPlainKey) { if (secureStore != null) secureStore.remove(encryptedKey, legacyPlainKey); }
    private String safeQuotaAccountName(JSONObject file, String authIndex) { String value = firstNonEmpty(file.optString("name", ""), file.optString("display_name", ""), file.optString("displayName", ""), file.optString("filename", ""), fileNameOnly(file.optString("file", "")), fileNameOnly(file.optString("path", ""))); if (value.length() > 0) return cleanText(value, 40); return authIndex.length() > 0 ? "Codex " + maskIdentifier(authIndex) : "Codex 账号"; }
    private String fileNameOnly(String value) { if (value == null) return ""; String raw = value.trim().replace('\\', '/'); if (raw.length() == 0) return ""; int slash = raw.lastIndexOf('/'); return slash >= 0 ? raw.substring(slash + 1) : raw; }
    private String maskIdentifier(String value) { if (value == null) return ""; String raw = value.trim(); if (raw.length() <= 4) return raw; if (raw.length() <= 8) return raw.substring(0, 2) + "..." + raw.substring(raw.length() - 2); return raw.substring(0, 4) + "..." + raw.substring(raw.length() - 4); }
    private void saveRangePrefs() { if (prefs != null) prefs.edit().putInt(KEY_SELECTED_RANGE, selectedRange).putString(KEY_CUSTOM_START_DATE, customStartDate).putString(KEY_CUSTOM_END_DATE, customEndDate).apply(); }
    private void ensureCustomDates() { if (!isValidLocalDate(customEndDate)) customEndDate = localDateString(System.currentTimeMillis()); if (!isValidLocalDate(customStartDate)) { java.util.Calendar calendar = java.util.Calendar.getInstance(); calendar.add(java.util.Calendar.DAY_OF_MONTH, -6); customStartDate = localDateString(calendar.getTimeInMillis()); } if (parseLocalDateStartMillis(customStartDate) > parseLocalDateStartMillis(customEndDate)) customStartDate = customEndDate; }
    private void showCustomDatePicker(boolean startDate) { ensureCustomDates(); String current = startDate ? customStartDate : customEndDate; java.util.Calendar calendar = calendarForLocalDate(current); DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> { String picked = localDateString(year, month, dayOfMonth); if (startDate) customStartDate = picked; else customEndDate = picked; if (parseLocalDateStartMillis(customStartDate) > parseLocalDateStartMillis(customEndDate)) { if (startDate) customEndDate = customStartDate; else customStartDate = customEndDate; } selectedRange = RANGE_CUSTOM; saveRangePrefs(); viewSnapshot = applyRange(allSnapshot, selectedRange); applyCost(viewSnapshot); render(); }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)); dialog.show(); }
    private java.util.Calendar calendarForLocalDate(String value) { java.util.Calendar calendar = java.util.Calendar.getInstance(); long millis = parseLocalDateStartMillis(value); if (millis > 0) calendar.setTimeInMillis(millis); return calendar; }
    private boolean isValidLocalDate(String value) { return parseLocalDateStartMillis(value) > 0; }
    private long parseLocalDateStartMillis(String value) { if (value == null || value.length() == 0) return 0L; try { SimpleDateFormat fmt = new SimpleDateFormat(DATE_PATTERN, Locale.CHINA); fmt.setLenient(false); Date date = fmt.parse(value); return date == null ? 0L : date.getTime(); } catch (Exception e) { return 0L; } }
    private String localDateString(long millis) { return new SimpleDateFormat(DATE_PATTERN, Locale.CHINA).format(new Date(millis)); }
    private String localDateString(int year, int month, int dayOfMonth) { java.util.Calendar calendar = java.util.Calendar.getInstance(); calendar.set(java.util.Calendar.YEAR, year); calendar.set(java.util.Calendar.MONTH, month); calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth); calendar.set(java.util.Calendar.HOUR_OF_DAY, 0); calendar.set(java.util.Calendar.MINUTE, 0); calendar.set(java.util.Calendar.SECOND, 0); calendar.set(java.util.Calendar.MILLISECOND, 0); return localDateString(calendar.getTimeInMillis()); }
    private void appendError(StringBuilder errors, String label, Exception e) { if (errors.length() > 0) errors.append('\n'); errors.append(label).append("接口：").append(cleanError(e)); }
    private String cleanError(Exception e) { String message = e.getMessage(); if (message == null || message.length() == 0) message = e.getClass().getSimpleName(); return cleanText(message, 180); }
    private String cleanText(String value, int limit) { if (value == null) return ""; String cleaned = value.replace('\r', ' ').replace('\n', ' ').trim(); if (cleaned.length() > limit) cleaned = cleaned.substring(0, limit) + "..."; return cleaned; }
    private String nonEmpty(String value, String fallback) { return value == null || value.length() == 0 ? fallback : value; }
    private String firstNonEmpty(String... values) { if (values == null) return ""; for (String value : values) if (value != null && value.trim().length() > 0) return value.trim(); return ""; }
    private String shortTime(String iso) { if (iso == null || iso.length() == 0) return ""; try { if (iso.length() >= 16) return iso.substring(5, 16).replace('T', ' '); } catch (Exception ignored) {} return iso; }
    private long parseIsoMillis(String iso) { if (iso == null || iso.length() == 0) return 0; try { String v = iso; int dot = v.indexOf('.'); if (dot >= 0) { int z = v.indexOf('Z', dot); v = v.substring(0, dot) + (z >= 0 ? "Z" : ""); } SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); fmt.setTimeZone(TimeZone.getTimeZone("UTC")); Date d = fmt.parse(v); return d == null ? 0 : d.getTime(); } catch (Exception e) { return 0; } }
    private long percentile(List<Long> sorted, double p) { if (sorted.isEmpty()) return 0; int index = (int) Math.ceil(sorted.size() * p) - 1; index = Math.max(0, Math.min(sorted.size() - 1, index)); return sorted.get(index); }
    private long optLongAny(JSONObject obj, String... keys) { for (String key : keys) { if (!obj.has(key) || obj.isNull(key)) continue; Object value = obj.opt(key); if (value instanceof Number) return ((Number) value).longValue(); try { return Long.parseLong(String.valueOf(value)); } catch (Exception ignored) {} } return 0; }
    private double optDoubleAny(JSONObject obj, String... keys) { for (String key : keys) { if (!obj.has(key) || obj.isNull(key)) continue; Object value = obj.opt(key); if (value instanceof Number) return ((Number) value).doubleValue(); try { return Double.parseDouble(String.valueOf(value)); } catch (Exception ignored) {} } return 0; }
    private Double optNullableDoubleAny(JSONObject obj, String... keys) { for (String key : keys) { if (!obj.has(key) || obj.isNull(key)) continue; Object value = obj.opt(key); if (value instanceof Number) return ((Number) value).doubleValue(); try { String raw = String.valueOf(value).trim(); if (raw.endsWith("%")) raw = raw.substring(0, raw.length() - 1); return Double.parseDouble(raw); } catch (Exception ignored) {} } return null; }
    private double parseDouble(String raw) { try { return Double.parseDouble(raw == null ? "0" : raw.trim()); } catch (Exception e) { return 0d; } }
    private String formatCount(long value) { long abs = Math.abs(value); if (abs >= 1_000_000_000L) return oneDecimal.format(value / 1_000_000_000d) + "B"; if (abs >= 1_000_000L) return oneDecimal.format(value / 1_000_000d) + "M"; if (abs >= 10_000L) return oneDecimal.format(value / 1_000d) + "K"; return String.format(Locale.CHINA, "%d", value); }
    private String percent(double value) { if (Double.isNaN(value) || Double.isInfinite(value)) return "0%"; return oneDecimal.format(value * 100d) + "%"; }
    private String formatLatency(long ms) { if (ms >= 1000) return oneDecimal.format(ms / 1000d) + "s"; return ms + "ms"; }
    private String inputOutputLabel(DataSnapshot s) { if (s.inputTokens + s.outputTokens == 0) return "输入/输出未汇总"; return "输入 " + formatCount(s.inputTokens) + " · 输出 " + formatCount(s.outputTokens); }
    private String rangeLabel(int range) { if (range == RANGE_4H) return "最近 4h"; if (range == RANGE_8H) return "最近 8h"; if (range == RANGE_24H) return "最近 24h"; if (range == RANGE_7D) return "最近 7天"; if (range == RANGE_30D) return "最近 30天"; if (range == RANGE_CUSTOM) { ensureCustomDates(); return customStartDate + " 至 " + customEndDate; } return "全部"; }

    private interface SegmentCallback { void onSelect(int index); }
    private static class Metric { final String label; final String value; final String sub; Metric(String label, String value, String sub) { this.label = label; this.value = value; this.sub = sub; } }
    private static class TrendPoint { final String label; final double value; final long count; TrendPoint(String label, double value, long count) { this.label = label == null ? "" : label; this.value = value; this.count = count; } }
    private static class HealthCell { final long startMs, endMs, total, failed; HealthCell(long startMs, long endMs, long total, long failed) { this.startMs = startMs; this.endMs = endMs; this.total = total; this.failed = failed; } double successRate() { return total <= 0 ? 0d : (total - failed) / (double) total; } }
    private class DataSnapshot {
        String baseUrl = DEFAULT_BASE_URL, lastRunAt = "", error = "", currency = ""; boolean authenticated, online, syncRunning, statusLoaded, usageLoaded; long totalRequests, successCount, failureCount, totalTokens, inputTokens, outputTokens, reasoningTokens, cachedTokens, avgLatencyMs, p95LatencyMs, p99LatencyMs, loadedAtMillis; double estimatedCost; Map<String, ModelSummary> models = new HashMap<>(); Map<String, CredentialSummary> credentials = new HashMap<>(); Map<String, Long> dayCounts = new HashMap<>(); List<LogEntry> logs = new ArrayList<>(); List<UsageEvent> events = new ArrayList<>();
        boolean hasAnyData() { return statusLoaded || usageLoaded || totalRequests > 0 || !models.isEmpty() || !credentials.isEmpty(); }
        double successRate() { long total = totalRequests > 0 ? totalRequests : successCount + failureCount; return total <= 0 ? 0 : successCount / (double) total; }
        int modelCount() { return models.size(); }
        String costLabel() { if (estimatedCost <= 0) return "待配置"; String unit = currency == null || currency.length() == 0 ? "$" : currency + " "; return unit + twoDecimal.format(estimatedCost); }
        String loadedAtLabel() { if (loadedAtMillis <= 0) return "未刷新"; return new SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(new Date(loadedAtMillis)); }
        DataSnapshot emptyCopyMeta() { DataSnapshot out = new DataSnapshot(); out.baseUrl = baseUrl; out.authenticated = authenticated; out.online = online; out.syncRunning = syncRunning; out.statusLoaded = statusLoaded; out.usageLoaded = usageLoaded; out.lastRunAt = lastRunAt; out.error = error; out.currency = currency; out.estimatedCost = estimatedCost; out.loadedAtMillis = loadedAtMillis; return out; }
        DataSnapshot copySummary() { DataSnapshot out = emptyCopyMeta(); if (!events.isEmpty()) { for (UsageEvent e : events) out.addEvent(e); out.finalizeComputed(); } else { out.totalRequests = totalRequests; out.successCount = successCount; out.failureCount = failureCount; out.totalTokens = totalTokens; out.inputTokens = inputTokens; out.outputTokens = outputTokens; out.reasoningTokens = reasoningTokens; out.cachedTokens = cachedTokens; out.avgLatencyMs = avgLatencyMs; out.p95LatencyMs = p95LatencyMs; out.p99LatencyMs = p99LatencyMs; out.models = models; out.credentials = credentials; out.dayCounts = dayCounts; out.logs = logs; } return out; }
        void recalculateFromEventsIfNeeded() { if (events.isEmpty()) return; DataSnapshot out = copySummary(); totalRequests = out.totalRequests; successCount = out.successCount; failureCount = out.failureCount; totalTokens = out.totalTokens; inputTokens = out.inputTokens; outputTokens = out.outputTokens; reasoningTokens = out.reasoningTokens; cachedTokens = out.cachedTokens; avgLatencyMs = out.avgLatencyMs; p95LatencyMs = out.p95LatencyMs; p99LatencyMs = out.p99LatencyMs; models = out.models; credentials = out.credentials; dayCounts = out.dayCounts; logs = out.logs; }
        void addEvent(UsageEvent e) { events.add(e); totalRequests++; if (e.failed) failureCount++; else successCount++; totalTokens += e.totalTokens; inputTokens += e.inputTokens; outputTokens += e.outputTokens; reasoningTokens += e.reasoningTokens; cachedTokens += e.cachedTokens; ModelSummary m = models.get(e.model); if (m == null) { m = new ModelSummary(e.model); models.put(e.model, m); } m.add(e); CredentialSummary c = credentials.get(e.credentialId); if (c == null) { c = new CredentialSummary(); c.id = e.credentialId; c.displayName = e.credentialName; credentials.put(e.credentialId, c); } c.add(e); if (e.timestamp.length() >= 10) { String day = e.timestamp.substring(0, 10); Long old = dayCounts.get(day); dayCounts.put(day, old == null ? 1L : old + 1L); } LogEntry log = new LogEntry(); log.timestamp = e.timestamp; log.failed = e.failed; log.model = e.model; log.credential = e.credentialName; log.message = e.failed ? "请求失败" : "请求成功"; log.latencyMs = e.latencyMs; logs.add(log); }
        void finalizeComputed() { List<Long> latencies = new ArrayList<>(); long sum = 0; for (UsageEvent e : events) if (e.latencyMs > 0) { latencies.add(e.latencyMs); sum += e.latencyMs; } if (!latencies.isEmpty()) { Collections.sort(latencies); avgLatencyMs = sum / latencies.size(); p95LatencyMs = percentile(latencies, 0.95); p99LatencyMs = percentile(latencies, 0.99); } Collections.sort(logs, (a, b) -> b.timestamp.compareTo(a.timestamp)); if (logs.size() > 500) logs = new ArrayList<>(logs.subList(0, 500)); }
    }
    private static class UsageEvent { String timestamp = "", model = "", credentialId = "", credentialName = ""; long epochMs, latencyMs, totalTokens, inputTokens, outputTokens, reasoningTokens, cachedTokens; boolean failed; }
    private static class ApiKeyItem { String id = "", name = "", value = "", createdAt = ""; int index = -1; }
    private static class ApiCallResponse { final int statusCode; final String body; ApiCallResponse(int statusCode, String body) { this.statusCode = statusCode; this.body = body == null ? "" : body; } }
    private static class CodexQuotaAccount {
        String name = "Codex 账号", authIndex = "", planType = "", subscriptionActiveUntil = "", chatgptAccountId = "", error = "", resetCreditsError = "", rawSummary = "", lastActionMessage = "";
        boolean loading, resetting, resetCreditsKnown;
        long resetCreditsAvailableCount;
        JSONObject rawFile = new JSONObject();
        List<QuotaWindow> windows = new ArrayList<>();
        List<String> resetCreditExpiries = new ArrayList<>();
    }
    private static class QuotaWindow { String id = "", label = "", resetLabel = ""; double usedPercent = -1d, remainingPercent = -1d; }
    private static class PriceRule { String model = ""; double inputPer1m, outputPer1m, cachePer1m; PriceRule copyFor(String modelName) { PriceRule copy = new PriceRule(); copy.model = modelName; copy.inputPer1m = inputPer1m; copy.outputPer1m = outputPer1m; copy.cachePer1m = cachePer1m; return copy; } }
    private static class ModelSummary { final String name; long totalRequests, successCount, failureCount, totalTokens, inputTokens, outputTokens, reasoningTokens, cachedTokens, latencySumMs, latencyCount; double estimatedCost; String lastUsedAt = ""; ModelSummary(String name) { this.name = name; } void add(UsageEvent e) { totalRequests++; if (e.failed) failureCount++; else successCount++; totalTokens += e.totalTokens; inputTokens += e.inputTokens; outputTokens += e.outputTokens; reasoningTokens += e.reasoningTokens; cachedTokens += e.cachedTokens; if (e.latencyMs > 0) { latencySumMs += e.latencyMs; latencyCount++; } if (e.timestamp.compareTo(lastUsedAt) > 0) lastUsedAt = e.timestamp; } double successRate() { long total = totalRequests > 0 ? totalRequests : successCount + failureCount; return total <= 0 ? 0 : successCount / (double) total; } long avgLatencyMs() { return latencyCount <= 0 ? 0 : latencySumMs / latencyCount; } }
    private static class CredentialSummary { String id = "", displayName = ""; long totalRequests, successCount, failureCount, totalTokens; void add(UsageEvent e) { totalRequests++; if (e.failed) failureCount++; else successCount++; totalTokens += e.totalTokens; if (displayName == null || displayName.length() == 0) displayName = e.credentialName; } double successRate() { long total = totalRequests > 0 ? totalRequests : successCount + failureCount; return total <= 0 ? 0 : successCount / (double) total; } int healthColor() { if (totalRequests > 0 && successCount == 0 && failureCount > 0) return RED; if (successRate() < 0.8 && totalRequests >= 5) return ORANGE; return GREEN; } String healthLabel() { int color = healthColor(); if (color == RED) return "异常"; if (color == ORANGE) return "需关注"; return "正常"; } }
    private static class LogEntry { String timestamp = "", model = "", credential = "", message = ""; boolean failed; long latencyMs; }

    private class LineChartView extends View {
        private final List<TrendPoint> points;
        private final boolean percentUnit;
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        LineChartView(Context context, List<TrendPoint> points, boolean percentUnit) {
            super(context);
            this.points = points == null ? new ArrayList<>() : new ArrayList<>(points);
            this.percentUnit = percentUnit;
            setWillNotDraw(false);
            gridPaint.setColor(BORDER);
            gridPaint.setStrokeWidth(dp(1));
            linePaint.setColor(BLUE);
            linePaint.setStrokeWidth(dp(2));
            linePaint.setStyle(Paint.Style.STROKE);
            fillPaint.setColor(Color.argb(32, 23, 107, 255));
            fillPaint.setStyle(Paint.Style.FILL);
            dotPaint.setColor(BLUE);
            dotPaint.setStyle(Paint.Style.FILL);
            textPaint.setColor(MUTED);
            textPaint.setTextSize(dp(10));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            int left = dp(8);
            int right = w - dp(8);
            int top = dp(12);
            int bottom = h - dp(20);
            if (w <= 0 || h <= 0 || points.isEmpty()) return;
            double max = percentUnit ? 100d : 0d;
            for (TrendPoint point : points) max = Math.max(max, point.value);
            if (max <= 0d) max = 1d;
            for (int i = 0; i <= 3; i++) {
                float y = top + (bottom - top) * i / 3f;
                canvas.drawLine(left, y, right, y, gridPaint);
            }
            Path line = new Path();
            Path fill = new Path();
            for (int i = 0; i < points.size(); i++) {
                float x = points.size() == 1 ? (left + right) / 2f : left + (right - left) * i / (float) (points.size() - 1);
                float y = (float) (bottom - (points.get(i).value / max) * (bottom - top));
                if (i == 0) {
                    line.moveTo(x, y);
                    fill.moveTo(x, bottom);
                    fill.lineTo(x, y);
                } else {
                    line.lineTo(x, y);
                    fill.lineTo(x, y);
                }
                if (i == points.size() - 1 || i == 0 || points.size() <= 8) canvas.drawCircle(x, y, dp(2), dotPaint);
            }
            fill.lineTo(right, bottom);
            fill.close();
            canvas.drawPath(fill, fillPaint);
            canvas.drawPath(line, linePaint);
            String maxLabel = percentUnit ? oneDecimal.format(max) + "%" : max >= 10000d ? formatCount(Math.round(max)) : twoDecimal.format(max);
            canvas.drawText(maxLabel, left, dp(10), textPaint);
        }
    }

    private class HealthHeatmapView extends View {
        private final List<HealthCell> cells;
        private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final int rows = 7;
        private int columns = 0;
        private float cellSize = 0f;
        private float gap = 0f;
        private float originX = 0f;
        private float originY = 0f;

        HealthHeatmapView(Context context, List<HealthCell> cells) {
            super(context);
            this.cells = cells == null ? new ArrayList<>() : new ArrayList<>(cells);
            setWillNotDraw(false);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (cells.isEmpty()) return;
            int w = getWidth();
            int h = getHeight();
            gap = dp(3);
            columns = (int) Math.ceil(cells.size() / (double) rows);
            float availableW = Math.max(1f, w - dp(2));
            float availableH = Math.max(1f, h - dp(2));
            cellSize = Math.min((availableW - gap * (columns - 1)) / columns, (availableH - gap * (rows - 1)) / rows);
            cellSize = Math.max(dp(4), cellSize);
            originX = (w - (columns * cellSize + (columns - 1) * gap)) / 2f;
            originY = (h - (rows * cellSize + (rows - 1) * gap)) / 2f;
            for (int i = 0; i < cells.size(); i++) {
                int col = i / rows;
                int row = i % rows;
                float left = originX + col * (cellSize + gap);
                float top = originY + row * (cellSize + gap);
                rect.set(left, top, left + cellSize, top + cellSize);
                cellPaint.setColor(healthCellColor(cells.get(i)));
                canvas.drawRoundRect(rect, dp(2), dp(2), cellPaint);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP || cells.isEmpty()) return true;
            int col = (int) ((event.getX() - originX) / (cellSize + gap));
            int row = (int) ((event.getY() - originY) / (cellSize + gap));
            if (col < 0 || row < 0 || row >= rows || col >= columns) return true;
            float localX = event.getX() - originX - col * (cellSize + gap);
            float localY = event.getY() - originY - row * (cellSize + gap);
            if (localX < 0 || localY < 0 || localX > cellSize || localY > cellSize) return true;
            int index = col * rows + row;
            if (index < 0 || index >= cells.size()) return true;
            HealthCell cell = cells.get(index);
            String start = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(cell.startMs));
            String end = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(cell.endMs));
            String message = start + " - " + end + "\n" + (cell.total <= 0 ? "暂无请求" : "请求 " + cell.total + "，失败 " + cell.failed + "，成功率 " + percent(cell.successRate()));
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            return true;
        }

        private int healthCellColor(HealthCell cell) {
            if (cell.total <= 0) return Color.rgb(224, 226, 230);
            double rate = cell.successRate();
            if (rate >= 0.99) return Color.rgb(34, 197, 94);
            if (rate >= 0.95) return Color.rgb(163, 230, 53);
            if (rate >= 0.85) return Color.rgb(250, 204, 21);
            if (rate >= 0.70) return Color.rgb(249, 115, 22);
            return Color.rgb(239, 68, 68);
        }
    }
}
