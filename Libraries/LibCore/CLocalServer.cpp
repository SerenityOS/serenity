#include <LibCore/CLocalServer.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include <stdio.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <unistd.h>

CLocalServer::CLocalServer(CObject* parent)
    : CObject(parent)
{
}

CLocalServer::~CLocalServer()
{
}

bool CLocalServer::take_over_from_system_server()
{
    if (m_listening)
        return false;

    constexpr auto socket_takeover = "SOCKET_TAKEOVER";

    if (getenv(socket_takeover)) {
        dbg() << "Taking the socket over from SystemServer";

        // Sanity check: it has to be a socket.
        struct stat stat;
        int rc = fstat(3, &stat);
        if (rc == 0 && S_ISSOCK(stat.st_mode)) {
            // The SystemServer has passed us the socket as fd 3,
            // so use that instead of creating our own.
            m_fd = 3;
            // It had to be !CLOEXEC for obvious reasons, but we
            // don't need it to be !CLOEXEC anymore, so set the
            // CLOEXEC flag now.
            fcntl(m_fd, F_SETFD, FD_CLOEXEC);
            // We wouldn't want our children to think we're passing
            // them a socket either, so unset the env variable.
            unsetenv(socket_takeover);

            m_listening = true;
            setup_notifier();
            return true;
        } else {
            if (rc != 0)
                perror("fstat");
            dbg() << "It's not a socket, what the heck??";
        }
    }

    dbg() << "Failed to take the socket over from SystemServer";

    return false;
}

void CLocalServer::setup_notifier()
{
    m_notifier = CNotifier::construct(m_fd, CNotifier::Event::Read, this);
    m_notifier->on_ready_to_read = [this] {
        if (on_ready_to_accept)
            on_ready_to_accept();
    };
}

bool CLocalServer::listen(const String& address)
{
    if (m_listening)
        return false;

    int rc;

    m_fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    ASSERT(m_fd >= 0);

    auto socket_address = CSocketAddress::local(address);
    auto un = socket_address.to_sockaddr_un();
    rc = ::bind(m_fd, (const sockaddr*)&un, sizeof(un));
    if (rc < 0) {
        perror("bind");
        ASSERT_NOT_REACHED();
    }

    rc = ::listen(m_fd, 5);
    if (rc < 0) {
        perror("listen");
        ASSERT_NOT_REACHED();
    }

    m_listening = true;
    setup_notifier();
    return true;
}

RefPtr<CLocalSocket> CLocalServer::accept()
{
    ASSERT(m_listening);
    sockaddr_un un;
    socklen_t un_size = sizeof(un);
    int accepted_fd = ::accept(m_fd, (sockaddr*)&un, &un_size);
    if (accepted_fd < 0) {
        perror("accept");
        return nullptr;
    }

    return CLocalSocket::construct(accepted_fd);
}
