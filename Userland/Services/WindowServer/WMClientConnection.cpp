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

void WMClientConnection::handle(const Messages::WindowManagerServer::SetAppletAreaPosition& message)
{
    if (m_window_id < 0) {
        did_misbehave("SetAppletAreaPosition: WM didn't assign window as manager yet");
        // FIXME: return ok boolean?
        return;
    }

    AppletManager::the().set_position(message.position());
}

void WMClientConnection::handle(const Messages::WindowManagerServer::SetActiveWindow& message)
{
    auto* client = WindowServer::ClientConnection::from_client_id(message.client_id());
    if (!client) {
        did_misbehave("SetActiveWindow: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(message.window_id());
    if (it == client->m_windows.end()) {
        did_misbehave("SetActiveWindow: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    WindowManager::the().minimize_windows(window, false);
    WindowManager::the().move_to_front_and_make_active(window);
}

void WMClientConnection::handle(const Messages::WindowManagerServer::PopupWindowMenu& message)
{
    auto* client = WindowServer::ClientConnection::from_client_id(message.client_id());
    if (!client) {
        did_misbehave("PopupWindowMenu: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(message.window_id());
    if (it == client->m_windows.end()) {
        did_misbehave("PopupWindowMenu: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    if (auto* modal_window = window.blocking_modal_window()) {
        modal_window->popup_window_menu(message.screen_position(), WindowMenuDefaultAction::BasedOnWindowState);
    } else {
        window.popup_window_menu(message.screen_position(), WindowMenuDefaultAction::BasedOnWindowState);
    }
}

void WMClientConnection::handle(const Messages::WindowManagerServer::StartWindowResize& request)
{
    auto* client = WindowServer::ClientConnection::from_client_id(request.client_id());
    if (!client) {
        did_misbehave("WM_StartWindowResize: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(request.window_id());
    if (it == client->m_windows.end()) {
        did_misbehave("WM_StartWindowResize: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    // FIXME: We are cheating a bit here by using the current cursor location and hard-coding the left button.
    //        Maybe the client should be allowed to specify what initiated this request?
    WindowManager::the().start_window_resize(window, Screen::the().cursor_location(), MouseButton::Left);
}

void WMClientConnection::handle(const Messages::WindowManagerServer::SetWindowMinimized& message)
{
    auto* client = WindowServer::ClientConnection::from_client_id(message.client_id());
    if (!client) {
        did_misbehave("WM_SetWindowMinimized: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(message.window_id());
    if (it == client->m_windows.end()) {
        did_misbehave("WM_SetWindowMinimized: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    WindowManager::the().minimize_windows(window, message.minimized());
}

void WMClientConnection::handle(const Messages::WindowManagerServer::SetEventMask& message)
{
    m_event_mask = message.event_mask();
}

void WMClientConnection::handle(const Messages::WindowManagerServer::SetManagerWindow& message)
{
    m_window_id = message.window_id();

    // Let the window manager know that we obtained a manager window, and should
    // receive information about other windows.
    WindowManager::the().greet_window_manager(*this);
}

void WMClientConnection::handle(const Messages::WindowManagerServer::SetWindowTaskbarRect& message)
{
    // Because the Taskbar (which should be the only user of this API) does not own the
    // window or the client id, there is a possibility that it may send this message for
    // a window or client that may have been destroyed already. This is not an error,
    // and we should not call did_misbehave() for either.
    auto* client = WindowServer::ClientConnection::from_client_id(message.client_id());
    if (!client)
        return;

    auto it = client->m_windows.find(message.window_id());
    if (it == client->m_windows.end())
        return;

    auto& window = *(*it).value;
    window.set_taskbar_rect(message.rect());
}

}
