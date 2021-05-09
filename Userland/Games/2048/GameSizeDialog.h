/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibGUI/Dialog.h>

class GameSizeDialog : public GUI::Dialog {
    C_OBJECT(GameSizeDialog)
public:
    GameSizeDialog(GUI::Window* parent);

    size_t board_size() const { return m_board_size; }
    u32 target_tile() const { return 1u << m_target_tile_power; }
    bool temporary() const { return m_temporary; }

private:
    size_t m_board_size { 4 };
    size_t m_target_tile_power { 11 };
    bool m_temporary { true };
};
