#include <Kernel/Socket.h>
#include <Kernel/LocalSocket.h>
#include <Kernel/UnixTypes.h>
#include <LibC/errno_numbers.h>

RetainPtr<Socket> Socket::create(int domain, int type, int protocol, int& error)
{
    (void)protocol;
    switch (domain) {
    case AF_LOCAL:
        return LocalSocket::create(type);
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


