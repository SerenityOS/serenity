#include <LibCore/CLocalSocket.h>
#include <sys/socket.h>
#include <errno.h>

CLocalSocket::CLocalSocket(Badge<CLocalServer>, int fd, CObject* parent)
    : CSocket(CSocket::Type::Local, parent)
{
    set_fd(fd);
    set_mode(CIODevice::ReadWrite);
    set_error(0);
}

CLocalSocket::CLocalSocket(CObject* parent)
    : CSocket(CSocket::Type::Local, parent)
{
    int fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    if (fd < 0) {
        set_error(fd);
    } else {
        set_fd(fd);
        set_mode(CIODevice::ReadWrite);
        set_error(0);
    }
}

CLocalSocket::~CLocalSocket()
{
}
