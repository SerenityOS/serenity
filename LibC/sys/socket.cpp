#include <sys/socket.h>
#include <errno.h>
#include <Kernel/Syscall.h>

extern "C" {

int socket(int domain, int type, int protocol)
{
    int rc = syscall(SC_socket, domain, type, protocol);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int bind(int sockfd, const sockaddr* addr, socklen_t addrlen)
{
    int rc = syscall(SC_bind, sockfd, addr, addrlen);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int listen(int sockfd, int backlog)
{
    int rc = syscall(SC_listen, sockfd, backlog);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int accept(int sockfd, sockaddr* addr, socklen_t* addrlen)
{
    int rc = syscall(SC_accept, sockfd, addr, addrlen);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int connect(int sockfd, const sockaddr* addr, socklen_t addrlen)
{
    int rc = syscall(SC_connect, sockfd, addr, addrlen);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t sendto(int sockfd, const void* data, size_t data_length, int flags, const struct sockaddr* addr, socklen_t addr_length)
{
    Syscall::SC_sendto_params params { sockfd, data, data_length, flags, addr, addr_length };
    int rc = syscall(SC_sendto, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t recvfrom(int sockfd, void* buffer, size_t buffer_length, int flags, const struct sockaddr* addr, socklen_t addr_length)
{
    Syscall::SC_recvfrom_params params { sockfd, buffer, buffer_length, flags, addr, addr_length };
    int rc = syscall(SC_recvfrom, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}
