/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/INILexer.h>
#include <LibGUI/INISyntaxHighlighter.h>
#include <LibGfx/Palette.h>

namespace GUI {

static Gfx::TextAttributes style_for_token_type(Gfx::Palette const& palette, IniToken::Type type)
{
    switch (type) {
    case IniToken::Type::LeftBracket:
    case IniToken::Type::RightBracket:
    case IniToken::Type::Section:
        return { palette.syntax_keyword(), {}, true };
    case IniToken::Type::Name:
        return { palette.syntax_identifier() };
    case IniToken::Type::Value:
        return { palette.syntax_string() };
    case IniToken::Type::Comment:
        return { palette.syntax_comment() };
    case IniToken::Type::Equal:
        return { palette.syntax_operator(), {}, true };
    default:
        return { palette.base_text() };
    }
}

bool IniSyntaxHighlighter::is_identifier(u64 token) const
{
    auto ini_token = static_cast<GUI::IniToken::Type>(token);
    return ini_token == GUI::IniToken::Type::Name;
}

void IniSyntaxHighlighter::rehighlight(Palette const& palette)
{
    auto text = m_client->get_text();
    IniLexer lexer(text);
    auto tokens = lexer.lex();

    Optional<IniToken> previous_section_token;
    IniToken previous_token;
    Vector<Syntax::TextDocumentFoldingRegion> folding_regions;

    Vector<Syntax::TextDocumentSpan> spans;
    for (auto& token : tokens) {
        Syntax::TextDocumentSpan span;
        span.range.set_start({ token.m_start.line, token.m_start.column });
        span.range.set_end({ token.m_end.line, token.m_end.column });
        span.attributes = style_for_token_type(palette, token.m_type);
        span.is_skippable = token.m_type == IniToken::Type::Whitespace;
        span.data = static_cast<u64>(token.m_type);
        spans.append(span);

        if (token.m_type == IniToken::Type::RightBracket && previous_token.m_type == IniToken::Type::Section) {
            previous_section_token = token;
        } else if (token.m_type == IniToken::Type::LeftBracket) {
            if (previous_section_token.has_value()) {
                Syntax::TextDocumentFoldingRegion region;
                region.range.set_start({ previous_section_token->m_end.line, previous_section_token->m_end.column });
                // If possible, leave a blank line between sections.
                // `end_line - start_line > 1` means the whitespace contains at least 1 blank line,
                // so we can end the region 1 line before the end of that whitespace token.
                // (Otherwise, we'd end the region at the start of the line that begins the next section.)
                auto end_line = token.m_start.line;
                if (previous_token.m_type == IniToken::Type::Whitespace
                    && (previous_token.m_end.line - previous_token.m_start.line > 1))
                    end_line--;
                region.range.set_end({ end_line, token.m_start.column });
                folding_regions.append(move(region));
            }
            previous_section_token = token;
        }

        previous_token = token;
    }
    if (previous_section_token.has_value()) {
        Syntax::TextDocumentFoldingRegion region;
        auto& end_token = tokens.last();
        region.range.set_start({ previous_section_token->m_end.line, previous_section_token->m_end.column });
        region.range.set_end({ end_token.m_end.line, end_token.m_end.column });
        folding_regions.append(move(region));
    }

    m_client->do_set_spans(move(spans));
    m_client->do_set_folding_regions(move(folding_regions));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<IniSyntaxHighlighter::MatchingTokenPair> IniSyntaxHighlighter::matching_token_pairs_impl() const
{
    static Vector<MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ static_cast<u64>(IniToken::Type::LeftBracket), static_cast<u64>(IniToken::Type::RightBracket) });
    }
    return pairs;
}

bool IniSyntaxHighlighter::token_types_equal(u64 token1, u64 token2) const
{
    return static_cast<GUI::IniToken::Type>(token1) == static_cast<GUI::IniToken::Type>(token2);
}

}
