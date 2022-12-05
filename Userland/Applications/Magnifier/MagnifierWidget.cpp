/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MagnifierWidget.h"
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/DisplayLink.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Rect.h>

MagnifierWidget::MagnifierWidget()
{
    GUI::DisplayLink::register_callback([this](auto) { sync(); });
}

void MagnifierWidget::set_scale_factor(int scale_factor)
{
    VERIFY(scale_factor == 2 || scale_factor == 4 || scale_factor == 8);
    if (m_scale_factor == scale_factor)
        return;
    m_scale_factor = scale_factor;
    layout_relevant_change_occurred();
    update();
}

void MagnifierWidget::lock_location(bool lock)
{
    if (lock)
        m_locked_location = GUI::ConnectionToWindowServer::the().get_global_cursor_position();
    else
        m_locked_location = {};
}

void MagnifierWidget::show_grid(bool new_value)
{
    if (m_show_grid == new_value)
        return;
    m_show_grid = new_value;
    update();
}

void MagnifierWidget::set_grid_color(Gfx::Color new_color)
{
    if (m_grid_color == new_color)
        return;
    m_grid_color = new_color;
    update();
}

void MagnifierWidget::set_color_filter(OwnPtr<Gfx::ColorBlindnessFilter> color_filter)
{
    m_color_filter = move(color_filter);
    sync();
}

void MagnifierWidget::display_previous_frame()
{
    --m_frame_offset_from_head;
    auto index = m_grabbed_bitmaps.head_index() + m_frame_offset_from_head;
    m_grabbed_bitmap = m_grabbed_bitmaps.at(index);
    update();
}

void MagnifierWidget::display_next_frame()
{
    ++m_frame_offset_from_head;
    auto index = m_grabbed_bitmaps.head_index() + m_frame_offset_from_head;
    m_grabbed_bitmap = m_grabbed_bitmaps.at(index);
    update();
}

void MagnifierWidget::sync()
{
    if (m_pause_capture)
        return;

    auto size = frame_inner_rect().size();
    Gfx::IntSize grab_size { (size.width() + m_scale_factor - 1) / m_scale_factor, (size.height() + m_scale_factor - 1) / m_scale_factor };
    VERIFY(grab_size.width() != 0 && grab_size.height() != 0);

    if (m_locked_location.has_value()) {
        m_grabbed_bitmap = GUI::ConnectionToWindowServer::the().get_screen_bitmap_around_location(grab_size, m_locked_location.value()).bitmap();
    } else {
        m_grabbed_bitmap = GUI::ConnectionToWindowServer::the().get_screen_bitmap_around_cursor(grab_size).bitmap();
    }

    m_grabbed_bitmaps.enqueue(m_grabbed_bitmap);
    update();
}

void MagnifierWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    if (!m_grabbed_bitmap)
        return;

    GUI::Painter painter(*this);

    if (m_pause_capture)
        painter.fill_rect(frame_inner_rect(), Gfx::Color::Black);

    // We have to clip the source rect for cases when the currently captured image is larger than the inner_rect
    int horizontal_clip = (frame_inner_rect().width() + m_scale_factor - 1) / m_scale_factor;
    int vertical_clip = (frame_inner_rect().height() + m_scale_factor - 1) / m_scale_factor;

    if (m_grabbed_bitmap)
        painter.draw_scaled_bitmap(
            frame_inner_rect().intersected(Gfx::IntRect { { 0, 0 }, m_grabbed_bitmap->rect().size() } * m_scale_factor),
            *m_grabbed_bitmap,
            m_grabbed_bitmap->rect().intersected({ 0, 0, horizontal_clip, vertical_clip }),
            1.0,
            Gfx::Painter::ScalingMode::NearestFractional);

    if (m_show_grid) {

        auto grid_rect = frame_inner_rect();
        if (m_grabbed_bitmap)
            grid_rect = frame_inner_rect().intersected({ 0,
                0,
                m_grabbed_bitmap->rect().width() * m_scale_factor,
                m_grabbed_bitmap->rect().height() * m_scale_factor });

        int start_y = grid_rect.top(),
            start_x = grid_rect.left();

        int end_y = grid_rect.bottom(),
            end_x = grid_rect.right();

        for (int current_y = start_y; current_y <= end_y; current_y += m_scale_factor)
            painter.draw_line({ start_x, current_y }, { end_x, current_y }, m_grid_color);

        for (int current_x = start_y; current_x <= end_x; current_x += m_scale_factor)
            painter.draw_line({ current_x, start_y }, { current_x, end_y }, m_grid_color);
    }
}

void MagnifierWidget::second_paint_event(GUI::PaintEvent&)
{
    if (!m_color_filter)
        return;

    GUI::Painter painter(*this);

    auto target = painter.target();
    auto bitmap_clone_or_error = target->clone();
    if (bitmap_clone_or_error.is_error())
        return;

    auto clone = bitmap_clone_or_error.release_value();
    auto rect = target->rect();

    m_color_filter->apply(*target, rect, *clone, rect);
}
