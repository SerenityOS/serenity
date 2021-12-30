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

bool Board::make_move(int cell_index, Game::Player player) {
    if(cell_index < 0 || cell_index > 8)
        return false;

    auto cell = get_cell(cell_index);

    if(!cell->is_empty())
        return false;

    if(player == Game::Player::X)
        cell->set_content(Cell::Content::X);
    else if(player == Game::Player::O)
        cell->set_content(Cell::Content::O);

    return true;
}

void Board::clear() {
    for(int i = 0; i < 9; i++) {
        get_cell(i)->set_content(Cell::Content::Empty);
    }
}

RefPtr<Cell> Board::get_cell(int cell_index) {
    auto cell_name = String::formatted("cell_{}", cell_index);
    return find_descendant_of_type_named<Cell>(cell_name);
}

}
