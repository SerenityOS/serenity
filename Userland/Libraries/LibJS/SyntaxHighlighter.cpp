/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibJS/Lexer.h>
#include <LibJS/SyntaxHighlighter.h>
#include <LibJS/Token.h>

namespace JS {

static Syntax::TextStyle style_for_token_type(const Gfx::Palette& palette, JS::TokenType type)
{
    switch (JS::Token::category(type)) {
    case JS::TokenCategory::Invalid:
        return { palette.syntax_comment() };
    case JS::TokenCategory::Number:
        return { palette.syntax_number() };
    case JS::TokenCategory::String:
        return { palette.syntax_string() };
    case JS::TokenCategory::Punctuation:
        return { palette.syntax_punctuation() };
    case JS::TokenCategory::Operator:
        return { palette.syntax_operator() };
    case JS::TokenCategory::Keyword:
        return { palette.syntax_keyword(), true };
    case JS::TokenCategory::ControlKeyword:
        return { palette.syntax_control_keyword(), true };
    case JS::TokenCategory::Identifier:
        return { palette.syntax_identifier() };
    default:
        return { palette.base_text() };
    }
}

bool SyntaxHighlighter::is_identifier(void* token) const
{
    auto js_token = static_cast<JS::TokenType>(reinterpret_cast<size_t>(token));
    return js_token == JS::TokenType::Identifier;
}

bool SyntaxHighlighter::is_navigatable([[maybe_unused]] void* token) const
{
    return false;
}

void SyntaxHighlighter::rehighlight(const Palette& palette)
{
    auto text = m_client->get_text();

    JS::Lexer lexer(text);

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

    auto append_token = [&](StringView str, const JS::Token& token, bool is_trivia) {
        if (str.is_empty())
            return;

        start = position;
        for (size_t i = 0; i < str.length() - 1; ++i)
            advance_position(str[i]);

        GUI::TextDocumentSpan span;
        span.range.set_start(start);
        span.range.set_end({ position.line(), position.column() });
        auto type = is_trivia ? JS::TokenType::Invalid : token.type();
        auto style = style_for_token_type(palette, type);
        span.attributes.color = style.color;
        span.attributes.bold = style.bold;
        span.is_skippable = is_trivia;
        span.data = reinterpret_cast<void*>(static_cast<size_t>(type));
        spans.append(span);
        advance_position(str[str.length() - 1]);

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

        if (token.type() == JS::TokenType::Eof)
            was_eof = true;
    }

    m_client->do_set_spans(move(spans));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<Syntax::Highlighter::MatchingTokenPair> SyntaxHighlighter::matching_token_pairs() const
{
    static Vector<Syntax::Highlighter::MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ reinterpret_cast<void*>(JS::TokenType::CurlyOpen), reinterpret_cast<void*>(JS::TokenType::CurlyClose) });
        pairs.append({ reinterpret_cast<void*>(JS::TokenType::ParenOpen), reinterpret_cast<void*>(JS::TokenType::ParenClose) });
        pairs.append({ reinterpret_cast<void*>(JS::TokenType::BracketOpen), reinterpret_cast<void*>(JS::TokenType::BracketClose) });
    }
    return pairs;
}

bool SyntaxHighlighter::token_types_equal(void* token1, void* token2) const
{
    return static_cast<JS::TokenType>(reinterpret_cast<size_t>(token1)) == static_cast<JS::TokenType>(reinterpret_cast<size_t>(token2));
}

SyntaxHighlighter::~SyntaxHighlighter()
{
}

}
