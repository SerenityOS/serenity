/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibGUI/Dialog.h>

namespace TwentyFourtyEight {
class GameSizeDialog : public GUI::Dialog {
    C_OBJECT(GameSizeDialog)
public:
    size_t board_size() const { return m_board_size; }
    u32 target_tile() const { return 1u << m_target_tile_power; }
    bool evil_ai() const { return m_evil_ai; }
    bool temporary() const { return m_temporary; }

private:
    GameSizeDialog(GUI::Window* parent, size_t board_size, size_t target_tile, bool evil_ai);

    size_t m_board_size;
    size_t m_target_tile_power;
    bool m_evil_ai;
    bool m_temporary { false };
};
}
