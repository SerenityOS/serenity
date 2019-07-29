#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/kmalloc.h>

extern "C" {

void* memcpy(void* dest_ptr, const void* src_ptr, size_t n)
{
    size_t dest = (size_t)dest_ptr;
    size_t src = (size_t)src_ptr;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && !(src & 0x3) && n >= 12) {
        size_t size_ts = n / sizeof(size_t);
        asm volatile(
            "rep movsl\n"
            : "=S"(src), "=D"(dest)
            : "S"(src), "D"(dest), "c"(size_ts)
            : "memory");
        n -= size_ts * sizeof(size_t);
        if (n == 0)
            return dest_ptr;
    }
    asm volatile(
        "rep movsb\n" ::"S"(src), "D"(dest), "c"(n)
        : "memory");
    return dest_ptr;
}

void* memmove(void* dest, const void* src, size_t n)
{
    if (dest < src)
        return memcpy(dest, src, n);

    u8* pd = (u8*)dest;
    const u8* ps = (const u8*)src;
    for (pd += n, ps += n; n--;)
        *--pd = *--ps;
    return dest;
}

char* strcpy(char* dest, const char* src)
{
    auto* dest_ptr = dest;
    auto* src_ptr = src;
    while ((*dest_ptr++ = *src_ptr++) != '\0')
        ;
    return dest;
}

char* strncpy(char* dest, const char* src, size_t n)
{
    size_t i;
    for (i = 0; i < n && src[i] != '\0'; ++i)
        dest[i] = src[i];
    for (; i < n; ++i)
        dest[i] = '\0';
    return dest;
}

void* memset(void* dest_ptr, int c, size_t n)
{
    size_t dest = (size_t)dest_ptr;
    // FIXME: Support starting at an unaligned address.
    if (!(dest & 0x3) && n >= 12) {
        size_t size_ts = n / sizeof(size_t);
        size_t expanded_c = (u8)c;
        expanded_c |= expanded_c << 8;
        expanded_c |= expanded_c << 16;
        asm volatile(
            "rep stosl\n"
            : "=D"(dest)
            : "D"(dest), "c"(size_ts), "a"(expanded_c)
            : "memory");
        n -= size_ts * sizeof(size_t);
        if (n == 0)
            return dest_ptr;
    }
    asm volatile(
        "rep stosb\n"
        : "=D"(dest), "=c"(n)
        : "0"(dest), "1"(n), "a"(c)
        : "memory");
    return dest_ptr;
}

char* strrchr(const char* str, int ch)
{
    char* last = nullptr;
    char c;
    for (; (c = *str); ++str) {
        if (c == ch)
            last = const_cast<char*>(str);
    }
    return last;
}

size_t strlen(const char* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

int strcmp(const char* s1, const char* s2)
{
    for (; *s1 == *s2; ++s1, ++s2) {
        if (*s1 == 0)
            return 0;
    }
    return *(const u8*)s1 < *(const u8*)s2 ? -1 : 1;
}

char* strdup(const char* str)
{
    size_t len = strlen(str);
    char* new_str = (char*)kmalloc(len + 1);
    strcpy(new_str, str);
    return new_str;
}

int memcmp(const void* v1, const void* v2, size_t n)
{
    auto* s1 = (const u8*)v1;
    auto* s2 = (const u8*)v2;
    while (n-- > 0) {
        if (*s1++ != *s2++)
            return s1[-1] < s2[-1] ? -1 : 1;
    }
    return 0;
}

[[noreturn]] void __cxa_pure_virtual()
{
    ASSERT_NOT_REACHED();
}

static inline uint32_t divq(uint64_t n, uint32_t d)
{
    uint32_t n1 = n >> 32;
    uint32_t n0 = n;
    uint32_t q;
    uint32_t r;
    asm volatile("divl %4"
                 : "=d"(r), "=a"(q)
                 : "0"(n1), "1"(n0), "rm"(d));
    return q;
}

static uint64_t unsigned_divide64(uint64_t n, uint64_t d)
{
    if ((d >> 32) == 0) {
        uint64_t b = 1ULL << 32;
        uint32_t n1 = n >> 32;
        uint32_t n0 = n;
        uint32_t d0 = d;
        return divq(b * (n1 % d0) + n0, d0) + b * (n1 / d0);
    }
    if (n < d)
        return 0;
    uint32_t d1 = d >> 32u;
    int s = __builtin_clz(d1);
    uint64_t q = divq(n >> 1, (d << s) >> 32) >> (31 - s);
    return n - (q - 1) * d < d ? q - 1 : q;
}

static uint32_t unsigned_modulo64(uint64_t n, uint64_t d)
{
    return n - d * unsigned_divide64(n, d);
}

static int64_t signed_divide64(int64_t n, int64_t d)
{
    uint64_t n_abs = n >= 0 ? (uint64_t)n : -(uint64_t)n;
    uint64_t d_abs = d >= 0 ? (uint64_t)d : -(uint64_t)d;
    uint64_t q_abs = unsigned_divide64(n_abs, d_abs);
    return (n < 0) == (d < 0) ? (int64_t)q_abs : -(int64_t)q_abs;
}

static int32_t signed_modulo64(int64_t n, int64_t d)
{
    return n - d * signed_divide64(n, d);
}

int64_t __divdi3(int64_t n, int64_t d)
{
    return signed_divide64(n, d);
}

int64_t __moddi3(int64_t n, int64_t d)
{
    return signed_modulo64(n, d);
}

uint64_t __udivdi3(uint64_t n, uint64_t d)
{
    return unsigned_divide64(n, d);
}

uint64_t __umoddi3(uint64_t n, uint64_t d)
{
    return unsigned_modulo64(n, d);
}

uint64_t __udivmoddi4(uint64_t n, uint64_t d, uint64_t* r)
{
    uint64_t q = 0;
    uint64_t qbit = 1;

    if (!d)
        return 1 / ((unsigned)d);

    while ((int64_t)d >= 0) {
        d <<= 1;
        qbit <<= 1;
    }

    while (qbit) {
        if (d <= n) {
            n -= d;
            q += qbit;
        }
        d >>= 1;
        qbit >>= 1;
    }

    if (r)
        *r = n;

    return q;
}

}
