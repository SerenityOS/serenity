/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Board.h"
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
        auto x = rand() % m_dimension;
        auto y = rand() % m_dimension;
        // Find a random square that isn't already fixed
        while (m_squares[x][y].is_fixed()) {
            x = rand() % m_dimension;
            y = rand() % m_dimension;
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
        auto x = rand() % m_dimension;
        auto y = rand() % m_dimension;
        // Find a random square that doesn't already have a value set
        while (squares[x][y].get_value()) {
            x = rand() % m_dimension;
            y = rand() % m_dimension;
        }

        auto value = generate_random_value(&squares, x, y);
        if (value == 0)
            return false;
        squares[x][y].set_value(value);
        squares[x][y].set_fixed(true);
    }
    return is_solveable(squares);
}

bool Board::is_solveable(Vector<Vector<Square>> squares)
{
    for (size_t x = 0; x < m_dimension; x++) {
        for (size_t y = 0; y < m_dimension; y++) {
            if (squares[x][y].get_value() == 0) {
                Vector<int> tried;
                while (tried.size() < m_dimension) {
                    int value = generate_random_value(&squares, x, y, tried);
                    if (value == 0)
                        return false;
                    tried.append(value);
                    squares[x][y].set_value(value);
                    return is_solveable(squares);
                }
                return false;
            }
        }
    }
    m_squares = squares;
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

int Board::generate_random_value(Vector<Vector<Square>>* squares, int x, int y,
    Vector<int> invalid)
{
    // A vector respresenting whether the index value is a valid option.
    // Faster than storing a Vector of 1...9 then searching for and removing
    // options

    // FIXME: This section could do with performance improvements, it's the
    // slowest part of generating a new board due to copying
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

    for (int i : invalid) {
        options[i - 1] = false;
    }

    Vector<int> valid_options;
    for (size_t i = 0; i < options.size(); i++) {
        if (options[i])
            valid_options.append((int)i + 1);
    }
    if (valid_options.size() > 0) {
        int index = rand() % valid_options.size();
        return valid_options[index];
    }
    return 0;
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
