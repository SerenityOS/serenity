#include <AK/IPv4Address.h>
#include <AK/Types.h>
#include <LibCore/CTCPServer.h>
#include <LibCore/CTCPSocket.h>
#include <LibCore/CNotifier.h>
#include <stdio.h>
#include <sys/socket.h>

CTCPServer::CTCPServer(CObject* parent)
    : CObject(parent)
{
    m_fd = socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    ASSERT(m_fd >= 0);
}

CTCPServer::~CTCPServer()
{
}

bool CTCPServer::listen(const IPv4Address& address, u16 port)
{
    if (m_listening)
        return false;

    int rc;
    auto socket_address = CSocketAddress(address, port);
    auto in = socket_address.to_sockaddr_in();
    rc = ::bind(m_fd, (const sockaddr*)&in, sizeof(in));
    ASSERT(rc == 0);

    rc = ::listen(m_fd, 5);
    ASSERT(rc == 0);
    m_listening = true;

    m_notifier = CNotifier::construct(m_fd, CNotifier::Event::Read);
    m_notifier->on_ready_to_read = [this] {
        if (on_ready_to_accept)
            on_ready_to_accept();
    };
    return true;
}

RefPtr<CTCPSocket> CTCPServer::accept()
{
    ASSERT(m_listening);
    sockaddr_in in;
    socklen_t in_size = sizeof(in);
    int accepted_fd = ::accept(m_fd, (sockaddr*)&in, &in_size);
    if (accepted_fd < 0) {
        perror("accept");
        return nullptr;
    }

    return CTCPSocket::construct(accepted_fd);
}

IPv4Address CTCPServer::local_address()
{
    if (m_fd == -1)
        return {};

    sockaddr_in address;
    socklen_t len = sizeof(address);
    if (getsockname(m_fd, (sockaddr*)&address, &len) != 0)
        return 0;

    return IPv4Address(address.sin_addr.s_addr);
}

u16 CTCPServer::local_port()
{
    if (m_fd == -1)
        return 0;

    sockaddr_in address;
    socklen_t len = sizeof(address);
    if (getsockname(m_fd, (sockaddr*)&address, &len) != 0)
        return 0;

    return ntohs(address.sin_port);
}
