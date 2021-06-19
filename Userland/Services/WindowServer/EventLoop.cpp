/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <Kernel/API/MousePacket.h>
#include <LibCore/LocalSocket.h>
#include <LibCore/Object.h>
#include <WindowServer/ClientConnection.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/Event.h>
#include <WindowServer/EventLoop.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WMClientConnection.h>
#include <WindowServer/WindowManager.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

namespace WindowServer {

EventLoop::EventLoop()
    : m_window_server(Core::LocalServer::construct())
    , m_wm_server(Core::LocalServer::construct())
{
    m_keyboard_fd = open("/dev/keyboard0", O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    m_mouse_fd = open("/dev/mouse0", O_RDONLY | O_NONBLOCK | O_CLOEXEC);

    bool ok = m_window_server->take_over_from_system_server("/tmp/portal/window");
    VERIFY(ok);
    ok = m_wm_server->take_over_from_system_server("/tmp/portal/wm");
    VERIFY(ok);

    m_window_server->on_ready_to_accept = [this] {
        auto client_socket = m_window_server->accept();
        if (!client_socket) {
            dbgln("WindowServer: accept failed.");
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<ClientConnection>(client_socket.release_nonnull(), client_id);
    };

    m_wm_server->on_ready_to_accept = [this] {
        auto client_socket = m_wm_server->accept();
        if (!client_socket) {
            dbgln("WindowServer: WM accept failed.");
            return;
        }
        static int s_next_wm_id = 0;
        int wm_id = ++s_next_wm_id;
        IPC::new_client_connection<WMClientConnection>(client_socket.release_nonnull(), wm_id);
    };

    if (m_keyboard_fd >= 0) {
        m_keyboard_notifier = Core::Notifier::construct(m_keyboard_fd, Core::Notifier::Read);
        m_keyboard_notifier->on_ready_to_read = [this] { drain_keyboard(); };
    } else {
        dbgln("Couldn't open /dev/keyboard0");
    }

    if (m_mouse_fd >= 0) {
        m_mouse_notifier = Core::Notifier::construct(m_mouse_fd, Core::Notifier::Read);
        m_mouse_notifier->on_ready_to_read = [this] { drain_mouse(); };
    } else {
        dbgln("Couldn't open /dev/mouse0");
    }
}

EventLoop::~EventLoop()
{
}

void EventLoop::drain_mouse()
{
    auto& screen_input = ScreenInput::the();
    MousePacket state;
    state.buttons = screen_input.mouse_button_state();
    unsigned buttons = state.buttons;
    MousePacket packets[32];

    ssize_t nread = read(m_mouse_fd, &packets, sizeof(packets));
    if (nread < 0) {
        perror("EventLoop::drain_mouse read");
        return;
    }
    size_t npackets = nread / sizeof(MousePacket);
    if (!npackets)
        return;
    for (size_t i = 0; i < npackets; ++i) {
        auto& packet = packets[i];
        dbgln_if(WSMESSAGELOOP_DEBUG, "EventLoop: Mouse X {}, Y {}, Z {}, relative={}", packet.x, packet.y, packet.z, packet.is_relative);
        buttons = packet.buttons;

        state.is_relative = packet.is_relative;
        if (packet.is_relative) {
            state.x += packet.x;
            state.y -= packet.y;
            state.z += packet.z;
        } else {
            state.x = packet.x;
            state.y = packet.y;
            state.z += packet.z;
        }

        if (buttons != state.buttons) {
            state.buttons = buttons;
            dbgln_if(WSMESSAGELOOP_DEBUG, "EventLoop: Mouse Button Event");
            screen_input.on_receive_mouse_data(state);
            if (state.is_relative) {
                state.x = 0;
                state.y = 0;
                state.z = 0;
            }
        }
    }
    if (state.is_relative && (state.x || state.y || state.z))
        screen_input.on_receive_mouse_data(state);
    if (!state.is_relative)
        screen_input.on_receive_mouse_data(state);
}

void EventLoop::drain_keyboard()
{
    auto& screen_input = ScreenInput::the();
    for (;;) {
        ::KeyEvent event;
        ssize_t nread = read(m_keyboard_fd, (u8*)&event, sizeof(::KeyEvent));
        if (nread == 0)
            break;
        VERIFY(nread == sizeof(::KeyEvent));
        screen_input.on_receive_keyboard_data(event);
    }
}

}
