#pragma once

#include <AK/Noncopyable.h>
#include <AK/WeakPtr.h>
#include <LibGUI/TextDocument.h>

namespace GUI {

enum class SyntaxLanguage {
    PlainText,
    Cpp,
    Javascript
};

struct TextStyle {
    Color color;
    const Gfx::Font* font { nullptr };
};

class SyntaxHighlighter {
    AK_MAKE_NONCOPYABLE(SyntaxHighlighter);
    AK_MAKE_NONMOVABLE(SyntaxHighlighter);

public:
    virtual ~SyntaxHighlighter();

    virtual SyntaxLanguage language() const = 0;
    virtual void rehighlight() = 0;
    virtual void highlight_matching_token_pair();

    virtual bool is_identifier(void*) const { return false; };
    virtual bool is_navigatable(void*) const { return false; };

    void attach(TextEditor& editor);
    void detach();
    void cursor_did_change();

protected:
    SyntaxHighlighter() {}

    WeakPtr<TextEditor> m_editor;

    struct MatchingTokenPair {
        void* open;
        void* close;
    };

    virtual Vector<MatchingTokenPair> matching_token_pairs() const = 0;
    virtual bool token_types_equal(void*, void*) const = 0;

    struct BuddySpan {
        int index { -1 };
        GUI::TextDocumentSpan span_backup;
    };

    bool m_has_brace_buddies { false };
    BuddySpan m_brace_buddies[2];
};

}
