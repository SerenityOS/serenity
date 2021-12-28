/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MagnifierWidget.h"
#include <LibGUI/DisplayLink.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Rect.h>

MagnifierWidget::MagnifierWidget()
{
    GUI::DisplayLink::register_callback([this](auto) { sync(); });
}

MagnifierWidget::~MagnifierWidget()
{
}

void MagnifierWidget::set_scale_factor(int scale_factor)
{
    VERIFY(scale_factor == 2 || scale_factor == 4 || scale_factor == 8);
    m_scale_factor = scale_factor;
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
    Gfx::IntSize grab_size { size.width() / m_scale_factor, size.height() / m_scale_factor };
    m_grabbed_bitmap = GUI::WindowServerConnection::the().get_screen_bitmap_around_cursor(grab_size).bitmap();
    m_grabbed_bitmaps.enqueue(m_grabbed_bitmap);
    update();
}

void MagnifierWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);

    if (m_grabbed_bitmap)
        painter.draw_scaled_bitmap(frame_inner_rect(), *m_grabbed_bitmap, m_grabbed_bitmap->rect());
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
