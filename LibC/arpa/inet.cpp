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
    snprintf(dst, len, "%u.%u.%u.%u", bytes[0], bytes[1], bytes[2], bytes[3]);
    return (const char*)dst;
}

int inet_pton(int af, const char* src, void* dst)
{
    if (af != AF_INET) {
        errno = EAFNOSUPPORT;
        return -1;
    }
    unsigned a;
    unsigned b;
    unsigned c;
    unsigned d;
    int count = sscanf(src, "%u.%u.%u.%u", &a, &b, &c, &d);
    if (count != 4) {
        errno = EINVAL;
        return -1;
    }
    union {
        struct {
            uint8_t a;
            uint8_t b;
            uint8_t c;
            uint8_t d;
        };
        uint32_t l;
    } u;
    u.a = a;
    u.b = b;
    u.c = c;
    u.d = d;
    *(uint32_t*)dst = u.l;
    return 0;
}

}

