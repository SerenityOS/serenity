#pragma once

#include <AK/Noncopyable.h>
#include <AK/WeakPtr.h>
#include <LibGUI/TextDocument.h>

namespace GUI {

class TextEditor;

class SyntaxHighlighter {
    AK_MAKE_NONCOPYABLE(SyntaxHighlighter);
    AK_MAKE_NONMOVABLE(SyntaxHighlighter);

public:
    virtual ~SyntaxHighlighter();

    virtual void rehighlight() = 0;
    virtual void highlight_matching_token_pair() = 0;

    void attach(TextEditor& editor);
    void detach();
    void cursor_did_change();

protected:
    SyntaxHighlighter() {}

    WeakPtr<TextEditor> m_editor;

    struct BuddySpan {
        int index { -1 };
        GUI::TextDocumentSpan span_backup;
    };

    bool m_has_brace_buddies { false };
    BuddySpan m_brace_buddies[2];
};

}
