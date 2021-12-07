/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <Kernel/API/MousePacket.h>
#include <WindowServer/ClientConnection.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/EventLoop.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WMClientConnection.h>
#include <WindowServer/WindowManager.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

namespace WindowServer {

EventLoop::EventLoop()
{
    m_keyboard_fd = open("/dev/keyboard0", O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    m_mouse_fd = open("/dev/mouse0", O_RDONLY | O_NONBLOCK | O_CLOEXEC);

    m_window_server = MUST(IPC::MultiServer<ClientConnection>::try_create("/tmp/portal/window"));
    m_wm_server = MUST(IPC::MultiServer<WMClientConnection>::try_create("/tmp/portal/wm"));

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
    MousePacket packets[32];

    ssize_t nread = read(m_mouse_fd, &packets, sizeof(packets));
    if (nread < 0) {
        perror("EventLoop::drain_mouse read");
        return;
    }
    size_t npackets = nread / sizeof(MousePacket);
    if (!npackets)
        return;

    bool state_is_sent = false;
    for (size_t i = 0; i < npackets; ++i) {
        auto& packet = packets[i];
        dbgln_if(WSMESSAGELOOP_DEBUG, "EventLoop: Mouse X {}, Y {}, Z {}, relative={}", packet.x, packet.y, packet.z, packet.is_relative);

        state.is_relative = packet.is_relative;
        if (packet.is_relative) {
            state.x += packet.x;
            state.y -= packet.y;
        } else {
            state.x = packet.x;
            state.y = packet.y;
        }
        state.z += packet.z;
        state_is_sent = false;

        if (packet.buttons != state.buttons) {
            state.buttons = packet.buttons;
            dbgln_if(WSMESSAGELOOP_DEBUG, "EventLoop: Mouse Button Event");

            // Swap primary (1) and secondary (2) buttons if checked in Settings.
            // Doing the swap here avoids all emulator and hardware issues.
            if (WindowManager::the().get_buttons_switched()) {
                bool has_primary = state.buttons & MousePacket::Button::LeftButton;
                bool has_secondary = state.buttons & MousePacket::Button::RightButton;
                state.buttons = state.buttons & ~(MousePacket::Button::LeftButton | MousePacket::Button::RightButton);
                // Invert the buttons:
                if (has_primary)
                    state.buttons |= MousePacket::Button::RightButton;
                if (has_secondary)
                    state.buttons |= MousePacket::Button::LeftButton;
            }

            screen_input.on_receive_mouse_data(state);
            state_is_sent = true;
            if (state.is_relative) {
                state.x = 0;
                state.y = 0;
                state.z = 0;
            }
        }
    }
    if (state_is_sent)
        return;
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
