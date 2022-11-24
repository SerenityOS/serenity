/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Queue.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibGfx/Point.h>
#include <stdio.h>

class Board {
public:
    Board(size_t rows, size_t columns);
    ~Board() = default;

    size_t columns() const { return m_columns; }
    size_t rows() const { return m_rows; }

    bool is_flooded() const;
    void set_cell(size_t row, size_t column, Color color);
    Color cell(size_t row, size_t column) const;
    auto const& cells() const { return m_cells; }

    void clear();
    void randomize();
    void reset();
    void resize(size_t rows, size_t columns);
    u32 update_colors(bool only_calculate_flooded_area = false);

    Color get_current_color() { return m_current_color; }
    Color get_previous_color() { return m_previous_color; }
    Vector<Color> get_color_scheme() { return m_colors; }

    void set_current_color(Color new_color);
    void set_color_scheme(Vector<Color> colors);

    struct RowAndColumn {
        size_t row { 0 };
        size_t column { 0 };
    };

private:
    size_t m_rows { 0 };
    size_t m_columns { 0 };

    Color m_current_color;
    Color m_previous_color;

    Vector<Color> m_colors;
    Vector<Vector<Color>> m_cells;
};
