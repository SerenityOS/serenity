/*
 * Copyright (c) 2021, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Random.h>

namespace TicTacToe {

static Game* s_the;


Game& Game::the()
{
    if(!s_the)
        s_the = new Game;
    return *s_the;
}

bool Game::make_move(int cell_index) {
    if(on_move)
        on_move(cell_index, m_current_player);

    m_current_player = m_current_player == Player::X ? Player::O : Player::X;
    return true;
}

void Game::start()
{

}

void Game::reset()
{
    //m_board = Board(Color::Black);
}

}
