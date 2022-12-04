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

NonnullRefPtr<SourceCode> SourceCode::create(DeprecatedString filename, DeprecatedString code)
{
    return adopt_ref(*new SourceCode(move(filename), move(code)));
}

SourceCode::SourceCode(DeprecatedString filename, DeprecatedString code)
    : m_filename(move(filename))
    , m_code(move(code))
{
}

DeprecatedString const& SourceCode::filename() const
{
    return m_filename;
}

DeprecatedString const& SourceCode::code() const
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
    // If the underlying code is an empty string, the range is 1,1 - 1,1 no matter what.
    if (m_code.is_empty())
        return { *this, { .line = 1, .column = 1, .offset = 0 }, { .line = 1, .column = 1, .offset = 0 } };

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

    Optional<Position> start;
    Optional<Position> end;

    size_t column = 1;

    bool previous_code_point_was_carriage_return = false;

    Utf8View view(m_code.view());
    for (auto it = view.iterator_at_byte_offset_without_validation(nearest_preceding_line_break_offset); it != view.end(); ++it) {

        // If we're on or after the start offset, this is the start position.
        if (!start.has_value() && view.byte_offset_of(it) >= start_offset) {
            start = Position {
                .line = line,
                .column = column,
                .offset = start_offset,
            };
        }

        // If we're on or after the end offset, this is the end position.
        if (!end.has_value() && view.byte_offset_of(it) >= end_offset) {
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

    // If we didn't find both a start and end position, just return 1,1-1,1.
    // FIXME: This is a hack. Find a way to return the nicest possible values here.
    if (!start.has_value() || !end.has_value())
        return SourceRange { *this, { .line = 1, .column = 1 }, { .line = 1, .column = 1 } };

    return SourceRange { *this, *start, *end };
}

}
