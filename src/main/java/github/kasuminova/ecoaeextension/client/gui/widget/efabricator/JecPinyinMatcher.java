package github.kasuminova.ecoaeextension.client.gui.widget.efabricator;

import java.lang.reflect.Method;

final class JecPinyinMatcher {

    private static final String MATCH_CLASS = "me.towdium.jecharacters.util.Match";

    private static Method containsMethod = findContainsMethod();

    private JecPinyinMatcher() {
    }

    static boolean isAvailable() {
        return containsMethod != null;
    }

    static boolean contains(final String text, final String query) {
        final Method method = containsMethod;
        if (method == null) {
            return text.contains(query);
        }

        try {
            return Boolean.TRUE.equals(method.invoke(null, text, query));
        } catch (ReflectiveOperationException e) {
            containsMethod = null;
            return text.contains(query);
        }
    }

    private static Method findContainsMethod() {
        try {
            return Class.forName(MATCH_CLASS).getMethod("contains", String.class, CharSequence.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

}
