/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/MouseTracker.h>
#include <LibGUI/Widget.h>
#include <LibGfx/AntiAliasingPainter.h>

class EyesWidget final : public GUI::Widget
    , GUI::MouseTracker {
    C_OBJECT(EyesWidget)

public:
    virtual ~EyesWidget() override = default;

    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;

protected:
    virtual void context_menu_event(GUI::ContextMenuEvent& event) override
    {
        if (on_context_menu_request)
            on_context_menu_request(event);
    }

private:
    EyesWidget(int num_eyes, int full_rows, int extra)
        : m_full_rows(full_rows)
        , m_extra_columns(extra)
    {
        m_num_rows = m_extra_columns > 0 ? m_full_rows + 1 : m_full_rows;
        m_eyes_in_row = m_full_rows > 0 ? (num_eyes - m_extra_columns) / m_full_rows : m_extra_columns;
    }

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void track_mouse_move(Gfx::IntPoint) override;

    void render_eyeball(int row, int column, Gfx::AntiAliasingPainter& aa_painter) const;
    Gfx::IntPoint pupil_center(Gfx::IntRect& eyeball_bounds) const;

    Gfx::IntPoint m_mouse_position;
    int m_eyes_in_row { -1 };
    int m_full_rows { -1 };
    int m_extra_columns { -1 };
    int m_num_rows { -1 };
};
