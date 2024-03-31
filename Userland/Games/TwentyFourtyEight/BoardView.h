/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Game.h"
#include <LibGUI/Frame.h>

class BoardView final : public GUI::Frame {
    C_OBJECT(BoardView);

public:
    virtual ~BoardView() override = default;
    void set_board(Game::Board const* board);

    Function<void(Game::Direction)> on_move;

private:
    explicit BoardView(Game::Board const*);

    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    size_t rows() const;
    size_t columns() const;

    void pick_font();
    void resize();

    Color background_color_for_cell(u32 value);
    Color text_color_for_cell(u32 value);

    float m_padding { 0 };
    float m_min_cell_size { 0 };
    float m_cell_size { 0 };

    Game::Board const* m_board { nullptr };

    static constexpr int frame_duration_ms = 1000 / 60;
    static constexpr int animation_duration = 5;

    int pop_in_animation_frame = 0;
    int slide_animation_frame = 0;
};
