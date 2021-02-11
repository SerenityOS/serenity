/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGUI/INILexer.h>
#include <LibGUI/INISyntaxHighlighter.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

namespace GUI {

static Syntax::TextStyle style_for_token_type(const Gfx::Palette& palette, IniToken::Type type)
{
    switch (type) {
    case IniToken::Type::LeftBracket:
    case IniToken::Type::RightBracket:
    case IniToken::Type::section:
        return { palette.syntax_keyword(), true };
    case IniToken::Type::Name:
        return { palette.syntax_identifier() };
    case IniToken::Type::Value:
        return { palette.syntax_string() };
    case IniToken::Type::Comment:
        return { palette.syntax_comment() };
    case IniToken::Type::Equal:
        return { palette.syntax_operator(), true };
    default:
        return { palette.base_text() };
    }
}

bool IniSyntaxHighlighter::is_identifier(void* token) const
{
    auto ini_token = static_cast<GUI::IniToken::Type>(reinterpret_cast<size_t>(token));
    return ini_token == GUI::IniToken::Type::Name;
}

void IniSyntaxHighlighter::rehighlight(const Palette& palette)
{
    auto text = m_client->get_text();
    IniLexer lexer(text);
    auto tokens = lexer.lex();

    Vector<GUI::TextDocumentSpan> spans;
    for (auto& token : tokens) {
        GUI::TextDocumentSpan span;
        span.range.set_start({ token.m_start.line, token.m_start.column });
        span.range.set_end({ token.m_end.line, token.m_end.column });
        auto style = style_for_token_type(palette, token.m_type);
        span.attributes.color = style.color;
        span.attributes.bold = style.bold;
        span.is_skippable = token.m_type == IniToken::Type::Whitespace;
        span.data = reinterpret_cast<void*>(token.m_type);
        spans.append(span);
    }
    m_client->do_set_spans(move(spans));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<IniSyntaxHighlighter::MatchingTokenPair> IniSyntaxHighlighter::matching_token_pairs() const
{
    static Vector<MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ reinterpret_cast<void*>(IniToken::Type::LeftBracket), reinterpret_cast<void*>(IniToken::Type::RightBracket) });
    }
    return pairs;
}

bool IniSyntaxHighlighter::token_types_equal(void* token1, void* token2) const
{
    return static_cast<GUI::IniToken::Type>(reinterpret_cast<size_t>(token1)) == static_cast<GUI::IniToken::Type>(reinterpret_cast<size_t>(token2));
}

IniSyntaxHighlighter::~IniSyntaxHighlighter()
{
}

}
