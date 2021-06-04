/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibWeb/HTML/Parser/HTMLTokenizer.h>
#include <LibWeb/HTML/SyntaxHighlighter/SyntaxHighlighter.h>

namespace Web::HTML {

enum class AugmentedTokenKind : u32 {
    AttributeName,
    AttributeValue,
    OpenTag,
    CloseTag,
    Comment,
    Doctype,
};

bool SyntaxHighlighter::is_identifier(void* token) const
{
    if (!token)
        return false;
    return false;
}

bool SyntaxHighlighter::is_navigatable(void*) const
{
    return false;
}

void SyntaxHighlighter::rehighlight(Palette const& palette)
{
    dbgln_if(SYNTAX_HIGHLIGHTING_DEBUG, "(HTML::SyntaxHighlighter) starting rehighlight");
    (void)palette;
    auto text = m_client->get_text();

    Vector<GUI::TextDocumentSpan> spans;
    auto highlight = [&](auto start_line, auto start_column, auto end_line, auto end_column, Gfx::TextAttributes attributes, AugmentedTokenKind kind) {
        if (start_line > end_line || (start_line == end_line && start_column >= end_column)) {
            dbgln_if(SYNTAX_HIGHLIGHTING_DEBUG, "(HTML::SyntaxHighlighter) discarding ({}-{}) to ({}-{}) because it has zero or negative length", start_line, start_column, end_line, end_column);
            return;
        }
        dbgln_if(SYNTAX_HIGHLIGHTING_DEBUG, "(HTML::SyntaxHighlighter) highlighting ({}-{}) to ({}-{}) with color {}", start_line, start_column, end_line, end_column, attributes.color);
        spans.empend(
            GUI::TextRange {
                { start_line, start_column },
                { end_line, end_column },
            },
            move(attributes),
            (void*)kind,
            false);
    };

    HTMLTokenizer tokenizer { text, "utf-8" };
    [[maybe_unused]] enum class State {
        HTML,
        Javascript,
        CSS,
    } state { State::HTML };
    for (;;) {
        auto token = tokenizer.next_token();
        if (!token.has_value() || token.value().is_end_of_file())
            break;
        dbgln_if(SYNTAX_HIGHLIGHTING_DEBUG, "(HTML::SyntaxHighlighter) got token of type {}", token->to_string());

        if (token->is_start_tag()) {
            if (token->tag_name() == "script"sv) {
                tokenizer.switch_to(HTMLTokenizer::State::ScriptData);
                state = State::Javascript;
            } else if (token->tag_name() == "style"sv) {
                tokenizer.switch_to(HTMLTokenizer::State::RAWTEXT);
                state = State::CSS;
            }
        } else if (token->is_end_tag()) {
            if (token->tag_name().is_one_of("script"sv, "style"sv)) {
                if (state == State::Javascript) {
                    // FIXME: Highlight javascript code here instead.
                } else if (state == State::CSS) {
                    // FIXME: Highlight CSS code here instead.
                }
                state = State::HTML;
            }
        }

        size_t token_start_offset = token->is_end_tag() ? 1 : 0;

        if (token->is_comment()) {
            highlight(
                token->start_position().line,
                token->start_position().column,
                token->end_position().line,
                token->end_position().column,
                { palette.syntax_comment(), {} },
                AugmentedTokenKind::Comment);
        } else if (token->is_start_tag() || token->is_end_tag()) {
            highlight(
                token->start_position().line,
                token->start_position().column + token_start_offset,
                token->start_position().line,
                token->start_position().column + token_start_offset + token->tag_name().length(),
                { palette.syntax_keyword(), {}, false, true },
                token->is_start_tag() ? AugmentedTokenKind::OpenTag : AugmentedTokenKind::CloseTag);

            for (auto& attribute : token->attributes()) {
                highlight(
                    attribute.name_start_position.line,
                    attribute.name_start_position.column + token_start_offset,
                    attribute.name_end_position.line,
                    attribute.name_end_position.column + token_start_offset,
                    { palette.syntax_identifier(), {} },
                    AugmentedTokenKind::AttributeName);
                highlight(
                    attribute.value_start_position.line,
                    attribute.value_start_position.column + token_start_offset,
                    attribute.value_end_position.line,
                    attribute.value_end_position.column + token_start_offset,
                    { palette.syntax_string(), {} },
                    AugmentedTokenKind::AttributeValue);
            }
        } else if (token->is_doctype()) {
            highlight(
                token->start_position().line,
                token->start_position().column,
                token->start_position().line,
                token->start_position().column,
                { palette.syntax_preprocessor_statement(), {} },
                AugmentedTokenKind::Doctype);
        }
    }

    if constexpr (SYNTAX_HIGHLIGHTING_DEBUG) {
        dbgln("(HTML::SyntaxHighlighter) list of all spans:");
        for (auto& span : spans)
            dbgln("{}, {}", span.range, span.attributes.color);
        dbgln("(HTML::SyntaxHighlighter) end of list");
    }

    m_client->do_set_spans(move(spans));
    m_has_brace_buddies = false;
    highlight_matching_token_pair();
    m_client->do_update();
}

Vector<Syntax::Highlighter::MatchingTokenPair> SyntaxHighlighter::matching_token_pairs() const
{
    static Vector<MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ (void*)AugmentedTokenKind::OpenTag, (void*)AugmentedTokenKind::CloseTag });
    }
    return pairs;
}

bool SyntaxHighlighter::token_types_equal(void* token0, void* token1) const
{
    return token0 == token1;
}

}
