package top.five915.cpausage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MainActivityRenderTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private LinearLayout content;
    private Object snapshot;

    @Before
    public void setUp() {
        // Leave onboarding unauthenticated so rendering tests never contact a server.
        controller = Robolectric.buildActivity(MainActivity.class).create();
        activity = controller.get();
        content = new LinearLayout(activity);
        ReflectionHelpers.setField(activity, "content", content);
        snapshot = ReflectionHelpers.getField(activity, "viewSnapshot");
    }

    @After
    public void tearDown() {
        controller.destroy();
    }

    @Test
    public void usageShowsRangeAndActualMetrics() {
        ReflectionHelpers.setField(snapshot, "usageLoaded", true);
        ReflectionHelpers.setField(snapshot, "totalRequests", 123L);
        ReflectionHelpers.setField(snapshot, "totalTokens", 456L);
        render(2);
        assertText("时间范围");
        assertText("趋势成本");
        assertText("请求总量");
        assertText("123");
        assertText("456");
    }

    @Test
    public void usageModelsShowsEmptyStateInsteadOfBlankPage() {
        ReflectionHelpers.setField(activity, "resourceTab", 1);
        render(2);
        assertText("模型用量");
        assertText("当前范围暂无模型数据");
    }

    @Test
    public void usageCredentialsShowsEmptyStateInsteadOfBlankPage() {
        ReflectionHelpers.setField(activity, "resourceTab", 2);
        render(2);
        assertText("凭证状态");
        assertText("当前范围暂无凭证数据");
    }

    @Test
    public void overviewShowsRangeAndMetrics() {
        ReflectionHelpers.setField(snapshot, "totalRequests", 123L);
        render(0);
        assertText("时间范围");
        assertText("运行概览");
        assertText("请求数");
        assertText("123");
    }

    @Test
    public void emptyLoadingShowsProgressOnBothTabs() {
        ReflectionHelpers.setField(activity, "loading", true);
        for (int tab : new int[]{0, 2}) {
            render(tab);
            assertText("时间范围");
            assertText("正在连接 Usage Keeper");
            assertFalse(containsText(content, "请求数"));
            assertFalse(containsText(content, "请求总量"));
        }
    }

    @Test
    public void failedLoadingShowsErrorOnBothTabs() {
        ReflectionHelpers.setField(snapshot, "error", "Test service unavailable");
        for (int tab : new int[]{0, 2}) {
            render(tab);
            assertText("接口异常");
            assertText("Test service unavailable");
            assertText(tab == 0 ? "请求数" : "请求总量");
        }
    }

    @Test
    public void cachedUsageRemainsVisibleDuringRefresh() {
        ReflectionHelpers.setField(activity, "loading", true);
        ReflectionHelpers.setField(snapshot, "fromCache", true);
        ReflectionHelpers.setField(snapshot, "totalRequests", 123L);
        render(2);
        assertText("请求总量");
        assertText("123");
        assertFalse(containsText(content, "正在连接 Usage Keeper"));
    }

    @Test
    public void changingRangeKeepsUsageContent() {
        render(2);
        ReflectionHelpers.callInstanceMethod(activity, "selectRange",
                ReflectionHelpers.ClassParameter.from(int.class, 5));
        assertText("当前范围：全部");
        assertText("请求总量");
    }

    @Test
    public void switchingTabsRestoresTheirContent() {
        render(0);
        assertText("运行概览");
        render(2);
        assertText("请求总量");
        assertFalse(containsText(content, "运行概览"));
        render(0);
        assertText("运行概览");
        assertFalse(containsText(content, "请求总量"));
    }

    private void render(int tab) {
        ReflectionHelpers.setField(activity, "selectedTab", tab);
        ReflectionHelpers.callInstanceMethod(activity, "render");
        assertSame(content, ReflectionHelpers.getField(activity, "content"));
    }

    private void assertText(String expected) {
        assertTrue("Missing rendered text: " + expected, containsText(content, expected));
    }

    private boolean containsText(View view, String expected) {
        if (view instanceof TextView
                && expected.contentEquals(((TextView) view).getText())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsText(group.getChildAt(i), expected)) return true;
            }
        }
        return false;
    }
}
