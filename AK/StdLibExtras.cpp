#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/kstdio.h>

extern "C" {

#ifndef KERNEL
void* mmx_memcpy(void* dest, const void* src, size_t len)
{
    ASSERT(len >= 1024);

    auto* dest_ptr = (u8*)dest;
    auto* src_ptr = (const u8*)src;

    if ((u32)dest_ptr & 7) {
        u32 prologue = 8 - ((u32)dest_ptr & 7);
        len -= prologue;
        asm volatile(
            "rep movsb\n"
            : "=S"(src_ptr), "=D"(dest_ptr), "=c"(prologue)
            : "0"(src_ptr), "1"(dest_ptr), "2"(prologue)
            : "memory");
    }
    for (u32 i = len / 64; i; --i) {
        asm volatile(
            "movq (%0), %%mm0\n"
            "movq 8(%0), %%mm1\n"
            "movq 16(%0), %%mm2\n"
            "movq 24(%0), %%mm3\n"
            "movq 32(%0), %%mm4\n"
            "movq 40(%0), %%mm5\n"
            "movq 48(%0), %%mm6\n"
            "movq 56(%0), %%mm7\n"
            "movq %%mm0, (%1)\n"
            "movq %%mm1, 8(%1)\n"
            "movq %%mm2, 16(%1)\n"
            "movq %%mm3, 24(%1)\n"
            "movq %%mm4, 32(%1)\n"
            "movq %%mm5, 40(%1)\n"
            "movq %%mm6, 48(%1)\n"
            "movq %%mm7, 56(%1)\n" ::"r"(src_ptr),
            "r"(dest_ptr)
            : "memory");
        src_ptr += 64;
        dest_ptr += 64;
    }
    asm volatile("emms" ::
                     : "memory");
    // Whatever remains we'll have to memcpy.
    len %= 64;
    if (len)
        memcpy(dest_ptr, src_ptr, len);
    return dest;
}
#endif

#ifdef KERNEL

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
#endif
}
