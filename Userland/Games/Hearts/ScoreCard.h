/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Player.h"
#include <AK/Function.h>
#include <LibGUI/Frame.h>

namespace Hearts {

class ScoreCard : public GUI::Frame {
    C_OBJECT(ScoreCard);

    Gfx::IntSize recommended_size();

private:
    ScoreCard(Player (&players)[4], bool game_over);

    virtual void paint_event(GUI::PaintEvent&) override;

    static constexpr int column_width = 70;
    static constexpr int cell_padding = 5;

    Player (&m_players)[4];
    bool m_game_over { false };
};

}
