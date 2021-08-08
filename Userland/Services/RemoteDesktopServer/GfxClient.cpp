/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Random.h>
#include <RemoteDesktopServer/Client.h>
#include <RemoteDesktopServer/GfxClient.h>
#include <RemoteDesktopServer/Server.h>

namespace RemoteDesktopServer {

HashMap<u32, NonnullRefPtr<GfxClient>> GfxClient::s_clients;
static int s_next_client_id = 0;

GfxClient::GfxClient(NonnullRefPtr<Core::LocalSocket> socket, Server& server)
    : IPC::Connection<RemoteGfxServerEndpoint, RemoteGfxClientEndpoint, Core::LocalSocket>(*this, move(socket))
    , RemoteGfxClientProxy<RemoteGfxServerEndpoint, RemoteGfxClientEndpoint, GfxClient>(*this, {})
    , m_client_id(++s_next_client_id)
    , m_server(server)
{
    VERIFY(m_client_id > 0);
    auto result = s_clients.set(m_client_id, *this);
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);

    this->socket().set_blocking(true);

    if (m_server.forwarding_client())
        notify_enable_remote_gfx(true);
}

GfxClient::~GfxClient()
{
    auto removed = s_clients.remove(m_client_id);
    VERIFY(removed);

    if (auto* forwarding_client = m_server.forwarding_client()) {
        forwarding_client->async_disassociate_gfx_client(m_client_id);
        forwarding_client->deferred_flush_send_buffer();
    }
}

void GfxClient::handle_raw_message(NonnullOwnPtr<IPC::Message>&& message, ReadonlyBytes const& bytes, bool is_peer)
{
    if (auto* forwarding_client = m_server.forwarding_client()) {
        dbgln_if(REMOTE_GFX_DEBUG, "{} forwarding raw message with {} bytes", *this, bytes.size());
        forwarding_client->async_gfx_message(m_client_id, ByteBuffer::copy((void const*)bytes.data(), bytes.size()).release_value());
        forwarding_client->deferred_flush_send_buffer();
        return;
    }

    Connection::handle_raw_message(move(message), bytes, is_peer);
}

void GfxClient::notify_enable_remote_gfx(bool enable)
{
    if (enable) {
        if (!m_cookie.has_value())
            m_cookie = get_random<u64>();
        async_enable_remote_gfx(m_cookie.value());
        auto* forwarding_client = m_server.forwarding_client();
        VERIFY(forwarding_client);
        forwarding_client->async_associate_gfx_client(m_client_id, m_cookie.value());
        forwarding_client->deferred_flush_send_buffer();
    } else {
        async_disable_remote_gfx();
    }
}

}
