#include <Kernel/Socket.h>
#include <Kernel/LocalSocket.h>
#include <Kernel/UnixTypes.h>
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
    m_clients.append(client.copy_ref());
    return client;
}
