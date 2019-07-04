#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

#define POLLIN (1u << 0)
#define POLLPRI (1u << 2)
#define POLLOUT (1u << 3)
#define POLLERR (1u << 4)
#define POLLHUP (1u << 5)
#define POLLNVAL (1u << 6)

struct pollfd {
    int fd;
    short events;
    short revents;
};

int poll(struct pollfd* fds, int nfds, int timeout);

__END_DECLS
