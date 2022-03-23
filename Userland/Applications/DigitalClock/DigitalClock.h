/*
 * Copyright (c) 2021, Erlend Høier <Erlend@ReasonablePanic.com>
 * Copyright (c) 2022, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/StringView.h"
#include "LibGfx/AntiAliasingPainter.h"
#include "LibGfx/Forward.h"
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>

class DigitalClock : public GUI::Widget {
    C_OBJECT(DigitalClock)
public:
    ~DigitalClock() override = default;
    void set_show_window_frame(bool);
    bool show_window_frame() const { return m_show_window_frame; }

    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;

private:
    DigitalClock()
    {
        start_timer(1000);
    }

    Gfx::Color dark = Gfx::Color::from_rgb(0x1F0000);
    Gfx::Color light = Gfx::Color::from_rgb(0xFF0000);

    bool m_show_window_frame { true };

protected:
    void context_menu_event(GUI::ContextMenuEvent& event) override;
    void paint_event(GUI::PaintEvent&) override;
    void draw_digit(Gfx::AntiAliasingPainter&, int num, Gfx::IntPoint pos, int digitWidth, int padding);
    void draw_side_segment(Gfx::AntiAliasingPainter&, Gfx::IntPoint pos, int width, int height, int sideDir, int upDir, int padding, Gfx::Color segmentColor);
    void draw_edge_segment(Gfx::AntiAliasingPainter&, Gfx::IntPoint pos, int width, int height, int direction, int padding, Gfx::Color segmentColor);
    void draw_middle_segment(Gfx::AntiAliasingPainter&, Gfx::IntPoint pos, int width, int height, int padding, Gfx::Color segmentColor);
    void draw_colon(Gfx::AntiAliasingPainter&, Gfx::IntPoint pos, int size, Gfx::Color c);

    void update_title_date();

    void timer_event(Core::TimerEvent&) override
    {
        update();
    }
};
