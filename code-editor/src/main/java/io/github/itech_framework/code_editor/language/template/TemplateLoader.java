package io.github.itech_framework.code_editor.language.template;

import io.github.itech_framework.code_editor.internal.json.JsonException;
import io.github.itech_framework.code_editor.internal.json.JsonReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class TemplateLoader {

    public static final int SUPPORTED_VERSION = 1;
    public static final String DEFAULT_START = "root";

    private static final Set<String> TOP_KEYS = Set.of("version", "id", "name", "extensions", "start", "states");
    private static final Set<String> RULE_KEYS =
            Set.of("match", "words", "begin", "end", "scope", "next", "push", "pop", "caseSensitive");

    private record Reference(String field, String target) {
    }

    private final String source;
    private final List<Reference> references = new ArrayList<>();

    private TemplateLoader(String source) {
        this.source = source;
    }

    public static LanguageTemplate load(String source, String json) {
        Object parsed;
        try {
            parsed = JsonReader.parse(json);
        } catch (JsonException ex) {
            throw new TemplateException(source, "json", ex.getMessage(), ex);
        }
        return new TemplateLoader(source).build(parsed);
    }

    public static LanguageTemplate load(Path path) throws IOException {
        return load(path.toString(), Files.readString(path, StandardCharsets.UTF_8));
    }

    private LanguageTemplate build(Object root) {
        Map<String, Object> map = object(root, "(root)");
        rejectUnknown(map, TOP_KEYS, "");

        Object version = map.get("version");
        if (version == null) {
            throw fail("version", "is required");
        }
        if (!(version instanceof Long number)) {
            throw fail("version", "must be an integer");
        }
        if (number != SUPPORTED_VERSION) {
            throw fail("version", "unsupported value " + number + ", expected " + SUPPORTED_VERSION);
        }

        String id = string(map, "id", "", true);
        String name = string(map, "name", "", false);
        String displayName = name == null ? id : name;

        List<String> extensions = new ArrayList<>();
        Object extensionValue = map.get("extensions");
        if (extensionValue != null) {
            List<Object> list = array(extensionValue, "extensions");
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (!(item instanceof String) || ((String) item).isBlank()) {
                    throw fail("extensions[" + i + "]", "must be a non-blank string");
                }
                extensions.add(((String) item).trim());
            }
        }

        Object statesValue = map.get("states");
        if (statesValue == null) {
            throw fail("states", "is required");
        }
        Map<String, Object> rawStates = object(statesValue, "states");
        if (rawStates.isEmpty()) {
            throw fail("states", "must define at least one state");
        }
        Map<String, List<TemplateRule>> states = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawStates.entrySet()) {
            if (entry.getKey().isBlank()) {
                throw fail("states", "state names must not be blank");
            }
            String stateField = join("states", entry.getKey());
            List<Object> rawRules = array(entry.getValue(), stateField);
            List<TemplateRule> rules = new ArrayList<>();
            for (int i = 0; i < rawRules.size(); i++) {
                rules.add(rule(rawRules.get(i), stateField + "[" + i + "]"));
            }
            states.put(entry.getKey(), rules);
        }

        String start = string(map, "start", "", false);
        if (start == null) {
            start = DEFAULT_START;
        }
        if (!states.containsKey(start)) {
            throw fail("start", unknownState(start, states.keySet()));
        }
        for (Reference reference : references) {
            if (!states.containsKey(reference.target())) {
                throw fail(reference.field(), unknownState(reference.target(), states.keySet()));
            }
        }
        return new LanguageTemplate(SUPPORTED_VERSION, id, displayName, extensions, start, states);
    }

    private TemplateRule rule(Object value, String path) {
        Map<String, Object> map = object(value, path);
        rejectUnknown(map, RULE_KEYS, path);

        boolean hasMatch = map.containsKey("match");
        boolean hasWords = map.containsKey("words");
        boolean hasBegin = map.containsKey("begin");
        int kinds = (hasMatch ? 1 : 0) + (hasWords ? 1 : 0) + (hasBegin ? 1 : 0);
        if (kinds != 1) {
            throw fail(path, "needs exactly one of 'match', 'words' or 'begin'");
        }
        String scope = string(map, "scope", path, true);

        if (hasBegin) {
            forbid(map, path, "begin", "next", "push", "pop", "caseSensitive");
            Pattern begin = regex(string(map, "begin", path, true), join(path, "begin"), false);
            if (!map.containsKey("end")) {
                throw fail(join(path, "end"), "is required with 'begin'");
            }
            Pattern end = regex(string(map, "end", path, true), join(path, "end"), true);
            return new TemplateRule.Block(begin, end, scope);
        }

        if (hasWords) {
            forbid(map, path, "words", "end", "next", "push", "pop");
            boolean caseSensitive = true;
            Object flag = map.get("caseSensitive");
            if (flag != null) {
                if (!(flag instanceof Boolean)) {
                    throw fail(join(path, "caseSensitive"), "must be true or false");
                }
                caseSensitive = (Boolean) flag;
            }
            String wordsField = join(path, "words");
            List<Object> list = array(map.get("words"), wordsField);
            if (list.isEmpty()) {
                throw fail(wordsField, "must contain at least one word");
            }
            Set<String> words = new LinkedHashSet<>();
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                String itemField = wordsField + "[" + i + "]";
                if (!(item instanceof String) || ((String) item).isEmpty()) {
                    throw fail(itemField, "must be a non-empty string");
                }
                String word = (String) item;
                if (word.chars().anyMatch(Character::isWhitespace)) {
                    throw fail(itemField, "must not contain whitespace");
                }
                words.add(caseSensitive ? word : word.toLowerCase(Locale.ROOT));
            }
            return new TemplateRule.Words(words, scope, caseSensitive);
        }

        forbid(map, path, "match", "end", "caseSensitive");
        Pattern pattern = regex(string(map, "match", path, true), join(path, "match"), false);
        return new TemplateRule.Match(pattern, scope, action(map, path));
    }

    private StateAction action(Map<String, Object> map, String path) {
        boolean hasNext = map.containsKey("next");
        boolean hasPush = map.containsKey("push");
        boolean hasPop = map.containsKey("pop");
        int count = (hasNext ? 1 : 0) + (hasPush ? 1 : 0) + (hasPop ? 1 : 0);
        if (count == 0) {
            return StateAction.none();
        }
        if (count > 1) {
            throw fail(path, "use only one of 'next', 'push' or 'pop'");
        }
        if (hasPop) {
            if (!Boolean.TRUE.equals(map.get("pop"))) {
                throw fail(join(path, "pop"), "must be true");
            }
            return StateAction.pop();
        }
        String key = hasNext ? "next" : "push";
        String target = string(map, key, path, true);
        references.add(new Reference(join(path, key), target));
        return new StateAction(hasNext ? StateAction.Type.NEXT : StateAction.Type.PUSH, target);
    }

    private Pattern regex(String text, String field, boolean allowEmpty) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(text);
        } catch (PatternSyntaxException ex) {
            throw fail(field, "invalid regex: " + ex.getDescription());
        }
        if (!allowEmpty && pattern.matcher("").lookingAt()) {
            throw fail(field, "regex must not match an empty string");
        }
        return pattern;
    }

    private void forbid(Map<String, Object> map, String path, String kind, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                throw fail(join(path, key), "is not allowed in " + kind + " rules");
            }
        }
    }

    private void rejectUnknown(Map<String, Object> map, Set<String> allowed, String path) {
        for (String key : map.keySet()) {
            if (!allowed.contains(key)) {
                throw fail(join(path, key), "unknown field");
            }
        }
    }

    private String string(Map<String, Object> map, String key, String path, boolean required) {
        String field = join(path, key);
        Object value = map.get(key);
        if (value == null) {
            if (required) {
                throw fail(field, "is required");
            }
            return null;
        }
        if (!(value instanceof String text)) {
            throw fail(field, "must be a string");
        }
        if (text.isBlank()) {
            throw fail(field, "must not be blank");
        }
        return text;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value, String field) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        throw fail(field, "must be an object");
    }

    @SuppressWarnings("unchecked")
    private List<Object> array(Object value, String field) {
        if (value instanceof List<?>) {
            return (List<Object>) value;
        }
        throw fail(field, "must be an array");
    }

    private static String join(String path, String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    private static String unknownState(String name, Set<String> defined) {
        return "unknown state '" + name + "'. Defined states: " + new TreeSet<>(defined);
    }

    private TemplateException fail(String field, String reason) {
        return new TemplateException(source, field, reason);
    }
}