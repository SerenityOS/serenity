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

void Board::draw_presentation_pattern()
{
    Cell::Content content = Cell::Content::X;
    for (uint8_t cell_index = 0; cell_index < 9; cell_index++) {
        auto cell = get_cell(cell_index);
        cell->reset_background();
        cell->set_content(content);
        content = content == Cell::Content::X ? Cell::Content::O : Cell::Content::X;
    }
}

void Board::clear()
{
    for (uint8_t cell_index = 0; cell_index < 9; cell_index++) {
        auto cell = get_cell(cell_index);
        cell->reset_background();
        cell->set_content(Cell::Content::Empty);
    }
}

RefPtr<Cell> Board::get_cell(uint8_t cell_index)
{
    auto cell_name = String::formatted("cell_{}", cell_index);
    return find_descendant_of_type_named<Cell>(cell_name);
}

}
