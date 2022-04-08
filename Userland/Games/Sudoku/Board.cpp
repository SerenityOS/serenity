/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Board.h"
#include <AK/Random.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>

Board::Board() { new_game(); }

void Board::new_game()
{
    // Create fully solved board
    while (!try_create_board()) { }
    // Fix extra numbers if needed
    for (auto i = 0; i < m_number_provided - 17; i++) {
        auto x = get_random<size_t>() % m_dimension;
        auto y = get_random<size_t>() % m_dimension;
        // Find a random square that isn't already fixed
        while (m_squares[x][y].is_fixed()) {
            x = get_random<size_t>() % m_dimension;
            y = get_random<size_t>() % m_dimension;
        }
        m_squares[x][y].set_fixed(true);
    }
    // Hide the answers but store them to make validation instant
    for (size_t x = 0; x < m_dimension; x++) {
        for (size_t y = 0; y < m_dimension; y++) {
            auto value = m_squares[x][y].get_value();
            m_squares[x][y].set_answer(value);
            m_squares[x][y].set_value(0);
        }
    }
}

bool Board::try_create_board()
{
    Vector<Vector<Square>> squares;
    squares.resize(m_dimension);
    for (size_t x = 0; x < m_dimension; x++) {
        squares[x].ensure_capacity(m_dimension);
        for (size_t y = 0; y < m_dimension; y++) {
            Square new_square = Square(x, y);
            squares[x].unchecked_append(new_square);
        }
    }

    // Randomly 17 numbers to the board - the lowest number to provide a unique
    // solution - https://arxiv.org/pdf/1201.0749.pdf
    for (auto i = 0; i < 17; i++) {
        auto x = get_random<size_t>() % m_dimension;
        auto y = get_random<size_t>() % m_dimension;
        // Find a random square that doesn't already have a value set
        while (squares[x][y].get_value()) {
            x = get_random<size_t>() % m_dimension;
            y = get_random<size_t>() % m_dimension;
        }

        auto possible_values = generate_possible_values(&squares, x, y);
        if (possible_values.size() == 0)
            return false;
        size_t index = get_random<size_t>() % possible_values.size();
        int value = possible_values[index];
        squares[x][y].set_value(value);
        squares[x][y].set_fixed(true);
    }
    return is_solveable(&squares);
}

bool Board::is_solveable(Vector<Vector<Square>>* squares)
{
    for (size_t x = 0; x < m_dimension; x++) {
        for (size_t y = 0; y < m_dimension; y++) {
            if (squares->at(x)[y].get_value() == 0) {
                Vector<int> possible_values = generate_possible_values(squares, x, y);
                while (possible_values.size() > 0) {
                    int value = possible_values.take_first();
                    if (value == 0) {
                        squares->at(x)[y].set_value(0);
                        return false;
                    }
                    squares->at(x)[y].set_value(value);
                    if (is_solveable(squares))
                        return true;
                }
                squares->at(x)[y].set_value(0);
                return false;
            }
        }
    }
    m_squares = *squares;
    return true;
}

bool Board::is_valid(Vector<Vector<Square>>* squares, int x, int y, int value)
{
    for (size_t i = 0; i < m_dimension; i++) {
        int row_value = squares->at(x)[i].get_value();
        if (value == row_value)
            return false;

        int column_value = squares->at(i)[y].get_value();
        if (value == column_value)
            return false;
    }

    Vector<int> sub_square_values = get_sub_square(squares, x, y);
    return !sub_square_values.contains_slow(value);
}

Vector<int> Board::get_sub_square(Vector<Vector<Square>>* squares, int x,
    int y)
{
    // Traverse the 3x3 square that contains the position x,y and return all none
    // 0 values
    Vector<int> values;
    int start_x = x - (x % 3);
    int start_y = y - (y % 3);
    for (size_t i = 0; i < m_dimension; i++) {
        int value = squares->at(start_x)[start_y].get_value();
        if (value != 0)
            values.append(value);
        start_x++;
        if (start_x % 3 == 0) {
            start_x = start_x - 3;
            start_y++;
        }
    }
    return values;
}

Vector<int> Board::generate_possible_values(Vector<Vector<Square>>* squares, int x, int y)
{
    Vector<bool> options = { true, true, true, true, true, true, true, true, true };

    for (size_t i = 0; i < m_dimension; i++) {
        int row_value = squares->at(x)[i].get_value();
        int column_value = squares->at(i)[y].get_value();
        if (row_value != 0)
            options[row_value - 1] = false;
        if (column_value != 0)
            options[column_value - 1] = false;
    }

    for (int i : get_sub_square(squares, x, y)) {
        options[i - 1] = false;
    }

    Vector<int> valid_options;
    for (size_t i = 0; i < options.size(); i++) {
        if (options[i])
            valid_options.append((int)i + 1);
    }
    return valid_options;
}

bool Board::is_board_solved()
{
    for (size_t x = 0; x < m_dimension; x++) {
        for (size_t y = 0; y < m_dimension; y++) {
            if (!m_squares[x][y].is_correct()) {
                return false;
            }
        }
    }
    return true;
}
