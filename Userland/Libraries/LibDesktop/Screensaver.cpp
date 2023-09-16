/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibDesktop/Screensaver.h>
#include <LibGUI/Icon.h>

namespace Desktop {

static constexpr int mouse_max_distance_move = 10;
static constexpr int mouse_tracking_delay_milliseconds = 750;

ErrorOr<NonnullRefPtr<GUI::Window>> Screensaver::create_window(StringView title, StringView icon)
{
    auto window = GUI::Window::construct();
    window->set_double_buffering_enabled(false);
    window->set_frameless(true);
    window->set_fullscreen(true);
    window->set_minimizable(false);
    window->set_resizable(false);
    window->set_title(title);

    auto app_icon = TRY(GUI::Icon::try_create_default_icon(icon));
    window->set_icon(app_icon.bitmap_for_size(16));

    return window;
}

void Screensaver::keydown_event(GUI::KeyEvent&)
{
    trigger_exit();
}

void Screensaver::mousedown_event(GUI::MouseEvent&)
{
    trigger_exit();
}

void Screensaver::mousemove_event(GUI::MouseEvent& event)
{
    auto now = MonotonicTime::now();
    if ((now - m_start_time).to_milliseconds() < mouse_tracking_delay_milliseconds)
        return;

    if (!m_mouse_origin.has_value())
        m_mouse_origin = event.position();
    else if (event.position().distance_from(m_mouse_origin.value()) > mouse_max_distance_move)
        trigger_exit();
}

void Screensaver::trigger_exit()
{
    if (on_screensaver_exit)
        on_screensaver_exit();
}

}
