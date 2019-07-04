#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

#define UNIX_PATH_MAX 108
struct sockaddr_un {
    uint16_t sun_family;
    char sun_path[UNIX_PATH_MAX];
};

__END_DECLS
