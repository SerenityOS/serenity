/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/ByteBuffer.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/Notifier.h>

namespace WindowServer {

class ClientConnection;

class EventLoop {
public:
    EventLoop();
    virtual ~EventLoop();

    int exec() { return m_event_loop.exec(); }

private:
    void drain_mouse();
    void drain_keyboard();

    Core::EventLoop m_event_loop;
    int m_keyboard_fd { -1 };
    RefPtr<Core::Notifier> m_keyboard_notifier;
    int m_mouse_fd { -1 };
    RefPtr<Core::Notifier> m_mouse_notifier;
    RefPtr<Core::LocalServer> m_window_server;
    RefPtr<Core::LocalServer> m_wm_server;
};

}
