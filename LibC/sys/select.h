#pragma once

#include <fd_set.h>
#include <string.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

int select(int nfds, fd_set* readfds, fd_set* writefds, fd_set* exceptfds, struct timeval* timeout);

__END_DECLS
