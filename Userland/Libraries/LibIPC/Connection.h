/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Result.h>
#include <AK/Try.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalSocket.h>
#include <LibCore/Notifier.h>
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

class ConnectionBase : public Core::Object {
    C_OBJECT_ABSTRACT(ConnectionBase);

public:
    virtual ~ConnectionBase() override;

    bool is_open() const { return m_socket->is_open(); }
    void post_message(Message const&);

    void shutdown();
    virtual void die() { }

protected:
    explicit ConnectionBase(IPC::Stub&, NonnullRefPtr<Core::LocalSocket>);

    Core::LocalSocket& socket() { return *m_socket; }

    virtual void may_have_become_unresponsive() { }
    virtual void did_become_responsive() { }

    void wait_for_socket_to_become_readable();
    Result<Vector<u8>, bool> read_as_much_as_possible_from_socket_without_blocking();
    void post_message(MessageBuffer);
    void handle_messages(u32 local_endpoint_magic);

    IPC::Stub& m_local_stub;

    NonnullRefPtr<Core::LocalSocket> m_socket;
    RefPtr<Core::Timer> m_responsiveness_timer;

    RefPtr<Core::Notifier> m_notifier;
    NonnullOwnPtrVector<Message> m_unprocessed_messages;
    ByteBuffer m_unprocessed_bytes;
};

template<typename LocalEndpoint, typename PeerEndpoint>
class Connection : public ConnectionBase {
public:
    Connection(IPC::Stub& local_stub, NonnullRefPtr<Core::LocalSocket> socket)
        : ConnectionBase(local_stub, move(socket))
    {
        m_notifier->on_ready_to_read = [this] {
            NonnullRefPtr protect = *this;
            drain_messages_from_peer();
            handle_messages(LocalEndpoint::static_magic());
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
        post_message(RequestType(forward<Args>(args)...));
        auto response = wait_for_specific_endpoint_message<typename RequestType::ResponseType, PeerEndpoint>();
        VERIFY(response);
        return response.release_nonnull();
    }

    template<typename RequestType, typename... Args>
    OwnPtr<typename RequestType::ResponseType> send_sync_but_allow_failure(Args&&... args)
    {
        post_message(RequestType(forward<Args>(args)...));
        return wait_for_specific_endpoint_message<typename RequestType::ResponseType, PeerEndpoint>();
    }

protected:
    template<typename MessageType, typename Endpoint>
    OwnPtr<MessageType> wait_for_specific_endpoint_message()
    {
        for (;;) {
            // Double check we don't already have the event waiting for us.
            // Otherwise we might end up blocked for a while for no reason.
            for (size_t i = 0; i < m_unprocessed_messages.size(); ++i) {
                auto& message = m_unprocessed_messages[i];
                if (message.endpoint_magic() != Endpoint::static_magic())
                    continue;
                if (message.message_id() == MessageType::static_message_id())
                    return m_unprocessed_messages.take(i).template release_nonnull<MessageType>();
            }

            if (!m_socket->is_open())
                break;

            wait_for_socket_to_become_readable();

            if (!drain_messages_from_peer())
                break;
        }
        return {};
    }

    bool drain_messages_from_peer()
    {
        auto bytes = TRY(read_as_much_as_possible_from_socket_without_blocking());

        size_t index = 0;
        u32 message_size = 0;
        for (; index + sizeof(message_size) < bytes.size(); index += message_size) {
            memcpy(&message_size, bytes.data() + index, sizeof(message_size));
            if (message_size == 0 || bytes.size() - index - sizeof(uint32_t) < message_size)
                break;
            index += sizeof(message_size);
            auto remaining_bytes = ReadonlyBytes { bytes.data() + index, message_size };
            if (auto message = LocalEndpoint::decode_message(remaining_bytes, m_socket->fd())) {
                m_unprocessed_messages.append(message.release_nonnull());
            } else if (auto message = PeerEndpoint::decode_message(remaining_bytes, m_socket->fd())) {
                m_unprocessed_messages.append(message.release_nonnull());
            } else {
                dbgln("Failed to parse a message");
                break;
            }
        }

        if (index < bytes.size()) {
            // Sometimes we might receive a partial message. That's okay, just stash away
            // the unprocessed bytes and we'll prepend them to the next incoming message
            // in the next run of this function.
            auto remaining_bytes_result = ByteBuffer::copy(bytes.span().slice(index));
            if (!remaining_bytes_result.has_value()) {
                dbgln("{}::drain_messages_from_peer: Failed to allocate buffer", *this);
                return false;
            }
            if (!m_unprocessed_bytes.is_empty()) {
                dbgln("{}::drain_messages_from_peer: Already have unprocessed bytes", *this);
                shutdown();
                return false;
            }
            m_unprocessed_bytes = remaining_bytes_result.release_value();
        }

        if (!m_unprocessed_messages.is_empty()) {
            deferred_invoke([this] {
                handle_messages(LocalEndpoint::static_magic());
            });
        }
        return true;
    }
};

}

template<typename LocalEndpoint, typename PeerEndpoint>
struct AK::Formatter<IPC::Connection<LocalEndpoint, PeerEndpoint>> : Formatter<Core::Object> {
};
