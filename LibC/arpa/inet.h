#pragma once

#include <sys/cdefs.h>
#include <sys/socket.h>

__BEGIN_DECLS

#define INET_ADDRSTRLEN 16

const char* inet_ntop(int af, const void* src, char* dst, socklen_t);
int inet_pton(int af, const char* src, void* dst);

static inline uint16_t htons(uint16_t hs)
{
    uint8_t* s = (uint8_t*)&hs;
    return (uint16_t)(s[0] << 8 | s[1]);
}

static inline uint16_t ntohs(uint16_t ns)
{
    return htons(ns);
}

static inline uint32_t htonl(uint32_t hs)
{
    uint8_t* s = (uint8_t*)&hs;
    return (uint32_t)(s[0] << 24 | s[1] << 16 | s[2] << 8 | s[3]);
}

static inline uint32_t ntohl(uint32_t ns)
{
    return htonl(ns);
}

__END_DECLS
