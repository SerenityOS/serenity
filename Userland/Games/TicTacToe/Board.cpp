/*
 * Copyright (c) 2021, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Board.h"
#include <AK/Random.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Painter.h>

REGISTER_WIDGET(TicTacToe, Board);

namespace TicTacToe {

bool Board::do_move(uint8_t cell_index, Game::Player player)
{
    if (cell_index > 8)
        return false;

    auto cell = get_cell(cell_index);

    if (!cell->is_empty())
        return false;

    if (player == Game::Player::X)
        cell->set_content(Cell::Content::X);
    else if (player == Game::Player::O)
        cell->set_content(Cell::Content::O);

    return true;
}

void Board::highlight_cell(uint8_t cell_index)
{
    get_cell(cell_index)->highlight();
}

void Board::clear()
{
    for (uint8_t i = 0; i < 9; i++) {
        auto cell = get_cell(i);
        cell->set_content(Cell::Content::Empty);
        cell->reset_background();
    }
}

RefPtr<Cell> Board::get_cell(uint8_t cell_index)
{
    auto cell_name = String::formatted("cell_{}", cell_index);
    return find_descendant_of_type_named<Cell>(cell_name);
}

}
