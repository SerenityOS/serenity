/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AnyOf.h>
#include <AK/DistinctNumeric.h>
#include <AK/Noncopyable.h>
#include <AK/Vector.h>
#include <LibVT/Attribute.h>
#include <LibVT/Position.h>
#include <LibVT/XtermColors.h>

namespace VT {

AK_TYPEDEF_DISTINCT_ORDERED_ID(u32, Mark);

inline static constexpr Mark Unmarked = 0;

class Line {
    AK_MAKE_NONCOPYABLE(Line);
    AK_MAKE_NONMOVABLE(Line);

public:
    explicit Line(size_t length);
    ~Line() = default;

    struct Cell {
        u32 code_point { ' ' };
        Attribute attribute;

        bool operator!=(Cell const& other) const { return code_point != other.code_point || attribute != other.attribute; }
    };

    Attribute const& attribute_at(size_t index) const { return m_cells[index].attribute; }
    Attribute& attribute_at(size_t index) { return m_cells[index].attribute; }

    Cell& cell_at(size_t index) { return m_cells[index]; }
    Cell const& cell_at(size_t index) const { return m_cells[index]; }

    void clear(Attribute const& attribute = Attribute())
    {
        m_terminated_at.clear();
        m_mark = Unmarked;
        clear_range(0, m_cells.size() - 1, attribute);
    }
    void clear_range(size_t first_column, size_t last_column, Attribute const& attribute = Attribute());
    bool has_only_one_background_color() const;

    bool is_empty() const
    {
        return !any_of(m_cells, [](auto& cell) { return cell != Cell(); });
    }

    size_t length() const
    {
        return m_cells.size();
    }
    void set_length(size_t);
    void rewrap(size_t new_length, Line* next_line, CursorPosition* cursor, bool cursor_is_on_next_line = true);

    u32 code_point(size_t index) const
    {
        return m_cells[index].code_point;
    }

    void set_code_point(size_t index, u32 code_point)
    {
        if (m_terminated_at.has_value()) {
            if (index > *m_terminated_at) {
                m_terminated_at = index + 1;
            }
        }

        m_cells[index].code_point = code_point;
    }

    bool is_dirty() const { return m_dirty; }
    void set_dirty(bool b) { m_dirty = b; }

    Optional<Mark> mark() const
    {
        return m_mark == Unmarked ? OptionalNone {} : Optional<Mark>(m_mark);
    }
    void set_marked(Mark mark)
    {
        set_dirty(m_mark != mark);
        m_mark = mark;
    }

    Optional<u16> termination_column() const { return m_terminated_at; }
    void set_terminated(u16 column) { m_terminated_at = column; }

private:
    void take_cells_from_next_line(size_t new_length, Line* next_line, bool cursor_is_on_next_line, CursorPosition* cursor);
    void push_cells_into_next_line(size_t new_length, Line* next_line, bool cursor_is_on_next_line, CursorPosition* cursor);

    Vector<Cell> m_cells;
    Mark m_mark { Unmarked };
    bool m_dirty { false };
    // Note: The alignment is 8, so this member lives in the padding (that already existed before it was introduced)
    [[no_unique_address]] Optional<u16> m_terminated_at;
};

}
