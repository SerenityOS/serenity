/*
 * Copyright (c) 2021, Erlend Høier <Erlend@ReasonablePanic.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AnalogClock.h"
#include <LibCore/DateTime.h>
#include <LibGUI/Application.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Path.h>
#include <LibTimeZone/TimeZone.h>

void AnalogClock::draw_graduations(GUI::Painter& painter, Gfx::IntRect rect, int x, int y)
{
    rect.set_location({ x, y });

    painter.fill_rect(rect, palette().active_window_border2());

    rect.shrink(0, 1, 1, 0);
    painter.draw_line(rect.top_left(), rect.top_right(), palette().threed_highlight());
    painter.draw_line(rect.bottom_left(), rect.bottom_right(), palette().active_window_border1().darkened(0.7f));
    painter.draw_line(rect.bottom_right(), rect.top_right(), palette().active_window_border1().darkened(0.7f));
    painter.draw_line(rect.top_left(), rect.bottom_left(), palette().threed_highlight());
}

// To create an even clock face it's necessary to mirror the graduations positions
void AnalogClock::draw_mirrored_graduations(GUI::Painter& painter, Gfx::IntRect& rect, int x, int y, int rect_center_offset)
{
    auto w = this->rect().center().x() - rect_center_offset;
    auto h = this->rect().center().y() - rect_center_offset;

    draw_graduations(painter, rect, x + w, y + h);
    draw_graduations(painter, rect, y + w, x + h);
    draw_graduations(painter, rect, -x + w, y + h);
    draw_graduations(painter, rect, -y + w, x + h);
    draw_graduations(painter, rect, x + w, -y + h);
    draw_graduations(painter, rect, y + w, -x + h);
    draw_graduations(painter, rect, -x + w, -y + h);
    draw_graduations(painter, rect, -y + w, -x + h);
}

void AnalogClock::draw_face(GUI::Painter& painter)
{
    int x, y;
    double angle = 2 * M_PI / 60;

    for (int i = 0; i <= 7; ++i) {
        x = sin(angle * static_cast<double>(i)) * m_clock_face_radius;
        y = cos(angle * static_cast<double>(i)) * m_clock_face_radius;

        draw_mirrored_graduations(painter, m_small_graduation_square, x, y, 1);

        if (i % 5 == 0)
            draw_mirrored_graduations(painter, m_big_graduation_square, x, y, 2);
    }
}

void AnalogClock::draw_hand(GUI::Painter& painter, double angle, double length, Gfx::Color hand_color)
{
    if (angle >= 2 * M_PI)
        angle -= 2 * M_PI;

    double cosine = cos(angle);
    double sine = sin(angle);

    double hand_x = (rect().center().x() + (cosine * length));
    double hand_y = (rect().center().y() + (sine * length));

    Gfx::IntPoint indicator_point(hand_x, hand_y);
    Gfx::IntPoint tail_point(rect().center().x() + (-cosine * m_hand_tail_length), rect().center().y() + (-sine * m_hand_tail_length));
    Gfx::IntPoint right_wing_point(rect().center().x() + (-sine * m_hand_wing_span), rect().center().y() + (cosine * m_hand_wing_span));
    Gfx::IntPoint left_wing_point(rect().center().x() + (sine * m_hand_wing_span), rect().center().y() + (-cosine * m_hand_wing_span));

    Gfx::Path hand_fill;
    hand_fill.move_to(static_cast<Gfx::FloatPoint>(indicator_point));
    hand_fill.line_to(static_cast<Gfx::FloatPoint>(left_wing_point));
    hand_fill.line_to(static_cast<Gfx::FloatPoint>(tail_point));
    hand_fill.line_to(static_cast<Gfx::FloatPoint>(right_wing_point));
    hand_fill.close();

    painter.fill_path(hand_fill, hand_color, Gfx::WindingRule::Nonzero);

    // Draw highlight depending on the angle, this creates a subtle 3d effect.
    // remember the angle value is offset by half pi.
    if (angle > M_PI_2 - (M_PI / 3) && angle < M_PI + (M_PI / 3)) {
        painter.draw_line(left_wing_point, indicator_point, hand_color.darkened(0.7f));
        painter.draw_line(left_wing_point, tail_point, hand_color.darkened(0.7f));

        painter.draw_line(right_wing_point, indicator_point, palette().threed_highlight());
        painter.draw_line(right_wing_point, tail_point, palette().threed_highlight());
    } else {
        painter.draw_line(right_wing_point, indicator_point, hand_color.darkened(0.7f));
        painter.draw_line(right_wing_point, tail_point, hand_color.darkened(0.7f));

        painter.draw_line(left_wing_point, indicator_point, palette().threed_highlight());
        painter.draw_line(left_wing_point, tail_point, palette().threed_highlight());
    }
}

void AnalogClock::draw_seconds_hand(GUI::Painter& painter, double angle)
{
    double hand_x = (rect().center().x() + (cos(angle)) * (m_clock_face_radius - 10));
    double hand_y = (rect().center().y() + (sin(angle)) * (m_clock_face_radius - 10));

    Gfx::IntPoint indicator_point(hand_x, hand_y);
    painter.draw_line(rect().center(), indicator_point, palette().base_text());
}

void AnalogClock::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.clear_rect(event.rect(), m_show_window_frame ? palette().window() : Gfx::Color::Transparent);

    if (m_show_time_zone) {
        painter.draw_text(
            Gfx::IntRect { { event.rect().width() / 2, (int)(event.rect().height() - m_clock_face_radius) / 2 }, {} },
            m_time_zone,
            Gfx::FontDatabase::default_font().bold_variant(),
            Gfx::TextAlignment::Center);
    }

    draw_face(painter);

    auto now_seconds = Core::DateTime::now().timestamp();
    auto maybe_time_zone_offset = TimeZone::get_time_zone_offset(m_time_zone, UnixDateTime::from_seconds_since_epoch(now_seconds));
    VERIFY(maybe_time_zone_offset.has_value());

    auto time = Core::DateTime::from_timestamp(now_seconds + maybe_time_zone_offset.value().seconds);
    auto minute = time.minute() * (2 * M_PI) / 60;
    auto hour = (minute + (2 * M_PI) * time.hour()) / 12;
    auto seconds = time.second() * (2 * M_PI) / 60;
    auto angle_offset = M_PI_2;

    draw_hand(painter, minute - angle_offset, m_minute_hand_length, palette().active_window_border2());
    draw_hand(painter, hour - angle_offset, m_hour_hand_length, palette().active_window_border1());
    draw_seconds_hand(painter, seconds - angle_offset);

    if (time.hour() == 0)
        update_title_date();
}

void AnalogClock::update_title_date()
{
    window()->set_title(Core::DateTime::now().to_byte_string("%Y-%m-%d"sv));
}

void AnalogClock::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (on_context_menu_request)
        on_context_menu_request(event);
}

void AnalogClock::set_show_window_frame(bool show)
{
    if (show == m_show_window_frame)
        return;
    m_show_window_frame = show;
    auto& w = *window();
    w.set_frameless(!m_show_window_frame);
    w.set_has_alpha_channel(!m_show_window_frame);
}
