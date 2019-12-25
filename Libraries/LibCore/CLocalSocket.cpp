#include <LibCore/CLocalSocket.h>
#include <sys/socket.h>
#include <errno.h>

#ifndef SOCK_NONBLOCK
#include <sys/ioctl.h>
#endif

CLocalSocket::CLocalSocket(int fd, CObject* parent)
    : CSocket(CSocket::Type::Local, parent)
{
    // NOTE: This constructor is used by CLocalServer::accept(), so the socket is already connected.
    m_connected = true;
    set_fd(fd);
    set_mode(CIODevice::ReadWrite);
    set_error(0);
}

CLocalSocket::CLocalSocket(CObject* parent)
    : CSocket(CSocket::Type::Local, parent)
{
    
#ifdef SOCK_NONBLOCK
    int fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
#else
    int fd = socket(AF_LOCAL, SOCK_STREAM, 0);
    int option = 1;
    ioctl(fd, FIONBIO, &option);
    fcntl(fd, F_SETFD, FD_CLOEXEC);
#endif

    if (fd < 0) {
        set_error(errno);
    } else {
        set_fd(fd);
        set_mode(CIODevice::ReadWrite);
        set_error(0);
    }
}

CLocalSocket::~CLocalSocket()
{
}
