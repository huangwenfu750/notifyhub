package io.notifyhub.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TemplateRendererTest {

    private final Message msg = new Message("evt-1", "alert.db", "标题", "内容",
            Map.of("env", "prod", "level", "high"), 1234567890L);

    @Test
    void rendersBasicPlaceholders() {
        assertEquals("标题: 内容", TemplateRenderer.render("{{title}}: {{content}}", msg));
    }

    @Test
    void rendersParams() {
        assertEquals("[prod][high] 标题", TemplateRenderer.render("[{{params.env}}][{{params.level}}] {{title}}", msg));
        assertEquals("[prod]", TemplateRenderer.render("[{{env}}]", msg)); // 直接写参数名
    }

    @Test
    void unknownPlaceholderBecomesEmpty() {
        assertEquals("x", TemplateRenderer.render("x{{params.nope}}", msg));
    }

    @Test
    void unterminatedTagsAreKept() {
        assertEquals("{{title", TemplateRenderer.render("{{title", msg));
        // 已闭合的未知占位符与 params 行为一致：替换为空
        assertEquals("ab", TemplateRenderer.render("a{{nope}}b", msg));
    }

    @Test
    void blankTemplateReturnsNull() {
        assertNull(TemplateRenderer.render(null, msg));
        assertNull(TemplateRenderer.render("  ", msg));
    }
}
