#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>
#include <string.h>

__BEGIN_DECLS

#define FD_SETSIZE 64
#define FD_ZERO(set) memset((set), 0, sizeof(fd_set));
#define FD_CLR(fd, set) ((set)->bits[(fd / 8)] &= ~(1 << (fd) % 8))
#define FD_SET(fd, set) ((set)->bits[(fd / 8)] |= (1 << (fd) % 8))
#define FD_ISSET(fd, set) ((set)->bits[(fd / 8)] & (1 << (fd) % 8))

struct __fd_set {
    unsigned char bits[FD_SETSIZE / 8];
};

typedef struct __fd_set fd_set;

int select(int nfds, fd_set* readfds, fd_set* writefds, fd_set* exceptfds, struct timeval* timeout);

__END_DECLS

