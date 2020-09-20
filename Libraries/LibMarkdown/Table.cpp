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

#include <AK/StringBuilder.h>
#include <LibMarkdown/Table.h>

namespace Markdown {

String Table::render_for_terminal(size_t view_width) const
{
    auto unit_width_length = view_width == 0 ? 4 : ((float)(view_width - m_columns.size()) / (float)m_total_width);
    StringBuilder format_builder;
    StringBuilder builder;

    auto write_aligned = [&](const auto& text, auto width, auto alignment) {
        size_t original_length = 0;
        for (auto& span : text.spans())
            original_length += span.text.length();
        auto string = text.render_for_terminal();
        format_builder.clear();
        if (alignment == Alignment::Center) {
            auto padding_length = (width - original_length) / 2;
            builder.appendf("%*s%s%*s", padding_length, "", string.characters(), padding_length, "");
            if ((width - original_length) % 2)
                builder.append(' ');
        } else {
            format_builder.appendf("%%%s%zus", alignment == Alignment::Left ? "-" : "", width + (string.length() - original_length));
            builder.appendf(
                format_builder.to_string().characters(),
                string.characters());
        }
    };

    bool first = true;
    for (auto& col : m_columns) {
        if (!first)
            builder.append('|');
        first = false;
        size_t width = col.relative_width * unit_width_length;
        write_aligned(col.header, width, col.alignment);
    }

    builder.append("\n");
    for (size_t i = 0; i < view_width; ++i)
        builder.append('-');
    builder.append("\n");

    for (size_t i = 0; i < m_row_count; ++i) {
        bool first = true;
        for (auto& col : m_columns) {
            ASSERT(i < col.rows.size());
            auto& cell = col.rows[i];

            if (!first)
                builder.append('|');
            first = false;

            size_t width = col.relative_width * unit_width_length;
            write_aligned(cell, width, col.alignment);
        }
        builder.append("\n");
    }

    return builder.to_string();
}

String Table::render_to_html() const
{
    StringBuilder builder;

    builder.append("<table>");
    builder.append("<thead>");
    builder.append("<tr>");
    for (auto& column : m_columns) {
        builder.append("<th>");
        builder.append(column.header.render_to_html());
        builder.append("</th>");
    }
    builder.append("</tr>");
    builder.append("</thead>");
    builder.append("<tbody>");
    for (size_t i = 0; i < m_row_count; ++i) {
        builder.append("<tr>");
        for (auto& column : m_columns) {
            ASSERT(i < column.rows.size());
            builder.append("<td>");
            builder.append(column.rows[i].render_to_html());
            builder.append("</td>");
        }
        builder.append("</tr>");
    }
    builder.append("</tbody>");
    builder.append("</table>");

    return builder.to_string();
}

OwnPtr<Table> Table::parse(Vector<StringView>::ConstIterator& lines)
{
    auto peek_it = lines;
    auto first_line = *peek_it;
    if (!first_line.starts_with('|'))
        return nullptr;

    ++peek_it;

    if (peek_it.is_end())
        return nullptr;

    auto header_segments = first_line.split_view('|', true);
    auto header_delimiters = peek_it->split_view('|', true);

    if (!header_segments.is_empty())
        header_segments.take_first();
    if (!header_segments.is_empty() && header_segments.last().is_empty())
        header_segments.take_last();

    if (!header_delimiters.is_empty())
        header_delimiters.take_first();
    if (!header_delimiters.is_empty() && header_delimiters.last().is_empty())
        header_delimiters.take_last();

    ++peek_it;

    if (header_delimiters.size() != header_segments.size())
        return nullptr;

    if (header_delimiters.is_empty())
        return nullptr;

    size_t total_width = 0;

    auto table = make<Table>();
    table->m_columns.resize(header_delimiters.size());

    for (size_t i = 0; i < header_segments.size(); ++i) {
        auto text_option = Text::parse(header_segments[i]);
        if (!text_option.has_value())
            return nullptr; // An invalid 'text' in the header should just fail the table parse.

        auto text = text_option.release_value();
        auto& column = table->m_columns[i];

        column.header = move(text);

        auto delimiter = header_delimiters[i].trim_whitespace();

        auto align_left = delimiter.starts_with(':');
        auto align_right = delimiter != ":" && delimiter.ends_with(':');

        if (align_left)
            delimiter = delimiter.substring_view(1, delimiter.length() - 1);
        if (align_right)
            delimiter = delimiter.substring_view(0, delimiter.length() - 1);

        if (align_left && align_right)
            column.alignment = Alignment::Center;
        else if (align_right)
            column.alignment = Alignment::Right;
        else
            column.alignment = Alignment::Left;

        size_t relative_width = delimiter.length();
        for (auto ch : delimiter) {
            if (ch != '-') {
                dbg() << "Invalid character _" << ch << "_ in table heading delimiter (ignored)";
                --relative_width;
            }
        }

        column.relative_width = relative_width;
        total_width += relative_width;
    }

    table->m_total_width = total_width;

    for (off_t i = 0; i < peek_it - lines; ++i)
        ++lines;

    size_t row_count = 0;
    ++lines;
    while (!lines.is_end()) {
        auto& line = *lines;
        if (!line.starts_with('|'))
            break;

        ++lines;

        auto segments = line.split_view('|', true);
        segments.take_first();
        if (!segments.is_empty() && segments.last().is_empty())
            segments.take_last();
        ++row_count;

        for (size_t i = 0; i < header_segments.size(); ++i) {
            if (i >= segments.size()) {
                // Ran out of segments, but still have headers.
                // Just make an empty cell.
                table->m_columns[i].rows.append(Text { "" });
            } else {
                auto text_option = Text::parse(segments[i]);
                // We treat an invalid 'text' as a literal.
                if (text_option.has_value()) {
                    auto text = text_option.release_value();
                    table->m_columns[i].rows.append(move(text));
                } else {
                    table->m_columns[i].rows.append(Text { segments[i] });
                }
            }
        }
    }

    table->m_row_count = row_count;

    return move(table);
}

}
