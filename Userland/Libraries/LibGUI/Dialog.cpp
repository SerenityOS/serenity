/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Event.h>

namespace GUI {

Dialog::Dialog(Window* parent_window, ScreenPosition screen_position)
    : Window(parent_window)
    , m_screen_position(screen_position)
{
    set_modal(true);
    set_minimizable(false);
}

Dialog::ExecResult Dialog::exec()
{
    VERIFY(!m_event_loop);
    m_event_loop = make<Core::EventLoop>();

    auto desktop_rect = Desktop::the().rect();
    auto window_rect = rect();

    auto top_align = [](Gfx::Rect<int>& rect) { rect.set_y(32); };
    auto bottom_align = [this, desktop_rect](Gfx::Rect<int>& rect) { rect.set_y(desktop_rect.height() - Desktop::the().taskbar_height() - height() - 12); };

    auto left_align = [](Gfx::Rect<int>& rect) { rect.set_x(12); };
    auto right_align = [this, desktop_rect](Gfx::Rect<int>& rect) { rect.set_x(desktop_rect.width() - width() - 12); };

    switch (m_screen_position) {
    case ScreenPosition::CenterWithinParent:
        if (parent() && is<Window>(parent())) {
            auto& parent_window = *static_cast<Window*>(parent());
            if (parent_window.is_visible()) {
                window_rect.center_within(parent_window.rect());
                break;
            }
        }
        [[fallthrough]]; // Fall back to `Center` if parent window is invalid or not visible
    case ScreenPosition::Center:
        window_rect.center_within(desktop_rect);
        break;
    case ScreenPosition::CenterLeft:
        left_align(window_rect);
        window_rect.center_vertically_within(desktop_rect);
        break;
    case ScreenPosition::CenterRight:
        right_align(window_rect);
        window_rect.center_vertically_within(desktop_rect);
        break;
    case ScreenPosition::TopLeft:
        left_align(window_rect);
        top_align(window_rect);
        break;
    case ScreenPosition::TopCenter:
        window_rect.center_horizontally_within(desktop_rect);
        top_align(window_rect);
        break;
    case ScreenPosition::TopRight:
        right_align(window_rect);
        top_align(window_rect);
        break;
    case ScreenPosition::BottomLeft:
        left_align(window_rect);
        bottom_align(window_rect);
        break;
    case ScreenPosition::BottomCenter:
        window_rect.center_horizontally_within(desktop_rect);
        bottom_align(window_rect);
        break;
    case ScreenPosition::BottomRight:
        right_align(window_rect);
        bottom_align(window_rect);
        break;
    }

    set_rect(window_rect);
    show();
    auto result = m_event_loop->exec();
    m_event_loop = nullptr;
    dbgln("{}: Event loop returned with result {}", *this, result);
    remove_from_parent();
    return static_cast<ExecResult>(result);
}

void Dialog::done(ExecResult result)
{
    if (!m_event_loop)
        return;
    m_result = result;
    dbgln("{}: Quit event loop with result {}", *this, to_underlying(result));
    m_event_loop->quit(to_underlying(result));
}

void Dialog::event(Core::Event& event)
{
    if (event.type() == Event::KeyUp || event.type() == Event::KeyDown) {
        auto& key_event = static_cast<KeyEvent&>(event);
        if (key_event.key() == KeyCode::Key_Escape) {
            done(ExecResult::Cancel);
            event.accept();
            return;
        }
    }

    Window::event(event);
}

void Dialog::close()
{
    Window::close();
    done(ExecResult::Cancel);
}

}
