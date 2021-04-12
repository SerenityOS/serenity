/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Board.h"
#include <AK/Random.h>
#include <time.h>

Board::Board(size_t rows, size_t columns)
    : m_columns(columns)
    , m_rows(rows)
{
    m_cells.resize(total_size());
    for (size_t i = 0; i < total_size(); ++i) {
        m_cells[i] = false;
    }
}

Board::~Board()
{
}

void Board::run_generation()
{
    m_stalled = true;
    Vector<bool> new_cells;
    new_cells.resize(total_size());

    for (size_t i = 0; i < total_size(); ++i) {
        bool old_val = m_cells[i];
        new_cells[i] = calculate_next_value(i);
        if (old_val != new_cells[i]) {
            m_stalled = false;
        }
    }

    if (m_stalled)
        return;

    m_cells = new_cells;
}

bool Board::calculate_next_value(size_t index) const
{
    size_t row = index / columns();
    size_t column = index % columns();

    int top_left = cell(row - 1, column - 1);
    int top_mid = cell(row - 1, column);
    int top_right = cell(row - 1, column + 1);
    int left = cell(row, column - 1);
    int right = cell(row, column + 1);
    int bottom_left = cell(row + 1, column - 1);
    int bottom_mid = cell(row + 1, column);
    int bottom_right = cell(row + 1, column + 1);

    int sum = top_left + top_mid + top_right + left + right + bottom_left + bottom_mid + bottom_right;

    bool current = m_cells[index];
    bool new_value = current;

    if (current) {
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
    for (size_t i = 0; i < total_size(); ++i)
        set_cell(i, false);
}

void Board::randomize()
{
    for (size_t i = 0; i < total_size(); ++i)
        set_cell(i, get_random<u32>() % 2);
}

void Board::toggle_cell(size_t index)
{
    VERIFY(index < total_size());

    m_cells[index] = !m_cells[index];
}

void Board::toggle_cell(size_t row, size_t column)
{
    VERIFY(column < total_size() && row < total_size());

    size_t index = calculate_index(row, column);
    set_cell(index, !m_cells[index]);
}

void Board::set_cell(size_t index, bool on)
{
    VERIFY(index < total_size());

    m_cells[index] = on;
}

void Board::set_cell(size_t row, size_t column, bool on)
{
    VERIFY(column < total_size() && row < total_size());

    size_t index = calculate_index(row, column);
    set_cell(index, on);
}

bool Board::cell(size_t index) const
{
    if (index > total_size() - 1)
        return false;

    return m_cells[index];
}

bool Board::cell(size_t row, size_t column) const
{
    if (column > total_size() - 1 || row > total_size() - 1)
        return false;

    size_t index = calculate_index(row, column);
    return cell(index);
}
