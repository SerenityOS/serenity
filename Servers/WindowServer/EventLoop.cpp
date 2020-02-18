/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Clipboard.h"
#include <Kernel/KeyCode.h>
#include <Kernel/MousePacket.h>
#include <LibCore/LocalSocket.h>
#include <LibCore/Object.h>
#include <WindowServer/ClientConnection.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/Event.h>
#include <WindowServer/EventLoop.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

//#define WSMESSAGELOOP_DEBUG

namespace WindowServer {

EventLoop::EventLoop()
    : m_server(Core::LocalServer::construct())
{
    m_keyboard_fd = open("/dev/keyboard", O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    m_mouse_fd = open("/dev/mouse", O_RDONLY | O_NONBLOCK | O_CLOEXEC);

    bool ok = m_server->take_over_from_system_server();
    ASSERT(ok);

    m_server->on_ready_to_accept = [this] {
        auto client_socket = m_server->accept();
        if (!client_socket) {
            dbg() << "WindowServer: accept failed.";
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<ClientConnection>(*client_socket, client_id);
    };

    ASSERT(m_keyboard_fd >= 0);
    ASSERT(m_mouse_fd >= 0);

    m_keyboard_notifier = Core::Notifier::construct(m_keyboard_fd, Core::Notifier::Read);
    m_keyboard_notifier->on_ready_to_read = [this] { drain_keyboard(); };

    m_mouse_notifier = Core::Notifier::construct(m_mouse_fd, Core::Notifier::Read);
    m_mouse_notifier->on_ready_to_read = [this] { drain_mouse(); };

    Clipboard::the().on_content_change = [&] {
        ClientConnection::for_each_client([&](auto& client) {
            client.notify_about_clipboard_contents_changed();
        });
    };
}

EventLoop::~EventLoop()
{
}

void EventLoop::drain_mouse()
{
    auto& screen = Screen::the();
    MousePacket state;
    state.buttons = screen.mouse_button_state();
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
#ifdef WSMESSAGELOOP_DEBUG
        dbgprintf("EventLoop: Mouse X %d, Y %d, Z %d, relative %d\n", packet.x, packet.y, packet.z, packet.is_relative);
#endif
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
#ifdef WSMESSAGELOOP_DEBUG
            dbgprintf("EventLoop: Mouse Button Event\n");
#endif
            screen.on_receive_mouse_data(state);
            if (state.is_relative) {
                state.x = 0;
                state.y = 0;
                state.z = 0;
            }
        }
    }
    if (state.is_relative && (state.x || state.y || state.z))
        screen.on_receive_mouse_data(state);
    if (!state.is_relative)
        screen.on_receive_mouse_data(state);
}

void EventLoop::drain_keyboard()
{
    auto& screen = Screen::the();
    for (;;) {
        ::KeyEvent event;
        ssize_t nread = read(m_keyboard_fd, (u8*)&event, sizeof(::KeyEvent));
        if (nread == 0)
            break;
        ASSERT(nread == sizeof(::KeyEvent));
        screen.on_receive_keyboard_data(event);
    }
}

}
