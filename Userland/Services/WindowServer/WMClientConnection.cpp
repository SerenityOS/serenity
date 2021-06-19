/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <WindowServer/AppletManager.h>
#include <WindowServer/ClientConnection.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WMClientConnection.h>

namespace WindowServer {

HashMap<int, NonnullRefPtr<WMClientConnection>> WMClientConnection::s_connections {};

WMClientConnection::WMClientConnection(NonnullRefPtr<Core::LocalSocket> client_socket, int client_id)
    : IPC::ClientConnection<WindowManagerClientEndpoint, WindowManagerServerEndpoint>(*this, move(client_socket), client_id)
{
    s_connections.set(client_id, *this);
}

WMClientConnection::~WMClientConnection()
{
    // The WM has gone away, so take away the applet manager (cause there's nowhere
    // to draw it in).
    AppletManager::the().set_position({});
}

void WMClientConnection::die()
{
    deferred_invoke([this](auto&) {
        s_connections.remove(client_id());
    });
}

void WMClientConnection::set_applet_area_position(Gfx::IntPoint const& position)
{
    if (m_window_id < 0) {
        did_misbehave("SetAppletAreaPosition: WM didn't assign window as manager yet");
        // FIXME: return ok boolean?
        return;
    }

    AppletManager::the().set_position(position);
}

void WMClientConnection::set_active_window(i32 client_id, i32 window_id)
{
    auto* client = WindowServer::ClientConnection::from_client_id(client_id);
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
    WindowManager::the().minimize_windows(window, false);
    WindowManager::the().move_to_front_and_make_active(window);
}

void WMClientConnection::popup_window_menu(i32 client_id, i32 window_id, Gfx::IntPoint const& screen_position)
{
    auto* client = WindowServer::ClientConnection::from_client_id(client_id);
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

void WMClientConnection::start_window_resize(i32 client_id, i32 window_id)
{
    auto* client = WindowServer::ClientConnection::from_client_id(client_id);
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
    WindowManager::the().start_window_resize(window, ScreenInput::the().cursor_location(), MouseButton::Left);
}

void WMClientConnection::set_window_minimized(i32 client_id, i32 window_id, bool minimized)
{
    auto* client = WindowServer::ClientConnection::from_client_id(client_id);
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

void WMClientConnection::set_event_mask(u32 event_mask)
{
    m_event_mask = event_mask;
}

void WMClientConnection::set_manager_window(i32 window_id)
{
    m_window_id = window_id;

    // Let the window manager know that we obtained a manager window, and should
    // receive information about other windows.
    WindowManager::the().greet_window_manager(*this);
}

void WMClientConnection::set_window_taskbar_rect(i32 client_id, i32 window_id, Gfx::IntRect const& rect)
{
    // Because the Taskbar (which should be the only user of this API) does not own the
    // window or the client id, there is a possibility that it may send this message for
    // a window or client that may have been destroyed already. This is not an error,
    // and we should not call did_misbehave() for either.
    auto* client = WindowServer::ClientConnection::from_client_id(client_id);
    if (!client)
        return;

    auto it = client->m_windows.find(window_id);
    if (it == client->m_windows.end())
        return;

    auto& window = *(*it).value;
    window.set_taskbar_rect(rect);
}

}
