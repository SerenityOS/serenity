/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Point.h>
#include <stdio.h>

class Board {
public:
    Board(size_t rows, size_t columns);
    ~Board() = default;

    size_t columns() const { return m_columns; }
    size_t rows() const { return m_rows; }

    void toggle_cell(size_t row, size_t column);
    void set_cell(size_t row, size_t column, bool on);
    bool cell(size_t row, size_t column) const;
    Vector<Vector<bool>> const& cells() const { return m_cells; }

    void run_generation();
    bool is_stalled() const { return m_stalled; }

    void clear();
    void randomize();
    void resize(size_t rows, size_t columns);

    struct RowAndColumn {
        size_t row { 0 };
        size_t column { 0 };
    };

private:
    bool calculate_next_value(size_t row, size_t column) const;

    template<typename Callback>
    void for_each_cell(Callback callback)
    {
        for (size_t row = 0; row < m_rows; ++row) {
            for (size_t column = 0; column < m_columns; ++column)
                callback(row, column);
        }
    }

    size_t m_rows { 0 };
    size_t m_columns { 0 };

    bool m_stalled { false };

    Vector<Vector<bool>> m_cells;
};
