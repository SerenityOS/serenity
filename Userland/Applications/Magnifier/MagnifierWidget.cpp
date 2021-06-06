/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MagnifierWidget.h"
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Rect.h>

MagnifierWidget::MagnifierWidget()
{
}

MagnifierWidget::~MagnifierWidget()
{
}

void MagnifierWidget::track_cursor_globally()
{
    VERIFY(window());
    auto window_id = window()->window_id();
    VERIFY(window_id >= 0);

    set_global_cursor_tracking(true);
    GUI::WindowServerConnection::the().set_global_cursor_tracking(window_id, true);
}

void MagnifierWidget::set_scale_factor(int scale_factor)
{
    VERIFY(scale_factor == 2 || scale_factor == 4);
    m_scale_factor = scale_factor;
    update();
}

void MagnifierWidget::timer_event(Core::TimerEvent&)
{
    m_mouse_position = GUI::WindowServerConnection::the().get_global_cursor_position();
    m_desktop_display_scale = GUI::WindowServerConnection::the().get_desktop_display_scale();
    update();
}

void MagnifierWidget::paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);

    // Grab and paint our screenshot.
    Gfx::IntSize region_size { size().width() / m_scale_factor, size().height() / m_scale_factor };
    Gfx::Rect region { (m_mouse_position.x() * m_desktop_display_scale) - (region_size.width() / 2), (m_mouse_position.y() * m_desktop_display_scale) - (region_size.height() / 2), region_size.width(), region_size.height() };
    auto map = GUI::WindowServerConnection::the().get_screen_bitmap(region);
    painter.draw_scaled_bitmap(rect(), *map.bitmap(), map.bitmap()->rect());
}
