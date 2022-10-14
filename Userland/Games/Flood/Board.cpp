/*
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

void Board::clear()
{
    for (size_t row = 0; row < m_rows; ++row) {
        for (size_t column = 0; column < m_columns; ++column) {
            set_cell(row, column, 0);
        }
    }
}

bool Board::is_flooded() const
{
    auto first_cell_value = cell(0, 0);
    for (size_t row = 0; row < rows(); ++row) {
        for (size_t column = 0; column < columns(); ++column) {
            if (first_cell_value == cell(row, column))
                continue;
            return false;
        }
    }
    return true;
}

void Board::randomize()
{
    for (size_t row = 0; row < m_rows; ++row) {
        for (size_t column = 0; column < m_columns; ++column) {
            set_cell(row, column, get_random_uniform(m_colors.size()));
        }
    }
    m_previous_value = m_cells[0][0];
    m_current_value = m_cells[0][0];
}

void Board::resize(size_t rows, size_t columns)
{
    m_rows = rows;
    m_columns = columns;

    // Vector values get default-initialized, we don't need to set them explicitly.
    m_cells.resize(rows);
    for (size_t row = 0; row < rows; ++row)
        m_cells[row].resize(columns);
}

void Board::set_cell(size_t row, size_t column, int value)
{
    VERIFY(row < m_rows && column < m_columns);

    m_cells[row][column] = value;
}

int Board::cell(size_t row, size_t column) const
{
    return m_cells[row][column];
}

void Board::set_current_value(int new_value)
{
    m_previous_value = m_current_value;
    m_current_value = new_value;
}

void Board::set_color_scheme(Vector<Color> colors)
{
    VERIFY(colors.size() == 8);
    m_colors = move(colors);
}

void Board::reset()
{
    clear();
    m_current_value = 0;
    m_previous_value = 0;
}

// Adapted from Userland/PixelPaint/Tools/BucketTool.cpp::flood_fill.
u32 Board::update_values(bool only_calculate_flooded_area)
{
    Queue<Gfx::IntPoint> points_to_visit;

    points_to_visit.enqueue({ 0, 0 });
    set_cell(0, 0, get_current_value());

    Vector<Vector<bool>> visited_board;
    visited_board.resize(cells().size());
    for (size_t row = 0; row < cells().size(); ++row)
        visited_board[row].resize(cells()[row].size());
    u32 painted = 1;

    // This implements a non-recursive flood fill. This is a breadth-first search of paintable neighbors
    // As we find neighbors that are paintable we update their pixel, add them to the queue, and mark them in the "visited_board".
    while (!points_to_visit.is_empty()) {
        auto current_point = points_to_visit.dequeue();
        auto candidate_points = Array {
            current_point.moved_left(1),
            current_point.moved_right(1),
            current_point.moved_up(1),
            current_point.moved_down(1)
        };
        for (auto candidate_point : candidate_points) {
            if (candidate_point.y() >= static_cast<int>(m_rows) || candidate_point.x() >= static_cast<int>(m_columns) || candidate_point.y() < 0 || candidate_point.x() < 0)
                continue;
            if (!visited_board[candidate_point.y()][candidate_point.x()] && cell(candidate_point.y(), candidate_point.x()) == (only_calculate_flooded_area ? get_current_value() : get_previous_value())) {
                ++painted;
                points_to_visit.enqueue(candidate_point);
                visited_board[candidate_point.y()][candidate_point.x()] = true;
                if (!only_calculate_flooded_area)
                    set_cell(candidate_point.y(), candidate_point.x(), get_current_value());
            }
        }
    }
    return painted;
}
