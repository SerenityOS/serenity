#pragma once

#include <sys/cdefs.h>
#include <sys/ioctl_numbers.h>

__BEGIN_DECLS

struct winsize {
    unsigned short ws_row;
    unsigned short ws_col;
};

int ioctl(int fd, unsigned request, ...);

__END_DECLS
