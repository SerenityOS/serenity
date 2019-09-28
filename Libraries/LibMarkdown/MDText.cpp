#include <AK/StringBuilder.h>
#include <LibMarkdown/MDText.h>

static String unescape(const StringView& text)
{
    StringBuilder builder;
    for (int i = 0; i < text.length(); ++i) {
        if (text[i] == '\\' && i != text.length() - 1) {
            builder.append(text[i + 1]);
            i++;
            continue;
        }
        builder.append(text[i]);
    }
    return builder.build();
}

String MDText::render_to_html() const
{
    StringBuilder builder;

    Vector<String> open_tags;
    Style current_style;

    for (auto& span : m_spans) {
        struct TagAndFlag {
            String tag;
            bool Style::*flag;
        };
        TagAndFlag tags_and_flags[] = {
            { "i", &Style::emph },
            { "b", &Style::strong },
            { "code", &Style::code }
        };
        auto it = open_tags.find([&](const String& open_tag) {
            for (auto& tag_and_flag : tags_and_flags) {
                if (open_tag == tag_and_flag.tag && !(span.style.*tag_and_flag.flag))
                    return true;
            }
            return false;
        });

        if (!it.is_end()) {
            // We found an open tag that should
            // not be open for the new span. Close
            // it and all the open tags that follow
            // it.
            for (auto it2 = --open_tags.end(); it2 >= it; --it2) {
                const String& tag = *it2;
                builder.appendf("</%s>", tag.characters());
                for (auto& tag_and_flag : tags_and_flags)
                    if (tag == tag_and_flag.tag)
                        current_style.*tag_and_flag.flag = false;
            }
            open_tags.shrink(it.index());
        }
        for (auto& tag_and_flag : tags_and_flags) {
            if (current_style.*tag_and_flag.flag != span.style.*tag_and_flag.flag) {
                open_tags.append(tag_and_flag.tag);
                builder.appendf("<%s>", tag_and_flag.tag.characters());
            }
        }

        current_style = span.style;
        builder.append(span.text);
    }

    for (auto it = --open_tags.end(); it >= open_tags.begin(); --it) {
        const String& tag = *it;
        builder.appendf("</%s>", tag.characters());
    }

    return builder.build();
}

String MDText::render_for_terminal() const
{
    StringBuilder builder;

    for (auto& span : m_spans) {
        bool needs_styling = span.style.strong || span.style.emph || span.style.code;
        if (needs_styling) {
            builder.append("\033[");
            bool first = true;
            if (span.style.strong || span.style.code) {
                builder.append('1');
                first = false;
            }
            if (span.style.emph) {
                if (!first)
                    builder.append(';');
                builder.append('4');
            }
            builder.append('m');
        }

        builder.append(span.text.characters());

        if (needs_styling)
            builder.append("\033[0m");
    }

    return builder.build();
}

bool MDText::parse(const StringView& str)
{
    Style current_style;
    int current_span_start = 0;

    for (int offset = 0; offset < str.length(); offset++) {
        char ch = str[offset];

        bool is_escape = ch == '\\';
        if (is_escape && offset != str.length() - 1) {
            offset++;
            continue;
        }

        bool is_special_character = false;
        is_special_character |= ch == '`';
        if (!current_style.code)
            is_special_character |= ch == '*' || ch == '_';
        if (!is_special_character)
            continue;

        if (current_span_start != offset) {
            Span span {
                str.substring_view(current_span_start, offset - current_span_start),
                current_style
            };
            m_spans.append(move(span));
        }

        if (ch == '`') {
            current_style.code = !current_style.code;
        } else {
            if (offset + 1 < str.length() && str[offset + 1] == ch) {
                offset++;
                current_style.strong = !current_style.strong;
            } else {
                current_style.emph = !current_style.emph;
            }
        }

        current_span_start = offset + 1;
    }

    if (current_span_start < str.length()) {
        Span span {
            unescape(str.substring_view(current_span_start, str.length() - current_span_start)),
            current_style
        };
        m_spans.append(move(span));
    }

    return true;
}
