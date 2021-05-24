/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalSocket.h>
#include <LibCore/Notifier.h>
#include <LibCore/Timer.h>
#include <LibIPC/Message.h>
#include <errno.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

namespace IPC {

template<typename LocalEndpoint, typename PeerEndpoint>
class Connection : public Core::Object {
public:
    using LocalStub = typename LocalEndpoint::Stub;

    Connection(LocalStub& local_stub, NonnullRefPtr<Core::LocalSocket> socket)
        : m_local_stub(local_stub)
        , m_socket(move(socket))
        , m_notifier(Core::Notifier::construct(m_socket->fd(), Core::Notifier::Read, this))
    {
        m_responsiveness_timer = Core::Timer::create_single_shot(3000, [this] { may_have_become_unresponsive(); });
        m_notifier->on_ready_to_read = [this] {
            NonnullRefPtr protect = *this;
            drain_messages_from_peer();
            handle_messages();
        };
    }

    template<typename MessageType>
    OwnPtr<MessageType> wait_for_specific_message()
    {
        return wait_for_specific_endpoint_message<MessageType, LocalEndpoint>();
    }

    void post_message(const Message& message)
    {
        post_message(message.encode());
    }

    // FIXME: unnecessary copy
    void post_message(MessageBuffer buffer)
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
                    dbgln("{}::post_message: Disconnected from peer", *this);
                    shutdown();
                    return;
                case EAGAIN:
                    dbgln("{}::post_message: Peer buffer overflowed", *this);
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

    virtual void may_have_become_unresponsive() { }
    virtual void did_become_responsive() { }

    void shutdown()
    {
        m_notifier->close();
        m_socket->close();
        die();
    }

    virtual void die() { }

protected:
    Core::LocalSocket& socket() { return *m_socket; }

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
            fd_set rfds;
            FD_ZERO(&rfds);
            FD_SET(m_socket->fd(), &rfds);
            for (;;) {
                if (auto rc = select(m_socket->fd() + 1, &rfds, nullptr, nullptr, nullptr); rc < 0) {
                    if (errno == EINTR)
                        continue;
                    perror("wait_for_specific_endpoint_message: select");
                    VERIFY_NOT_REACHED();
                } else {
                    VERIFY(rc > 0);
                    VERIFY(FD_ISSET(m_socket->fd(), &rfds));
                    break;
                }
            }

            if (!drain_messages_from_peer())
                break;
        }
        return {};
    }

    bool drain_messages_from_peer()
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
                    deferred_invoke([this](auto&) { die(); });
                }
                return false;
            }
            bytes.append(buffer, nread);
        }

        if (!bytes.is_empty()) {
            m_responsiveness_timer->stop();
            did_become_responsive();
        }

        size_t index = 0;
        u32 message_size = 0;
        for (; index + sizeof(message_size) < bytes.size(); index += message_size) {
            memcpy(&message_size, bytes.data() + index, sizeof(message_size));
            if (message_size == 0 || bytes.size() - index - sizeof(uint32_t) < message_size)
                break;
            index += sizeof(message_size);
            auto remaining_bytes = ReadonlyBytes { bytes.data() + index, bytes.size() - index };
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
            auto remaining_bytes = ByteBuffer::copy(bytes.data() + index, bytes.size() - index);
            if (!m_unprocessed_bytes.is_empty()) {
                dbgln("{}::drain_messages_from_peer: Already have unprocessed bytes", *this);
                shutdown();
                return false;
            }
            m_unprocessed_bytes = remaining_bytes;
        }

        if (!m_unprocessed_messages.is_empty()) {
            deferred_invoke([this](auto&) {
                handle_messages();
            });
        }
        return true;
    }

    void handle_messages()
    {
        auto messages = move(m_unprocessed_messages);
        for (auto& message : messages) {
            if (message.endpoint_magic() == LocalEndpoint::static_magic())
                if (auto response = m_local_stub.handle(message))
                    post_message(*response);
        }
    }

protected:
    LocalStub& m_local_stub;
    NonnullRefPtr<Core::LocalSocket> m_socket;
    RefPtr<Core::Timer> m_responsiveness_timer;

    RefPtr<Core::Notifier> m_notifier;
    NonnullOwnPtrVector<Message> m_unprocessed_messages;
    ByteBuffer m_unprocessed_bytes;
};

}

template<>
template<typename LocalEndpoint, typename PeerEndpoint>
struct AK::Formatter<IPC::Connection<LocalEndpoint, PeerEndpoint>> : Formatter<Core::Object> {
};
