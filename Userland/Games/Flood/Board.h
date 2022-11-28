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
    void set_cell(size_t row, size_t column, int value);
    int cell(size_t row, size_t column) const;
    auto const& cells() const { return m_cells; }

    void clear();
    void randomize();
    void reset();
    void resize(size_t rows, size_t columns);
    u32 update_values(bool only_calculate_flooded_area = false);

    int get_current_value() { return m_current_value; }
    int get_previous_value() { return m_previous_value; }
    Vector<Color> get_color_scheme() { return m_colors; }

    void set_current_value(int new_value);
    void set_color_scheme(Vector<Color> colors);

    struct RowAndColumn {
        size_t row { 0 };
        size_t column { 0 };
    };

private:
    size_t m_rows { 0 };
    size_t m_columns { 0 };

    int m_current_value;
    int m_previous_value;

    Vector<Color> m_colors;
    Vector<Vector<int>> m_cells;
};
