/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Try.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalSocket.h>
#include <LibCore/Notifier.h>
#include <LibCore/System.h>
#include <LibCore/TCPSocket.h>
#include <LibCore/Timer.h>
#include <LibIPC/Forward.h>
#include <LibIPC/Message.h>
#include <errno.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

namespace IPC {

template<typename Handler, typename LocalEndpoint, typename PeerEndpoint>
class RawConnection {
public:
    RawConnection(Handler& handler)
        : m_handler(handler)
    {
    }

    bool received_bytes(ReadonlyBytes const& bytes, int socket_fd)
    {
        size_t last_parsed_index;
        if (!m_unprocessed_bytes.is_empty()) {
            m_unprocessed_bytes.append(bytes);

            last_parsed_index = try_parse_messages(m_unprocessed_bytes, socket_fd);
            if (last_parsed_index < m_unprocessed_bytes.size()) {
                // Sometimes we might receive a partial message. That's okay, just stash away
                // the unprocessed bytes and we'll prepend them to the next incoming message
                // in the next run of this function.
                auto remaining_bytes_result = ByteBuffer::copy(m_unprocessed_bytes.span().slice(last_parsed_index));
                if (!remaining_bytes_result.has_value()) {
                    dbgln("RawConnection::drain_messages_from_peer: Failed to allocate buffer");
                    return false;
                }
                m_unprocessed_bytes = remaining_bytes_result.release_value();
            } else {
                m_unprocessed_bytes.clear();
            }
        } else {
            last_parsed_index = try_parse_messages(bytes, socket_fd);
            if (last_parsed_index < bytes.size()) {
                // Sometimes we might receive a partial message. That's okay, just stash away
                // the unprocessed bytes and we'll prepend them to the next incoming message
                // in the next run of this function.
                auto remaining_bytes_result = ByteBuffer::copy(bytes.slice(last_parsed_index));
                if (!remaining_bytes_result.has_value()) {
                    dbgln("RawConnection::drain_messages_from_peer: Failed to allocate buffer");
                    return false;
                }
                m_unprocessed_bytes = remaining_bytes_result.release_value();
            }
        }
        return true;
    }

    bool is_empty() const { return m_unprocessed_bytes.is_empty(); }

private:
    size_t try_parse_messages(ReadonlyBytes const& bytes, int socket_fd)
    {
        size_t index = 0;
        u32 message_size = 0;
        for (; index + sizeof(message_size) < bytes.size(); index += message_size) {
            memcpy(&message_size, bytes.data() + index, sizeof(message_size));
            if (message_size == 0 || bytes.size() - index - sizeof(uint32_t) < message_size)
                break;
            index += sizeof(message_size);
            auto remaining_bytes = ReadonlyBytes { bytes.data() + index, message_size };
            if (auto message = LocalEndpoint::decode_message(remaining_bytes, socket_fd)) {
                m_handler.handle_raw_message(message.release_nonnull(), remaining_bytes, false);
            } else if (auto message = PeerEndpoint::decode_message(remaining_bytes, socket_fd)) {
                m_handler.handle_raw_message(message.release_nonnull(), remaining_bytes, true);
            } else {
                dbgln("RawConnection: Failed to parse a message, size: {}", message_size);
                break;
            }
        }
        return index;
    }

    Handler& m_handler;
    ByteBuffer m_unprocessed_bytes;
};

template<typename SocketType = Core::LocalSocket>
class ConnectionBase : public Core::Object {
    C_OBJECT_ABSTRACT(ConnectionBase);

public:
    virtual ~ConnectionBase() override;

    bool is_open() const { return m_socket->is_open(); }

    u64 bytes_sent() const { return m_bytes_sent; }
    u64 bytes_received() const { return m_bytes_received; }

    Function<void()> on_disconnect;
    Function<void()> on_idle;
    Function<bool(bool, ReadonlyBytes const&)> on_handle_raw_message;

    SocketType& socket() { return *m_socket; }
    SocketType const& socket() const { return *m_socket; }
    bool is_connected() const { return m_socket->is_connected(); }

    ErrorOr<void> post_message(Message const&);
    ErrorOr<void> post_message(MessageBuffer);

    void shutdown();
    virtual void die() { }

protected:
    explicit ConnectionBase(IPC::Stub&, NonnullRefPtr<SocketType>, u32 local_endpoint_magic);

    virtual void may_have_become_unresponsive() { }
    virtual void did_become_responsive() { }
    virtual bool has_partial_pending_message() const = 0;
    virtual void try_parse_messages(ReadonlyBytes const& bytes) = 0;
    virtual void handle_raw_message(NonnullOwnPtr<IPC::Message>&&, ReadonlyBytes const&, bool);

