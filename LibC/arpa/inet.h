#pragma once

#include <sys/cdefs.h>
#include <sys/socket.h>

__BEGIN_DECLS

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

__END_DECLS

