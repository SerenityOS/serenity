#pragma once

#include <LibCore/CObject.h>
#include <LibCore/CEvent.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CIODevice.h>
#include <LibCore/CNotifier.h>

#include <errno.h>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <stdio.h>

//#define CIPC_DEBUG

class CIPCServerEvent : public CEvent {
public:
    enum Type {
        Invalid = 2000,
        ClientDisconnected,
    };
    CIPCServerEvent() {}
    explicit CIPCServerEvent(Type type)
        : CEvent(type)
    {
    }
};

class ASClientDisconnectedNotification : public CIPCServerEvent {
public:
    explicit ASClientDisconnectedNotification(int client_id)
        : CIPCServerEvent(ClientDisconnected)
        , m_client_id(client_id)
    {
    }

    int client_id() const { return m_client_id; }

private:
    int m_client_id { 0 };
};

template <typename T, class... Args>
T* CIPCServerSideClientCreator(Args&& ... args)
{
    auto conn = new T(AK::forward<Args>(args)...) /* arghs */;
    conn->send_greeting();
    return conn;
};

template <typename ServerMessage, typename ClientMessage>
class CIPCServerSideClient : public CObject
{
public:
    CIPCServerSideClient(int fd, int client_id)
        : m_socket(fd)
        , m_notifier(CNotifier(fd, CNotifier::Read))
        , m_client_id(client_id)
    {
        m_notifier.on_ready_to_read = [this] { drain_client(); };
#if defined(CIPC_DEBUG)
        dbg() << "S: Created new CIPCServerSideClient " << fd << client_id << " and said hello";
#endif
    }

    ~CIPCServerSideClient()
    {
#if defined(CIPC_DEBUG)
        dbg() << "S: Destroyed CIPCServerSideClient " << m_socket.fd() << client_id();
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
                dbgprintf("WSClientConnection::post_message: Disconnected from peer.\n");
                delete_later();
                return;
                break;
            case EAGAIN:
                dbgprintf("WSClientConnection::post_message: Client buffer overflowed.\n");
                did_misbehave();
                return;
                break;
            default:
                perror("WSClientConnection::post_message writev");
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
                    CEventLoop::current().post_event(*this, make<ASClientDisconnectedNotification>(client_id()));
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
        dbgprintf("CIPCServerSideClient{%p} (id=%d, pid=%d) misbehaved, disconnecting.\n", this, client_id(), m_pid);
        delete_later();
        m_notifier.set_enabled(false);
    }

    const char* class_name() const override { return "CIPCServerSideClient"; }

    int client_id() const { return m_client_id; }
    pid_t client_pid() const { return m_pid; }
    void set_client_pid(pid_t pid) { m_pid = pid; }

    // ### having this public is sad
    virtual void send_greeting() = 0;

protected:
    void event(CEvent& event)
    {
        if (event.type() == CIPCServerEvent::ClientDisconnected) {
            int client_id = static_cast<const ASClientDisconnectedNotification&>(event).client_id();
            dbgprintf("CIPCServerSideClient: Client disconnected: %d\n", client_id);
            delete this;
            return;
        }

        CObject::event(event);
    }

    virtual bool handle_message(const ClientMessage&, const ByteBuffer&& = {}) = 0;

private:
    // TODO: A way to create some kind of CIODevice with an open FD would be nice.
    class COpenedSocket : public CIODevice
    {
    public:
        const char* class_name() const override { return "COpenedSocket"; }
        COpenedSocket(int fd)
        {
            set_fd(fd);
            set_mode(CIODevice::OpenMode::ReadWrite);
        }

        bool open(CIODevice::OpenMode) override
        {
            ASSERT_NOT_REACHED();
            return true;
        };

        int fd() const { return CIODevice::fd(); }
    };

    COpenedSocket m_socket;
    CNotifier m_notifier;
    int m_client_id;
    int m_pid;
};


