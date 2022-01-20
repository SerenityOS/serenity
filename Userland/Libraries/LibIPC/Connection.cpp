/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibIPC/Connection.h>
#include <LibIPC/Stub.h>
#include <sys/select.h>

namespace IPC {

ConnectionBase::ConnectionBase(IPC::Stub& local_stub, NonnullOwnPtr<Core::Stream::LocalSocket> socket, u32 local_endpoint_magic)
    : m_local_stub(local_stub)
    , m_socket(move(socket))
    , m_local_endpoint_magic(local_endpoint_magic)
{
    m_responsiveness_timer = Core::Timer::create_single_shot(3000, [this] { may_have_become_unresponsive(); });
}

ConnectionBase::~ConnectionBase()
{
}

ErrorOr<void> ConnectionBase::post_message(Message const& message)
{
    return post_message(message.encode());
}

ErrorOr<void> ConnectionBase::post_message(MessageBuffer buffer)
{
    // NOTE: If this connection is being shut down, but has not yet been destroyed,
    //       the socket will be closed. Don't try to send more messages.
    if (!m_socket->is_open())
        return Error::from_string_literal("Trying to post_message during IPC shutdown"sv);

    // Prepend the message size.
    uint32_t message_size = buffer.data.size();
    TRY(buffer.data.try_prepend(reinterpret_cast<const u8*>(&message_size), sizeof(message_size)));

#ifdef __serenity__
    for (auto& fd : buffer.fds) {
        if (auto result = m_socket->send_fd(fd.value()); result.is_error()) {
            dbgln("{}", result.error());
            shutdown();
            return result;
        }
    }
#else
    if (!buffer.fds.is_empty())
        warnln("fd passing is not supported on this platform, sorry :(");
#endif

    ReadonlyBytes bytes_to_write { buffer.data.span() };
    while (!bytes_to_write.is_empty()) {
        auto maybe_nwritten = m_socket->write(bytes_to_write);
        if (maybe_nwritten.is_error()) {
            auto error = maybe_nwritten.release_error();
            if (error.is_errno()) {
                switch (error.code()) {
                case EPIPE:
                    shutdown();
                    return Error::from_string_literal("IPC::Connection::post_message: Disconnected from peer"sv);
                case EAGAIN:
                    shutdown();
                    return Error::from_string_literal("IPC::Connection::post_message: Peer buffer overflowed"sv);
                default:
                    shutdown();
                    return Error::from_syscall("IPC::Connection::post_message write"sv, -error.code());
                }
            } else {
                return error;
            }
        }

        bytes_to_write = bytes_to_write.slice(maybe_nwritten.value());
    }

    m_responsiveness_timer->start();
    return {};
}

void ConnectionBase::shutdown()
{
    m_socket->close();
    die();
}

void ConnectionBase::handle_messages()
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
}

void ConnectionBase::wait_for_socket_to_become_readable()
{
    auto maybe_did_become_readable = m_socket->can_read_without_blocking(-1);
    if (maybe_did_become_readable.is_error()) {
        dbgln("ConnectionBase::wait_for_socket_to_become_readable: {}", maybe_did_become_readable.error());
        warnln("ConnectionBase::wait_for_socket_to_become_readable: {}", maybe_did_become_readable.error());
        VERIFY_NOT_REACHED();
    }

    VERIFY(maybe_did_become_readable.value());
}

ErrorOr<Vector<u8>> ConnectionBase::read_as_much_as_possible_from_socket_without_blocking()
{
    Vector<u8> bytes;

    if (!m_unprocessed_bytes.is_empty()) {
        bytes.append(m_unprocessed_bytes.data(), m_unprocessed_bytes.size());
        m_unprocessed_bytes.clear();
    }

    u8 buffer[4096];
    while (m_socket->is_open()) {
        auto maybe_nread = m_socket->read_without_waiting({ buffer, 4096 });
        if (maybe_nread.is_error()) {
            auto error = maybe_nread.release_error();
            if (error.is_syscall() && error.code() == EAGAIN) {
                break;
            }

            dbgln("ConnectionBase::read_as_much_as_possible_from_socket_without_blocking: {}", error);
            warnln("ConnectionBase::read_as_much_as_possible_from_socket_without_blocking: {}", error);
            VERIFY_NOT_REACHED();
        }

        auto nread = maybe_nread.release_value();
        if (nread == 0) {
            if (bytes.is_empty()) {
                deferred_invoke([this] { shutdown(); });
                return Error::from_string_literal("IPC connection EOF"sv);
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

ErrorOr<void> ConnectionBase::drain_messages_from_peer()
{
    auto bytes = TRY(read_as_much_as_possible_from_socket_without_blocking());

    size_t index = 0;
    try_parse_messages(bytes, index);

    if (index < bytes.size()) {
        // Sometimes we might receive a partial message. That's okay, just stash away
        // the unprocessed bytes and we'll prepend them to the next incoming message
        // in the next run of this function.
        auto remaining_bytes = TRY(ByteBuffer::copy(bytes.span().slice(index)));
        if (!m_unprocessed_bytes.is_empty()) {
            shutdown();
            return Error::from_string_literal("drain_messages_from_peer: Already have unprocessed bytes"sv);
        }
        m_unprocessed_bytes = move(remaining_bytes);
    }

    if (!m_unprocessed_messages.is_empty()) {
        deferred_invoke([this] {
            handle_messages();
        });
    }
    return {};
}

OwnPtr<IPC::Message> ConnectionBase::wait_for_specific_endpoint_message_impl(u32 endpoint_magic, int message_id)
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

}
