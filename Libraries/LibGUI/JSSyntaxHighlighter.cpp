/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <LibGUI/JSSyntaxHighlighter.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibJS/Lexer.h>
#include <LibJS/Token.h>

namespace GUI {

static TextStyle style_for_token_type(Gfx::Palette palette, JS::TokenType type)
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
        return { palette.syntax_keyword(), &Gfx::Font::default_bold_fixed_width_font() };
    case JS::TokenCategory::ControlKeyword:
        return { palette.syntax_control_keyword(), &Gfx::Font::default_bold_fixed_width_font() };
    case JS::TokenCategory::Identifier:
        return { palette.syntax_identifier() };
    default:
        return { palette.base_text() };
    }
}

bool JSSyntaxHighlighter::is_identifier(void* token) const
{
    auto js_token = static_cast<JS::TokenType>(reinterpret_cast<size_t>(token));
    return js_token == JS::TokenType::Identifier;
}

bool JSSyntaxHighlighter::is_navigatable(void* token) const
{
    (void)token;
    return false;
}

void JSSyntaxHighlighter::rehighlight(Gfx::Palette palette)
{
    ASSERT(m_editor);
    auto text = m_editor->text();

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
        span.color = style.color;
        span.font = style.font;
        span.is_skippable = is_trivia;
        span.data = reinterpret_cast<void*>(static_cast<size_t>(type));
        spans.append(span);
        advance_position(str[str.length() - 1]);

#ifdef DEBUG_SYNTAX_HIGHLIGHTING
        dbg() << token.name() << (is_trivia ? " (trivia) @ \"" : " @ \"") << token.value() << "\" "
              << span.range.start().line() << ":" << span.range.start().column() << " - "
              << span.range.end().line() << ":" << span.range.end().column();
#endif
    };

    bool was_eof = false;
    for (auto token = lexer.next(); !was_eof; token = lexer.next()) {
        append_token(token.trivia(), token, true);
        append_token(token.value(), token, false);

        if (token.type() == JS::TokenType::Eof)
            was_eof = true;
    }

    m_editor->document().set_spans(spans);

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_editor->update();
}

Vector<SyntaxHighlighter::MatchingTokenPair> JSSyntaxHighlighter::matching_token_pairs() const
{
    static Vector<SyntaxHighlighter::MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ reinterpret_cast<void*>(JS::TokenType::CurlyOpen), reinterpret_cast<void*>(JS::TokenType::CurlyClose) });
        pairs.append({ reinterpret_cast<void*>(JS::TokenType::ParenOpen), reinterpret_cast<void*>(JS::TokenType::ParenClose) });
        pairs.append({ reinterpret_cast<void*>(JS::TokenType::BracketOpen), reinterpret_cast<void*>(JS::TokenType::BracketClose) });
    }
    return pairs;
}

bool JSSyntaxHighlighter::token_types_equal(void* token1, void* token2) const
{
    return static_cast<JS::TokenType>(reinterpret_cast<size_t>(token1)) == static_cast<JS::TokenType>(reinterpret_cast<size_t>(token2));
}

JSSyntaxHighlighter::~JSSyntaxHighlighter()
{
}

}
