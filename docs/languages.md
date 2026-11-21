# Language templates

A language is a JSON file. It tells the editor how to color text, which comment prefix Ctrl+/ uses, which characters close themselves, which formatter to run, and which words the autocomplete list offers. You don't write a tokenizer.

`json` and `javascript` are built in, and so is `plaintext`. A `java.json` template ships in the jar but isn't registered by default. If you want full control over coloring, implement the `Tokenizer` interface and build an `EditorLanguage` yourself. This page covers only templates.

## A small example

This describes a `.properties` file:

```json
{
  "version": 1,
  "id": "props",
  "name": "Properties",
  "extensions": ["properties"],
  "lineComment": "#",
  "states": {
    "root": [
      { "match": "#.*", "scope": "comment.line" },
      { "match": "[A-Za-z0-9_.]+(?=\\s*=)", "scope": "variable.property" },
      { "match": "=", "scope": "operator" },
      { "words": ["true", "false"], "scope": "constant.language" },
      { "match": "\\d+", "scope": "number" }
    ]
  }
}
```

Load it and select it:

```java
LanguageTemplate template = TemplateLoader.load(Path.of("props.json"));
LanguageRegistry.getDefault().register(TemplateLanguages.fromTemplate(template));
editor.setLanguage("props");
```

`TemplateLoader.load(Path)` throws `IOException` if the file can't be read, and `TemplateException` (unchecked) if the template is wrong. To load one of the files in the jar, use `TemplateLanguages.fromResource("java.json")`.

Backslashes in a regex must be doubled inside JSON, so `\d` is written `\\d`.

## Top-level fields

Unknown fields are errors, at every level.

| Field | Required | Meaning |
|---|---|---|
| `version` | yes | Must be the integer `1`. |
| `id` | yes | The name you pass to `setLanguage`. Trimmed and lowercased. |
| `name` | no | Display name. Defaults to the id. |
| `extensions` | no | File extensions without the dot, such as `["js", "mjs"]`. Lowercased. `LanguageRegistry.findByExtension` uses them. |
| `start` | no | The state the tokenizer starts in. Defaults to `root`. |
| `autoClosingPairs` | no | See below. |
| `lineComment` | no | The prefix Ctrl+/ toggles, such as `"//"` or `"#"`. Trimmed. Must be a single line. Without it, Ctrl+/ does nothing. |
| `formatter` | no | See below. |
| `states` | yes | An object of named states. At least one. |

## How coloring works

The editor colors one line at a time. Each line starts in the state the previous line ended in. Inside a state, the rules are tried in the order you wrote them, and the first rule that matches at the current position wins. Text that no rule matches is left uncolored, and the tokenizer skips a whole word or a single character before trying again.

A state is an array of rules. Every rule has exactly one of `match`, `words`, `begin` or `eol`.

| Kind | Needs | Can also have | Can't have |
|---|---|---|---|
| `match` | `match`, `scope` | `next`, `push` or `pop` | `end`, `caseSensitive` |
| `words` | `words`, `scope` | `caseSensitive` | `end`, `next`, `push`, `pop` |
| `begin` | `begin`, `end`, `scope` | nothing | `next`, `push`, `pop`, `caseSensitive` |
| `eol` | `"eol": true` and one of `next`, `push`, `pop` | nothing | `scope`, `end`, `caseSensitive` |

**match** takes a Java regular expression. It has to match starting exactly at the current position, and it can't match an empty string. `^` means the start of the line. A lookbehind such as `(?<!\\\\)` can see the text before the current position.

**words** takes a list of words with no whitespace. A word matches only as a whole word. A word is a run of letters, digits, `_` and `$`, so `if` doesn't match inside `iffy`. With `"caseSensitive": false`, the words are stored in lowercase and matched ignoring case. The default is case-sensitive.

**begin** and **end** color a stretch of text that may run over several lines, such as a block comment. The `begin` regex has to match at the current position and can't be empty. The `end` regex is searched from there to the end of the line, and it may match an empty string. If no end is found, the rest of the line gets the scope, and the next line starts inside the block.

**eol** runs when a line has been used up completely, including an empty line. It changes the state without coloring anything.

## State changes

A `match` rule can switch states after it matches, and an `eol` rule always does. The tokenizer keeps a stack of states.

- `"next": "name"` replaces the current state with `name`.
- `"push": "name"` enters `name` and remembers where it came from.
- `"pop": true` goes back to the state below.

Pushing is how you handle a string that has its own rules inside it:

```json
"states": {
  "root": [
    { "match": "\"", "scope": "string", "push": "string" }
  ],
  "string": [
    { "match": "\\\\.", "scope": "string.escape" },
    { "match": "\"", "scope": "string", "pop": true },
    { "match": "[^\"\\\\]+", "scope": "string" },
    { "eol": true, "pop": true }
  ]
}
```

The last rule ends an unterminated string at the end of the line. Without it, the string state would carry on into the next line. A `pop` on the bottom state and a `next` to the same state are ignored.

