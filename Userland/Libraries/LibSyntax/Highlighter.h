/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/WeakPtr.h>
#include <LibGUI/TextDocument.h>
#include <LibGfx/Palette.h>
#include <LibSyntax/HighlighterClient.h>

namespace Syntax {

enum class Language {
    Cpp,
    GML,
    HTML,
    INI,
    JavaScript,
    PlainText,
    SQL,
    Shell,
};

struct TextStyle {
    const Gfx::Color color;
    const bool bold { false };
};

class Highlighter {
    AK_MAKE_NONCOPYABLE(Highlighter);
    AK_MAKE_NONMOVABLE(Highlighter);

public:
    virtual ~Highlighter();

    virtual Language language() const = 0;
    virtual void rehighlight(const Palette&) = 0;
    virtual void highlight_matching_token_pair();

    virtual bool is_identifier(void*) const { return false; };
    virtual bool is_navigatable(void*) const { return false; };

    void attach(HighlighterClient&);
    void detach();
    void cursor_did_change();

protected:
    Highlighter() { }

    // FIXME: This should be WeakPtr somehow
    HighlighterClient* m_client { nullptr };

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
