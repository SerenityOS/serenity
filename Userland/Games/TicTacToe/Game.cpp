/*
 * Copyright (c) 2021, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Format.h>
#include <AK/Random.h>

namespace TicTacToe {

static Game* s_the;

Game& Game::the()
{
    if(!s_the)
        s_the = new Game;
    return *s_the;
}

bool Game::make_move(uint8_t cell_index) {
    if(!m_moves_remaining || cell_index > 8 || m_board[cell_index])
        return false;

    m_moves_remaining--;
    m_board[cell_index] = m_current_player;

    if(on_move)
        on_move(cell_index, m_current_player);

    WinnerCheckResult winner_check_result = check_if_current_player_won();
    if(winner_check_result.has_won) {
        m_moves_remaining = 0;
        uint16_t victories = m_current_player == Player::X ? ++m_x_victories : ++m_o_victories;
        if(on_win)
            on_win(winner_check_result.cells, m_current_player, victories);

    } else if(!m_moves_remaining) {
        if(on_tie)
            on_tie(++m_ties);
    }

    m_current_player = m_current_player == Player::X ? Player::O : Player::X;

    return true;
}

void Game::start_new_game()
{
    m_moves_remaining = sizeof(m_board);
    memset(m_board, 0, m_moves_remaining);
    if(on_new_game)
        on_new_game();
}

void Game::reset()
{
}

Game::WinnerCheckResult Game::check_if_current_player_won() {
    WinnerCheckResult check_result;

    uint8_t winner_cells_1[3];
    uint8_t winner_cells_2[3];

    // Check lines and columns
    for (uint8_t i = 0; i < 3; i++) {
        bool closed_lin = true;
        bool closed_col = true;
        for (uint8_t j = 0; j < 3; j++) {
            uint8_t lin_cell_index = row_col_to_cell_index(i, j);
            uint8_t col_cell_index = row_col_to_cell_index(j, i);

            winner_cells_1[j] = lin_cell_index;
            winner_cells_2[j] = col_cell_index;

            closed_lin &= (m_board[lin_cell_index] == m_current_player);
            closed_col &= (m_board[col_cell_index] == m_current_player);
        }
        if (closed_lin || closed_col) {
            check_result.has_won = true;
            memcpy(check_result.cells, (closed_lin ? winner_cells_1 : winner_cells_2), 3);
            return check_result;
        }
    }

    // Check diagonals
    uint8_t diagonal_cell_index_1 = row_col_to_cell_index(1, 1);
    uint8_t diagonal_cell_index_2 = diagonal_cell_index_1;

    winner_cells_1[1] = diagonal_cell_index_1;
    winner_cells_2[1] = diagonal_cell_index_2;

    bool closed_diagonal_1 = (m_board[diagonal_cell_index_1] == m_current_player);
    bool closed_diagonal_2 = closed_diagonal_1;
    for(uint8_t i = 0; i < 3; i += 2) {
        diagonal_cell_index_1 = row_col_to_cell_index(i, i);
        diagonal_cell_index_2 = row_col_to_cell_index(i, i ? 0 : 2);

        winner_cells_1[i] = diagonal_cell_index_1;
        winner_cells_2[i] = diagonal_cell_index_2;

        closed_diagonal_1 &= (m_board[diagonal_cell_index_1] == m_current_player);
        closed_diagonal_2 &= (m_board[diagonal_cell_index_2] == m_current_player);
    }
    if(closed_diagonal_1 || closed_diagonal_2) {
        check_result.has_won = true;
        memcpy(check_result.cells, (closed_diagonal_1 ? winner_cells_1 : winner_cells_2), 3);
    }
    return check_result;
}

uint8_t Game::row_col_to_cell_index(uint8_t row, uint8_t col) {
    return col + row * 3;
}

}
