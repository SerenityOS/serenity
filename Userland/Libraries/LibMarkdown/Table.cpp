/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibMarkdown/Table.h>
#include <LibMarkdown/Visitor.h>

namespace Markdown {

Vector<ByteString> Table::render_lines_for_terminal(size_t view_width) const
{
    auto unit_width_length = view_width == 0 ? 4 : ((float)(view_width - m_columns.size()) / (float)m_total_width);
    StringBuilder builder;
    Vector<ByteString> lines;

    auto write_aligned = [&](auto const& text, auto width, auto alignment) {
        size_t original_length = text.terminal_length();
        auto string = text.render_for_terminal();
        if (alignment == Alignment::Center) {
            auto padding_length = (width - original_length) / 2;
            // FIXME: We're using a StringView literal to bypass the compile-time AK::Format checking here, since it can't handle the "}}"
            builder.appendff("{:{1}}"sv, "", (int)padding_length);
            builder.append(string);
            builder.appendff("{:{1}}"sv, "", (int)padding_length);
            if ((width - original_length) % 2)
                builder.append(' ');
        } else {
            // FIXME: We're using StringView literals to bypass the compile-time AK::Format checking here, since it can't handle the "}}"
            builder.appendff(alignment == Alignment::Left ? "{:<{1}}"sv : "{:>{1}}"sv, string, (int)(width + (string.length() - original_length)));
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

    lines.append(builder.to_byte_string());
    builder.clear();

    for (size_t i = 0; i < view_width; ++i)
        builder.append('-');
    lines.append(builder.to_byte_string());
    builder.clear();

    for (size_t i = 0; i < m_row_count; ++i) {
        bool first = true;
        for (auto& col : m_columns) {
            VERIFY(i < col.rows.size());
            auto& cell = col.rows[i];

            if (!first)
                builder.append('|');
            first = false;

            size_t width = col.relative_width * unit_width_length;
            write_aligned(cell, width, col.alignment);
        }
        lines.append(builder.to_byte_string());
        builder.clear();
    }

    lines.append("");

    return lines;
}

ByteString Table::render_to_html(bool) const
{
    auto alignment_string = [](Alignment alignment) {
        switch (alignment) {
        case Alignment::Center:
            return "center"sv;
        case Alignment::Left:
            return "left"sv;
        case Alignment::Right:
            return "right"sv;
        }
        VERIFY_NOT_REACHED();
    };

    StringBuilder builder;

    builder.append("<table>"sv);
    builder.append("<thead>"sv);
    builder.append("<tr>"sv);
    for (auto& column : m_columns) {
        builder.appendff("<th style='text-align: {}'>", alignment_string(column.alignment));
        builder.append(column.header.render_to_html());
        builder.append("</th>"sv);
    }
    builder.append("</tr>"sv);
    builder.append("</thead>"sv);
    builder.append("<tbody>"sv);
    for (size_t i = 0; i < m_row_count; ++i) {
        builder.append("<tr>"sv);
        for (auto& column : m_columns) {
            VERIFY(i < column.rows.size());
            builder.appendff("<td style='text-align: {}'>", alignment_string(column.alignment));
            builder.append(column.rows[i].render_to_html());
            builder.append("</td>"sv);
        }
        builder.append("</tr>"sv);
    }
    builder.append("</tbody>"sv);
    builder.append("</table>"sv);

    return builder.to_byte_string();
}

RecursionDecision Table::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;

    for (auto const& column : m_columns) {
        rd = column.walk(visitor);
        if (rd == RecursionDecision::Break)
            return rd;
    }

    return RecursionDecision::Continue;
}

OwnPtr<Table> Table::parse(LineIterator& lines)
{
    auto peek_it = lines;
    auto first_line = *peek_it;
    if (!first_line.starts_with('|'))
        return {};

    ++peek_it;

    if (peek_it.is_end())
        return {};

    auto header_segments = first_line.split_view('|', SplitBehavior::KeepEmpty);
    auto header_delimiters = peek_it->split_view('|', SplitBehavior::KeepEmpty);

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
        return {};

    if (header_delimiters.is_empty())
        return {};

    size_t total_width = 0;

    auto table = make<Table>();
    table->m_columns.resize(header_delimiters.size());

    for (size_t i = 0; i < header_segments.size(); ++i) {
        auto text = Text::parse(header_segments[i]);

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
                dbgln_if(MARKDOWN_DEBUG, "Invalid character _{}_ in table heading delimiter (ignored)", ch);
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
        auto line = *lines;
        if (!line.starts_with('|'))
            break;

        ++lines;

        auto segments = line.split_view('|', SplitBehavior::KeepEmpty);
        segments.take_first();
        if (!segments.is_empty() && segments.last().is_empty())
            segments.take_last();
        ++row_count;

        for (size_t i = 0; i < header_segments.size(); ++i) {
            if (i >= segments.size()) {
                // Ran out of segments, but still have headers.
                // Just make an empty cell.
                table->m_columns[i].rows.append(Text::parse(""sv));
            } else {
                auto text = Text::parse(segments[i]);
                table->m_columns[i].rows.append(move(text));
            }
        }
    }

    table->m_row_count = row_count;

    return table;
}

RecursionDecision Table::Column::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;

    rd = header.walk(visitor);
    if (rd != RecursionDecision::Recurse)
        return rd;

    for (auto const& row : rows) {
        rd = row.walk(visitor);
        if (rd == RecursionDecision::Break)
            return rd;
    }

    return RecursionDecision::Continue;
}

}
