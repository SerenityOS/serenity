/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Vector.h>

#include <LibCore/TCPSocket.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibRemoteDesktop/RemoteCompositor.h>

// The order here matters. These must come after all headers defining
// the various objects used in the IPC messages
#include <LibGfx/Remote/RemoteGfxClientEndpoint.h>
#include <LibGfx/Remote/RemoteGfxRenderer.h>
#include <LibGfx/Remote/RemoteGfxServerEndpoint.h>
#include <LibRemoteDesktop/RemoteCompositorClientEndpoint.h>
#include <LibRemoteDesktop/RemoteCompositorServerEndpoint.h>
#include <LibRemoteDesktop/RemoteDesktopClientEndpoint.h>
#include <LibRemoteDesktop/RemoteDesktopServerEndpoint.h>

namespace RemoteDesktop {

class RemoteDesktopServerConnection final : public IPC::Connection<RemoteDesktopClientEndpoint, RemoteDesktopServerEndpoint, Core::TCPSocket>
    , public RemoteDesktopClientEndpoint::Stub
    , public RemoteDesktopServerProxy<RemoteDesktopClientEndpoint, RemoteDesktopServerEndpoint, RemoteDesktopServerConnection> {
    C_OBJECT(RemoteDesktopServerConnection)

    class CompositorServer : public RemoteCompositorServerProxy<RemoteCompositorClientEndpoint, RemoteCompositorServerEndpoint, CompositorServer> {
    public:
        CompositorServer(RemoteDesktopServerConnection& connection)
            : RemoteCompositorServerProxy<RemoteCompositorClientEndpoint, RemoteCompositorServerEndpoint, CompositorServer>(*this, {})
            , m_connection(connection)
        {
        }

        void post_message(IPC::Message const& message)
        {
            post_message(message.encode());
        }

        // FIXME: unnecessary copy
        void post_message(IPC::MessageBuffer buffer)
        {
            VERIFY(buffer.fds.is_empty());
            m_connection.async_send_compositor_message(buffer.data);
        }

    private:
        RemoteDesktopServerConnection& m_connection;
    };

public:
    RemoteDesktopServerConnection()
        : IPC::Connection<RemoteDesktopClientEndpoint, RemoteDesktopServerEndpoint, Core::TCPSocket>(*this, Core::TCPSocket::construct())
        , RemoteDesktopServerProxy<RemoteDesktopClientEndpoint, RemoteDesktopServerEndpoint, RemoteDesktopServerConnection>(*this, {})
        , m_compositor_server(*this)
    {
        // We want to rate-limit our clients
        this->socket().set_blocking(true);
    }

    bool connect(IPv4Address const& address, u16 port)
    {
        if (!this->socket().connect({ address, port }, port)) {
            perror("connect");
            return false;
        }
        return true;
    }

    virtual void compositor_message(ByteBuffer const& bytes) override
    {
        // dbgln("RemoteDesktopServerConnection::compositor_message {} bytes, have {} compositors", bytes.size(), m_compositors.size());
        if (auto message = RemoteCompositorClientEndpoint::decode_message(bytes, -1)) {
            for (auto& compositor : m_compositors)
                compositor->handle(*message);
        } else {
            dbgln("RemoteDesktopServerConnection::compositor_message failed to decode message with {} bytes", bytes.size());
        }
    }

    virtual void associate_gfx_client(int gfx_client_id, u64 cookie) override
    {
        dbgln("RemoteDesktopServerConnection::associate_remote_gfx gfx_client_id {} cookie {}", gfx_client_id, cookie);
        auto it = m_cookie_data.find(cookie);
        if (it == m_cookie_data.end()) {
            m_cookie_data.set(cookie, { .gfx_client_id = gfx_client_id });
        } else {
            auto& cookie_data = it->value;
            VERIFY(cookie_data.gfx_client_id == 0);
            VERIFY(cookie_data.windowserver_client_id > 0);
            cookie_data.gfx_client_id = gfx_client_id;
            auto result = m_gfx_to_window_client_map.set(gfx_client_id, cookie_data.windowserver_client_id);
            VERIFY(result == AK::HashSetResult::InsertedNewEntry);
            result = m_window_to_gfx_client_map.set(cookie_data.windowserver_client_id, gfx_client_id);
            VERIFY(result == AK::HashSetResult::InsertedNewEntry);

            dbgln("RemoteDesktopServerConnection::associate_window_client: windowserver_client_id {} <-> gfx_client_id {}", cookie_data.windowserver_client_id, gfx_client_id);
        }
    }

    virtual void disassociate_gfx_client(int gfx_client_id) override
    {
        dbgln("RemoteDesktopServerConnection::disassociate_gfx_client gfx_client_id {}", gfx_client_id);
        for (auto it = m_cookie_data.begin(); it != m_cookie_data.end(); ++it) {
            auto& cookie_data = it->value;
            if (cookie_data.gfx_client_id == gfx_client_id) {
                if (cookie_data.windowserver_client_id != 0) {
                    bool removed = m_gfx_to_window_client_map.remove(gfx_client_id);
                    VERIFY(removed);
                    removed = m_window_to_gfx_client_map.remove(cookie_data.windowserver_client_id);
                    VERIFY(removed);
                    dbgln("RemoteDesktopServerConnection::disassociate_gfx_client windowserver_client_id {} no longer associated with gfx_client_id {}", cookie_data.windowserver_client_id, gfx_client_id);
                } else {
                    dbgln("RemoteDesktopServerConnection::disassociate_gfx_client removing cookie {} from gfx_client_id {}", it->key, gfx_client_id);
                    m_cookie_data.remove(it);
                }
                if (on_delete_gfx_client)
                    on_delete_gfx_client(gfx_client_id);
                return;
            }
        }
    }

    void handle_associate_window_client(int windowserver_client_id, u64 cookie)
    {
        dbgln("RemoteDesktopServerConnection::handle_associate_window_client windowserver_client_id {} cookie {}", windowserver_client_id, cookie);
        auto it = m_cookie_data.find(cookie);
        if (it == m_cookie_data.end()) {
            m_cookie_data.set(cookie, { .windowserver_client_id = windowserver_client_id });
        } else {
            auto& cookie_data = it->value;
            VERIFY(cookie_data.gfx_client_id > 0);
            VERIFY(cookie_data.windowserver_client_id == 0);
            cookie_data.windowserver_client_id = windowserver_client_id;
            auto result = m_window_to_gfx_client_map.set(windowserver_client_id, cookie_data.gfx_client_id);
            VERIFY(result == AK::HashSetResult::InsertedNewEntry);
            result = m_gfx_to_window_client_map.set(cookie_data.gfx_client_id, windowserver_client_id);
            VERIFY(result == AK::HashSetResult::InsertedNewEntry);
            dbgln("RemoteDesktopServerConnection::handle_associate_window_client: windowserver_client_id {} <-> gfx_client_id {}", windowserver_client_id, cookie_data.gfx_client_id);
            if (on_associate_clients)
                on_associate_clients(windowserver_client_id, cookie_data.gfx_client_id);
        }
    }

    void handle_disassociate_window_client(int windowserver_client_id)
    {
        dbgln("RemoteDesktopServerConnection::handle_disassociate_window_client windowserver_client_id {}", windowserver_client_id);
        for (auto it = m_cookie_data.begin(); it != m_cookie_data.end(); ++it) {
            auto& cookie_data = it->value;
            if (cookie_data.windowserver_client_id == windowserver_client_id) {
                if (cookie_data.gfx_client_id != 0) {
                    bool removed = m_gfx_to_window_client_map.remove(cookie_data.gfx_client_id);
                    VERIFY(removed);
                    removed = m_window_to_gfx_client_map.remove(windowserver_client_id);
                    VERIFY(removed);
                    dbgln("RemoteDesktopServerConnection::handle_disassociate_window_client windowserver_client_id {} no longer associated with gfx_client_id {}", windowserver_client_id, cookie_data.gfx_client_id);
                } else {
                    dbgln("RemoteDesktopServerConnection::handle_disassociate_gfx_client removing cookie {} from windowserver_client_id {}", it->key, windowserver_client_id);
                    m_cookie_data.remove(it);
                }
                return;
            }
        }
    }

    void register_compositor(RemoteCompositorClientEndpoint::Stub& compositor)
    {
        VERIFY(!m_compositors.contains_slow(&compositor));
        m_compositors.append(&compositor);
    }

    void unregister_compositor(RemoteCompositorClientEndpoint::Stub& compositor)
    {
        bool removed = m_compositors.remove_first_matching([&](auto& c) {
            return c == &compositor;
        });
        VERIFY(removed);
    }

    virtual void gfx_message(int client_id, ByteBuffer const& bytes) override
    {
        // dbgln("RemoteDesktopServerConnection::gfx_message from {} with {} bytes, have {} compositors", client_id, bytes.size(), m_compositors.size());
        auto it = m_gfx_clients.find(client_id);
        if (it == m_gfx_clients.end() && on_new_gfx_client) {
            if (on_new_gfx_client(client_id))
                it = m_gfx_clients.find(client_id);
        }
        if (it != m_gfx_clients.end()) {
            if (auto message = RemoteGfxServerEndpoint::decode_message(bytes, -1)) {
                it->value->handle(*message);
            } else {
                dbgln("RemoteDesktopServerConnection::gfx_message failed to decode message from {} with {} bytes", client_id, bytes.size());
            }
        } else {
            dbgln("RemoteDesktopServerConnection::gfx_message dropping message from {} with {} bytes, no handler", client_id, bytes.size());
        }
    }

    void register_gfx(int client_id, RemoteGfx::RemoteGfxRenderer& gfx)
    {
        auto result = m_gfx_clients.set(client_id, &gfx);
        VERIFY(result == AK::HashSetResult::InsertedNewEntry);
    }

    void unregister_gfx(int client_id)
    {
        auto removed = m_gfx_clients.remove(client_id);
        VERIFY(removed);
    }

    Function<bool(int)> on_new_gfx_client;
    Function<void(int)> on_delete_gfx_client;
    Function<void(int, int)> on_associate_clients;

    int gfx_to_window_client(int gfx_client_id) const
    {
        auto it = m_gfx_to_window_client_map.find(gfx_client_id);
        return it != m_gfx_to_window_client_map.end() ? it->value : 0;
    }

    int window_to_gfx_client(int window_client_id) const
    {
        auto it = m_window_to_gfx_client_map.find(window_client_id);
        return it != m_window_to_gfx_client_map.end() ? it->value : 0;
    }

    auto* find_gfx_renderer(int gfx_client_id)
    {
        auto it = m_gfx_clients.find(gfx_client_id);
        return it != m_gfx_clients.end() ? it->value : nullptr;
    }

    auto& compositor_server() { return m_compositor_server; }

private:
    struct CookieData {
        int windowserver_client_id { 0 };
        int gfx_client_id { 0 };
    };
    Vector<RemoteCompositorClientEndpoint::Stub*, 1> m_compositors;
    CompositorServer m_compositor_server;
    HashMap<int, RemoteGfx::RemoteGfxRenderer*> m_gfx_clients;
    HashMap<u64, CookieData> m_cookie_data;
    HashMap<int, int> m_gfx_to_window_client_map;
    HashMap<int, int> m_window_to_gfx_client_map;
};

}
