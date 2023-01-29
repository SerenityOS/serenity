/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Board.h"
#include <AK/Random.h>
#include <time.h>

Board::Board(size_t rows, size_t columns)
{
    resize(rows, columns);
}

void Board::run_generation()
{
    m_stalled = true;
    Vector<Vector<bool>> new_cells;
    new_cells.resize(m_rows);
    for (size_t row = 0; row < m_rows; ++row)
        new_cells[row].resize(m_columns);

    for_each_cell([&](auto row, auto column) {
        bool old_value = m_cells[row][column];
        bool new_value = calculate_next_value(row, column);
        new_cells[row][column] = new_value;
        if (old_value != new_value)
            m_stalled = false;
    });

    if (m_stalled)
        return;

    m_cells = move(new_cells);
}

bool Board::calculate_next_value(size_t row, size_t column) const
{
    int top_left = cell(row - 1, column - 1);
    int top_mid = cell(row - 1, column);
    int top_right = cell(row - 1, column + 1);
    int left = cell(row, column - 1);
    int right = cell(row, column + 1);
    int bottom_left = cell(row + 1, column - 1);
    int bottom_mid = cell(row + 1, column);
    int bottom_right = cell(row + 1, column + 1);

    int sum = top_left + top_mid + top_right + left + right + bottom_left + bottom_mid + bottom_right;

    bool old_value = m_cells[row][column];
    bool new_value = old_value;

    if (old_value) {
        if (sum < 2 || sum > 3)
            new_value = false;
    } else {
        if (sum == 3)
            new_value = true;
    }

    return new_value;
}

void Board::clear()
{
    for_each_cell([this](auto row, auto column) {
        set_cell(row, column, false);
    });
}

void Board::randomize()
{
    for_each_cell([this](auto row, auto column) {
        set_cell(row, column, get_random<u32>() % 2);
    });
}

void Board::resize(size_t rows, size_t columns)
{
    m_rows = rows;
    m_columns = columns;

    // Vector values get default-initialized, we don't need to set them to false explicitly.
    m_cells.resize(rows);
    for (size_t row = 0; row < rows; ++row)
        m_cells[row].resize(columns);
}

void Board::toggle_cell(size_t row, size_t column)
{
    VERIFY(row < m_rows && column < m_columns);

    m_cells[row][column] = !m_cells[row][column];
}

void Board::set_cell(size_t row, size_t column, bool on)
{
    VERIFY(row < m_rows && column < m_columns);

    m_cells[row][column] = on;
}

bool Board::cell(size_t row, size_t column) const
{
    if (row >= m_rows || column >= m_columns)
        return false;

    return m_cells[row][column];
}
