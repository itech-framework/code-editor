package io.github.itech_framework.code_editor.language;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class EditorLanguage {

    private final String id;
    private final String displayName;
    private final List<String> extensions;
    private final Tokenizer tokenizer;
    private final List<BracketPair> autoClosingPairs;
    private final String lineComment;
    private final FormatterSpec formatterSpec;
    private final List<String> keywords;

    private EditorLanguage(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName == null ? builder.id : builder.displayName;
        this.extensions = List.copyOf(builder.extensions);
        this.tokenizer = builder.tokenizer;
        this.autoClosingPairs = builder.autoClosingPairs == null
                ? BracketPair.DEFAULT_AUTO_CLOSING
                : List.copyOf(builder.autoClosingPairs);
        this.lineComment = builder.lineComment;
        this.formatterSpec = builder.formatterSpec;
        this.keywords = List.copyOf(builder.keywords);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    public List<BracketPair> getAutoClosingPairs() {
        return autoClosingPairs;
    }

    public Optional<String> getLineComment() {
        return Optional.ofNullable(lineComment);
    }
    
    public Optional<FormatterSpec> getFormatterSpec() {
        return Optional.ofNullable(formatterSpec);
    }
    
    public List<String> getKeywords() {
        return keywords;
    }

    static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Language id must not be blank");
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeExtension(String extension) {
        if (extension == null) {
            throw new IllegalArgumentException("File extension must not be null");
        }
        String value = extension.trim().toLowerCase(Locale.ROOT);
        while (value.startsWith(".")) {
            value = value.substring(1);
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException("File extension must not be blank");
        }
        return value;
    }

    public static final class Builder {

        private final String id;
        private String displayName;
        private final List<String> extensions = new ArrayList<>();
        private Tokenizer tokenizer;
        private List<BracketPair> autoClosingPairs;
        private String lineComment;
        
        private FormatterSpec formatterSpec;
        
        private final Set<String> keywords = new LinkedHashSet<>();
        
        private Builder(String id) {
            this.id = normalizeId(id);
        }

        public Builder displayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        public Builder extension(String extension) {
            String value = normalizeExtension(extension);
            if (!extensions.contains(value)) {
                extensions.add(value);
            }
            return this;
        }

        public Builder extensions(Collection<String> values) {
            Objects.requireNonNull(values, "extensions");
            values.forEach(this::extension);
            return this;
        }

        public Builder tokenizer(Tokenizer tokenizer) {
            this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer");
            return this;
        }

        public Builder autoClosingPairs(Collection<BracketPair> pairs) {
            Objects.requireNonNull(pairs, "pairs");
            this.autoClosingPairs = new ArrayList<>(pairs);
            return this;
        }

        public Builder lineComment(String prefix) {
            if (prefix == null) {
                this.lineComment = null;
                return this;
            }
            String value = prefix.trim();
            if (value.isEmpty() || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("Line comment must be a non-blank single line");
            }
            this.lineComment = value;
            return this;
        }
        
        public Builder formatter(FormatterSpec spec) {
            this.formatterSpec = spec;
            return this;
        }
        
        public Builder keywords(Collection<String> values) {
            Objects.requireNonNull(values, "keywords");
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    throw new IllegalArgumentException("Keyword must not be blank");
                }
                keywords.add(value.trim());
            }
            return this;
        }

        public EditorLanguage build() {
            if (tokenizer == null) {
                throw new IllegalStateException("Language '" + id + "' has no tokenizer");
            }
            return new EditorLanguage(this);
        }
    }
}