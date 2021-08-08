/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/MappedFile.h>
#include <AK/StringBuilder.h>
#include <LibCore/MimeData.h>
#include <RemoteDesktopServer/Client.h>
#include <RemoteDesktopServer/GfxClient.h>
#include <RemoteDesktopServer/Server.h>

namespace RemoteDesktopServer {

Client::Client(NonnullRefPtr<Core::TCPSocket> socket, Server& server)
    : RemoteDesktop::RemoteDesktopClientConnection(move(socket))
    , m_server(server)
{
    dbgln_if(REMOTE_DESKTOP_SERVER_DEBUG, "Client {:p}: connected", this);

    on_disconnect = [this]() {
        dbgln_if(REMOTE_DESKTOP_SERVER_DEBUG, "Client {:p}: disconnected", this);
        m_server->client_disconnected(*this);
        GfxClient::for_each([&](auto& gfx_client) {
            gfx_client.notify_enable_remote_gfx(false);
        });
        if (m_compositor_connection)
            m_compositor_connection->shutdown();
    };
}

Client::~Client()
{
    m_server->remove_client(*this);
}

Messages::RemoteDesktopServer::StartSessionResponse Client::start_session(Vector<ByteBuffer> const& available_fonts)
{
    if (!m_server->set_forwarding_client(this)) {
        dbgln("Client::start_session failed: Another client already started session");
        return { true, "Another client already started session"sv };
    }
    m_compositor_connection = adopt_ref(*new RemoteDesktop::RemoteCompositorServerConnection(*this));
    auto result = m_compositor_connection->start_session();
    if (result.error()) {
        m_server->set_forwarding_client(nullptr);
        m_compositor_connection->shutdown();
        m_compositor_connection = nullptr;
        dbgln("Client::start_session failed: {}", result.error_msg());
        return { true, result.error_msg() };
    }

    dbgln("Client::start_session: Session started, start forwarding");

    // Start forwarding messages
    m_compositor_connection->set_forwarding(true);
    GfxClient::for_each([&](auto& gfx_client) {
        gfx_client.notify_enable_remote_gfx(true);
        if (!available_fonts.is_empty())
            gfx_client.async_notify_remote_fonts(available_fonts);
    });

    // Start requesting updates
    m_compositor_connection->async_ready_for_more();

    deferred_invoke([this]() {
        enable_send_buffer(1500);
    });
    return { false, {} };
}

void Client::send_compositor_message(Vector<u8> const& message_bytes)
{
    if (auto result = m_compositor_connection->post_message({ .data = message_bytes, .fds = {} }); result.is_error())
        dbgln("Client::start_session failed to post compositor message: {}", result.error());
}

}
