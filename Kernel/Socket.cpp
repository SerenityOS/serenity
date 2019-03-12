#include <Kernel/Socket.h>
#include <Kernel/LocalSocket.h>
#include <Kernel/IPv4Socket.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/Process.h>
#include <LibC/errno_numbers.h>

KResultOr<Retained<Socket>> Socket::create(int domain, int type, int protocol)
{
    (void)protocol;
    switch (domain) {
    case AF_LOCAL:
        return LocalSocket::create(type & SOCK_TYPE_MASK);
    case AF_INET:
        return IPv4Socket::create(type & SOCK_TYPE_MASK, protocol);
    default:
        return KResult(-EAFNOSUPPORT);
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

KResult Socket::listen(int backlog)
{
    LOCKER(m_lock);
    if (m_type != SOCK_STREAM)
        return KResult(-EOPNOTSUPP);
    m_backlog = backlog;
    kprintf("Socket{%p} listening with backlog=%d\n", this, m_backlog);
    return KSuccess;
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

KResult Socket::queue_connection_from(Socket& peer)
{
    LOCKER(m_lock);
    if (m_pending.size() >= m_backlog)
        return KResult(-ECONNREFUSED);
    m_pending.append(peer);
    return KSuccess;
}
