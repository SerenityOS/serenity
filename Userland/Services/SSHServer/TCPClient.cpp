/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TCPClient.h"
#include <LibCore/EventLoop.h>
#include <LibCore/Socket.h>
#include <LibCore/TCPServer.h>

namespace SSH::Server {

TCPClient::TCPClient(NonnullOwnPtr<Core::TCPSocket>&& socket, Function<void()>&& on_quit)
    : m_on_quit(move(on_quit))
    , m_socket(move(socket))
    , m_ssh_client(*m_socket)
{
}

NonnullOwnPtr<TCPClient> TCPClient::create(NonnullOwnPtr<Core::TCPSocket>&& socket, Function<void()>&& on_quit)
{
    auto client = MUST(adopt_nonnull_own_or_enomem(new TCPClient(move(socket), move(on_quit))));
    client->m_socket->on_ready_to_read = [ptr = client.ptr()]() {
        auto maybe_error = ptr->on_ready_to_read();
        if (maybe_error.is_error()) {
            dbgln("error: {}", maybe_error.error());
            ptr->die();
        }
    };

    return client;
}

ErrorOr<void> TCPClient::on_ready_to_read()
{
    auto buffer = TRY(ByteBuffer::create_uninitialized(PAGE_SIZE));

    for (;;) {
        if (!TRY(m_socket->can_read_without_blocking()))
            break;

        auto data = TRY(m_socket->read_some(buffer));
        TRY(m_read_buffer.try_append(data));

        if (m_socket->is_eof())
            break;
    }

    if (m_read_buffer.is_empty())
        return {};

    while (!m_read_buffer.is_empty()) {
        if (TRY(m_ssh_client.handle_data(m_read_buffer)) == SSHClient::ShouldDisconnect::Yes)
            die();
    }
    return {};
}

void TCPClient::die()
{
    m_socket->close();
    Core::EventLoop::current().deferred_invoke([on_quit = move(m_on_quit)] { on_quit(); });
}

} // SSHServer
