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

#include <AK/BinarySearch.h>
#include <AK/Function.h>
#include <AK/StringBuilder.h>
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
    size_t lines_used = 1;

    VT::save_cursor();
    VT::clear_lines(0, m_lines_used_for_last_suggestions);
    VT::restore_cursor();

    auto spans_entire_line { false };
    Vector<size_t> lines;
    for (size_t i = 0; i < m_prompt_lines_at_suggestion_initiation - 1; ++i)
        lines.append(0);
    lines.append(longest_suggestion_length);
    auto max_line_count = StringMetrics { move(lines) }.lines_with_addition({ { 0 } }, m_num_columns);
    if (longest_suggestion_length >= m_num_columns - 2) {
        spans_entire_line = true;
        // We should make enough space for the biggest entry in
        // the suggestion list to fit in the prompt line.
        auto start = max_line_count - m_prompt_lines_at_suggestion_initiation;
        for (size_t i = start; i < max_line_count; ++i) {
            fputc('\n', stderr);
        }
        lines_used += max_line_count;
        longest_suggestion_length = 0;
    }

    VT::move_absolute(max_line_count + m_origin_row, 1);

    if (m_pages.is_empty()) {
        size_t num_printed = 0;
        size_t lines_used = 1;
        // Cache the pages.
        manager.set_start_index(0);
        size_t page_start = 0;
        manager.for_each_suggestion([&](auto& suggestion, auto index) {
            size_t next_column = num_printed + suggestion.text_view.length() + longest_suggestion_length + 2;
            if (next_column > m_num_columns) {
                auto lines = (suggestion.text_view.length() + m_num_columns - 1) / m_num_columns;
                lines_used += lines;
                num_printed = 0;
            }

            if (lines_used + m_prompt_lines_at_suggestion_initiation >= m_num_lines) {
                m_pages.append({ page_start, index });
                page_start = index;
                lines_used = 1;
                num_printed = 0;
            }

            if (spans_entire_line)
                num_printed += m_num_columns;
            else
                num_printed += longest_suggestion_length + 2;

            return IterationDecision::Continue;
        });
        // Append the last page.
        m_pages.append({ page_start, manager.count() });
    }

    auto page_index = fit_to_page_boundary(manager.next_index());

    manager.set_start_index(m_pages[page_index].start);

    manager.for_each_suggestion([&](auto& suggestion, auto index) {
        size_t next_column = num_printed + suggestion.text_view.length() + longest_suggestion_length + 2;

        if (next_column > m_num_columns) {
            auto lines = (suggestion.text_view.length() + m_num_columns - 1) / m_num_columns;
            lines_used += lines;
            fputc('\n', stderr);
            num_printed = 0;
        }

        // Show just enough suggestions to fill up the screen
        // without moving the prompt out of view.
        if (lines_used + m_prompt_lines_at_suggestion_initiation >= m_num_lines)
            return IterationDecision::Break;

        // Only apply colour to the selection if something is *actually* added to the buffer.
        if (manager.is_current_suggestion_complete() && index == manager.next_index()) {
            VT::apply_style({ Style::Foreground(Style::XtermColor::Blue) });
            fflush(stderr);
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
            fflush(stderr);
        }
        return IterationDecision::Continue;
    });

    m_lines_used_for_last_suggestions = lines_used;

    // If we filled the screen, move back the origin.
    if (m_origin_row + lines_used >= m_num_lines) {
        m_origin_row = m_num_lines - lines_used;
    }

    if (m_pages.size() > 1) {
        auto left_arrow = page_index > 0 ? '<' : ' ';
        auto right_arrow = page_index < m_pages.size() - 1 ? '>' : ' ';
        auto string = String::format("%c page %d of %d %c", left_arrow, page_index + 1, m_pages.size(), right_arrow);

        if (string.length() > m_num_columns - 1) {
            // This would overflow into the next line, so just don't print an indicator.
            fflush(stderr);
            return;
        }

        VT::move_absolute(m_origin_row + lines_used, m_num_columns - string.length() - 1);
        VT::apply_style({ Style::Background(Style::XtermColor::Green) });
        fputs(string.characters(), stderr);
        VT::apply_style(Style::reset_style());
    }

    fflush(stderr);
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

size_t XtermSuggestionDisplay::fit_to_page_boundary(size_t selection_index)
{
    ASSERT(m_pages.size() > 0);
    size_t index = 0;

    auto* match = binary_search(
        m_pages.span(), { selection_index, selection_index }, [](auto& a, auto& b) -> int {
            if (a.start >= b.start && a.start < b.end)
                return 0;
            return a.start - b.start;
        },
        &index);

    if (!match)
        return m_pages.size() - 1;

    return index;
}

}
