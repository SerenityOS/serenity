#include <LibGUI/CppLexer.h>
#include <LibGUI/CppSyntaxHighlighter.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Font.h>

namespace GUI {

static TextStyle style_for_token_type(CppToken::Type type)
{
    switch (type) {
    case CppToken::Type::Keyword:
        return { Color::Black, &Gfx::Font::default_bold_fixed_width_font() };
    case CppToken::Type::KnownType:
        return { Color::from_rgb(0x800080), &Gfx::Font::default_bold_fixed_width_font() };
    case CppToken::Type::Identifier:
        return { Color::from_rgb(0x092e64) };
    case CppToken::Type::DoubleQuotedString:
    case CppToken::Type::SingleQuotedString:
    case CppToken::Type::Integer:
    case CppToken::Type::Float:
    case CppToken::Type::IncludePath:
        return { Color::from_rgb(0x800000) };
    case CppToken::Type::EscapeSequence:
        return { Color::from_rgb(0x800080), &Gfx::Font::default_bold_fixed_width_font() };
    case CppToken::Type::PreprocessorStatement:
    case CppToken::Type::IncludeStatement:
        return { Color::from_rgb(0x008080) };
    case CppToken::Type::Comment:
        return { Color::from_rgb(0x008000) };
    default:
        return { Color::Black };
    }
}

bool CppSyntaxHighlighter::is_identifier(void* token) const
{
    auto cpp_token = static_cast<GUI::CppToken::Type>(reinterpret_cast<size_t>(token));
    return cpp_token == GUI::CppToken::Type::Identifier;
}

bool CppSyntaxHighlighter::is_navigatable(void* token) const
{
    auto cpp_token = static_cast<GUI::CppToken::Type>(reinterpret_cast<size_t>(token));
    return cpp_token == GUI::CppToken::Type::IncludePath;
}

void CppSyntaxHighlighter::rehighlight()
{
    ASSERT(m_editor);
    auto text = m_editor->text();
    CppLexer lexer(text);
    auto tokens = lexer.lex();

    Vector<GUI::TextDocumentSpan> spans;
    for (auto& token : tokens) {
#ifdef DEBUG_SYNTAX_HIGHLIGHTING
        dbg() << token.to_string() << " @ " << token.m_start.line << ":" << token.m_start.column << " - " << token.m_end.line << ":" << token.m_end.column;
#endif
        GUI::TextDocumentSpan span;
        span.range.set_start({ token.m_start.line, token.m_start.column });
        span.range.set_end({ token.m_end.line, token.m_end.column });
        auto style = style_for_token_type(token.m_type);
        span.color = style.color;
        span.font = style.font;
        span.is_skippable = token.m_type == CppToken::Type::Whitespace;
        span.data = reinterpret_cast<void*>(token.m_type);
        spans.append(span);
    }
    m_editor->document().set_spans(spans);

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_editor->update();
}

Vector<SyntaxHighlighter::MatchingTokenPair> CppSyntaxHighlighter::matching_token_pairs() const
{
    static Vector<SyntaxHighlighter::MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ reinterpret_cast<void*>(CppToken::Type::LeftCurly), reinterpret_cast<void*>(CppToken::Type::RightCurly) });
        pairs.append({ reinterpret_cast<void*>(CppToken::Type::LeftParen), reinterpret_cast<void*>(CppToken::Type::RightParen) });
        pairs.append({ reinterpret_cast<void*>(CppToken::Type::LeftBracket), reinterpret_cast<void*>(CppToken::Type::RightBracket) });
    }
    return pairs;
}

bool CppSyntaxHighlighter::token_types_equal(void* token1, void* token2) const
{
    return static_cast<GUI::CppToken::Type>(reinterpret_cast<size_t>(token1)) == static_cast<GUI::CppToken::Type>(reinterpret_cast<size_t>(token2));
}

CppSyntaxHighlighter::~CppSyntaxHighlighter()
{
}

}
