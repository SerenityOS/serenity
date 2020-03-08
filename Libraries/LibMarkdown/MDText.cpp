/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/StringBuilder.h>
#include <LibMarkdown/MDText.h>
#include <string.h>

static String unescape(const StringView& text)
{
    StringBuilder builder;
    for (size_t i = 0; i < text.length(); ++i) {
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
            if (open_tag == "a" && current_style.href != span.style.href)
                return true;
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
            for (ssize_t j = open_tags.size() - 1; j >= static_cast<ssize_t>(it.index()); --j) {
                auto& tag = open_tags[j];
                builder.appendf("</%s>", tag.characters());
                if (tag == "a") {
                    current_style.href = {};
                    continue;
                }
                for (auto& tag_and_flag : tags_and_flags)
                    if (tag == tag_and_flag.tag)
                        current_style.*tag_and_flag.flag = false;
            }
            open_tags.shrink(it.index());
        }
        if (current_style.href.is_null() && !span.style.href.is_null()) {
            open_tags.append("a");
            builder.appendf("<a href=\"%s\">", span.style.href.characters());
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

    for (ssize_t i = open_tags.size() - 1; i >= 0; --i) {
        auto& tag = open_tags[i];
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

        if (!span.style.href.is_null()) {
            // When rendering for the terminal, ignore any
            // non-absolute links, because the user has no
            // chance to follow them anyway.
            if (strstr(span.style.href.characters(), "://") != nullptr) {
                builder.appendf(" <%s>", span.style.href.characters());
            }
        }
    }

    return builder.build();
}

bool MDText::parse(const StringView& str)
{
    Style current_style;
    size_t current_span_start = 0;
    int first_span_in_the_current_link = -1;

    auto append_span_if_needed = [&](size_t offset) {
        if (current_span_start != offset) {
            Span span {
                unescape(str.substring_view(current_span_start, offset - current_span_start)),
                current_style
            };
            m_spans.append(move(span));
        }
    };

    for (size_t offset = 0; offset < str.length(); offset++) {
        char ch = str[offset];

        bool is_escape = ch == '\\';
        if (is_escape && offset != str.length() - 1) {
            offset++;
            continue;
        }

        bool is_special_character = false;
        is_special_character |= ch == '`';
        if (!current_style.code)
            is_special_character |= ch == '*' || ch == '_' || ch == '[' || ch == ']';
        if (!is_special_character)
            continue;

        append_span_if_needed(offset);

        switch (ch) {
        case '`':
            current_style.code = !current_style.code;
            break;
        case '*':
        case '_':
            if (offset + 1 < str.length() && str[offset + 1] == ch) {
                offset++;
                current_style.strong = !current_style.strong;
            } else {
                current_style.emph = !current_style.emph;
            }
            break;
        case '[':
            ASSERT(first_span_in_the_current_link == -1);
            first_span_in_the_current_link = m_spans.size();
            break;
        case ']': {
            ASSERT(first_span_in_the_current_link != -1);
            ASSERT(offset + 2 < str.length());
            offset++;
            ASSERT(str[offset] == '(');
            offset++;
            size_t start_of_href = offset;

            do
                offset++;
            while (offset < str.length() && str[offset] != ')');

            const StringView href = str.substring_view(start_of_href, offset - start_of_href);
            for (size_t i = first_span_in_the_current_link; i < m_spans.size(); i++)
                m_spans[i].style.href = href;
            first_span_in_the_current_link = -1;
            break;
        }
        default:
            ASSERT_NOT_REACHED();
        }

        current_span_start = offset + 1;
    }

    append_span_if_needed(str.length());

    return true;
}
