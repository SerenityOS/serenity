/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibIPC/Connection.h>
#include <LibIPC/Stub.h>
#include <sys/select.h>

namespace IPC {

struct CoreEventLoopDeferredInvoker final : public DeferredInvoker {
    virtual ~CoreEventLoopDeferredInvoker() = default;

    virtual void schedule(Function<void()> callback) override
    {
        Core::deferred_invoke(move(callback));
    }
};

ConnectionBase::ConnectionBase(IPC::Stub& local_stub, NonnullOwnPtr<Core::Stream::LocalSocket> socket, u32 local_endpoint_magic)
    : m_local_stub(local_stub)
    , m_socket(move(socket))
    , m_local_endpoint_magic(local_endpoint_magic)
    , m_deferred_invoker(make<CoreEventLoopDeferredInvoker>())
{
    m_responsiveness_timer = Core::Timer::create_single_shot(3000, [this] { may_have_become_unresponsive(); });
}

void ConnectionBase::set_deferred_invoker(NonnullOwnPtr<DeferredInvoker> deferred_invoker)
{
    m_deferred_invoker = move(deferred_invoker);
}

void ConnectionBase::set_fd_passing_socket(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
{
    m_fd_passing_socket = move(socket);
}

Core::Stream::LocalSocket& ConnectionBase::fd_passing_socket()
{
    if (m_fd_passing_socket)
        return *m_fd_passing_socket;
    return *m_socket;
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
        return Error::from_string_literal("Trying to post_message during IPC shutdown");

    // Prepend the message size.
    uint32_t message_size = buffer.data.size();
    TRY(buffer.data.try_prepend(reinterpret_cast<u8 const*>(&message_size), sizeof(message_size)));

    for (auto& fd : buffer.fds) {
        if (auto result = fd_passing_socket().send_fd(fd.value()); result.is_error()) {
            shutdown_with_error(result.error());
            return result;
        }
    }

    ReadonlyBytes bytes_to_write { buffer.data.span() };
    int writes_done = 0;
    size_t initial_size = bytes_to_write.size();
    while (!bytes_to_write.is_empty()) {
        auto maybe_nwritten = m_socket->write(bytes_to_write);
        writes_done++;
        if (maybe_nwritten.is_error()) {
            auto error = maybe_nwritten.release_error();
            if (error.is_errno()) {
                // FIXME: This is a hacky way to at least not crash on large messages
                // The limit of 100 writes is arbitrary, and there to prevent indefinite spinning on the EventLoop
                if (error.code() == EAGAIN && writes_done < 100) {
                    sched_yield();
                    continue;
                }
                shutdown_with_error(error);
                switch (error.code()) {
                case EPIPE:
                    return Error::from_string_literal("IPC::Connection::post_message: Disconnected from peer");
                case EAGAIN:
                    return Error::from_string_literal("IPC::Connection::post_message: Peer buffer overflowed");
                default:
                    return Error::from_syscall("IPC::Connection::post_message write"sv, -error.code());
                }
            } else {
                return error;
            }
        }

        bytes_to_write = bytes_to_write.slice(maybe_nwritten.value());
    }
    if (writes_done > 1) {
        dbgln("LibIPC::Connection FIXME Warning, needed {} writes needed to send message of size {}B, this is pretty bad, as it spins on the EventLoop", writes_done, initial_size);
    }

    m_responsiveness_timer->start();
    return {};
}

void ConnectionBase::shutdown()
{
    m_socket->close();
    die();
}

void ConnectionBase::shutdown_with_error(Error const& error)
{
    dbgln("IPC::ConnectionBase ({:p}) had an error ({}), disconnecting.", this, error);
    shutdown();
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
        auto maybe_bytes_read = m_socket->read_without_waiting({ buffer, 4096 });
        if (maybe_bytes_read.is_error()) {
            auto error = maybe_bytes_read.release_error();
            if (error.is_syscall() && error.code() == EAGAIN) {
                break;
            }

            dbgln("ConnectionBase::read_as_much_as_possible_from_socket_without_blocking: {}", error);
            warnln("ConnectionBase::read_as_much_as_possible_from_socket_without_blocking: {}", error);
            VERIFY_NOT_REACHED();
        }

        auto bytes_read = maybe_bytes_read.release_value();
        if (bytes_read.is_empty()) {
            m_deferred_invoker->schedule([strong_this = NonnullRefPtr(*this)]() mutable {
                strong_this->shutdown();
            });
            if (!bytes.is_empty())
                break;
            return Error::from_string_literal("IPC connection EOF");
        }

        bytes.append(bytes_read.data(), bytes_read.size());
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
            return Error::from_string_literal("drain_messages_from_peer: Already have unprocessed bytes");
        }
        m_unprocessed_bytes = move(remaining_bytes);
    }

    if (!m_unprocessed_messages.is_empty()) {
        m_deferred_invoker->schedule([strong_this = NonnullRefPtr(*this)]() mutable {
            strong_this->handle_messages();
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
