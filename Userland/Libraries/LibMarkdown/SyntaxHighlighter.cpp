/*
 * Copyright (c) 2023, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibMarkdown/SyntaxHighlighter.h>

namespace Markdown {

Syntax::Language SyntaxHighlighter::language() const
{
    return Syntax::Language::Markdown;
}

Optional<StringView> SyntaxHighlighter::comment_prefix() const
{
    return {};
}

Optional<StringView> SyntaxHighlighter::comment_suffix() const
{
    return {};
}

enum class Token {
    Default,
    Header,
    Code
};

void SyntaxHighlighter::rehighlight(Palette const& palette)
{
    auto text = m_client->get_text();

    Vector<Syntax::TextDocumentSpan> spans;

    auto append_header = [&](Syntax::TextRange const& range) {
        Gfx::TextAttributes attributes;
        attributes.color = palette.base_text();
        attributes.bold = true;
        Syntax::TextDocumentSpan span {
            .range = range,
            .attributes = attributes,
            .data = static_cast<u32>(Token::Header),
            .is_skippable = false
        };
        spans.append(span);
    };

    auto append_code_block = [&](Syntax::TextRange const& range) {
        Gfx::TextAttributes attributes;
        attributes.color = palette.syntax_string();
        Syntax::TextDocumentSpan span {
            .range = range,
            .attributes = attributes,
            .data = static_cast<u32>(Token::Code),
            .is_skippable = false
        };
        spans.append(span);
    };

    // Headers, code blocks
    {
        size_t line_index = 0;
        Optional<size_t> code_block_start;
        for (auto const& line : StringView(text).lines()) {
            if (line.starts_with("```"sv)) {
                if (code_block_start.has_value()) {
                    append_code_block({ { *code_block_start, 0 }, { line_index, line.length() } });
                    code_block_start = {};
                } else {
                    code_block_start = line_index;
                }
            }

            if (!code_block_start.has_value()) {
                auto trimmed = line.trim_whitespace(TrimMode::Left);
                size_t indent = line.length() - trimmed.length();
                if (indent < 4 && trimmed.starts_with("#"sv)) {
                    append_header({ { line_index, 0 }, { line_index, line.length() } });
                }
            }
            line_index++;
        }
    }

    // TODO: Highlight text nodes (em, strong, link, image)

    m_client->do_set_spans(spans);
}

Vector<SyntaxHighlighter::MatchingTokenPair> SyntaxHighlighter::matching_token_pairs_impl() const
{
    return {};
}

bool SyntaxHighlighter::token_types_equal(u64 lhs, u64 rhs) const
{
    return static_cast<Token>(lhs) == static_cast<Token>(rhs);
}

}
