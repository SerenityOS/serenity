/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibIPC/Connection.h>
#include <LibIPC/Stub.h>
#include <sys/select.h>

namespace IPC {

ConnectionBase::ConnectionBase(IPC::Stub& local_stub, NonnullRefPtr<Core::LocalSocket> socket)
    : m_local_stub(local_stub)
    , m_socket(move(socket))
    , m_notifier(Core::Notifier::construct(m_socket->fd(), Core::Notifier::Read, this))
{
    m_responsiveness_timer = Core::Timer::create_single_shot(3000, [this] { may_have_become_unresponsive(); });
}

ConnectionBase::~ConnectionBase()
{
}

void ConnectionBase::post_message(Message const& message)
{
    post_message(message.encode());
}

void ConnectionBase::post_message(MessageBuffer buffer)
{
    // NOTE: If this connection is being shut down, but has not yet been destroyed,
    //       the socket will be closed. Don't try to send more messages.
    if (!m_socket->is_open())
        return;

    // Prepend the message size.
    uint32_t message_size = buffer.data.size();
    buffer.data.prepend(reinterpret_cast<const u8*>(&message_size), sizeof(message_size));

#ifdef __serenity__
    for (auto& fd : buffer.fds) {
        auto rc = sendfd(m_socket->fd(), fd->value());
        if (rc < 0) {
            perror("sendfd");
            shutdown();
        }
    }
#else
    if (!buffer.fds.is_empty())
        warnln("fd passing is not supported on this platform, sorry :(");
#endif

    size_t total_nwritten = 0;
    while (total_nwritten < buffer.data.size()) {
        auto nwritten = write(m_socket->fd(), buffer.data.data() + total_nwritten, buffer.data.size() - total_nwritten);
        if (nwritten < 0) {
            switch (errno) {
            case EPIPE:
                dbgln("{}::post_message: Disconnected from peer", static_cast<Core::Object const&>(*this));
                shutdown();
                return;
            case EAGAIN:
                dbgln("{}::post_message: Peer buffer overflowed", static_cast<Core::Object const&>(*this));
                shutdown();
                return;
            default:
                perror("Connection::post_message write");
                shutdown();
                return;
            }
        }
        total_nwritten += nwritten;
    }

    m_responsiveness_timer->start();
}

void ConnectionBase::shutdown()
{
    m_notifier->close();
    m_socket->close();
    die();
}

void ConnectionBase::handle_messages(u32 local_endpoint_magic)
{
    auto messages = move(m_unprocessed_messages);
    for (auto& message : messages) {
        if (message.endpoint_magic() == local_endpoint_magic)
            if (auto response = m_local_stub.handle(message))
                post_message(*response);
    }
}

void ConnectionBase::wait_for_socket_to_become_readable()
{
    fd_set read_fds;
    FD_ZERO(&read_fds);
    FD_SET(m_socket->fd(), &read_fds);
    for (;;) {
        if (auto rc = select(m_socket->fd() + 1, &read_fds, nullptr, nullptr, nullptr); rc < 0) {
            if (errno == EINTR)
                continue;
            perror("wait_for_specific_endpoint_message: select");
            VERIFY_NOT_REACHED();
        } else {
            VERIFY(rc > 0);
            VERIFY(FD_ISSET(m_socket->fd(), &read_fds));
            break;
        }
    }
}

Result<Vector<u8>, bool> ConnectionBase::read_as_much_as_possible_from_socket_without_blocking()
{
    Vector<u8> bytes;

    if (!m_unprocessed_bytes.is_empty()) {
        bytes.append(m_unprocessed_bytes.data(), m_unprocessed_bytes.size());
        m_unprocessed_bytes.clear();
    }

    while (m_socket->is_open()) {
        u8 buffer[4096];
        ssize_t nread = recv(m_socket->fd(), buffer, sizeof(buffer), MSG_DONTWAIT);
        if (nread < 0) {
            if (errno == EAGAIN)
                break;
            perror("recv");
            exit(1);
            return false;
        }
        if (nread == 0) {
            if (bytes.is_empty()) {
                deferred_invoke([this] { shutdown(); });
                return false;
            }
            break;
        }
        bytes.append(buffer, nread);
    }

    if (!bytes.is_empty()) {
        m_responsiveness_timer->stop();
        did_become_responsive();
    }

    return bytes;
}

bool ConnectionBase::drain_messages_from_peer(u32 local_endpoint_magic)
{
    auto bytes = TRY(read_as_much_as_possible_from_socket_without_blocking());

    size_t index = 0;
    try_parse_messages(bytes, index);

    if (index < bytes.size()) {
        // Sometimes we might receive a partial message. That's okay, just stash away
        // the unprocessed bytes and we'll prepend them to the next incoming message
        // in the next run of this function.
        auto remaining_bytes_result = ByteBuffer::copy(bytes.span().slice(index));
        if (!remaining_bytes_result.has_value()) {
            dbgln("{}::drain_messages_from_peer: Failed to allocate buffer", static_cast<Core::Object const&>(*this));
            return false;
        }
        if (!m_unprocessed_bytes.is_empty()) {
            dbgln("{}::drain_messages_from_peer: Already have unprocessed bytes", static_cast<Core::Object const&>(*this));
            shutdown();
            return false;
        }
        m_unprocessed_bytes = remaining_bytes_result.release_value();
    }

    if (!m_unprocessed_messages.is_empty()) {
        deferred_invoke([this, local_endpoint_magic] {
            handle_messages(local_endpoint_magic);
        });
    }
    return true;
}

OwnPtr<IPC::Message> ConnectionBase::wait_for_specific_endpoint_message_impl(u32 endpoint_magic, int message_id, u32 local_endpoint_magic)
{
    for (;;) {
        // Double check we don't already have the event waiting for us.
        // Otherwise we might end up blocked for a while for no reason.
        for (size_t i = 0; i < m_unprocessed_messages.size(); ++i) {
            auto& message = m_unprocessed_messages[i];
            if (message.endpoint_magic() != endpoint_magic)
                continue;
            if (message.message_id() == message_id)
                return m_unprocessed_messages.take(i);
        }

        if (!m_socket->is_open())
            break;

        wait_for_socket_to_become_readable();
        if (!drain_messages_from_peer(local_endpoint_magic))
            break;
    }
    return {};
}

}
