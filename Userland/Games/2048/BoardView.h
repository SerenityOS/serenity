/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Game.h"
#include <LibGUI/Frame.h>

class BoardView final : public GUI::Frame {
    C_OBJECT(BoardView);

public:
    virtual ~BoardView() override;
    void set_board(const Game::Board* board);

    Function<void(Game::Direction)> on_move;

private:
    explicit BoardView(const Game::Board*);

    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

    size_t rows() const;
    size_t columns() const;

    void pick_font();
    void resize();

    Color background_color_for_cell(u32 value);
    Color text_color_for_cell(u32 value);

    float m_padding { 0 };
    float m_cell_size { 0 };

    const Game::Board* m_board { nullptr };
};
