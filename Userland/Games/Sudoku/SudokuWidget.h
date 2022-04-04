/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Board.h"
#include "LibGUI/Event.h"
#include <LibGUI/Frame.h>

class SudokuWidget final : public GUI::Frame {
    C_OBJECT(SudokuWidget);

public:
    virtual ~SudokuWidget() override = default;
    Function<void()> on_win;

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    void new_game();
    void set_board(Board* board);

private:
    SudokuWidget();
    void pick_font();
    float m_min_cell_size { 0 };
    float m_cell_size { 0 };
    Board* m_board { nullptr };
    Square* m_active_square { nullptr };
    Square* mouse_to_square(GUI::MouseEvent& event);
    void move_active_square(int x, int y);
};
