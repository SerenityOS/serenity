/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BasicSExpressionLexer.h>
#include <LibGUI/BasicSExpressionSyntaxHighlighter.h>
#include <LibGfx/Palette.h>

namespace GUI {

static Syntax::TextStyle style_for_token_type(const Gfx::Palette& palette, BasicSExpressionToken::Type type)
{
    switch (type) {
    case BasicSExpressionToken::Type::OpenParen:
    case BasicSExpressionToken::Type::OpenBrace:
    case BasicSExpressionToken::Type::OpenBracket:
    case BasicSExpressionToken::Type::CloseParen:
    case BasicSExpressionToken::Type::CloseBrace:
    case BasicSExpressionToken::Type::CloseBracket:
        return { palette.syntax_punctuation(), false };
    case BasicSExpressionToken::Type::Number:
        return { palette.syntax_number(), false };
    case BasicSExpressionToken::Type::Word:
        return { palette.syntax_identifier(), false };
    case BasicSExpressionToken::Type::FormName:
        return { palette.syntax_identifier(), true };
    case BasicSExpressionToken::Type::SingleQuotedString:
    case BasicSExpressionToken::Type::DoubleQuotedString:
        return { palette.syntax_string(), false };
    case BasicSExpressionToken::Type::Comment:
        return { palette.syntax_comment(), false };
    case BasicSExpressionToken::Type::Unknown:
        return { palette.syntax_punctuation(), false };
    }

    VERIFY_NOT_REACHED();
}

bool BasicSExpressionSyntaxHighlighter::is_identifier(u64 token) const
{
    return static_cast<GUI::BasicSExpressionToken::Type>(token) == GUI::BasicSExpressionToken::Type::Word;
}

void BasicSExpressionSyntaxHighlighter::rehighlight(const Palette& palette)
{
    auto text = m_client->get_text();
    BasicSExpressionLexer lexer(text);
    auto tokens = lexer.lex();

    Vector<GUI::TextDocumentSpan> spans;
    for (auto& token : tokens) {
        GUI::TextDocumentSpan span;
        span.range.set_start({ token.m_start.line, token.m_start.column });
        span.range.set_end({ token.m_end.line, token.m_end.column });
        auto style = style_for_token_type(palette, token.m_type);
        span.attributes.color = style.color;
        span.attributes.bold = style.bold;
        span.is_skippable = token.m_type != BasicSExpressionToken::Type::Word && token.m_type != BasicSExpressionToken::Type::Number;
        span.data = static_cast<u64>(token.m_type);
        spans.append(span);
    }
    m_client->do_set_spans(move(spans));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<BasicSExpressionSyntaxHighlighter::MatchingTokenPair> BasicSExpressionSyntaxHighlighter::matching_token_pairs_impl() const
{
    static Vector<MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ static_cast<u64>(BasicSExpressionToken::Type::OpenParen), static_cast<u64>(BasicSExpressionToken::Type::CloseParen) });
        pairs.append({ static_cast<u64>(BasicSExpressionToken::Type::OpenBrace), static_cast<u64>(BasicSExpressionToken::Type::CloseBrace) });
        pairs.append({ static_cast<u64>(BasicSExpressionToken::Type::OpenBracket), static_cast<u64>(BasicSExpressionToken::Type::CloseBracket) });
    }
    return pairs;
}

bool BasicSExpressionSyntaxHighlighter::token_types_equal(u64 token1, u64 token2) const
{
    return static_cast<GUI::BasicSExpressionToken::Type>(token1) == static_cast<GUI::BasicSExpressionToken::Type>(token2);
}

BasicSExpressionSyntaxHighlighter::~BasicSExpressionSyntaxHighlighter()
{
}

}
