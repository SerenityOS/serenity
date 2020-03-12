#include <LibGUI/JSSyntaxHighlighter.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Font.h>
#include <LibJS/Lexer.h>
#include <LibJS/Token.h>

namespace GUI {

static TextStyle style_for_token_type(JS::TokenType type)
{
    switch (type) {
    case JS::TokenType::BoolLiteral:
    case JS::TokenType::NullLiteral:
        return { Color::Black, &Gfx::Font::default_bold_fixed_width_font() };
    case JS::TokenType::Catch:
    case JS::TokenType::Class:
    case JS::TokenType::Const:
    case JS::TokenType::Delete:
    case JS::TokenType::Do:
    case JS::TokenType::Else:
    case JS::TokenType::Finally:
    case JS::TokenType::For:
    case JS::TokenType::Function:
    case JS::TokenType::If:
    case JS::TokenType::Interface:
    case JS::TokenType::Let:
    case JS::TokenType::New:
    case JS::TokenType::QuestionMark:
    case JS::TokenType::Return:
    case JS::TokenType::Try:
    case JS::TokenType::Var:
    case JS::TokenType::While:
        return { Color::Black, &Gfx::Font::default_bold_fixed_width_font() };
    case JS::TokenType::Identifier:
        return { Color::from_rgb(0x092e64) };
    case JS::TokenType::NumericLiteral:
    case JS::TokenType::StringLiteral:
    case JS::TokenType::RegexLiteral:
        return { Color::from_rgb(0x800000) };
    case JS::TokenType::Invalid:
    case JS::TokenType::Eof:
        return { Color::from_rgb(0x008000) };
    default:
        return { Color::Black };
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

void JSSyntaxHighlighter::rehighlight()
{
    ASSERT(m_editor);
    auto text = m_editor->text();

    JS::Lexer lexer(text);

    Vector<GUI::TextDocumentSpan> spans;
    GUI::TextPosition position { 0, 0 };
    GUI::TextPosition start { 0, 0 };

    auto advance_position = [&](char ch) {
        if (ch == '\n') {
            position.set_line(position.line() + 1);
            position.set_column(0);
        } else
            position.set_column(position.column() + 1);
    };

    bool was_eof = false;
    for (auto token = lexer.next(); !was_eof; token = lexer.next()) {
        start = position;
        if (!token.trivia().is_empty()) {
            for (auto ch : token.trivia())
                advance_position(ch);

            GUI::TextDocumentSpan span;

            span.range.set_start(start);
            if (position.column() > 0)
                span.range.set_end({ position.line(), position.column() - 1 });
            else
                span.range.set_end({ position.line() - 1, 0 });
            auto style = style_for_token_type(JS::TokenType::Invalid);
            span.color = style.color;
            span.font = style.font;
            span.is_skippable = true;
            spans.append(span);
#ifdef DEBUG_SYNTAX_HIGHLIGHTING
            dbg() << token.name() << " \"" << token.trivia() << "\" (trivia) @ " << span.range.start().line() << ":" << span.range.start().column() << " - " << span.range.end().line() << ":" << span.range.end().column();
#endif
        }

        start = position;
        if (!token.value().is_empty()) {
            for (auto ch : token.value())
                advance_position(ch);

            GUI::TextDocumentSpan span;
            span.range.set_start(start);
            if (position.column() > 0)
                span.range.set_end({ position.line(), position.column() - 1 });
            else
                span.range.set_end({ position.line() - 1, 0 });
            auto style = style_for_token_type(token.type());
            span.color = style.color;
            span.font = style.font;
            span.is_skippable = false;
            span.data = reinterpret_cast<void*>(token.type());
            spans.append(span);
#ifdef DEBUG_SYNTAX_HIGHLIGHTING
            dbg() << token.name() << " @ \"" << token.value() << "\" " << span.range.start().line() << ":" << span.range.start().column() << " - " << span.range.end().line() << ":" << span.range.end().column();
#endif
        }

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
