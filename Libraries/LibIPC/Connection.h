/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalSocket.h>
#include <LibCore/Notifier.h>
#include <LibCore/SyscallUtils.h>
#include <LibCore/Timer.h>
#include <LibIPC/Message.h>
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
    Connection(LocalEndpoint& local_endpoint, NonnullRefPtr<Core::LocalSocket> socket)
        : m_local_endpoint(local_endpoint)
        , m_socket(move(socket))
        , m_notifier(Core::Notifier::construct(m_socket->fd(), Core::Notifier::Read, this))
    {
        m_responsiveness_timer = Core::Timer::create_single_shot(3000, [this] { may_have_become_unresponsive(); });
        m_notifier->on_ready_to_read = [this] {
            drain_messages_from_peer();
            handle_messages();
        };
    }

    pid_t peer_pid() const { return m_peer_pid; }

    template<typename MessageType>
    OwnPtr<MessageType> wait_for_specific_message()
    {
        return wait_for_specific_endpoint_message<MessageType, LocalEndpoint>();
    }

    void post_message(const Message& message)
    {
        // NOTE: If this connection is being shut down, but has not yet been destroyed,
        //       the socket will be closed. Don't try to send more messages.
        if (!m_socket->is_open())
            return;

        auto buffer = message.encode();

        auto bytes_remaining = buffer.size();
        while (bytes_remaining) {
            auto nwritten = write(m_socket->fd(), buffer.data(), buffer.size());
            if (nwritten < 0) {
                switch (errno) {
                case EPIPE:
                    dbg() << *this << "::post_message: Disconnected from peer";
                    shutdown();
                    return;
                case EAGAIN:
                    dbg() << *this << "::post_message: Peer buffer overflowed";
                    shutdown();
                    return;
                default:
                    perror("Connection::post_message write");
                    shutdown();
                    return;
                }
            }
            bytes_remaining -= nwritten;
        }

        m_responsiveness_timer->start();
    }

    template<typename RequestType, typename... Args>
    OwnPtr<typename RequestType::ResponseType> send_sync(Args&&... args)
    {
        post_message(RequestType(forward<Args>(args)...));
        auto response = wait_for_specific_endpoint_message<typename RequestType::ResponseType, PeerEndpoint>();
        ASSERT(response);
        return response;
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
    void set_peer_pid(pid_t pid) { m_peer_pid = pid; }

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
            int rc = Core::safe_syscall(select, m_socket->fd() + 1, &rfds, nullptr, nullptr, nullptr);
            if (rc < 0) {
                perror("select");
            }
            ASSERT(rc > 0);
            ASSERT(FD_ISSET(m_socket->fd(), &rfds));
            if (!drain_messages_from_peer())
                break;
        }
        return nullptr;
    }

    bool drain_messages_from_peer()
    {
        Vector<u8> bytes;
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

        size_t decoded_bytes = 0;
        for (size_t index = 0; index < bytes.size(); index += decoded_bytes) {
            auto remaining_bytes = ByteBuffer::wrap(bytes.data() + index, bytes.size() - index);
            if (auto message = LocalEndpoint::decode_message(remaining_bytes, decoded_bytes)) {
                m_unprocessed_messages.append(message.release_nonnull());
            } else if (auto message = PeerEndpoint::decode_message(remaining_bytes, decoded_bytes)) {
                m_unprocessed_messages.append(message.release_nonnull());
            } else {
                ASSERT_NOT_REACHED();
            }
            ASSERT(decoded_bytes);
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
                if (auto response = m_local_endpoint.handle(message))
                    post_message(*response);
        }
    }

protected:
    void initialize_peer_info()
    {
        ucred creds;
        socklen_t creds_size = sizeof(creds);
        if (getsockopt(this->socket().fd(), SOL_SOCKET, SO_PEERCRED, &creds, &creds_size) < 0) {
            // FIXME: We should handle this more gracefully.
            ASSERT_NOT_REACHED();
        }
        m_peer_pid = creds.pid;
    }

    LocalEndpoint& m_local_endpoint;
    NonnullRefPtr<Core::LocalSocket> m_socket;
    RefPtr<Core::Timer> m_responsiveness_timer;

    RefPtr<Core::Notifier> m_notifier;
    NonnullOwnPtrVector<Message> m_unprocessed_messages;
    pid_t m_peer_pid { -1 };
};

}
