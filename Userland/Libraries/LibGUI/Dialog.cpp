/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Event.h>

namespace GUI {

Dialog::Dialog(Window* parent_window)
    : Window(parent_window)
{
    set_modal(true);
    set_minimizable(false);
}

Dialog::~Dialog()
{
}

int Dialog::exec()
{
    VERIFY(!m_event_loop);
    m_event_loop = make<Core::EventLoop>();
    if (parent() && is<Window>(parent())) {
        auto& parent_window = *static_cast<Window*>(parent());
        if (parent_window.is_visible()) {
            center_within(parent_window);
        } else {
            center_on_screen();
        }
    } else {
        center_on_screen();
    }
    show();
    auto result = m_event_loop->exec();
    m_event_loop = nullptr;
    dbgln("{}: Event loop returned with result {}", *this, result);
    remove_from_parent();
    return result;
}

void Dialog::done(int result)
{
    if (!m_event_loop)
        return;
    m_result = result;
    dbgln("{}: Quit event loop with result {}", *this, result);
    m_event_loop->quit(result);
}

void Dialog::event(Core::Event& event)
{
    if (event.type() == Event::KeyUp || event.type() == Event::KeyDown) {
        auto& key_event = static_cast<KeyEvent&>(event);
        if (key_event.key() == KeyCode::Key_Escape) {
            done(ExecCancel);
            event.accept();
            return;
        }
    }

    Window::event(event);
}

void Dialog::close()
{
    Window::close();
    m_event_loop->quit(ExecCancel);
}

}
