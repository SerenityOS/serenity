#pragma once

#include <AK/Lock.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/HashTable.h>
#include <AK/Vector.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/KResult.h>

enum class SocketRole { None, Listener, Accepted, Connected };

class Socket : public Retainable<Socket> {
public:
    static KResultOr<Retained<Socket>> create(int domain, int type, int protocol);
    virtual ~Socket();

    int domain() const { return m_domain; }
    int type() const { return m_type; }
    int protocol() const { return m_protocol; }

    bool can_accept() const { return !m_pending.is_empty(); }
    RetainPtr<Socket> accept();
    bool is_connected() const { return m_connected; }
    KResult listen(int backlog);

    virtual KResult bind(const sockaddr*, socklen_t) = 0;
    virtual KResult connect(const sockaddr*, socklen_t) = 0;
    virtual bool get_address(sockaddr*, socklen_t*) = 0;
    virtual bool is_local() const { return false; }
    virtual bool is_ipv4() const { return false; }
    virtual void attach_fd(SocketRole) = 0;
    virtual void detach_fd(SocketRole) = 0;
    virtual bool can_read(SocketRole) const = 0;
    virtual ssize_t read(SocketRole, byte*, ssize_t) = 0;
    virtual ssize_t write(SocketRole, const byte*, ssize_t) = 0;
    virtual bool can_write(SocketRole) const = 0;
    virtual ssize_t sendto(const void*, size_t, int flags, const sockaddr*, socklen_t) = 0;
    virtual ssize_t recvfrom(void*, size_t, int flags, sockaddr*, socklen_t*) = 0;

    KResult setsockopt(int level, int option, const void*, socklen_t);
    KResult getsockopt(int level, int option, void*, socklen_t*);

    pid_t origin_pid() const { return m_origin_pid; }

    timeval receive_deadline() const { return m_receive_deadline; }
    timeval send_deadline() const { return m_send_deadline; }

    void set_connected(bool connected) { m_connected = connected; }

    Lock& lock() { return m_lock; }

protected:
    Socket(int domain, int type, int protocol);

    KResult queue_connection_from(Socket&);

    void load_receive_deadline();
    void load_send_deadline();

private:
    Lock m_lock;
    pid_t m_origin_pid { 0 };
    int m_domain { 0 };
    int m_type { 0 };
    int m_protocol { 0 };
    int m_backlog { 0 };
    bool m_connected { false };

    timeval m_receive_timeout { 0, 0 };
    timeval m_send_timeout { 0, 0 };

    timeval m_receive_deadline { 0, 0 };
    timeval m_send_deadline { 0, 0 };

    Vector<RetainPtr<Socket>> m_pending;
    Vector<RetainPtr<Socket>> m_clients;
};

class SocketHandle {
public:
    SocketHandle() { }

    SocketHandle(RetainPtr<Socket>&& socket)
        : m_socket(move(socket))
    {
        if (m_socket)
            m_socket->lock().lock();
    }

    SocketHandle(SocketHandle&& other)
        : m_socket(move(other.m_socket))
    {
    }

    ~SocketHandle()
    {
        if (m_socket)
            m_socket->lock().unlock();
    }

    SocketHandle(const SocketHandle&) = delete;
    SocketHandle& operator=(const SocketHandle&) = delete;

    operator bool() const { return m_socket; }

    Socket* operator->() { return &socket(); }
    const Socket* operator->() const { return &socket(); }

    Socket& socket() { return *m_socket; }
    const Socket& socket() const { return *m_socket; }

private:
    RetainPtr<Socket> m_socket;
};
