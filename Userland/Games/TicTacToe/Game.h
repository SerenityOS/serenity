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
    static constexpr int width = 340;
    static constexpr int height = 340;

    static Game& the();

    enum Player {
        X, O
    };

    bool make_move(int);

    void start();
    void reset();

    Function<void(const int cell_index, const Player)> on_move;

private:
    Player m_current_player = { Player::X };
};

}
