/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SyntaxHighlighter.h"
#include <LibCMake/CMakeCache/Lexer.h>

namespace CMake::Cache {

static Gfx::TextAttributes style_for_token_type(Gfx::Palette const& palette, Token::Type type)
{
    switch (type) {
    case Token::Type::Comment:
    case Token::Type::HelpText:
        return { palette.syntax_comment() };
    case Token::Type::Key:
        return { palette.syntax_identifier() };
    case Token::Type::Type:
        return { palette.syntax_type() };
    case Token::Type::Colon:
    case Token::Type::Equals:
        return { palette.syntax_punctuation() };
    case Token::Type::Value:
        return { palette.syntax_string() };
    case Token::Type::Garbage:
        return { palette.red(), {}, false, Gfx::TextAttributes::UnderlineStyle::Wavy, palette.red() };
    default:
        return { palette.base_text() };
    }
}

bool SyntaxHighlighter::is_identifier(u64 token_type) const
{
    auto cmake_token = static_cast<Token::Type>(token_type);
    return cmake_token == Token::Type::Key;
}

void SyntaxHighlighter::rehighlight(Gfx::Palette const& palette)
{
    auto text = m_client->get_text();
    auto tokens = Lexer::lex(text).release_value_but_fixme_should_propagate_errors();

    Vector<Syntax::TextDocumentSpan> spans;
    auto highlight_span = [&](Token::Type type, Position const& start, Position const& end) {
        Syntax::TextDocumentSpan span;
        span.range.set_start({ start.line, start.column });
        span.range.set_end({ end.line, end.column });
        if (!span.range.is_valid())
            return;

        span.attributes = style_for_token_type(palette, type);
        span.is_skippable = false;
        span.data = static_cast<u64>(type);
        spans.append(move(span));
    };

    for (auto const& token : tokens) {
        highlight_span(token.type, token.start, token.end);
    }

    m_client->do_set_spans(move(spans));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<SyntaxHighlighter::MatchingTokenPair> SyntaxHighlighter::matching_token_pairs_impl() const
{
    static Vector<MatchingTokenPair> empty;
    return empty;
}

bool SyntaxHighlighter::token_types_equal(u64 token1, u64 token2) const
{
    return static_cast<Token::Type>(token1) == static_cast<Token::Type>(token2);
}

}
