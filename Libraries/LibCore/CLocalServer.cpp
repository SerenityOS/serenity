#include <LibCore/CLocalServer.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include <stdio.h>
#include <sys/socket.h>

CLocalServer::CLocalServer(CObject* parent)
    : CObject(parent)
{
    m_fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    ASSERT(m_fd >= 0);
}

CLocalServer::~CLocalServer()
{
}

bool CLocalServer::listen(const String& address)
{
    if (m_listening)
        return false;

    int rc;

    auto socket_address = CSocketAddress::local(address);
    auto un = socket_address.to_sockaddr_un();
    rc = ::bind(m_fd, (const sockaddr*)&un, sizeof(un));
    ASSERT(rc == 0);

    rc = ::listen(m_fd, 5);
    ASSERT(rc == 0);
    m_listening = true;

    m_notifier = make<CNotifier>(m_fd, CNotifier::Event::Read);
    m_notifier->on_ready_to_read = [this] {
        if (on_ready_to_accept)
            on_ready_to_accept();
    };
    return true;
}

CLocalSocket* CLocalServer::accept()
{
    ASSERT(m_listening);
    sockaddr_un un;
    socklen_t un_size = sizeof(un);
    int accepted_fd = ::accept(m_fd, (sockaddr*)&un, &un_size);
    if (accepted_fd < 0) {
        perror("accept");
        return nullptr;
    }

    return new CLocalSocket({}, accepted_fd);
}
