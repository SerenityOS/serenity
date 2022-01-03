/*
 * Copyright (c) 2021-2022, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Format.h>
#include <AK/Random.h>

#define MAX_POINTS INT_MAX / 2

namespace TicTacToe {

static Game* s_the;

Game& Game::the()
{
    if (!s_the)
        s_the = new Game;
    return *s_the;
}

bool Game::do_move(uint8_t cell_index)
{
    if (!m_moves_remaining || cell_index > s_max_moves - 1 || m_board[cell_index])
        return false;

    m_moves_remaining--;
    m_board[cell_index] = m_current_player;

    WinnerCheckResult winner_check_result = check_if_current_player_won();
    if (winner_check_result.has_won) {
        m_moves_remaining = 0;
        uint16_t victories = m_current_player == Player::X ? ++m_x_victories : ++m_o_victories;
        if (on_win)
            on_win(winner_check_result.cells, m_current_player, victories);

    } else if (!m_moves_remaining) {
        ++m_ties;
        if (on_tie)
            on_tie(m_ties);
    }

    Player next_player = m_current_player == Player::X ? Player::O : Player::X;
    if (on_move)
        on_move(cell_index, m_current_player, next_player);

    if (m_moves_remaining) {
        m_current_player = next_player;
        if (m_mode == Game::Mode::HumanVsMachine && next_player == Player::O)
            do_machine_move();
    }

    return true;
}

void Game::set_mode(Game::Mode mode)
{
    m_mode = mode;
    m_moves_remaining = 0;
}

void Game::start_new_game()
{
    m_current_player = m_current_player == Player::X ? Player::O : Player::X;
    m_moves_remaining = sizeof(m_board);
    memset(m_board, 0, m_moves_remaining);

    if (on_new_game)
        on_new_game();

    if (m_mode == Game::Mode::HumanVsMachine && m_current_player == Player::O)
        do_machine_move();
}

Game::WinnerCheckResult Game::check_if_current_player_won()
{
    return check_if_player_won(m_board, m_current_player);
}

Game::WinnerCheckResult Game::check_if_player_won(uint8_t const board[], Game::Player const player)
{
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

            closed_lin &= (board[lin_cell_index] == player);
            closed_col &= (board[col_cell_index] == player);
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

    bool closed_diagonal_1 = (board[diagonal_cell_index_1] == player);
    bool closed_diagonal_2 = closed_diagonal_1;
    for (uint8_t i = 0; i < 3; i += 2) {
        diagonal_cell_index_1 = row_col_to_cell_index(i, i);
        diagonal_cell_index_2 = row_col_to_cell_index(i, i ? 0 : 2);

        winner_cells_1[i] = diagonal_cell_index_1;
        winner_cells_2[i] = diagonal_cell_index_2;

        closed_diagonal_1 &= (board[diagonal_cell_index_1] == player);
        closed_diagonal_2 &= (board[diagonal_cell_index_2] == player);
    }
    if (closed_diagonal_1 || closed_diagonal_2) {
        check_result.has_won = true;
        memcpy(check_result.cells, (closed_diagonal_1 ? winner_cells_1 : winner_cells_2), 3);
    }
    return check_result;
}

uint8_t Game::row_col_to_cell_index(uint8_t row, uint8_t col)
{
    return col + row * 3;
}

void Game::do_machine_move()
{
    if (!m_moves_remaining)
        return;

    // For Hard difficulty only the first round uses a random move.
    uint8_t max_moves_for_random_move = m_difficulty == Game::Difficulty::Hard ? s_max_moves : s_max_moves - 2;
    if (m_moves_remaining >= max_moves_for_random_move) {
        uint8_t cell_index;
        while (m_board[cell_index])
            cell_index = get_random_uniform(s_max_moves);
        do_move(cell_index);

    } else {
        uint8_t virtual_board[s_max_moves];
        memcpy(virtual_board, m_board, s_max_moves);
        do_move(minimax(virtual_board, Game::Maximize::Yes, m_difficulty, 0).cell_index);
    }
}

Game::BestMove Game::minimax(uint8_t board[], Game::Maximize maximize, uint8_t max_depth, uint8_t depth)
{
    bool is_maximizing = maximize == Game::Maximize::Yes;
    Game::BestMove best_move;

    if (check_if_player_won(board, Game::Player::X).has_won) {
        best_move.score = depth - MAX_POINTS;
        return best_move;
    }
    if (check_if_player_won(board, Game::Player::O).has_won) {
        best_move.score = MAX_POINTS - depth;
        return best_move;
    }

    if (max_depth > 0 && depth > max_depth) {
        best_move.score = 0;
        return best_move;
    }

    int best_score = is_maximizing ? INT_MIN : INT_MAX;
    for (uint8_t cell_index; cell_index < s_max_moves; cell_index++) {
        if (board[cell_index])
            continue;

        board[cell_index] = is_maximizing ? Game::Player::O : Game::Player::X;
        Game::Maximize next_maximize = is_maximizing ? Game::Maximize::No : Game::Maximize::Yes;
        auto possible_best_move = minimax(board, next_maximize, max_depth, depth + 1);
        board[cell_index] = 0;

        best_score = is_maximizing
            ? max(best_score, possible_best_move.score)
            : min(best_score, possible_best_move.score);

        if (best_score == possible_best_move.score) {
            best_move.score = best_score;
            best_move.cell_index = cell_index;
        }
    }

    return best_move;
}

}
