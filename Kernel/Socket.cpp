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
    : m_lock("Socket")
    , m_domain(domain)
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

KResult Socket::setsockopt(int level, int option, const void* value, socklen_t value_size)
{
    ASSERT(level == SOL_SOCKET);
    switch (option) {
    case SO_SNDTIMEO:
        if (value_size != sizeof(timeval))
            return KResult(-EINVAL);
        m_send_timeout = *(const timeval*)value;
        return KSuccess;
    case SO_RCVTIMEO:
        if (value_size != sizeof(timeval))
            return KResult(-EINVAL);
        m_receive_timeout = *(const timeval*)value;
        return KSuccess;
    default:
        kprintf("%s(%u): setsockopt() at SOL_SOCKET with unimplemented option %d\n", option);
        return KResult(-ENOPROTOOPT);
    }
}

KResult Socket::getsockopt(int level, int option, void* value, socklen_t* value_size)
{
    ASSERT(level == SOL_SOCKET);
    switch (option) {
    case SO_SNDTIMEO:
        if (*value_size < sizeof(timeval))
            return KResult(-EINVAL);
        *(timeval*)value = m_send_timeout;
        *value_size = sizeof(timeval);
        return KSuccess;
    case SO_RCVTIMEO:
        if (*value_size < sizeof(timeval))
            return KResult(-EINVAL);
        *(timeval*)value = m_receive_timeout;
        *value_size = sizeof(timeval);
        return KSuccess;
    default:
        kprintf("%s(%u): getsockopt() at SOL_SOCKET with unimplemented option %d\n", option);
        return KResult(-ENOPROTOOPT);
    }
}

void Socket::load_receive_deadline()
{
    kgettimeofday(m_receive_deadline);
    m_receive_deadline.tv_sec += m_receive_timeout.tv_sec;
    m_receive_deadline.tv_usec += m_receive_timeout.tv_usec;
    m_receive_deadline.tv_sec += (m_send_timeout.tv_usec / 1000000) * 1;
    m_receive_deadline.tv_usec %= 1000000;
}

void Socket::load_send_deadline()
{
    kgettimeofday(m_send_deadline);
    m_send_deadline.tv_sec += m_send_timeout.tv_sec;
    m_send_deadline.tv_usec += m_send_timeout.tv_usec;
    m_send_deadline.tv_sec += (m_send_timeout.tv_usec / 1000000) * 1;
    m_send_deadline.tv_usec %= 1000000;
}
