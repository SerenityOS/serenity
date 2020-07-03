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
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalSocket.h>
#include <LibCore/Object.h>
#include <LibCore/Timer.h>
#include <LibIPC/Endpoint.h>
#include <LibIPC/Message.h>
#include <errno.h>
#include <stdio.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

namespace IPC {

class Event : public Core::Event {
public:
    enum Type {
        Invalid = 2000,
        Disconnected,
    };
    Event() {}
    explicit Event(Type type)
        : Core::Event(type)
    {
    }
};

class DisconnectedEvent : public Event {
public:
    explicit DisconnectedEvent(int client_id)
        : Event(Disconnected)
        , m_client_id(client_id)
    {
    }

    int client_id() const { return m_client_id; }

private:
    int m_client_id { 0 };
};

template<typename T, class... Args>
NonnullRefPtr<T> new_client_connection(Args&&... args)
{
    return T::construct(forward<Args>(args)...) /* arghs */;
}

template<typename Endpoint>
class ClientConnection : public Core::Object {
public:
    ClientConnection(Endpoint& endpoint, Core::LocalSocket& socket, int client_id)
        : m_endpoint(endpoint)
        , m_socket(socket)
        , m_client_id(client_id)
    {
        ASSERT(socket.is_connected());
        ucred creds;
        socklen_t creds_size = sizeof(creds);
        if (getsockopt(m_socket->fd(), SOL_SOCKET, SO_PEERCRED, &creds, &creds_size) < 0) {
            ASSERT_NOT_REACHED();
        }
        m_client_pid = creds.pid;
        add_child(socket);
        m_socket->on_ready_to_read = [this] { drain_messages_from_client(); };

        m_responsiveness_timer = Core::Timer::create_single_shot(3000, [this] { may_have_become_unresponsive(); });
    }

    virtual ~ClientConnection() override
    {
    }

    virtual void may_have_become_unresponsive() {}
    virtual void did_become_responsive() {}

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
                    dbg() << *this << "::post_message: Client buffer overflowed.";
                    did_misbehave();
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

    void drain_messages_from_client()
    {
        if (!m_socket->is_open())
            return;

        Vector<u8> bytes;
        for (;;) {
            u8 buffer[4096];
            ssize_t nread = recv(m_socket->fd(), buffer, sizeof(buffer), MSG_DONTWAIT);
            if (nread == 0 || (nread == -1 && errno == EAGAIN)) {
                if (bytes.is_empty()) {
                    Core::EventLoop::current().post_event(*this, make<DisconnectedEvent>(client_id()));
                    return;
                }
                break;
            }
            if (nread < 0) {
                perror("recv");
                shutdown();
                return;
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
            auto message = Endpoint::decode_message(remaining_bytes, decoded_bytes);
            if (!message) {
                dbg() << "drain_messages_from_client: Endpoint didn't recognize message";
                did_misbehave();
                return;
            }
            if (auto response = m_endpoint.handle(*message))
                post_message(*response);
            ASSERT(decoded_bytes);
        }
    }

    void did_misbehave()
    {
        dbg() << *this << " (id=" << m_client_id << ", pid=" << m_client_pid << ") misbehaved, disconnecting.";
        shutdown();
    }

    void did_misbehave(const char* message)
    {
        dbg() << *this << " (id=" << m_client_id << ", pid=" << m_client_pid << ") misbehaved (" << message << "), disconnecting.";
        shutdown();
    }

    void shutdown()
    {
        m_socket->close();
        die();
    }

    int client_id() const { return m_client_id; }

    pid_t client_pid() const { return m_client_pid; }
    void set_client_pid(pid_t pid) { m_client_pid = pid; }

    virtual void die() = 0;

protected:
    void event(Core::Event& event) override
    {
        if (event.type() == Event::Disconnected) {
#ifdef IPC_DEBUG
            int client_id = static_cast<const DisconnectedEvent&>(event).client_id();
            dbg() << *this << ": Client disconnected: " << client_id;
#endif
            die();
            return;
        }

        Core::Object::event(event);
    }

private:
    Endpoint& m_endpoint;
    RefPtr<Core::LocalSocket> m_socket;
    RefPtr<Core::Timer> m_responsiveness_timer;
    int m_client_id { -1 };
    int m_client_pid { -1 };
};

}
