#include <LibCore/CTCPSocket.h>
#include <sys/socket.h>

CTCPSocket::CTCPSocket(CObject* parent)
    : CSocket(CSocket::Type::TCP, parent)
{
    int fd = socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK, 0);
    if (fd < 0) {
        set_error(fd);
    } else {
        set_fd(fd);
        set_mode(CIODevice::ReadWrite);
        set_error(0);
    }
}

CTCPSocket::~CTCPSocket()
{
}
