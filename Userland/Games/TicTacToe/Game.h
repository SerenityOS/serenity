/*
 * Copyright (c) 2021, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>

namespace TicTacToe {

struct WinnerCheckResult  {
    bool has_won = false;
    uint8_t cells[3];
};

class Game final {
public:
    static constexpr int width = 340;
    static constexpr int height = 340;

    static Game& the();

    enum Player {
        X = 1, O = 2
    };

    bool make_move(uint8_t);

    void start_new_game();
    void reset();

    Function<void()> on_new_game;
    Function<void(uint8_t const cell_index, Player const)> on_move;
    Function<void(uint8_t* const winner_cells, Player const)> on_win;
    Function<void()> on_tae;

private:
    WinnerCheckResult check_if_current_player_won();
    uint8_t row_col_to_cell_index(uint8_t row, uint8_t col);

    Player m_current_player = { Player::X };
    uint8_t m_board[9];
    uint8_t m_moves_remaining = 0;
};

}
