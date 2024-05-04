/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <Kernel/API/MousePacket.h>
#include <LibCore/DirIterator.h>
#include <LibFileSystem/FileSystem.h>
#include <WindowServer/ConnectionFromClient.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/EventLoop.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WMConnectionFromClient.h>
#include <WindowServer/WindowManager.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

namespace WindowServer {

EventLoop::EventLoop()
{
    m_window_server = MUST(IPC::MultiServer<ConnectionFromClient>::try_create("/tmp/portal/window"));
    m_wm_server = MUST(IPC::MultiServer<WMConnectionFromClient>::try_create("/tmp/portal/wm"));

    m_mouse_devices_watcher = MUST(Core::FileWatcher::create());
    m_keyboard_devices_watcher = MUST(Core::FileWatcher::create());

    m_mouse_devices_watcher->on_change = [this](auto&) {
        refresh_mouse_devices();
    };

    m_keyboard_devices_watcher->on_change = [this](auto&) {
        refresh_keyboard_devices();
    };

    MUST(m_mouse_devices_watcher->add_watch("/tmp/system/devicemap/family/mouse/",
        Core::FileWatcherEvent::Type::ChildCreated | Core::FileWatcherEvent::Type::ChildDeleted));

    MUST(m_keyboard_devices_watcher->add_watch("/tmp/system/devicemap/family/keyboard/",
        Core::FileWatcherEvent::Type::ChildCreated | Core::FileWatcherEvent::Type::ChildDeleted));

    refresh_keyboard_devices();
    refresh_mouse_devices();
}

void EventLoop::refresh_keyboard_devices()
{
    for (auto& device : m_keyboard_devices) {
        close(device.fd);
        device.notifier->close();
    }
    m_keyboard_devices.clear();
    Core::DirIterator di("/dev/input/keyboard/", Core::DirIterator::SkipParentAndBaseDir);
    while (di.has_next()) {
        auto path = di.next_path();
        auto full_path = ByteString::formatted("/dev/input/keyboard/{}", path);
        if (!FileSystem::is_device(full_path))
            continue;

        auto keyboard_fd_or_error = Core::System::open(full_path, O_RDONLY | O_NONBLOCK | O_CLOEXEC);
        if (keyboard_fd_or_error.is_error()) {
            dbgln("Couldn't open {}", full_path);
            continue;
        }

        auto keyboard_fd = keyboard_fd_or_error.release_value();

        auto keyboard_notifier = Core::Notifier::construct(keyboard_fd, Core::Notifier::Type::Read);
        keyboard_notifier->on_activation = [this, keyboard_fd] { drain_keyboard(keyboard_fd); };
        m_keyboard_devices.append({ keyboard_fd, keyboard_notifier });
    }
}

void EventLoop::refresh_mouse_devices()
{
    for (auto& device : m_mouse_devices) {
        close(device.fd);
        device.notifier->close();
    }
    m_mouse_devices.clear();
    Core::DirIterator di("/dev/input/mouse/", Core::DirIterator::SkipParentAndBaseDir);
    while (di.has_next()) {
        auto path = di.next_path();
        auto full_path = ByteString::formatted("/dev/input/mouse/{}", path);
        if (!FileSystem::is_device(full_path))
            continue;

        auto mouse_fd_or_error = Core::System::open(full_path, O_RDONLY | O_NONBLOCK | O_CLOEXEC);
        if (mouse_fd_or_error.is_error()) {
            dbgln("Couldn't open {}", full_path);
            continue;
        }

        auto mouse_fd = mouse_fd_or_error.release_value();

        auto mouse_notifier = Core::Notifier::construct(mouse_fd, Core::Notifier::Type::Read);
        mouse_notifier->on_activation = [this, mouse_fd] { drain_mouse(mouse_fd); };
        m_keyboard_devices.append({ mouse_fd, mouse_notifier });
    }
}

void EventLoop::drain_mouse(int fd)
{
    auto& screen_input = ScreenInput::the();
    MousePacket state;
    state.buttons = screen_input.mouse_button_state();
    MousePacket packets[32];

    ssize_t nread = read(fd, &packets, sizeof(packets));
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
        dbgln_if(WSMESSAGELOOP_DEBUG, "EventLoop: Mouse X {}, Y {}, Z {}, W {}, relative={}", packet.x, packet.y, packet.z, packet.w, packet.is_relative);

        state.is_relative = packet.is_relative;
        if (packet.is_relative) {
            state.x += packet.x;
            state.y -= packet.y;
        } else {
            state.x = packet.x;
            state.y = packet.y;
        }
        state.w += packet.w;
        state_is_sent = false;

        // Invert scroll direction if checked in the settings.
        if (WindowManager::the().is_natural_scroll())
            state.z -= packet.z;
        else
            state.z += packet.z;

        if (packet.buttons != state.buttons) {
            state.buttons = packet.buttons;
            dbgln_if(WSMESSAGELOOP_DEBUG, "EventLoop: Mouse Button Event");

            // Swap primary (1) and secondary (2) buttons if checked in Settings.
            // Doing the swap here avoids all emulator and hardware issues.
            if (WindowManager::the().are_mouse_buttons_switched()) {
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
                state.w = 0;
            }
        }
    }
    if (state_is_sent)
        return;
    if (state.is_relative && (state.x || state.y || state.z || state.w))
        screen_input.on_receive_mouse_data(state);
    if (!state.is_relative)
        screen_input.on_receive_mouse_data(state);
}

void EventLoop::drain_keyboard(int fd)
{
    auto& screen_input = ScreenInput::the();
    for (;;) {
        ::KeyEvent event;
        ssize_t nread = read(fd, (u8*)&event, sizeof(::KeyEvent));
        if (nread == 0)
            break;
        VERIFY(nread == sizeof(::KeyEvent));
        screen_input.on_receive_keyboard_data(event);
    }
}

}
