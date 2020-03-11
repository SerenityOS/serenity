#include <LibGUI/CppLexer.h>
#include <LibGUI/CppSyntaxHighlighter.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Font.h>

namespace GUI {

struct TextStyle {
    Color color;
    const Gfx::Font* font { nullptr };
};

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
        return { Color::from_rgb(0x800000) };
    case CppToken::Type::EscapeSequence:
        return { Color::from_rgb(0x800080), &Gfx::Font::default_bold_fixed_width_font() };
    case CppToken::Type::PreprocessorStatement:
        return { Color::from_rgb(0x008080) };
    case CppToken::Type::Comment:
        return { Color::from_rgb(0x008000) };
    default:
        return { Color::Black };
    }
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
        span.data = (void*)token.m_type;
        spans.append(span);
    }
    m_editor->document().set_spans(spans);

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_editor->update();
}

void CppSyntaxHighlighter::highlight_matching_token_pair()
{
    ASSERT(m_editor);
    auto& document = m_editor->document();

    enum class Direction {
        Forward,
        Backward,
    };

    auto find_span_of_type = [&](auto i, CppToken::Type type, CppToken::Type not_type, Direction direction) -> Optional<size_t> {
        size_t nesting_level = 0;
        bool forward = direction == Direction::Forward;

        if (forward) {
            ++i;
            if (i >= document.spans().size())
                return {};
        } else {
            if (i == 0)
                return {};
            --i;
        }

        for (;;) {
            auto& span = document.spans().at(i);
            auto span_token_type = (CppToken::Type)((FlatPtr)span.data);
            if (span_token_type == not_type) {
                ++nesting_level;
            } else if (span_token_type == type) {
                if (nesting_level-- <= 0)
                    return i;
            }

            if (forward) {
                ++i;
                if (i >= document.spans().size())
                    return {};
            } else {
                if (i == 0)
                    return {};
                --i;
            }
        }

        return {};
    };

    auto make_buddies = [&](int index0, int index1) {
        auto& buddy0 = const_cast<GUI::TextDocumentSpan&>(document.spans()[index0]);
        auto& buddy1 = const_cast<GUI::TextDocumentSpan&>(document.spans()[index1]);
        m_has_brace_buddies = true;
        m_brace_buddies[0].index = index0;
        m_brace_buddies[1].index = index1;
        m_brace_buddies[0].span_backup = buddy0;
        m_brace_buddies[1].span_backup = buddy1;
        buddy0.background_color = Color::DarkCyan;
        buddy1.background_color = Color::DarkCyan;
        buddy0.color = Color::White;
        buddy1.color = Color::White;
        m_editor->update();
    };

    struct MatchingTokenPair {
        CppToken::Type open;
        CppToken::Type close;
    };

    MatchingTokenPair pairs[] = {
        { CppToken::Type::LeftCurly, CppToken::Type::RightCurly },
        { CppToken::Type::LeftParen, CppToken::Type::RightParen },
        { CppToken::Type::LeftBracket, CppToken::Type::RightBracket },
    };

    for (size_t i = 0; i < document.spans().size(); ++i) {
        auto& span = const_cast<GUI::TextDocumentSpan&>(document.spans().at(i));
        auto token_type = (CppToken::Type)((FlatPtr)span.data);

        for (auto& pair : pairs) {
            if (token_type == pair.open && span.range.start() == m_editor->cursor()) {
                auto buddy = find_span_of_type(i, pair.close, pair.open, Direction::Forward);
                if (buddy.has_value())
                    make_buddies(i, buddy.value());
                return;
            }
        }

        auto right_of_end = span.range.end();
        right_of_end.set_column(right_of_end.column() + 1);

        for (auto& pair : pairs) {
            if (token_type == pair.close && right_of_end == m_editor->cursor()) {
                auto buddy = find_span_of_type(i, pair.open, pair.close, Direction::Backward);
                if (buddy.has_value())
                    make_buddies(i, buddy.value());
                return;
            }
        }
    }
}

CppSyntaxHighlighter::~CppSyntaxHighlighter()
{
}

}
