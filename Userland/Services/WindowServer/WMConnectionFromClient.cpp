/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <WindowServer/AppletManager.h>
#include <WindowServer/ConnectionFromClient.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WMConnectionFromClient.h>

namespace WindowServer {

HashMap<int, NonnullRefPtr<WMConnectionFromClient>> WMConnectionFromClient::s_connections {};

WMConnectionFromClient::WMConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> client_socket, int client_id)
    : IPC::ConnectionFromClient<WindowManagerClientEndpoint, WindowManagerServerEndpoint>(*this, move(client_socket), client_id)
{
    s_connections.set(client_id, *this);
}

WMConnectionFromClient::~WMConnectionFromClient()
{
    // The WM has gone away, so take away the applet manager (cause there's nowhere
    // to draw it in).
    AppletManager::the().set_position({});
}

void WMConnectionFromClient::die()
{
    deferred_invoke([this] {
        s_connections.remove(client_id());
    });
}

void WMConnectionFromClient::set_applet_area_position(Gfx::IntPoint position)
{
    if (m_window_id < 0) {
        did_misbehave("SetAppletAreaPosition: WM didn't assign window as manager yet");
        // FIXME: return ok boolean?
        return;
    }

    AppletManager::the().set_position(position);

    WindowServer::ConnectionFromClient::for_each_client([](auto& connection) {
        if (auto result = connection.post_message(Messages::WindowClient::AppletAreaRectChanged(AppletManager::the().window()->rect())); result.is_error()) {
            dbgln("WMConnectionFromClient::set_applet_area_position: {}", result.error());
        }
    });
}

void WMConnectionFromClient::set_active_window(i32 client_id, i32 window_id)
{
    auto* client = WindowServer::ConnectionFromClient::from_client_id(client_id);
    if (!client) {
        did_misbehave("SetActiveWindow: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(window_id);
    if (it == client->m_windows.end()) {
        did_misbehave("SetActiveWindow: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    WindowManager::the().move_to_front_and_make_active(window);
}

void WMConnectionFromClient::popup_window_menu(i32 client_id, i32 window_id, Gfx::IntPoint screen_position)
{
    auto* client = WindowServer::ConnectionFromClient::from_client_id(client_id);
    if (!client) {
        did_misbehave("PopupWindowMenu: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(window_id);
    if (it == client->m_windows.end()) {
        did_misbehave("PopupWindowMenu: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    if (auto* modal_window = window.blocking_modal_window()) {
        modal_window->popup_window_menu(screen_position, WindowMenuDefaultAction::BasedOnWindowState);
    } else {
        window.popup_window_menu(screen_position, WindowMenuDefaultAction::BasedOnWindowState);
    }
}

void WMConnectionFromClient::start_window_resize(i32 client_id, i32 window_id, i32 resize_direction)
{
    auto* client = WindowServer::ConnectionFromClient::from_client_id(client_id);
    if (!client) {
        did_misbehave("WM_StartWindowResize: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(window_id);
    if (it == client->m_windows.end()) {
        did_misbehave("WM_StartWindowResize: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    // FIXME: We are cheating a bit here by using the current cursor location and hard-coding the left button.
    //        Maybe the client should be allowed to specify what initiated this request?
    WindowManager::the().start_window_resize(window, ScreenInput::the().cursor_location(), MouseButton::Primary, (ResizeDirection)resize_direction);
}

void WMConnectionFromClient::set_window_minimized(i32 client_id, i32 window_id, bool minimized)
{
    auto* client = WindowServer::ConnectionFromClient::from_client_id(client_id);
    if (!client) {
        did_misbehave("WM_SetWindowMinimized: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(window_id);
    if (it == client->m_windows.end()) {
        did_misbehave("WM_SetWindowMinimized: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    WindowManager::the().minimize_windows(window, minimized);
}

void WMConnectionFromClient::toggle_show_desktop()
{
    bool should_hide = false;
    auto& current_window_stack = WindowManager::the().current_window_stack();
    current_window_stack.for_each_window([&](auto& window) {
        if (window.type() == WindowType::Normal && window.is_minimizable()) {
            if (!window.is_hidden() && !window.is_minimized()) {
                should_hide = true;
                return IterationDecision::Break;
            }
        }
        return IterationDecision::Continue;
    });

    current_window_stack.for_each_window([&](auto& window) {
        if (window.type() == WindowType::Normal && window.is_minimizable()) {
            auto state = window.minimized_state();
            if (state == WindowMinimizedState::None || state == WindowMinimizedState::Hidden) {
                WindowManager::the().hide_windows(window, should_hide);
            }
        }
        return IterationDecision::Continue;
    });
}

void WMConnectionFromClient::set_event_mask(u32 event_mask)
{
    m_event_mask = event_mask;
}

void WMConnectionFromClient::set_manager_window(i32 window_id)
{
    m_window_id = window_id;

    // Let the window manager know that we obtained a manager window, and should
    // receive information about other windows.
    WindowManager::the().greet_window_manager(*this);
}

void WMConnectionFromClient::set_workspace(u32 row, u32 col)
{
    WindowManager::the().switch_to_window_stack(row, col);
}

void WMConnectionFromClient::set_window_taskbar_rect(i32 client_id, i32 window_id, Gfx::IntRect const& rect)
{
    // Because the Taskbar (which should be the only user of this API) does not own the
    // window or the client id, there is a possibility that it may send this message for
    // a window or client that may have been destroyed already. This is not an error,
    // and we should not call did_misbehave() for either.
    auto* client = WindowServer::ConnectionFromClient::from_client_id(client_id);
    if (!client)
        return;

    auto it = client->m_windows.find(window_id);
    if (it == client->m_windows.end())
        return;

    auto& window = *(*it).value;
    window.set_taskbar_rect(rect);
}

void WMConnectionFromClient::set_keymap(ByteString const& keymap)
{
    WindowManager::the().keymap_switcher()->set_keymap(keymap);
}

}
