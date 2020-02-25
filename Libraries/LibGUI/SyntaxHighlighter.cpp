#include <LibGUI/SyntaxHighlighter.h>
#include <LibGUI/TextEditor.h>

namespace GUI {

SyntaxHighlighter::~SyntaxHighlighter()
{
}

void SyntaxHighlighter::attach(TextEditor& editor)
{
    ASSERT(!m_editor);
    m_editor = editor.make_weak_ptr();
}

void SyntaxHighlighter::detach()
{
    ASSERT(m_editor);
    m_editor = nullptr;
}

void SyntaxHighlighter::cursor_did_change()
{
    ASSERT(m_editor);
    auto& document = m_editor->document();
    if (m_has_brace_buddies) {
        if (m_brace_buddies[0].index >= 0 && m_brace_buddies[0].index < static_cast<int>(document.spans().size()))
            document.set_span_at_index(m_brace_buddies[0].index, m_brace_buddies[0].span_backup);
        if (m_brace_buddies[1].index >= 0 && m_brace_buddies[1].index < static_cast<int>(document.spans().size()))
            document.set_span_at_index(m_brace_buddies[1].index, m_brace_buddies[1].span_backup);
        m_has_brace_buddies = false;
        m_editor->update();
    }
    highlight_matching_token_pair();
}

}
