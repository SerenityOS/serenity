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

void MagnifierWidget::sync()
{
    Gfx::IntSize grab_size { size().width() / m_scale_factor, size().height() / m_scale_factor };
    m_grabbed_bitmap = GUI::WindowServerConnection::the().get_screen_bitmap_around_cursor(grab_size).bitmap();
    update();
}

void MagnifierWidget::paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);

    if (m_grabbed_bitmap)
        painter.draw_scaled_bitmap(rect(), *m_grabbed_bitmap, m_grabbed_bitmap->rect());
}
