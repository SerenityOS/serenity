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
    , m_ssh_client(*m_socket, [this]() {
        Core::EventLoop::current().deferred_invoke([this]() { die(); });
    })
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

    client->m_write_notifier = Core::Notifier::construct(client->m_socket->fd(), Core::Notifier::Type::Write);
    client->m_write_notifier->on_activation = [ptr = client.ptr()] {
        ptr->on_ready_to_write();
    };
    client->m_write_notifier->set_enabled(false);
    auto write_notifier = client->m_write_notifier;
    client->m_ssh_client.set_output_pending_callback([write_notifier = move(write_notifier)](bool is_pending) {
        write_notifier->set_enabled(is_pending);
    });

    return client;
}

ErrorOr<void> TCPClient::on_ready_to_read()
{
    if (m_is_dying)
        return {};

    auto buffer = TRY(ByteBuffer::create_uninitialized(PAGE_SIZE));
    bool reached_eof = false;

    // Parse before draining more of the socket so a peer cannot grow the
    // application buffer by flooding data faster than we inspect it.
    for (;;) {
        if (!m_read_buffer.is_empty()) {
            auto next_behavior = TRY(m_ssh_client.handle_data(m_read_buffer));
            switch (next_behavior) {
            case SSH::BehaviorControl::ContinueExecution:
                continue;
            case SSH::BehaviorControl::WaitForMoreData:
                if (reached_eof) {
                    die();
                    return {};
                }
                break;
            case SSH::BehaviorControl::Disconnect:
                die();
                return {};
            }
        }

        if (reached_eof) {
            die();
            return {};
        }

        if (!TRY(m_socket->can_read_without_blocking()))
            return {};

        auto data = TRY(m_socket->read_some(buffer));
        TRY(m_read_buffer.try_append(data));
        reached_eof = m_socket->is_eof();
    }
}

void TCPClient::on_ready_to_write()
{
    if (m_is_dying)
        return;

    auto maybe_error = m_ssh_client.flush_pending_output();
    if (maybe_error.is_error()) {
        dbgln("error while writing SSH output: {}", maybe_error.error());
        die();
    }
}

void TCPClient::die()
{
    if (m_is_dying)
        return;

    m_is_dying = true;
    m_ssh_client.shutdown();
    if (m_write_notifier)
        m_write_notifier->close();
    m_socket->set_notifications_enabled(false);
    m_socket->close();
    Core::EventLoop::current().deferred_invoke([on_quit = move(m_on_quit)] { on_quit(); });
}

} // SSHServer
