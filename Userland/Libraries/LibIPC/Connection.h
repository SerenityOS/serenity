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
    ErrorOr<void> post_message(Message const&);

    void shutdown();
    virtual void die() { }

protected:
    explicit ConnectionBase(IPC::Stub&, NonnullRefPtr<Core::LocalSocket>, u32 local_endpoint_magic);

    Core::LocalSocket& socket() { return *m_socket; }

    virtual void may_have_become_unresponsive() { }
    virtual void did_become_responsive() { }
    virtual void try_parse_messages(Vector<u8> const& bytes, size_t& index) = 0;

    OwnPtr<IPC::Message> wait_for_specific_endpoint_message_impl(u32 endpoint_magic, int message_id);
    void wait_for_socket_to_become_readable();
    ErrorOr<Vector<u8>> read_as_much_as_possible_from_socket_without_blocking();
    ErrorOr<void> drain_messages_from_peer();

    ErrorOr<void> post_message(MessageBuffer);
    void handle_messages();

    IPC::Stub& m_local_stub;

    NonnullRefPtr<Core::LocalSocket> m_socket;
    RefPtr<Core::Timer> m_responsiveness_timer;

    RefPtr<Core::Notifier> m_notifier;
    NonnullOwnPtrVector<Message> m_unprocessed_messages;
    ByteBuffer m_unprocessed_bytes;

    u32 m_local_endpoint_magic { 0 };
};

template<typename LocalEndpoint, typename PeerEndpoint>
class Connection : public ConnectionBase {
public:
    Connection(IPC::Stub& local_stub, NonnullRefPtr<Core::LocalSocket> socket)
        : ConnectionBase(local_stub, move(socket), LocalEndpoint::static_magic())
    {
        m_notifier->on_ready_to_read = [this] {
            NonnullRefPtr protect = *this;
            // FIXME: Do something about errors.
            (void)drain_messages_from_peer();
            handle_messages();
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
        MUST(post_message(RequestType(forward<Args>(args)...)));
        auto response = wait_for_specific_endpoint_message<typename RequestType::ResponseType, PeerEndpoint>();
        VERIFY(response);
        return response.release_nonnull();
    }

    template<typename RequestType, typename... Args>
    OwnPtr<typename RequestType::ResponseType> send_sync_but_allow_failure(Args&&... args)
    {
        if (post_message(RequestType(forward<Args>(args)...)).is_error())
            return nullptr;
        return wait_for_specific_endpoint_message<typename RequestType::ResponseType, PeerEndpoint>();
    }

protected:
    template<typename MessageType, typename Endpoint>
    OwnPtr<MessageType> wait_for_specific_endpoint_message()
    {
        if (auto message = wait_for_specific_endpoint_message_impl(Endpoint::static_magic(), MessageType::static_message_id()))
            return message.template release_nonnull<MessageType>();
        return {};
    }

    virtual void try_parse_messages(Vector<u8> const& bytes, size_t& index) override
    {
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
    }
};

}

template<typename LocalEndpoint, typename PeerEndpoint>
struct AK::Formatter<IPC::Connection<LocalEndpoint, PeerEndpoint>> : Formatter<Core::Object> {
};
