/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Square.h"
#include <AK/Vector.h>

class Board final {

public:
    Board();
    Square* get_square(int x, int y) { return &m_squares[x][y]; };
    size_t const& dimension() const { return m_dimension; }
    bool is_board_solved();
    void new_game();

private:
    size_t m_dimension { 9 };
    Vector<Vector<Square>> m_squares;
    bool is_valid(Vector<Vector<Square>>* squares, int x, int y, int value);
    int generate_random_value(Vector<Vector<Square>>* squares, int x, int y,
        Vector<int> invalid = {});
    bool try_create_board();
    bool is_solveable(Vector<Vector<Square>>* squares);
    int m_number_provided { 45 };
    Vector<int> get_sub_square(Vector<Vector<Square>>* squares, int x, int y);
};
