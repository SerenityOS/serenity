#pragma once

#include <sys/cdefs.h>
#include <sys/ioctl_numbers.h>

__BEGIN_DECLS

int ioctl(int fd, unsigned request, ...);

__END_DECLS
