/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibVT/Attribute.h>
#include <LibVT/XtermColors.h>

namespace VT {

class Line {
    AK_MAKE_NONCOPYABLE(Line);
    AK_MAKE_NONMOVABLE(Line);

public:
    explicit Line(size_t length);
    ~Line();

    struct Cell {
        u32 code_point { ' ' };
        Attribute attribute;
    };

    const Attribute& attribute_at(size_t index) const { return m_cells[index].attribute; }
    Attribute& attribute_at(size_t index) { return m_cells[index].attribute; }

    Cell& cell_at(size_t index) { return m_cells[index]; }
    const Cell& cell_at(size_t index) const { return m_cells[index]; }

    void clear(const Attribute&);
    bool has_only_one_background_color() const;

    size_t length() const { return m_cells.size(); }
    void set_length(size_t);

    u32 code_point(size_t index) const
    {
        return m_cells[index].code_point;
    }

    void set_code_point(size_t index, u32 code_point)
    {
        m_cells[index].code_point = code_point;
    }

    bool is_dirty() const { return m_dirty; }
    void set_dirty(bool b) { m_dirty = b; }

private:
    Vector<Cell> m_cells;
    bool m_dirty { false };
};

}
