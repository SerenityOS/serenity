/*
 * Copyright (c) 2021, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Cell.h"
#include "Game.h"
#include <LibGUI/Widget.h>

namespace TicTacToe {

class Board final : public GUI::Widget {
    C_OBJECT(Board);

public:
    bool make_move(int, Game::Player);
    void clear();

private:
    RefPtr<Cell> get_cell(int);
};

}
