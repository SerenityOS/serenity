/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibIPC/Connection.h>
#include <LibIPC/Stub.h>
#include <sys/select.h>

namespace IPC {

template<typename SocketType>
ConnectionBase<SocketType>::ConnectionBase(IPC::Stub& local_stub, NonnullRefPtr<SocketType> socket, u32 local_endpoint_magic)
    : m_local_stub(local_stub)
    , m_socket(move(socket))
    , m_notifier(Core::Notifier::construct(m_socket->fd(), Core::Notifier::Read, this))
    , m_local_endpoint_magic(local_endpoint_magic)
{
    m_responsiveness_timer = Core::Timer::create_single_shot(3000, [this] { may_have_become_unresponsive(); });
}

template<typename SocketType>
ConnectionBase<SocketType>::~ConnectionBase()
{
}

template<typename SocketType>
ErrorOr<void> ConnectionBase<SocketType>::post_message(Message const& message)
{
    return post_message(message.encode());
}

template<typename SocketType>
ErrorOr<void> ConnectionBase<SocketType>::post_message(MessageBuffer buffer)
{
    // NOTE: If this connection is being shut down, but has not yet been destroyed,
    //       the socket will be closed. Don't try to send more messages.
    if (!m_socket->is_open())
        return Error::from_string_literal("Trying to post_message during IPC shutdown"sv);

    // Prepend the message size.
    uint32_t message_size = buffer.data.size();
    TRY(buffer.data.try_prepend(reinterpret_cast<const u8*>(&message_size), sizeof(message_size)));

    for (auto& fd : buffer.fds) {
        if (auto result = SocketConnectionSendFd<SocketType>::send_fd(m_socket->fd(), fd.value()); result.is_error()) {
            shutdown();
            return result;
        }
    }

    size_t total_nwritten = 0;
    while (total_nwritten < buffer.data.size()) {
        if (m_buffer_outgoing) {
            auto available = m_send_buffer.capacity() - m_send_buffer.size();
            if (available == 0) {
                if (auto result = flush_send_buffer(); result.is_error())
                    return result;
                continue;
            }
            auto need = buffer.data.size() - total_nwritten;
            auto write_bytes = min(need, available);
            m_send_buffer.append(buffer.data.data() + total_nwritten, write_bytes);
            total_nwritten += write_bytes;
            if (need >= available) {
                if (auto result = flush_send_buffer(); result.is_error())
                    return result;
                continue;
            } else if (!buffer.fds.is_empty()) {
                if (auto result = flush_send_buffer(); result.is_error())
                    return result;
            }
            break;
        } else if (!m_send_buffer.is_empty()) {
            if (auto result = flush_send_buffer(); result.is_error())
                return result;
        }

        auto nwritten = write(m_socket->fd(), buffer.data.data() + total_nwritten, buffer.data.size() - total_nwritten);
        if (nwritten < 0) {
            switch (errno) {
            case EPIPE:
                shutdown();
                return Error::from_string_literal("IPC::Connection::post_message: Disconnected from peer"sv);
            case EAGAIN:
                shutdown();
                return Error::from_string_literal("IPC::Connection::post_message: Peer buffer overflowed"sv);
            default:
                shutdown();
                return Error::from_syscall("IPC::Connection::post_message write"sv, -errno);
            }
        }
        m_bytes_sent += (size_t)nwritten;
        total_nwritten += nwritten;
    }

    m_responsiveness_timer->start();
    return {};
}

template<typename SocketType>
void ConnectionBase<SocketType>::shutdown()
{
    m_notifier->close();
    m_socket->close();
    if (on_disconnect)
        on_disconnect();
    die();
}

template<typename SocketType>
void ConnectionBase<SocketType>::handle_messages()
{
    auto messages = move(m_unprocessed_messages);
    for (auto& message : messages) {
        if (message.endpoint_magic() == m_local_endpoint_magic) {
            if (auto response = m_local_stub.handle(message)) {
                if (auto result = post_message(*response); result.is_error()) {
                    dbgln("IPC::ConnectionBase::handle_messages: {}", result.error());
                }
            }
        }
    }
    notify_if_idle();
}

template<typename SocketType>
void ConnectionBase<SocketType>::wait_for_socket_to_become_readable()
{
    fd_set read_fds;
    FD_ZERO(&read_fds);
    FD_SET(m_socket->fd(), &read_fds);
    for (;;) {
        if (!m_send_buffer.is_empty()) {
            if (auto result = flush_send_buffer(); result.is_error())
                break;
        }

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

template<typename SocketType>
ErrorOr<void> ConnectionBase<SocketType>::drain_messages_from_peer()
{
    while (m_socket->is_open()) {
        u8 buffer[4096];
        ssize_t nread = recv(m_socket->fd(), buffer, sizeof(buffer), MSG_DONTWAIT);
        if (nread < 0) {
            if (errno == EAGAIN)
                break;
            perror("recv");
            exit(1);
        }
        if (nread == 0) {
            if (!has_partial_pending_message()) {
                deferred_invoke([this] { shutdown(); });
                return Error::from_string_literal("IPC connection EOF"sv);
            }
            break;
        }
        m_bytes_received += (size_t)nread;
        try_parse_messages(ReadonlyBytes { buffer, (size_t)nread });
    }

    if (has_partial_pending_message()) {
        m_responsiveness_timer->stop();
        did_become_responsive();
    } else {
        notify_if_idle();
    }
    return {};
}

template<typename SocketType>
OwnPtr<IPC::Message> ConnectionBase<SocketType>::wait_for_specific_endpoint_message_impl(u32 endpoint_magic, int message_id)
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
        if (drain_messages_from_peer().is_error())
            break;
    }
    return {};
}

template<typename SocketType>
void ConnectionBase<SocketType>::handle_raw_message(NonnullOwnPtr<IPC::Message>&& message, ReadonlyBytes const& bytes, bool is_peer)
{
    if (on_handle_raw_message && !on_handle_raw_message(is_peer, bytes))
        return;
    bool was_empty = m_unprocessed_messages.is_empty();
    m_unprocessed_messages.append(move(message));
    if (was_empty) {
        deferred_invoke([this]() {
            handle_messages();
        });
    }
}

template<typename SocketType>
void ConnectionBase<SocketType>::notify_if_idle()
{
    if (!m_unprocessed_messages.is_empty() || !m_unprocessed_bytes.is_empty())
        return;
    if (on_idle)
        on_idle();
}

template<typename SocketType>
ErrorOr<void> ConnectionBase<SocketType>::flush_send_buffer()
{
    if (m_send_buffer.is_empty())
        return {};

    bool was_blocking = socket().set_blocking(true);
    size_t total_nwritten = 0;
    while (total_nwritten < m_send_buffer.size()) {
        auto nwritten = write(m_socket->fd(), m_send_buffer.data() + total_nwritten, m_send_buffer.size() - total_nwritten);
        if (nwritten < 0) {
            switch (errno) {
            case EPIPE:
                dbgln("Connection {:p} flush_send_buffer: Disconnected from peer", this);
                shutdown();
                return Error::from_string_literal("Disconnected from peer"sv);
            case EAGAIN:
                dbgln("Connection {:p} flush_send_buffer: Peer buffer overflowed", this);
                shutdown();
                return Error::from_string_literal("Peer buffer overflowed"sv);
            default:
                perror("Connection::flush_send_buffer write");
                shutdown();
                return Error::from_string_literal("Write failed"sv);
            }
        }
        m_bytes_sent += (size_t)nwritten;
        total_nwritten += (size_t)nwritten;
    }
    if (!was_blocking)
        socket().set_blocking(false);

    if (m_buffer_outgoing)
        m_send_buffer.clear_with_capacity();
    else
        m_send_buffer.clear();
    return {};
}

template<typename SocketType>
void ConnectionBase<SocketType>::enable_send_buffer(size_t size)
{
    VERIFY(size > 0);
    m_buffer_outgoing = true;
    m_send_buffer.ensure_capacity(size);
}

template<typename SocketType>
void ConnectionBase<SocketType>::disable_send_buffer()
{
    m_buffer_outgoing = false;
    if (auto result = flush_send_buffer(); result.is_error())
        dbgln("Disabling send buffer failed: {}", result.error());
}

template<typename SocketType>
void ConnectionBase<SocketType>::deferred_flush_send_buffer()
{
    if (m_deferred_send_flush_pending || m_send_buffer.is_empty())
        return;

    m_deferred_send_flush_pending = true;
    deferred_invoke([this]() {
        m_deferred_send_flush_pending = false;
        if (auto result = flush_send_buffer(); result.is_error())
            dbgln("Flushing send buffer failed: {}", result.error());
    });
}

template class ConnectionBase<Core::LocalSocket>;
template class ConnectionBase<Core::TCPSocket>;

}
