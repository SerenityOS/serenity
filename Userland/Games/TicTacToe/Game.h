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
        X = 1,
        O = 2
    };

    enum Mode {
        HumanVsHuman,
        HumanVsMachine
    };

    bool do_move(uint8_t);
    void do_machine_move();
    void start_new_game();
    Player current_player() { return m_current_player; };
    Mode mode() { return m_mode; }
    void set_mode(Mode mode);
    uint16_t moves_remaining() { return m_moves_remaining; }

    Function<void()> on_new_game;
    Function<void(uint8_t const cell_index, Player const current_player, const Player next_player)> on_move;
    Function<void(uint8_t const* winner_cells, Player const, uint16_t const num_victories)> on_win;
    Function<void(uint16_t num_ties)> on_tie;

private:
    static const uint8_t s_max_moves { 9 };

    struct WinnerCheckResult {
        bool has_won = false;
        uint8_t cells[3];
    };

    struct BestMove {
        int score { 0 };
        uint8_t cell_index { 0 };
    };

    enum Maximize {
        Yes,
        No
    };

    WinnerCheckResult check_if_current_player_won();
    WinnerCheckResult check_if_player_won(uint8_t const board[], Game::Player const);
    uint8_t row_col_to_cell_index(uint8_t row, uint8_t col);
    BestMove minimax(uint8_t board[], Maximize, uint8_t max_depth, uint8_t depth);

    Player m_current_player { Player::O };
    uint8_t m_board[s_max_moves];
    uint8_t m_moves_remaining { 0 };
    uint16_t m_x_victories { 0 };
    uint16_t m_o_victories { 0 };
    uint16_t m_ties { 0 };
    Mode m_mode { Mode::HumanVsMachine };
};

}
