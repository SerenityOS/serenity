#pragma once

#include <LibCore/CEvent.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CIODevice.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CObject.h>

#include <errno.h>
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
    T* new_connection_for_client(Args&&... args)
    {
        auto conn = new T(AK::forward<Args>(args)...) /* arghs */;
        conn->send_greeting();
        return conn;
    };

    template<typename ServerMessage, typename ClientMessage>
    class Connection : public CObject {
        C_OBJECT(Connection)
    public:
        Connection(CLocalSocket& socket, int client_id)
            : m_socket(socket)
            , m_client_id(client_id)
        {
            add_child(socket);
            m_socket.on_ready_to_read = [this] { drain_client(); };
#if defined(CIPC_DEBUG)
            dbg() << "S: Created new Connection " << fd << client_id << " and said hello";
#endif
        }

        ~Connection()
        {
#if defined(CIPC_DEBUG)
            dbg() << "S: Destroyed Connection " << m_socket.fd() << client_id();
#endif
        }

        void post_message(const ServerMessage& message, const ByteBuffer& extra_data = {})
        {
#if defined(CIPC_DEBUG)
            dbg() << "S: -> C " << int(message.type) << " extra " << extra_data.size();
#endif
            if (!extra_data.is_empty())
                const_cast<ServerMessage&>(message).extra_size = extra_data.size();

            struct iovec iov[2];
            int iov_count = 1;

            iov[0].iov_base = const_cast<ServerMessage*>(&message);
            iov[0].iov_len = sizeof(message);

            if (!extra_data.is_empty()) {
                iov[1].iov_base = const_cast<u8*>(extra_data.data());
                iov[1].iov_len = extra_data.size();
                ++iov_count;
            }

            int nwritten = writev(m_socket.fd(), iov, iov_count);
            if (nwritten < 0) {
                switch (errno) {
                case EPIPE:
                    dbgprintf("Connection::post_message: Disconnected from peer.\n");
                    delete_later();
                    return;
                    break;
                case EAGAIN:
                    dbgprintf("Connection::post_message: Client buffer overflowed.\n");
                    did_misbehave();
                    return;
                    break;
                default:
                    perror("Connection::post_message writev");
                    ASSERT_NOT_REACHED();
                }
            }

            ASSERT(nwritten == (int)(sizeof(message) + extra_data.size()));
        }

        void drain_client()
        {
            unsigned messages_received = 0;
            for (;;) {
                ClientMessage message;
                // FIXME: Don't go one message at a time, that's so much context switching, oof.
                ssize_t nread = recv(m_socket.fd(), &message, sizeof(ClientMessage), MSG_DONTWAIT);
                if (nread == 0 || (nread == -1 && errno == EAGAIN)) {
                    if (!messages_received) {
                        // TODO: is delete_later() sufficient?
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
                    int extra_nread = read(m_socket.fd(), extra_data.data(), extra_data.size());
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
            m_socket.close();
            delete_later();
        }

        int client_id() const { return m_client_id; }
        pid_t client_pid() const { return m_client_pid; }
        void set_client_pid(pid_t pid) { m_client_pid = pid; }

        // ### having this public is sad
        virtual void send_greeting() = 0;

    protected:
        void event(CEvent& event)
        {
            if (event.type() == Event::Disconnected) {
                int client_id = static_cast<const DisconnectedEvent&>(event).client_id();
                dbgprintf("Connection: Client disconnected: %d\n", client_id);
                delete this;
                return;
            }

            CObject::event(event);
        }

        virtual bool handle_message(const ClientMessage&, const ByteBuffer&& = {}) = 0;

    private:
        CLocalSocket& m_socket;
        int m_client_id { -1 };
        int m_client_pid { -1 };
    };

} // Server
} // IPC
