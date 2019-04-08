#include <LibGUI/GTCPSocket.h>
#include <sys/socket.h>

GTCPSocket::GTCPSocket(GObject* parent)
    : GSocket(GSocket::Type::TCP, parent)
{
    int fd = socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK, 0);
    if (fd < 0) {
        set_error(fd);
    } else {
        set_fd(fd);
        set_mode(GIODevice::ReadWrite);
        set_error(0);
    }
}

GTCPSocket::~GTCPSocket()
{
}
