/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCpp/Lexer.h>
#include <LibCpp/SyntaxHighlighter.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

namespace Cpp {

static Syntax::TextStyle style_for_token_type(const Gfx::Palette& palette, Cpp::Token::Type type)
{
    switch (type) {
    case Cpp::Token::Type::Keyword:
        return { palette.syntax_keyword(), true };
    case Cpp::Token::Type::KnownType:
        return { palette.syntax_type(), true };
    case Cpp::Token::Type::Identifier:
        return { palette.syntax_identifier(), false };
    case Cpp::Token::Type::DoubleQuotedString:
    case Cpp::Token::Type::SingleQuotedString:
    case Cpp::Token::Type::RawString:
        return { palette.syntax_string(), false };
    case Cpp::Token::Type::Integer:
    case Cpp::Token::Type::Float:
        return { palette.syntax_number(), false };
    case Cpp::Token::Type::IncludePath:
        return { palette.syntax_preprocessor_value(), false };
    case Cpp::Token::Type::EscapeSequence:
        return { palette.syntax_keyword(), true };
    case Cpp::Token::Type::PreprocessorStatement:
    case Cpp::Token::Type::IncludeStatement:
        return { palette.syntax_preprocessor_statement(), false };
    case Cpp::Token::Type::Comment:
        return { palette.syntax_comment(), false };
    default:
        return { palette.base_text(), false };
    }
}

bool SyntaxHighlighter::is_identifier(void* token) const
{
    auto cpp_token = static_cast<Cpp::Token::Type>(reinterpret_cast<size_t>(token));
    return cpp_token == Cpp::Token::Type::Identifier;
}

bool SyntaxHighlighter::is_navigatable(void* token) const
{
    auto cpp_token = static_cast<Cpp::Token::Type>(reinterpret_cast<size_t>(token));
    return cpp_token == Cpp::Token::Type::IncludePath;
}

void SyntaxHighlighter::rehighlight(const Palette& palette)
{
    auto text = m_client->get_text();
    Cpp::Lexer lexer(text);
    auto tokens = lexer.lex();

    Vector<GUI::TextDocumentSpan> spans;
    for (auto& token : tokens) {
        dbgln_if(SYNTAX_HIGHLIGHTING_DEBUG, "{} @ {}:{} - {}:{}", token.to_string(), token.start().line, token.start().column, token.end().line, token.end().column);
        GUI::TextDocumentSpan span;
        span.range.set_start({ token.start().line, token.start().column });
        span.range.set_end({ token.end().line, token.end().column });
        auto style = style_for_token_type(palette, token.type());
        span.attributes.color = style.color;
        span.attributes.bold = style.bold;
        span.is_skippable = token.type() == Cpp::Token::Type::Whitespace;
        span.data = reinterpret_cast<void*>(token.type());
        spans.append(span);
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
        pairs.append({ reinterpret_cast<void*>(Cpp::Token::Type::LeftCurly), reinterpret_cast<void*>(Cpp::Token::Type::RightCurly) });
        pairs.append({ reinterpret_cast<void*>(Cpp::Token::Type::LeftParen), reinterpret_cast<void*>(Cpp::Token::Type::RightParen) });
        pairs.append({ reinterpret_cast<void*>(Cpp::Token::Type::LeftBracket), reinterpret_cast<void*>(Cpp::Token::Type::RightBracket) });
    }
    return pairs;
}

bool SyntaxHighlighter::token_types_equal(void* token1, void* token2) const
{
    return static_cast<Cpp::Token::Type>(reinterpret_cast<size_t>(token1)) == static_cast<Cpp::Token::Type>(reinterpret_cast<size_t>(token2));
}

SyntaxHighlighter::~SyntaxHighlighter()
{
}

}