    OwnPtr<IPC::Message> wait_for_specific_endpoint_message_impl(u32 endpoint_magic, int message_id);
    void wait_for_socket_to_become_readable();
    ErrorOr<void> drain_messages_from_peer();

    void handle_messages();

    void notify_if_idle();
    ErrorOr<void> flush_send_buffer();
    void enable_send_buffer(size_t);
    void disable_send_buffer();

    IPC::Stub& m_local_stub;

    NonnullRefPtr<SocketType> m_socket;
    RefPtr<Core::Timer> m_responsiveness_timer;

    RefPtr<Core::Notifier> m_notifier;
    NonnullOwnPtrVector<Message> m_unprocessed_messages;
    ByteBuffer m_unprocessed_bytes;

    u32 m_local_endpoint_magic { 0 };

    u64 m_bytes_received { 0 };
    u64 m_bytes_sent { 0 };

    Vector<u8> m_send_buffer;
    bool m_buffer_outgoing { false };
};

template<typename SocketType>
struct SocketConnectionSendFd {
};

template<>
struct SocketConnectionSendFd<Core::TCPSocket> {
    static constexpr bool can_send_or_receive_fds = false;
    static ErrorOr<void> send_fd(int, int)
    {
        VERIFY_NOT_REACHED(); // Can't send FDs over TCP!
    }
};

template<>
struct SocketConnectionSendFd<Core::LocalSocket> {
    static constexpr bool can_send_or_receive_fds = true;
    static ErrorOr<void> send_fd([[maybe_unused]] int socket_fd, [[maybe_unused]] int fd)
    {
#ifdef __serenity__
        if (auto result = Core::System::sendfd(socket_fd, fd); result.is_error()) {
            dbgln("{}", result.error());
            return result;
        }
#else
        warnln("fd passing is not supported on this platform, sorry :(");
#endif
        return {};
    }
};

template<typename LocalEndpoint, typename PeerEndpoint, typename SocketType = Core::LocalSocket>
class Connection : public ConnectionBase<SocketType> {
public:
    using RawConnectionType = typename IPC::RawConnection<Connection, LocalEndpoint, PeerEndpoint>;
    template<typename RawHandler, typename RawLocalEndpoint, typename RawPeerEndpoint>
    friend class IPC::RawConnection;

    Connection(IPC::Stub& local_stub, NonnullRefPtr<SocketType> socket)
        : ConnectionBase<SocketType>(local_stub, move(socket), LocalEndpoint::static_magic())
        , m_raw_connection(*this)
    {
        this->m_notifier->on_ready_to_read = [this] {
            NonnullRefPtr protect = *this;
            // FIXME: Do something about errors.
            (void)this->drain_messages_from_peer();
            this->handle_messages();
        };
    }

    template<typename MessageType>
    OwnPtr<MessageType> wait_for_specific_message()
    {
        return wait_for_specific_endpoint_message<MessageType, LocalEndpoint>();
    }

    template<typename RequestType, typename... Args>
    NonnullOwnPtr<typename RequestType::ResponseType> send_sync(Args&&... args)
    {
        MUST(this->post_message(RequestType(forward<Args>(args)...)));
        auto response = this->wait_for_specific_endpoint_message<typename RequestType::ResponseType, PeerEndpoint>();
        VERIFY(response);
        return response.release_nonnull();
    }

    template<typename RequestType, typename... Args>
    OwnPtr<typename RequestType::ResponseType> send_sync_but_allow_failure(Args&&... args)
    {
        if (this->post_message(RequestType(forward<Args>(args)...)).is_error())
            return nullptr;
        return this->wait_for_specific_endpoint_message<typename RequestType::ResponseType, PeerEndpoint>();
    }

protected:
    template<typename MessageType, typename Endpoint>
    OwnPtr<MessageType> wait_for_specific_endpoint_message()
    {
        if (auto message = this->wait_for_specific_endpoint_message_impl(Endpoint::static_magic(), MessageType::static_message_id()))
            return message.template release_nonnull<MessageType>();
        return {};
    }

    virtual bool has_partial_pending_message() const override
    {
        return !m_raw_connection.is_empty();
    }

    virtual void try_parse_messages(ReadonlyBytes const& bytes) override
    {
        m_raw_connection.received_bytes(bytes, SocketConnectionSendFd<SocketType>::can_send_or_receive_fds ? this->m_socket->fd() : -1);
    }

private:
    RawConnectionType m_raw_connection;
};

}

template<typename LocalEndpoint, typename PeerEndpoint>
struct AK::Formatter<IPC::Connection<LocalEndpoint, PeerEndpoint>> : Formatter<Core::Object> {
};
