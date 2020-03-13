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

#include <LibGUI/SyntaxHighlighter.h>
#include <LibGUI/TextEditor.h>

namespace GUI {

SyntaxHighlighter::~SyntaxHighlighter()
{
}

void SyntaxHighlighter::highlight_matching_token_pair()
{
    ASSERT(m_editor);
    auto& document = m_editor->document();

    enum class Direction {
        Forward,
        Backward,
    };

    auto find_span_of_type = [&](auto i, void* type, void* not_type, Direction direction) -> Optional<size_t> {
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
            auto span_token_type = span.data;
            if (token_types_equal(span_token_type, not_type)) {
                ++nesting_level;
            } else if (token_types_equal(span_token_type, type)) {
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
        auto& buddy0 = document.spans()[index0];
        auto& buddy1 = document.spans()[index1];
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

    auto pairs = matching_token_pairs();

    for (size_t i = 0; i < document.spans().size(); ++i) {
        auto& span = const_cast<GUI::TextDocumentSpan&>(document.spans().at(i));
        auto token_type = span.data;

        for (auto& pair : pairs) {
            if (token_types_equal(token_type, pair.open) && span.range.start() == m_editor->cursor()) {
                auto buddy = find_span_of_type(i, pair.close, pair.open, Direction::Forward);
                if (buddy.has_value())
                    make_buddies(i, buddy.value());
                return;
            }
        }

        auto right_of_end = span.range.end();
        right_of_end.set_column(right_of_end.column() + 1);

        for (auto& pair : pairs) {
            if (token_types_equal(token_type, pair.close) && right_of_end == m_editor->cursor()) {
                auto buddy = find_span_of_type(i, pair.open, pair.close, Direction::Backward);
                if (buddy.has_value())
                    make_buddies(i, buddy.value());
                return;
            }
        }
    }
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
