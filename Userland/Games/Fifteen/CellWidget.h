/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Forward.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Color.h>
#include <AK/SignalSlot.h>

class BoardWidget;

class CellWidget final : public GUI::Widget {
    C_OBJECT(CellWidget);

public:
    virtual ~CellWidget() override;
    void set_current_index(int current_index);
    int get_current_index() const { return m_current_index; }
    bool is_in_place() const;
    void position_cell();
    void fire_on_cell_move_request();

    AK::Signal<int> on_cell_move_request;

private:
    BoardWidget *m_board { nullptr };

    explicit CellWidget(BoardWidget *board, int real_index);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;

    void resize_cell();
    Gfx::IntPoint get_current_index_screen_position();

    Gfx::Color background_color_for_cell;
    Gfx::Color text_color_for_cell;

    int m_real_index, m_current_index;

    AK::ConnectionBag cons;
};
