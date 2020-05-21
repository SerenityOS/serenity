/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/Function.h>
#include <LibLine/SuggestionDisplay.h>
#include <LibLine/VT.h>
#include <stdio.h>

namespace Line {

void XtermSuggestionDisplay::display(const SuggestionManager& manager)
{
    size_t longest_suggestion_length = 0;
    size_t longest_suggestion_byte_length = 0;

    manager.for_each_suggestion([&](auto& suggestion, auto) {
        longest_suggestion_length = max(longest_suggestion_length, suggestion.text_view.length());
        longest_suggestion_byte_length = max(longest_suggestion_byte_length, suggestion.text_string.length());
        return IterationDecision::Continue;
    });

    size_t num_printed = 0;
    size_t lines_used { 1 };

    VT::save_cursor();
    VT::clear_lines(0, m_lines_used_for_last_suggestions);
    VT::restore_cursor();

    auto spans_entire_line { false };
    auto max_line_count = (m_prompt_length + longest_suggestion_length + m_num_columns - 1) / m_num_columns;
    if (longest_suggestion_length >= m_num_columns - 2) {
        spans_entire_line = true;
        // we should make enough space for the biggest entry in
        // the suggestion list to fit in the prompt line
        auto start = max_line_count - m_prompt_lines_at_suggestion_initiation;
        for (size_t i = start; i < max_line_count; ++i) {
            putchar('\n');
        }
        lines_used += max_line_count;
        longest_suggestion_length = 0;
    }
    VT::move_absolute(max_line_count + m_origin_x, 1);

    manager.for_each_suggestion([&](auto& suggestion, auto index) {
        size_t next_column = num_printed + suggestion.text_view.length() + longest_suggestion_length + 2;

        if (next_column > m_num_columns) {
            auto lines = (suggestion.text_view.length() + m_num_columns - 1) / m_num_columns;
            lines_used += lines;
            putchar('\n');
            num_printed = 0;
        }

        // show just enough suggestions to fill up the screen
        // without moving the prompt out of view
        if (lines_used + m_prompt_lines_at_suggestion_initiation >= m_num_lines)
            return IterationDecision::Break;

        // only apply colour to the selection if something is *actually* added to the buffer
        if (manager.is_current_suggestion_complete() && index == manager.next_index()) {
            VT::apply_style({ Style::Foreground(Style::XtermColor::Blue) });
            fflush(stdout);
        }

        if (spans_entire_line) {
            num_printed += m_num_columns;
            fprintf(stderr, "%s", suggestion.text_string.characters());
        } else {
            fprintf(stderr, "%-*s", static_cast<int>(longest_suggestion_byte_length) + 2, suggestion.text_string.characters());
            num_printed += longest_suggestion_length + 2;
        }

        if (manager.is_current_suggestion_complete() && index == manager.next_index()) {
            VT::apply_style(Style::reset_style());
            fflush(stdout);
        }
        return IterationDecision::Continue;
    });

    m_lines_used_for_last_suggestions = lines_used;

    // if we filled the screen, move back the origin
    if (m_origin_x + lines_used >= m_num_lines) {
        m_origin_x = m_num_lines - lines_used;
    }
}

bool XtermSuggestionDisplay::cleanup()
{
    if (m_lines_used_for_last_suggestions) {
        VT::clear_lines(0, m_lines_used_for_last_suggestions);
        m_lines_used_for_last_suggestions = 0;
        return true;
    }

    return false;
}

}
