/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
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

#pragma once

#include <AK/Noncopyable.h>
#include <AK/WeakPtr.h>
#include <LibGUI/TextDocument.h>
#include <LibGfx/Palette.h>
#include <LibSyntax/HighlighterClient.h>

namespace Syntax {

enum class Language {
    PlainText,
    Cpp,
    JavaScript,
    INI,
    GML,
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
