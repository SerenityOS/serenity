#pragma once

#include <bits/stdint.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

typedef uint32_t in_addr_t;
in_addr_t inet_addr(const char*);

#define INADDR_ANY ((in_addr_t)0)
#define INADDR_NONE ((in_addr_t)-1)
#define INADDR_LOOPBACK 0x7f000001

#define IP_TTL 2

struct in_addr {
    uint32_t s_addr;
};

struct sockaddr_in {
    uint16_t sin_family;
    uint16_t sin_port;
    struct in_addr sin_addr;
    char sin_zero[8];
};

__END_DECLS
