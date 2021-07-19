/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinarySearch.h>
#include <AK/FileStream.h>
#include <AK/Function.h>
#include <AK/StringBuilder.h>
#include <LibLine/SuggestionDisplay.h>
#include <LibLine/VT.h>
#include <stdio.h>

namespace Line {

void XtermSuggestionDisplay::display(const SuggestionManager& manager)
{
    did_display();

    OutputFileStream stderr_stream { stderr };

    size_t longest_suggestion_length = 0;
    size_t longest_suggestion_byte_length = 0;

    manager.set_start_index(0);
    manager.for_each_suggestion([&](auto& suggestion, auto) {
        longest_suggestion_length = max(longest_suggestion_length, suggestion.text_view.length());
        longest_suggestion_byte_length = max(longest_suggestion_byte_length, suggestion.text_string.length());
        return IterationDecision::Continue;
    });

    size_t num_printed = 0;
    size_t lines_used = 1;

    VT::save_cursor(stderr_stream);
    VT::clear_lines(0, m_lines_used_for_last_suggestions, stderr_stream);
    VT::restore_cursor(stderr_stream);

    auto spans_entire_line { false };
    Vector<StringMetrics::LineMetrics> lines;
    for (size_t i = 0; i < m_prompt_lines_at_suggestion_initiation - 1; ++i)
        lines.append({ {}, 0 });
    lines.append({ {}, longest_suggestion_length });
    auto max_line_count = StringMetrics { move(lines) }.lines_with_addition({ { { {}, 0 } } }, m_num_columns);
    if (longest_suggestion_length >= m_num_columns - 2) {
        spans_entire_line = true;
        // We should make enough space for the biggest entry in
        // the suggestion list to fit in the prompt line.
        auto start = max_line_count - m_prompt_lines_at_suggestion_initiation;
        for (size_t i = start; i < max_line_count; ++i)
            stderr_stream.write("\n"sv.bytes());
        lines_used += max_line_count;
        longest_suggestion_length = 0;
    }

    VT::move_absolute(max_line_count + m_origin_row, 1, stderr_stream);

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
            stderr_stream.write("\n"sv.bytes());
            num_printed = 0;
        }

        // Show just enough suggestions to fill up the screen
        // without moving the prompt out of view.
        if (lines_used + m_prompt_lines_at_suggestion_initiation >= m_num_lines)
            return IterationDecision::Break;

        // Only apply color to the selection if something is *actually* added to the buffer.
        if (manager.is_current_suggestion_complete() && index == manager.next_index()) {
            VT::apply_style({ Style::Foreground(Style::XtermColor::Blue) }, stderr_stream);
        }

        if (spans_entire_line) {
            num_printed += m_num_columns;
            stderr_stream.write(suggestion.text_string.bytes());
        } else {
            stderr_stream.write(String::formatted("{: <{}}", suggestion.text_string, longest_suggestion_byte_length + 2).bytes());
            num_printed += longest_suggestion_length + 2;
        }

        if (manager.is_current_suggestion_complete() && index == manager.next_index())
            VT::apply_style(Style::reset_style(), stderr_stream);
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
        auto string = String::formatted("{:c} page {} of {} {:c}", left_arrow, page_index + 1, m_pages.size(), right_arrow);

        if (string.length() > m_num_columns - 1) {
            // This would overflow into the next line, so just don't print an indicator.
            return;
        }

        VT::move_absolute(m_origin_row + lines_used, m_num_columns - string.length() - 1, stderr_stream);
        VT::apply_style({ Style::Background(Style::XtermColor::Green) }, stderr_stream);
        stderr_stream.write(string.bytes());
        VT::apply_style(Style::reset_style(), stderr_stream);
    }
}

bool XtermSuggestionDisplay::cleanup()
{
    did_cleanup();

    if (m_lines_used_for_last_suggestions) {
        OutputFileStream stderr_stream { stderr };
        VT::clear_lines(0, m_lines_used_for_last_suggestions, stderr_stream);
        m_lines_used_for_last_suggestions = 0;
        return true;
    }

    return false;
}

size_t XtermSuggestionDisplay::fit_to_page_boundary(size_t selection_index)
{
    VERIFY(m_pages.size() > 0);
    size_t index = 0;

    auto* match = binary_search(
        m_pages.span(),
        PageRange { selection_index, selection_index },
        &index,
        [](auto& a, auto& b) -> int {
            if (a.start >= b.start && a.start < b.end)
                return 0;
            return a.start - b.start;
        });

    if (!match)
        return m_pages.size() - 1;

    return index;
}

}
