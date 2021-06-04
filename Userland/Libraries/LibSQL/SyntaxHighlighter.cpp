/*
 * Copyright (c) 2021, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibGfx/Palette.h>
#include <LibSQL/Lexer.h>
#include <LibSQL/SyntaxHighlighter.h>

namespace SQL {

static Syntax::TextStyle style_for_token_type(Gfx::Palette const& palette, TokenType type)
{
    switch (Token::category(type)) {
    case TokenCategory::Keyword:
        return { palette.syntax_keyword(), true };
    case TokenCategory::Identifier:
        return { palette.syntax_identifier(), false };
    case TokenCategory::Number:
        return { palette.syntax_number(), false };
    case TokenCategory::Blob:
    case TokenCategory::String:
        return { palette.syntax_string(), false };
    case TokenCategory::Operator:
        return { palette.syntax_operator(), false };
    case TokenCategory::Punctuation:
        return { palette.syntax_punctuation(), false };
    case TokenCategory::Invalid:
    default:
        return { palette.base_text(), false };
    }
}

bool SyntaxHighlighter::is_identifier(void* token) const
{
    auto sql_token = static_cast<SQL::TokenType>(reinterpret_cast<size_t>(token));
    return sql_token == SQL::TokenType::Identifier;
}

void SyntaxHighlighter::rehighlight(Palette const& palette)
{
    auto text = m_client->get_text();

    SQL::Lexer lexer(text);

    Vector<GUI::TextDocumentSpan> spans;

    auto append_token = [&](StringView str, SQL::Token const& token) {
        if (str.is_empty())
            return;

        GUI::TextPosition position { token.line_number() - 1, token.line_column() - 1 };
        for (char c : str) {
            if (c == '\n') {
                position.set_line(position.line() + 1);
                position.set_column(0);
            } else
                position.set_column(position.column() + 1);
        }

        GUI::TextDocumentSpan span;
        span.range.set_start({ token.line_number() - 1, token.line_column() - 1 });
        span.range.set_end({ position.line(), position.column() });
        auto style = style_for_token_type(palette, token.type());
        span.attributes.color = style.color;
        span.attributes.bold = style.bold;
        span.data = reinterpret_cast<void*>(static_cast<size_t>(token.type()));
        spans.append(span);

        dbgln_if(SYNTAX_HIGHLIGHTING_DEBUG, "{} @ '{}' {}:{} - {}:{}",
            token.name(),
            token.value(),
            span.range.start().line(), span.range.start().column(),
            span.range.end().line(), span.range.end().column());
    };

    for (;;) {
        auto token = lexer.next();
        append_token(token.value(), token);
        if (token.type() == SQL::TokenType::Eof)
            break;
    }

    m_client->do_set_spans(move(spans));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<SyntaxHighlighter::MatchingTokenPair> SyntaxHighlighter::matching_token_pairs() const
{
    static Vector<SyntaxHighlighter::MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ reinterpret_cast<void*>(TokenType::ParenOpen), reinterpret_cast<void*>(TokenType::ParenClose) });
    }
    return pairs;
}

bool SyntaxHighlighter::token_types_equal(void* token1, void* token2) const
{
    return static_cast<TokenType>(reinterpret_cast<size_t>(token1)) == static_cast<TokenType>(reinterpret_cast<size_t>(token2));
}

SyntaxHighlighter::~SyntaxHighlighter()
{
}

}
