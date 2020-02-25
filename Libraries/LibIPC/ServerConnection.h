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
#include <LibCore/LocalSocket.h>
#include <LibCore/Notifier.h>
#include <LibCore/SyscallUtils.h>
#include <LibIPC/Message.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

namespace IPC {

template<typename LocalEndpoint, typename PeerEndpoint>
class ServerConnection : public Core::Object {
public:
    ServerConnection(LocalEndpoint& local_endpoint, const StringView& address)
        : m_local_endpoint(local_endpoint)
        , m_connection(Core::LocalSocket::construct(this))
        , m_notifier(Core::Notifier::construct(m_connection->fd(), Core::Notifier::Read, this))
    {
        // We want to rate-limit our clients
        m_connection->set_blocking(true);
        m_notifier->on_ready_to_read = [this] {
            drain_messages_from_server();
            handle_messages();
        };

        int retries = 100000;
        while (retries) {
            if (m_connection->connect(Core::SocketAddress::local(address))) {
                break;
            }

            dbgprintf("Client::Connection: connect failed: %d, %s\n", errno, strerror(errno));
            usleep(10000);
            --retries;
        }

        ucred creds;
        socklen_t creds_size = sizeof(creds);
        if (getsockopt(m_connection->fd(), SOL_SOCKET, SO_PEERCRED, &creds, &creds_size) < 0) {
            ASSERT_NOT_REACHED();
        }
        m_server_pid = creds.pid;

        ASSERT(m_connection->is_connected());
    }

    virtual void handshake() = 0;

    pid_t server_pid() const { return m_server_pid; }
    void set_my_client_id(int id) { m_my_client_id = id; }
    int my_client_id() const { return m_my_client_id; }

    template<typename MessageType>
    OwnPtr<MessageType> wait_for_specific_message()
    {
        // Double check we don't already have the event waiting for us.
        // Otherwise we might end up blocked for a while for no reason.
        for (size_t i = 0; i < m_unprocessed_messages.size(); ++i) {
            if (m_unprocessed_messages[i]->message_id() == MessageType::static_message_id()) {
                auto message = move(m_unprocessed_messages[i]);
                m_unprocessed_messages.remove(i);
                return message;
            }
        }
        for (;;) {
            fd_set rfds;
            FD_ZERO(&rfds);
            FD_SET(m_connection->fd(), &rfds);
            int rc = Core::safe_syscall(select, m_connection->fd() + 1, &rfds, nullptr, nullptr, nullptr);
            if (rc < 0) {
                perror("select");
            }
            ASSERT(rc > 0);
            ASSERT(FD_ISSET(m_connection->fd(), &rfds));
            if (!drain_messages_from_server())
                return nullptr;
            for (size_t i = 0; i < m_unprocessed_messages.size(); ++i) {
                if (m_unprocessed_messages[i]->message_id() == MessageType::static_message_id()) {
                    auto message = move(m_unprocessed_messages[i]);
                    m_unprocessed_messages.remove(i);
                    return message;
                }
            }
        }
    }

    bool post_message(const Message& message)
    {
        auto buffer = message.encode();
        int nwritten = write(m_connection->fd(), buffer.data(), (size_t)buffer.size());
        if (nwritten < 0) {
            perror("write");
            ASSERT_NOT_REACHED();
            return false;
        }
        ASSERT(static_cast<size_t>(nwritten) == buffer.size());
        return true;
    }

    template<typename RequestType, typename... Args>
    OwnPtr<typename RequestType::ResponseType> send_sync(Args&&... args)
    {
        bool success = post_message(RequestType(forward<Args>(args)...));
        ASSERT(success);
        auto response = wait_for_specific_message<typename RequestType::ResponseType>();
        ASSERT(response);
        return response;
    }

private:
    bool drain_messages_from_server()
    {
        Vector<u8> bytes;
        for (;;) {
            u8 buffer[4096];
            ssize_t nread = recv(m_connection->fd(), buffer, sizeof(buffer), MSG_DONTWAIT);
            if (nread < 0) {
                if (errno == EAGAIN)
                    break;
                perror("read");
                exit(1);
                return false;
            }
            if (nread == 0) {
                dbg() << "EOF on IPC fd";
                // FIXME: Dying is definitely not always appropriate!
                exit(1);
                return false;
            }
            bytes.append(buffer, nread);
        }

        size_t decoded_bytes = 0;
        for (size_t index = 0; index < (size_t)bytes.size(); index += decoded_bytes) {
            auto remaining_bytes = ByteBuffer::wrap(bytes.data() + index, bytes.size() - index);
            if (auto message = LocalEndpoint::decode_message(remaining_bytes, decoded_bytes)) {
                m_unprocessed_messages.append(move(message));
            } else if (auto message = PeerEndpoint::decode_message(remaining_bytes, decoded_bytes)) {
                m_unprocessed_messages.append(move(message));
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
            if (message->endpoint_magic() == LocalEndpoint::static_magic())
                m_local_endpoint.handle(*message);
        }
    }

    LocalEndpoint& m_local_endpoint;
    RefPtr<Core::LocalSocket> m_connection;
    RefPtr<Core::Notifier> m_notifier;
    Vector<OwnPtr<Message>> m_unprocessed_messages;
    int m_server_pid { -1 };
    int m_my_client_id { -1 };
};

}