Every `next` and `push` has to name a state that exists. So does `start`.

## Scopes and colors

`scope` is a dotted name such as `comment.line` or `keyword.control`. The editor turns it into style classes by adding each prefix: `comment.line` becomes `tok-comment` and `tok-comment-line`. The built-in themes color these first parts:

`keyword`, `keyword.control`, `string`, `string.escape`, `number`, `comment`, `type`, `function`, `variable`, `constant`, `operator`, `punctuation`, `regexp`, `tag`, `attribute`, `invalid`.

So `variable.property` gets the color of `variable`. A theme can color the longer name separately. A scope the themes don't know is still valid. It just gets the normal text color.

Scope names matter beyond color. Bracket matching, folding and autocomplete's automatic popup treat text with a `string` or `comment` scope as non-code. The brace formatter does the same and also ignores `regexp`. If your block comments are called `note`, a `{` inside one will still fold.

## Auto-closing pairs

`autoClosingPairs` is a list of pairs, each an array of two one-character strings:

```json
"autoClosingPairs": [["(", ")"], ["[", "]"], ["{", "}"], ["\"", "\""]]
```

A quote uses the same character twice. An opening character can appear only once, and neither character can be whitespace. If you leave the field out, the pairs are `()`, `[]`, `{}`, `""` and `''`. An empty list, `[]`, turns auto-closing off for that language.

## Formatter

The `formatter` object selects what Shift+Alt+F runs for this language. `type` is required and is either `json` or `brace`.

```json
"formatter": { "type": "brace", "trimTrailingWhitespace": true, "insertFinalNewline": false }
```

**json** rewrites a valid JSON document with the editor's indent. It leaves the text alone if it is blank or has any syntax error. That includes comments, so a `.jsonc` file with comments isn't formatted. It takes no other options.

**brace** re-indents every line from the bracket nesting. It has these options, and none of them is allowed with `json`:

| Option | Default | Meaning |
|---|---|---|
| `brackets` | `["{}", "[]", "()"]` | Strings of exactly two characters, an opener and a closer. The characters must differ, and no opener or closer may repeat. |
| `trimTrailingWhitespace` | `true` | Removes trailing spaces from lines that don't end inside a multi-line token. |
| `insertFinalNewline` | `false` | Adds a newline at the end of the text if it's missing. |

What the brace formatter does:

- Several openers on one line add a single level.
- A line that starts with closers is dedented first.
- Lines after a `case` or `default` label that ends with a colon go one level deeper, until the next label.
- A line that continues the one above gets one more level. That means the line above ends with an operator (`+ - * / % = & | ^ ? .` or `=>`, but not `++` or `--`), or this line starts with `.name`, `?`, `*`, `%`, `^`, `=`, `&`, `|`, or `:`, `+`, `-` followed by a space.
- Lines that start inside a multi-line string or comment, or inside a pushed state, are not touched.
- Line endings stay as they are, so CRLF files stay CRLF.

It does nothing for text longer than 1,000,000 characters, or when any line is longer than 10,000 characters.

The formatter reads the language's own scopes, which is why correct `comment`, `string` and `regexp` names matter.

## Autocomplete keywords

Every word in every `words` rule, in every state, becomes a keyword in the completion list. A rule with `caseSensitive: false` offers its words in lowercase. Words you don't want suggested should go in a `match` rule instead.

## Limits

| Limit | Value | What happens |
|---|---|---|
| Line length | 10,000 characters | The line gets no colors and its state is unchanged. |
| Time per line | 50 ms | The line gets no colors and its state is unchanged. |
| State stack | 64 deep | Further `push` rules are ignored. |
| `eol` steps | 64 per line | The line stops changing states. |

A regex that backtracks badly will hit the 50 ms limit. Possessive quantifiers such as `*+` and `++` avoid most of that. The built-in templates use them.

## Registering languages

```java
LanguageRegistry registry = LanguageRegistry.getDefault();
registry.register(language);
registry.find("props");
registry.findByExtension("properties");
registry.ids();
```

Registering an id that already exists replaces the old language. `editor.setLanguage` throws `IllegalArgumentException` for an unknown id and lists the registered ones.

## Error messages

A bad template throws `TemplateException`. The message always has three parts: the source, the field, and the reason. The source is the file path, or the resource path for a built-in template.

```
props.json: version: unsupported value 2, expected 1
props.json: states.root[1].match: regex must not match an empty string
props.json: states.root[2]: needs exactly one of 'match', 'words', 'begin' or 'eol'
props.json: states.root[3].scoop: unknown field
props.json: states.root[4].end: is not allowed in match rules
props.json: states.root[0].push: unknown state 'strng'. Defined states: [root, string]
props.json: formatter.type: unknown formatter type 'xml'. Supported types: brace, json
props.json: json: Unexpected end of input in object (line 9, column 1)
```

The template has to be strict JSON. Comments, trailing commas and duplicate keys are errors.