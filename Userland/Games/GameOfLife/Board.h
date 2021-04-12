/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Point.h>
#include <stdio.h>

class Board {
public:
    Board(size_t rows, size_t column);
    ~Board();

    size_t total_size() const { return m_columns * m_rows; }
    size_t columns() const { return m_columns; }
    size_t rows() const { return m_rows; }

    size_t calculate_index(size_t row, size_t column) const { return row * m_columns + column; };

    void toggle_cell(size_t index);
    void toggle_cell(size_t row, size_t column);

    void set_cell(size_t row, size_t column, bool on);
    void set_cell(size_t index, bool on);

    bool cell(size_t row, size_t column) const;
    bool cell(size_t index) const;

    const Vector<bool>& cells() const { return m_cells; }

    void run_generation();
    bool is_stalled() const { return m_stalled; }

    void clear();
    void randomize();

private:
    bool calculate_next_value(size_t index) const;

    size_t m_columns { 1 };
    size_t m_rows { 1 };

    bool m_stalled { false };

    Vector<bool> m_cells;
};
