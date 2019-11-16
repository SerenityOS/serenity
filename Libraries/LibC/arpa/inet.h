#pragma once

#include <endian.h>
#include <sys/cdefs.h>
#include <sys/socket.h>

__BEGIN_DECLS

#define INET_ADDRSTRLEN 16

const char* inet_ntop(int af, const void* src, char* dst, socklen_t);
int inet_pton(int af, const char* src, void* dst);

inline uint16_t htons(uint16_t value)
{
#if __BYTE_ORDER == __LITTLE_ENDIAN
    return __builtin_bswap16(value);
#else
    return value;
#endif
}

inline uint16_t ntohs(uint16_t value)
{
    return htons(value);
}

inline uint32_t htonl(uint32_t value)
{
#if __BYTE_ORDER == __LITTLE_ENDIAN
    return __builtin_bswap32(value);
#else
    return value;
#endif
}

inline uint32_t ntohl(uint32_t value)
{
    return htonl(value);
}

__END_DECLS
