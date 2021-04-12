/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Board.h"
#include <LibCore/Timer.h>
#include <LibGUI/Widget.h>

class BoardWidget final : public GUI::Widget {
    C_OBJECT(BoardWidget);

public:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;

    void set_toggling_cells(bool toggling)
    {
        m_toggling_cells = toggling;
        if (!toggling)
            m_last_cell_toggled = m_board->total_size();
    }

    size_t last_toggled() const { return m_last_cell_toggled; }
    bool is_toggling() const { return m_toggling_cells; }

    void toggle_cell(size_t index);
    void clear_cells() { m_board->clear(); }
    void randomize_cells() { m_board->randomize(); }

    int get_cell_size() const;
    Gfx::IntSize get_board_offset() const;

    size_t get_index_for_point(int x, int y) const;

    void update_board(size_t rows, size_t columns);
    const Board* board() const { return m_board.ptr(); }

    bool is_running() const { return m_running; }
    void set_running(bool r);

    void set_toolbar_enabled(bool);

    void run_generation();

    int running_timer_interval() const { return m_running_timer_interval; }
    void set_running_timer_interval(int interval);

    Function<void()> on_running_state_change;
    Function<void()> on_stall;
    Function<void(Board*, size_t)> on_cell_toggled;

private:
    BoardWidget(size_t rows, size_t columns);

    bool m_toggling_cells { false };
    size_t m_last_cell_toggled { 0 };

    OwnPtr<Board> m_board { nullptr };

    bool m_running { false };

    int m_running_timer_interval { 500 };
    RefPtr<Core::Timer> m_timer;
};
