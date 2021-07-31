/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/GMLLexer.h>
#include <LibGUI/GMLSyntaxHighlighter.h>
#include <LibGfx/Palette.h>

namespace GUI {

static Syntax::TextStyle style_for_token_type(const Gfx::Palette& palette, GMLToken::Type type)
{
    switch (type) {
    case GMLToken::Type::LeftCurly:
    case GMLToken::Type::RightCurly:
        return { palette.syntax_punctuation() };
    case GMLToken::Type::ClassMarker:
        return { palette.syntax_keyword() };
    case GMLToken::Type::ClassName:
        return { palette.syntax_identifier(), true };
    case GMLToken::Type::Identifier:
        return { palette.syntax_identifier() };
    case GMLToken::Type::JsonValue:
        return { palette.syntax_string() };
    case GMLToken::Type::Comment:
        return { palette.syntax_comment() };
    default:
        return { palette.base_text() };
    }
}

bool GMLSyntaxHighlighter::is_identifier(u64 token) const
{
    auto ini_token = static_cast<GUI::GMLToken::Type>(token);
    return ini_token == GUI::GMLToken::Type::Identifier;
}

void GMLSyntaxHighlighter::rehighlight(const Palette& palette)
{
    auto text = m_client->get_text();
    GMLLexer lexer(text);
    auto tokens = lexer.lex();

    Vector<GUI::TextDocumentSpan> spans;
    for (auto& token : tokens) {
        GUI::TextDocumentSpan span;
        span.range.set_start({ token.m_start.line, token.m_start.column });
        span.range.set_end({ token.m_end.line, token.m_end.column });
        auto style = style_for_token_type(palette, token.m_type);
        span.attributes.color = style.color;
        span.attributes.bold = style.bold;
        span.is_skippable = false;
        span.data = static_cast<u64>(token.m_type);
        spans.append(span);
    }
    m_client->do_set_spans(move(spans));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<GMLSyntaxHighlighter::MatchingTokenPair> GMLSyntaxHighlighter::matching_token_pairs_impl() const
{
    static Vector<MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ static_cast<u64>(GMLToken::Type::LeftCurly), static_cast<u64>(GMLToken::Type::RightCurly) });
    }
    return pairs;
}

bool GMLSyntaxHighlighter::token_types_equal(u64 token1, u64 token2) const
{
    return static_cast<GUI::GMLToken::Type>(token1) == static_cast<GUI::GMLToken::Type>(token2);
}

GMLSyntaxHighlighter::~GMLSyntaxHighlighter()
{
}

}
