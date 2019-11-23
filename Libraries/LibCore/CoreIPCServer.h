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
    NonnullRefPtr<T> new_connection_for_client(Args&&... args)
    {
        auto conn = T::construct(forward<Args>(args)...);
        conn->send_greeting();
        return conn;
    }

    template<typename T, class... Args>
    NonnullRefPtr<T> new_connection_ng_for_client(Args&&... args)
    {
        return T::construct(forward<Args>(args)...) /* arghs */;
    }

    template<typename ServerMessage, typename ClientMessage>
    class Connection : public CObject {
    protected:
        Connection(CLocalSocket& socket, int client_id)
            : m_socket(socket)
            , m_client_id(client_id)
        {
            add_child(socket);
            m_socket->on_ready_to_read = [this] {
                drain_client();
                flush_outgoing_messages();
            };
#if defined(CIPC_DEBUG)
            dbg() << "S: Created new Connection " << fd << client_id << " and said hello";
#endif
        }

    public:
        ~Connection()
        {
#if defined(CIPC_DEBUG)
            dbg() << "S: Destroyed Connection " << m_socket->fd() << client_id();
#endif
        }

        void post_message(const ServerMessage& message, const ByteBuffer& extra_data = {})
        {
#if defined(CIPC_DEBUG)
            dbg() << "S: -> C " << int(message.type) << " extra " << extra_data.size();
#endif
            flush_outgoing_messages();
            if (try_send_message(message, extra_data))
                return;
            if (m_queue.size() >= max_queued_messages) {
                dbg() << "Connection::post_message: Client has too many queued messages already, disconnecting it.";
                shutdown();
                return;
            }

            QueuedMessage queued_message { message, extra_data };
            if (!extra_data.is_empty())
                queued_message.message.extra_size = extra_data.size();
            m_queue.enqueue(move(queued_message));
        }

        bool try_send_message(const ServerMessage& message, const ByteBuffer& extra_data)
        {
            struct iovec iov[2];
            int iov_count = 1;

            iov[0].iov_base = const_cast<ServerMessage*>(&message);
            iov[0].iov_len = sizeof(message);

            if (!extra_data.is_empty()) {
                iov[1].iov_base = const_cast<u8*>(extra_data.data());
                iov[1].iov_len = extra_data.size();
                ++iov_count;
            }

            int nwritten = writev(m_socket->fd(), iov, iov_count);
            if (nwritten < 0) {
                switch (errno) {
                case EPIPE:
                    dbgprintf("Connection::post_message: Disconnected from peer.\n");
                    shutdown();
                    return false;
                case EAGAIN:
#ifdef CIPC_DEBUG
                    dbg() << "EAGAIN when trying to send WindowServer message, queue size: " << m_queue.size();
#endif
                    return false;
                default:
                    perror("Connection::post_message writev");
                    ASSERT_NOT_REACHED();
                }
            }

            ASSERT(nwritten == (int)(sizeof(message) + extra_data.size()));
            return true;
        }

        void flush_outgoing_messages()
        {
            while (!m_queue.is_empty()) {
                auto& queued_message = m_queue.head();
                if (!try_send_message(queued_message.message, queued_message.extra_data))
                    break;
                m_queue.dequeue();
            }
        }

        void drain_client()
        {
            unsigned messages_received = 0;
            for (;;) {
                ClientMessage message;
                // FIXME: Don't go one message at a time, that's so much context switching, oof.
                ssize_t nread = recv(m_socket->fd(), &message, sizeof(ClientMessage), MSG_DONTWAIT);
                if (nread == 0 || (nread == -1 && errno == EAGAIN)) {
                    if (!messages_received) {
                        CEventLoop::current().post_event(*this, make<DisconnectedEvent>(client_id()));
                    }
                    break;
                }
                if (nread < 0) {
                    perror("recv");
                    ASSERT_NOT_REACHED();
                }
                ByteBuffer extra_data;
                if (message.extra_size) {
                    if (message.extra_size >= 32768) {
                        dbgprintf("message.extra_size is way too large\n");
                        return did_misbehave();
                    }
                    extra_data = ByteBuffer::create_uninitialized(message.extra_size);
                    // FIXME: We should allow this to time out. Maybe use a socket timeout?
                    int extra_nread = read(m_socket->fd(), extra_data.data(), extra_data.size());
                    if (extra_nread != (int)message.extra_size) {
                        dbgprintf("extra_nread(%d) != extra_size(%d)\n", extra_nread, extra_data.size());
                        if (extra_nread < 0)
                            perror("read");
                        return did_misbehave();
                    }
                }
#if defined(CIPC_DEBUG)
                dbg() << "S: <- C " << int(message.type) << " extra " << extra_data.size();
#endif
                if (!handle_message(message, move(extra_data)))
                    return;
                ++messages_received;
            }
        }

        void did_misbehave()
        {
            dbgprintf("Connection{%p} (id=%d, pid=%d) misbehaved, disconnecting.\n", this, client_id(), m_client_pid);
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

        // ### having this public is sad
        virtual void send_greeting() = 0;

        virtual void die() = 0;

    protected:
        void event(CEvent& event)
        {
            if (event.type() == Event::Disconnected) {
                int client_id = static_cast<const DisconnectedEvent&>(event).client_id();
                dbgprintf("Connection: Client disconnected: %d\n", client_id);
                die();
                return;
            }

            CObject::event(event);
        }

        virtual bool handle_message(const ClientMessage&, const ByteBuffer&& = {}) = 0;

    private:
        RefPtr<CLocalSocket> m_socket;

        struct QueuedMessage {
            ServerMessage message;
            ByteBuffer extra_data;
        };

        static const int max_queued_messages = 200;
        Queue<QueuedMessage, 16> m_queue;

        int m_client_id { -1 };
        int m_client_pid { -1 };
    };

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
