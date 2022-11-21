/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

SourceRange SourceCode::range_from_offsets(u32 start_offset, u32 end_offset) const
{
    Position start;
    Position end;

    size_t line = 1;
    size_t column = 1;

    bool previous_code_point_was_carriage_return = false;

    Utf8View view(m_code);
    for (auto it = view.begin(); it != view.end(); ++it) {

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
