#include <arpa/inet.h>
#include <stdio.h>
#include <errno.h>

extern "C" {

const char* inet_ntop(int af, const void* src, char* dst, socklen_t len)
{
    if (af != AF_INET) {
        errno = EAFNOSUPPORT;
        return nullptr;
    }
    auto* bytes = (const unsigned char*)src;
    snprintf(dst, len, "%u.%u.%u.%u", bytes[3], bytes[2], bytes[1], bytes[0]);
    return (const char*)dst;
}

}

