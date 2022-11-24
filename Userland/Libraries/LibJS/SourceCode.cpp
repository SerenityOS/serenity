/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinarySearch.h>
#include <AK/Utf8View.h>
#include <LibJS/SourceCode.h>
#include <LibJS/SourceRange.h>
#include <LibJS/Token.h>

namespace JS {

NonnullRefPtr<SourceCode> SourceCode::create(String filename, String code)
{
    return adopt_ref(*new SourceCode(move(filename), move(code)));
}

SourceCode::SourceCode(String filename, String code)
    : m_filename(move(filename))
    , m_code(move(code))
{
}

String const& SourceCode::filename() const
{
    return m_filename;
}

String const& SourceCode::code() const
{
    return m_code;
}

void SourceCode::compute_line_break_offsets() const
{
    m_line_break_offsets = Vector<size_t> {};

    if (m_code.is_empty())
        return;

    bool previous_code_point_was_carriage_return = false;
    Utf8View view(m_code.view());
    for (auto it = view.begin(); it != view.end(); ++it) {
        u32 code_point = *it;
        bool is_line_terminator = code_point == '\r' || (code_point == '\n' && !previous_code_point_was_carriage_return) || code_point == LINE_SEPARATOR || code_point == PARAGRAPH_SEPARATOR;
        previous_code_point_was_carriage_return = code_point == '\r';

        if (is_line_terminator)
            m_line_break_offsets->append(view.byte_offset_of(it));
    }
}

SourceRange SourceCode::range_from_offsets(u32 start_offset, u32 end_offset) const
{
    if (m_code.is_empty())
        return { *this, {}, {} };

    if (!m_line_break_offsets.has_value())
        compute_line_break_offsets();

    size_t line = 1;
    size_t nearest_line_break_index = 0;
    size_t nearest_preceding_line_break_offset = 0;

    if (!m_line_break_offsets->is_empty()) {
        binary_search(*m_line_break_offsets, start_offset, &nearest_line_break_index);
        line = 1 + nearest_line_break_index;
        nearest_preceding_line_break_offset = (*m_line_break_offsets)[nearest_line_break_index];
    }

    Position start;
    Position end;

    size_t column = 1;

    bool previous_code_point_was_carriage_return = false;

    Utf8View view(m_code.view());
    for (auto it = view.iterator_at_byte_offset_without_validation(nearest_preceding_line_break_offset); it != view.end(); ++it) {

        if (start_offset == view.byte_offset_of(it)) {
            start = Position {
                .line = line,
                .column = column,
                .offset = start_offset,
            };
        }

        if (end_offset == view.byte_offset_of(it)) {
            end = Position {
                .line = line,
                .column = column,
                .offset = end_offset,
            };
            break;
        }

        u32 code_point = *it;

        bool is_line_terminator = code_point == '\r' || (code_point == '\n' && !previous_code_point_was_carriage_return) || code_point == LINE_SEPARATOR || code_point == PARAGRAPH_SEPARATOR;
        previous_code_point_was_carriage_return = code_point == '\r';

        if (is_line_terminator) {
            ++line;
            column = 1;
            continue;
        }
        ++column;
    }

    return SourceRange { *this, start, end };
}

}
