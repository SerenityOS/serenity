/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ConnectionFromClient.h"
#include "WMConnectionFromClient.h"
#include <AK/ByteBuffer.h>
#include <LibCore/EventLoop.h>
#include <LibCore/FileWatcher.h>
#include <LibCore/Notifier.h>
#include <LibIPC/MultiServer.h>

namespace WindowServer {

class ConnectionFromClient;

class EventLoop {
public:
    EventLoop();
    virtual ~EventLoop() = default;

    int exec() { return m_event_loop.exec(); }

private:
    void refresh_keyboard_devices();
    void refresh_mouse_devices();

    void drain_mouse(int fd);
    void drain_keyboard(int fd);

    Core::EventLoop m_event_loop;

    struct InputDevice {
        int fd { -1 };
        NonnullRefPtr<Core::Notifier> notifier;
    };

    Vector<InputDevice> m_keyboard_devices;
    Vector<InputDevice> m_mouse_devices;

    OwnPtr<IPC::MultiServer<ConnectionFromClient>> m_window_server;
    OwnPtr<IPC::MultiServer<WMConnectionFromClient>> m_wm_server;

    RefPtr<Core::FileWatcher> m_mouse_devices_watcher;
    RefPtr<Core::FileWatcher> m_keyboard_devices_watcher;
};

}
