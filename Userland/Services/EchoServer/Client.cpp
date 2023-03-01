/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Client.h"
#include <LibCore/EventLoop.h>
#include <LibCore/Socket.h>

Client::Client(int id, NonnullOwnPtr<Core::TCPSocket> socket)
    : m_id(id)
    , m_socket(move(socket))
{
    m_socket->on_ready_to_read = [this] {
        if (m_socket->is_eof())
            return;

        auto result = drain_socket();
        if (result.is_error()) {
            dbgln("Failed while trying to drain the socket: {}", result.error());
            Core::deferred_invoke([this, strong_this = NonnullRefPtr(*this)] { quit(); });
        }
    };
}

ErrorOr<void> Client::drain_socket()
{
    NonnullRefPtr<Client> protect(*this);

    auto buffer = TRY(ByteBuffer::create_uninitialized(1024));

    while (TRY(m_socket->can_read_without_blocking())) {
        auto bytes_read = TRY(m_socket->read_some(buffer));

        dbgln("Read {} bytes.", bytes_read.size());

        if (m_socket->is_eof()) {
            Core::deferred_invoke([this, strong_this = NonnullRefPtr(*this)] { quit(); });
            break;
        }

        TRY(m_socket->write_until_depleted(bytes_read));
    }

    return {};
}

void Client::quit()
{
    m_socket->close();
    if (on_exit)
        on_exit();
}
