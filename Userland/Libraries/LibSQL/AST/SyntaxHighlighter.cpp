/*
 * Copyright (c) 2021, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibGfx/Palette.h>
#include <LibSQL/AST/Lexer.h>
#include <LibSQL/AST/SyntaxHighlighter.h>

namespace SQL::AST {

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

bool SyntaxHighlighter::is_identifier(u64 token) const
{
    auto sql_token = static_cast<TokenType>(static_cast<size_t>(token));
    return sql_token == TokenType::Identifier;
}

void SyntaxHighlighter::rehighlight(Palette const& palette)
{
    auto text = m_client->get_text();

    Lexer lexer(text);

    Vector<GUI::TextDocumentSpan> spans;

    auto append_token = [&](Token const& token) {
        if (token.value().is_empty())
            return;
        GUI::TextDocumentSpan span;
        span.range.set_start({ token.start_position().line - 1, token.start_position().column - 1 });
        span.range.set_end({ token.end_position().line - 1, token.end_position().column - 1 });
        auto style = style_for_token_type(palette, token.type());
        span.attributes.color = style.color;
        span.attributes.bold = style.bold;
        span.data = static_cast<u64>(token.type());
        spans.append(span);

        dbgln_if(SYNTAX_HIGHLIGHTING_DEBUG, "{} @ '{}' {}:{} - {}:{}",
            token.name(),
            token.value(),
            span.range.start().line(), span.range.start().column(),
            span.range.end().line(), span.range.end().column());
    };

    for (;;) {
        auto token = lexer.next();
        append_token(token);
        if (token.type() == TokenType::Eof)
            break;
    }

    m_client->do_set_spans(move(spans));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<SyntaxHighlighter::MatchingTokenPair> SyntaxHighlighter::matching_token_pairs_impl() const
{
    static Vector<SyntaxHighlighter::MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ static_cast<u64>(TokenType::ParenOpen), static_cast<u64>(TokenType::ParenClose) });
    }
    return pairs;
}

bool SyntaxHighlighter::token_types_equal(u64 token1, u64 token2) const
{
    return static_cast<TokenType>(token1) == static_cast<TokenType>(token2);
}

SyntaxHighlighter::~SyntaxHighlighter()
{
}

}
