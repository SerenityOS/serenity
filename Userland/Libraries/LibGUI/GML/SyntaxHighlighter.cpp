/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SyntaxHighlighter.h"
#include "Lexer.h"
#include <LibGfx/Palette.h>

namespace GUI::GML {

static Gfx::TextAttributes style_for_token_type(Gfx::Palette const& palette, Token::Type type)
{
    switch (type) {
    case Token::Type::LeftCurly:
    case Token::Type::RightCurly:
        return { palette.syntax_punctuation() };
    case Token::Type::ClassMarker:
        return { palette.syntax_keyword() };
    case Token::Type::ClassName:
        return { palette.syntax_identifier(), {}, true };
    case Token::Type::Identifier:
        return { palette.syntax_identifier() };
    case Token::Type::JsonValue:
        return { palette.syntax_string() };
    case Token::Type::Comment:
        return { palette.syntax_comment() };
    default:
        return { palette.base_text() };
    }
}

bool SyntaxHighlighter::is_identifier(u64 token) const
{
    auto ini_token = static_cast<Token::Type>(token);
    return ini_token == Token::Type::Identifier;
}

void SyntaxHighlighter::rehighlight(Palette const& palette)
{
    auto text = m_client->get_text();
    Lexer lexer(text);
    auto tokens = lexer.lex();

    Vector<Token> folding_region_start_tokens;

    Vector<Syntax::TextDocumentSpan> spans;
    Vector<Syntax::TextDocumentFoldingRegion> folding_regions;

    for (auto& token : tokens) {
        Syntax::TextDocumentSpan span;
        span.range.set_start({ token.m_start.line, token.m_start.column });
        span.range.set_end({ token.m_end.line, token.m_end.column });
        span.attributes = style_for_token_type(palette, token.m_type);
        span.is_skippable = false;
        span.data = static_cast<u64>(token.m_type);
        spans.append(span);

        // Create folding regions for {} blocks
        if (token.m_type == Token::Type::LeftCurly) {
            folding_region_start_tokens.append(token);
        } else if (token.m_type == Token::Type::RightCurly) {
            if (!folding_region_start_tokens.is_empty()) {
                auto left_curly = folding_region_start_tokens.take_last();
                Syntax::TextDocumentFoldingRegion region;
                region.range.set_start({ left_curly.m_end.line, left_curly.m_end.column });
                region.range.set_end({ token.m_start.line, token.m_start.column });
                folding_regions.append(region);
            }
        }
    }
    m_client->do_set_spans(move(spans));
    m_client->do_set_folding_regions(move(folding_regions));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<SyntaxHighlighter::MatchingTokenPair> SyntaxHighlighter::matching_token_pairs_impl() const
{
    static Vector<MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ static_cast<u64>(Token::Type::LeftCurly), static_cast<u64>(Token::Type::RightCurly) });
    }
    return pairs;
}

bool SyntaxHighlighter::token_types_equal(u64 token1, u64 token2) const
{
    return static_cast<Token::Type>(token1) == static_cast<Token::Type>(token2);
}

}
