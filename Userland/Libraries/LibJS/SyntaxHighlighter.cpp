/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibGfx/Palette.h>
#include <LibJS/Lexer.h>
#include <LibJS/SyntaxHighlighter.h>
#include <LibJS/Token.h>

namespace JS {

static Syntax::TextStyle style_for_token_type(Gfx::Palette const& palette, TokenType type)
{
    switch (Token::category(type)) {
    case TokenCategory::Invalid:
        return { palette.syntax_comment() };
    case TokenCategory::Number:
        return { palette.syntax_number() };
    case TokenCategory::String:
        return { palette.syntax_string() };
    case TokenCategory::Punctuation:
        return { palette.syntax_punctuation() };
    case TokenCategory::Operator:
        return { palette.syntax_operator() };
    case TokenCategory::Keyword:
        return { palette.syntax_keyword(), true };
    case TokenCategory::ControlKeyword:
        return { palette.syntax_control_keyword(), true };
    case TokenCategory::Identifier:
        return { palette.syntax_identifier() };
    default:
        return { palette.base_text() };
    }
}

bool SyntaxHighlighter::is_identifier(u64 token) const
{
    auto js_token = static_cast<TokenType>(static_cast<size_t>(token));
    return js_token == TokenType::Identifier;
}

bool SyntaxHighlighter::is_navigatable([[maybe_unused]] u64 token) const
{
    return false;
}

void SyntaxHighlighter::rehighlight(Palette const& palette)
{
    auto text = m_client->get_text();

    Lexer lexer(text);

    Vector<GUI::TextDocumentSpan> spans;
    GUI::TextPosition position { 0, 0 };
    GUI::TextPosition start { 0, 0 };

    auto advance_position = [&position](char ch) {
        if (ch == '\n') {
            position.set_line(position.line() + 1);
            position.set_column(0);
        } else
            position.set_column(position.column() + 1);
    };

    auto append_token = [&](StringView str, Token const& token, bool is_trivia) {
        if (str.is_empty())
            return;

        start = position;
        for (size_t i = 0; i < str.length(); ++i)
            advance_position(str[i]);

        GUI::TextDocumentSpan span;
        span.range.set_start(start);
        span.range.set_end({ position.line(), position.column() });
        auto type = is_trivia ? TokenType::Invalid : token.type();
        auto style = style_for_token_type(palette, type);
        span.attributes.color = style.color;
        span.attributes.bold = style.bold;
        span.is_skippable = is_trivia;
        span.data = static_cast<u64>(type);
        spans.append(span);

        dbgln_if(SYNTAX_HIGHLIGHTING_DEBUG, "{}{} @ '{}' {}:{} - {}:{}",
            token.name(),
            is_trivia ? " (trivia)" : "",
            token.value(),
            span.range.start().line(), span.range.start().column(),
            span.range.end().line(), span.range.end().column());
    };

    bool was_eof = false;
    for (auto token = lexer.next(); !was_eof; token = lexer.next()) {
        append_token(token.trivia(), token, true);
        append_token(token.value(), token, false);

        if (token.type() == TokenType::Eof)
            was_eof = true;
    }

    m_client->do_set_spans(move(spans));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<Syntax::Highlighter::MatchingTokenPair> SyntaxHighlighter::matching_token_pairs_impl() const
{
    static Vector<Syntax::Highlighter::MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ static_cast<u64>(TokenType::CurlyOpen), static_cast<u64>(TokenType::CurlyClose) });
        pairs.append({ static_cast<u64>(TokenType::ParenOpen), static_cast<u64>(TokenType::ParenClose) });
        pairs.append({ static_cast<u64>(TokenType::BracketOpen), static_cast<u64>(TokenType::BracketClose) });
    }
    return pairs;
}

bool SyntaxHighlighter::token_types_equal(u64 token1, u64 token2) const
{
    return static_cast<TokenType>(token1) == static_cast<TokenType>(token2);
}

}
