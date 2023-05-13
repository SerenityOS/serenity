/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Event.h>
#include <LibGfx/Palette.h>

namespace GUI {

Dialog::Dialog(Window* parent_window, ScreenPosition screen_position)
    : Window(parent_window)
    , m_screen_position(screen_position)
{
    set_window_mode(WindowMode::Blocking);
}

Dialog::ExecResult Dialog::exec()
{
    VERIFY(!m_event_loop);
    m_event_loop = make<Core::EventLoop>();

    switch (m_screen_position) {
    case ScreenPosition::DoNotPosition:
        break;
    case ScreenPosition::CenterWithinParent:
        if (auto parent = find_parent_window(); parent && parent->is_visible()) {
            center_within(*parent);
            constrain_to_desktop();
            break;
        }
        [[fallthrough]];
    case ScreenPosition::Center:
        center_on_screen();
        break;
    }

    show();
    auto result = m_event_loop->exec();
    m_event_loop = nullptr;
    dbgln("{}: Event loop returned with result {}", *this, result);
    remove_from_parent();
    return static_cast<ExecResult>(result);
}

void Dialog::done(ExecResult result)
{
    Window::close();

    if (!m_event_loop)
        return;
    m_result = result;
    on_done(m_result);

    dbgln("{}: Quit event loop with result {}", *this, to_underlying(result));
    m_event_loop->quit(to_underlying(result));
}

void Dialog::event(Core::Event& event)
{
    if (event.type() == Event::KeyDown) {
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
    done(ExecResult::Cancel);
}

}
