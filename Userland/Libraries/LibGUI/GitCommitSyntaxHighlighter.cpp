/*
 * Copyright (c) 2022, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/GitCommitLexer.h>
#include <LibGUI/GitCommitSyntaxHighlighter.h>
#include <LibGfx/Palette.h>

namespace GUI {
static Syntax::TextStyle style_for_token_type(const Gfx::Palette& palette, GitCommitToken::Type type)
{
    switch (type) {
    case GitCommitToken::Type::Comment:
        return { palette.syntax_comment() };
    default:
        return { palette.base_text() };
    }
}

void GitCommitSyntaxHighlighter::rehighlight(Palette const& palette)
{
    auto text = m_client->get_text();
    GitCommitLexer lexer(text);
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
    m_client->do_update();
}

Vector<GitCommitSyntaxHighlighter::MatchingTokenPair> GitCommitSyntaxHighlighter::matching_token_pairs_impl() const
{
    static Vector<MatchingTokenPair> empty;
    return empty;
}

bool GitCommitSyntaxHighlighter::token_types_equal(u64 token1, u64 token2) const
{
    return static_cast<GUI::GitCommitToken::Type>(token1) == static_cast<GUI::GitCommitToken::Type>(token2);
}

GitCommitSyntaxHighlighter::~GitCommitSyntaxHighlighter()
{
}

}
