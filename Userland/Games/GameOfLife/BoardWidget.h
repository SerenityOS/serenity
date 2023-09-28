/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Board.h"
#include "Pattern.h"
#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <LibCore/Timer.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Widget.h>

class BoardWidget final : public GUI::Widget {
    C_OBJECT(BoardWidget);

public:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void context_menu_event(GUI::ContextMenuEvent&) override;

    void set_toggling_cells(bool toggling)
    {
        m_toggling_cells = toggling;
        if (!toggling)
            m_last_cell_toggled = { m_board->rows(), m_board->columns() };
    }

    void toggle_cell(size_t row, size_t column);
    void clear_cells();
    void randomize_cells();

    int get_cell_size() const;
    Gfx::IntSize get_board_offset() const;

    Optional<Board::RowAndColumn> get_row_and_column_for_point(int x, int y) const;

    void resize_board(size_t rows, size_t columns);
    Board const* board() const { return m_board.ptr(); }

    bool is_running() const { return m_running; }
    void set_running(bool r);

    Pattern* selected_pattern() { return m_selected_pattern; }
    Function<void(Pattern*)> on_pattern_selection;
    template<typename Callback>
    void for_each_pattern(Callback callback)
    {
        for (auto& pattern : m_patterns)
            callback(*pattern);
    }

    void run_generation();

    int running_timer_interval() const { return m_running_timer_interval; }
    void set_running_timer_interval(int interval);

    Function<void(u64)> on_tick;
    Function<void()> on_running_state_change;
    Function<void()> on_stall;
    Function<void()> on_pattern_selection_state_change;
    Function<void(Board*, size_t row, size_t column)> on_cell_toggled;

private:
    BoardWidget(size_t rows, size_t columns);
    void setup_patterns();
    void place_pattern(size_t row, size_t column);
    void clear_selected_pattern();

    bool m_toggling_cells { false };
    Board::RowAndColumn m_last_cell_toggled {};
    Board::RowAndColumn m_last_cell_hovered {};
    Pattern* m_selected_pattern { nullptr };
    Vector<NonnullOwnPtr<Pattern>> m_patterns;

    NonnullOwnPtr<Board> m_board;

    bool m_running { false };
    bool m_dragging_enabled { true };

    int m_running_timer_interval { 500 };
    int m_running_pattern_preview_timer_interval { 100 };

    u64 m_ticks { 0 };

    RefPtr<GUI::Menu> m_context_menu;

    RefPtr<Core::Timer> m_timer;
    RefPtr<Core::Timer> m_pattern_preview_timer;
};
