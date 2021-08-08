/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibRemoteDesktop/RemoteCompositorServerConnection.h>
#include <LibRemoteDesktop/RemoteDesktopClientConnection.h>

namespace RemoteDesktop {

RemoteCompositorServerConnection::RemoteCompositorServerConnection(RefPtr<RemoteDesktopClientConnection> client_connection)
    : IPC::ServerConnection<RemoteCompositorClientEndpoint, RemoteCompositorServerEndpoint>(*this, "/tmp/portal/remotecompositor")
    , m_client_connection(move(client_connection))
{
    dbgln("RemoteCompositorServerConnection {}", this);
    if (m_client_connection) {
        on_idle = [this]() {
            if (m_should_request_mode) {
                m_should_request_mode = false;
                if (m_client_connection->is_connected()) {
                    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "RemoteCompositorServerConnection: requesting more");
                    async_ready_for_more();
                }
            }
        };
        on_handle_raw_message = [this](bool, ReadonlyBytes const& bytes) {
            if (!m_forwarding)
                return true;
            dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "RemoteCompositorServerConnection: forwarding raw message with {} bytes requested more: {}", bytes.size(), m_should_request_mode);
            m_client_connection->async_compositor_message(ByteBuffer::copy((void const*)bytes.data(), bytes.size()).release_value());
            m_should_request_mode = true;
            return false;
        };
    }
}

RemoteCompositorServerConnection::~RemoteCompositorServerConnection()
{
    dbgln("~RemoteCompositorServerConnection {}", this);
}

void RemoteCompositorServerConnection::die()
{
    NonnullRefPtr<RemoteCompositorServerConnection> protect(*this);
    if (on_disconnect)
        on_disconnect();
}

void RemoteCompositorServerConnection::fast_greet(Vector<Gfx::IntRect> const&, Gfx::Color const&, Gfx::IntPoint const&)
{
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "RemoteCompositorServerConnection::fast_greet");
}

void RemoteCompositorServerConnection::associate_window_client(int windowserver_client_id, u64 cookie)
{
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "associate_window_client::fast_greet windowserver_client_id: {} cookie: {}", windowserver_client_id, cookie);
}

void RemoteCompositorServerConnection::disassociate_window_client(int windowserver_client_id)
{
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "disassociate_window_client::fast_greet windowserver_client_id: {}", windowserver_client_id);
}

void RemoteCompositorServerConnection::update_display(Vector<Compositor::WindowId> const& window_order, Vector<Compositor::Window> const& windows, Vector<Compositor::WindowId> const& delete_windows, Vector<Compositor::WindowDirtyRects> const& window_dirty_rects)
{
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "update_occlusions: window order changed: {}, windows: {} delete windows: {} dirty windows: {}", !window_order.is_empty(), windows.size(), delete_windows.size(), window_dirty_rects.size());
    for (auto& dirty_rects : window_dirty_rects) {
        dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "    window {}: {} rects", dirty_rects.id, dirty_rects.dirty_rects.size());
        for (auto& rect : dirty_rects.dirty_rects)
            dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "        {}", rect);
    }
}

void RemoteCompositorServerConnection::cursor_position_changed(Gfx::IntPoint const& cursor_position)
{
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "cursor_position_changed: cursor_position: {}", cursor_position);
}

}
