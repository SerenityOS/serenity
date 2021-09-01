/*
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GlobalColorSelect.h"
#include <LibGUI/WindowServerConnection.h>

namespace GUI {

GlobalColorSelect::GlobalColorSelect()
{
    m_window = GUI::Window::construct();
    m_window->set_main_widget(this);
    m_window->set_has_alpha_channel(true);
    m_window->set_background_color(Color::Transparent);
    m_window->set_fullscreen(true);
    m_window->set_frameless(true);
    set_override_cursor(Gfx::StandardCursor::Eyedropper);
}

void GlobalColorSelect::begin_selecting()
{
    m_window->show();
}

void GlobalColorSelect::mousedown_event(GUI::MouseEvent&)
{
    m_window->close();
    if (on_finished) {
        on_finished(m_col);
    }
}

void GlobalColorSelect::mousemove_event(GUI::MouseEvent&)
{
    // FIXME: The screenshot returned from WindowServer includes the cursor,
    // so we need to take a pixel beside it to avoid just sampling the color
    // of the cursor. 
    Gfx::IntSize size { 2, 2 };
    const auto& shared_bitmap = GUI::WindowServerConnection::the().get_screen_bitmap_around_cursor(size);
    const auto bitmap = shared_bitmap.bitmap();
    m_col = bitmap->get_pixel(0, 0);
    if (on_color_changed) {
        on_color_changed(m_col);
    }
}

}