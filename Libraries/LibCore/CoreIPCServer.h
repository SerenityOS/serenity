#pragma once

#include <AK/Queue.h>
#include <LibCore/CEvent.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CIODevice.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CObject.h>
#include <LibIPC/IEndpoint.h>
#include <LibIPC/IMessage.h>
#include <errno.h>
#include <sched.h>
#include <stdio.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

//#define CIPC_DEBUG

namespace IPC {
namespace Server {

    class Event : public CEvent {
    public:
        enum Type {
            Invalid = 2000,
            Disconnected,
        };
        Event() {}
        explicit Event(Type type)
            : CEvent(type)
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
    NonnullRefPtr<T> new_connection_ng_for_client(Args&&... args)
    {
        return T::construct(forward<Args>(args)...) /* arghs */;
    }

    template<typename Endpoint>
    class ConnectionNG : public CObject {
    public:
        ConnectionNG(Endpoint& endpoint, CLocalSocket& socket, int client_id)
            : m_endpoint(endpoint)
            , m_socket(socket)
            , m_client_id(client_id)
        {
            add_child(socket);
            m_socket->on_ready_to_read = [this] { drain_messages_from_client(); };
        }

        virtual ~ConnectionNG() override
        {
        }

        void post_message(const IMessage& message)
        {
            auto buffer = message.encode();

            int nwritten = write(m_socket->fd(), buffer.data(), (size_t)buffer.size());
            if (nwritten < 0) {
                switch (errno) {
                case EPIPE:
                    dbg() << "Connection::post_message: Disconnected from peer";
                    shutdown();
                    return;
                case EAGAIN:
                    dbg() << "Connection::post_message: Client buffer overflowed.";
                    did_misbehave();
                    return;
                default:
                    perror("Connection::post_message write");
                    ASSERT_NOT_REACHED();
                }
            }

            ASSERT(nwritten == buffer.size());
        }

        void drain_messages_from_client()
        {
            Vector<u8> bytes;
            for (;;) {
                u8 buffer[4096];
                ssize_t nread = recv(m_socket->fd(), buffer, sizeof(buffer), MSG_DONTWAIT);
                if (nread == 0 || (nread == -1 && errno == EAGAIN)) {
                    if (bytes.is_empty()) {
                        CEventLoop::current().post_event(*this, make<DisconnectedEvent>(client_id()));
                        return;
                    }
                    break;
                }
                if (nread < 0) {
                    perror("recv");
                    ASSERT_NOT_REACHED();
                }
                bytes.append(buffer, nread);
            }

            size_t decoded_bytes = 0;
            for (size_t index = 0; index < (size_t)bytes.size(); index += decoded_bytes) {
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
            dbg() << "Connection{" << this << "} (id=" << m_client_id << ", pid=" << m_client_pid << ") misbehaved, disconnecting.";
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
        void event(CEvent& event) override
        {
            if (event.type() == Event::Disconnected) {
                int client_id = static_cast<const DisconnectedEvent&>(event).client_id();
                dbgprintf("Connection: Client disconnected: %d\n", client_id);
                die();
                return;
            }

            CObject::event(event);
        }

    private:
        Endpoint& m_endpoint;
        RefPtr<CLocalSocket> m_socket;
        int m_client_id { -1 };
        int m_client_pid { -1 };
    };

} // Server
} // IPC
