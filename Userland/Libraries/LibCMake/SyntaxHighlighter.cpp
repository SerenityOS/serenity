/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SyntaxHighlighter.h"
#include <LibCMake/Lexer.h>
#include <LibCMake/Token.h>

namespace CMake {

static Syntax::TextStyle style_for_token_type(Gfx::Palette const& palette, Token::Type type)
{
    switch (type) {
    case Token::Type::BracketComment:
    case Token::Type::LineComment:
        return { palette.syntax_comment() };
    case Token::Type::Identifier:
        return { palette.syntax_function() };
    case Token::Type::ControlKeyword:
        return { palette.syntax_control_keyword() };
    case Token::Type::OpenParen:
    case Token::Type::CloseParen:
        return { palette.syntax_punctuation() };
    case Token::Type::BracketArgument:
        return { palette.syntax_parameter() };
    case Token::Type::QuotedArgument:
        return { palette.syntax_string() };
    case Token::Type::UnquotedArgument:
        return { palette.syntax_parameter() };
    case Token::Type::Garbage:
        return { palette.red() };
    case Token::Type::VariableReference:
        // This is a bit arbitrary, since we don't have a color specifically for this.
        return { palette.syntax_preprocessor_value() };
    default:
        return { palette.base_text() };
    }
}

bool SyntaxHighlighter::is_identifier(u64 token_type) const
{
    auto cmake_token = static_cast<Token::Type>(token_type);
    return cmake_token == Token::Type::Identifier;
}

void SyntaxHighlighter::rehighlight(Gfx::Palette const& palette)
{
    auto text = m_client->get_text();
    auto tokens = Lexer::lex(text).release_value_but_fixme_should_propagate_errors();

    Vector<GUI::TextDocumentSpan> spans;
    auto highlight_span = [&](Token::Type type, Position const& start, Position const& end) {
        GUI::TextDocumentSpan span;
        span.range.set_start({ start.line, start.column });
        span.range.set_end({ end.line, end.column });
        if (!span.range.is_valid())
            return;

        auto style = style_for_token_type(palette, type);
        span.attributes.color = style.color;
        span.attributes.bold = style.bold;
        if (type == Token::Type::Garbage) {
            span.attributes.underline = true;
            span.attributes.underline_color = palette.red();
            span.attributes.underline_style = Gfx::TextAttributes::UnderlineStyle::Wavy;
        }
        span.is_skippable = false;
        span.data = static_cast<u64>(type);
        spans.append(move(span));
    };

    for (auto const& token : tokens) {
        if (token.type == Token::Type::QuotedArgument || token.type == Token::Type::UnquotedArgument) {
            // Alternately highlight the regular/variable-reference parts.
            // 0-length ranges are caught in highlight_span() so we don't have to worry about them.
            Position previous_position = token.start;
            for (auto const& reference : token.variable_references) {
                highlight_span(token.type, previous_position, reference.start);
                highlight_span(Token::Type::VariableReference, reference.start, reference.end);
                previous_position = reference.end;
            }
            highlight_span(token.type, previous_position, token.end);
            continue;
        }

        highlight_span(token.type, token.start, token.end);
    }
    m_client->do_set_spans(move(spans));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<SyntaxHighlighter::MatchingTokenPair> SyntaxHighlighter::matching_token_pairs_impl() const
{
    static Vector<MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ static_cast<u64>(Token::Type::OpenParen), static_cast<u64>(Token::Type::CloseParen) });
    }
    return pairs;
}

bool SyntaxHighlighter::token_types_equal(u64 token1, u64 token2) const
{
    return static_cast<Token::Type>(token1) == static_cast<Token::Type>(token2);
}

}
