#include <Kernel/Socket.h>
#include <Kernel/LocalSocket.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/Process.h>
#include <LibC/errno_numbers.h>

RetainPtr<Socket> Socket::create(int domain, int type, int protocol, int& error)
{
    (void)protocol;
    switch (domain) {
    case AF_LOCAL:
        return LocalSocket::create(type & SOCK_TYPE_MASK);
    default:
        error = EAFNOSUPPORT;
        return nullptr;
    }
}

Socket::Socket(int domain, int type, int protocol)
    : m_domain(domain)
    , m_type(type)
    , m_protocol(protocol)
{
    m_origin_pid = current->pid();
}

Socket::~Socket()
{
}

bool Socket::listen(int backlog, int& error)
{
    LOCKER(m_lock);
    if (m_type != SOCK_STREAM) {
        error = -EOPNOTSUPP;
        return false;
    }
    m_backlog = backlog;
    m_listening = true;
    kprintf("Socket{%p} listening with backlog=%d\n", m_backlog);
    return true;
}

RetainPtr<Socket> Socket::accept()
{
    LOCKER(m_lock);
    if (m_pending.is_empty())
        return nullptr;
    auto client = m_pending.take_first();
    ASSERT(!client->is_connected());
    client->m_connected = true;
    m_clients.append(client.copy_ref());
    return client;
}

bool Socket::queue_connection_from(Socket& peer, int& error)
{
    LOCKER(m_lock);
    if (m_pending.size() >= m_backlog) {
        error = -ECONNREFUSED;
        return false;
    }
    m_pending.append(peer);
    return true;
}
