/*
 * Copyright (c) 2021, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>

namespace TicTacToe {

class Game final {
public:
    static constexpr int width = 342;
    static constexpr int height = 342;

    static Game& the();

    enum Player {
        X = 1, O = 2
    };

    bool make_move(uint8_t);

    void start_new_game();
    void reset();
    Player get_current_player() { return m_current_player; };

    Function<void()> on_new_game;
    Function<void(uint8_t const cell_index, Player const)> on_move;
    Function<void(uint8_t* const winner_cells, Player const, uint16_t num_victories)> on_win;
    Function<void(uint16_t num_ties)> on_tie;

private:
    struct WinnerCheckResult  {
        bool has_won = false;
        uint8_t cells[3];
    };

    WinnerCheckResult check_if_current_player_won();
    uint8_t row_col_to_cell_index(uint8_t row, uint8_t col);

    Player m_current_player = { Player::X };
    uint8_t m_board[9];
    uint8_t m_moves_remaining = 0;
    uint16_t m_x_victories = 0;
    uint16_t m_o_victories = 0;
    uint16_t m_ties = 0;
};

}
