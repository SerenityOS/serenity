/*
 * Copyright (c) 2021, Erlend Høier <Erlend@ReasonablePanic.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>

class AnalogClock : public GUI::Widget {
    C_OBJECT(AnalogClock)
public:
    ~AnalogClock() override = default;
    void set_show_window_frame(bool);
    bool show_window_frame() const { return m_show_window_frame; }

    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;

    void set_time_zone(StringView time_zone) { m_time_zone = time_zone; }
    void set_show_time_zone(bool value) { m_show_time_zone = value; }

private:
    AnalogClock()
        : m_small_graduation_square(Gfx::IntRect({}, { 3, 3 }))
        , m_big_graduation_square(Gfx::IntRect({}, { 5, 5 }))
    {
        start_timer(1000);
    }

    unsigned m_clock_face_radius { 70 };
    Gfx::IntRect m_small_graduation_square;
    Gfx::IntRect m_big_graduation_square;

    unsigned m_minute_hand_length { 58 };
    unsigned m_hour_hand_length { 42 };

    double m_hand_tail_length { 22 };
    double m_hand_wing_span { 5 };

    bool m_show_window_frame { true };

    StringView m_time_zone;
    bool m_show_time_zone { false };

protected:
    void context_menu_event(GUI::ContextMenuEvent& event) override;
    void paint_event(GUI::PaintEvent&) override;
    void draw_face(GUI::Painter&);
    void draw_mirrored_graduations(GUI::Painter&, Gfx::IntRect&, int x, int y, int rect_center_offset);
    void draw_graduations(GUI::Painter&, Gfx::IntRect, int x, int y);
    void draw_hand(GUI::Painter&, double angle, double length, Gfx::Color hand_color);
    void draw_seconds_hand(GUI::Painter&, double angle);
    void update_title_date();

    void timer_event(Core::TimerEvent&) override
    {
        update();
    }
};
