/*
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#include "Utils.h"
#include "Syscalls.h"

extern "C" {
union overlay64 {
    u64 longw;
    struct {
        u32 lower;
        u32 higher;
    } words;
};

u64 __ashldi3(u64 num, unsigned int shift)
{
    union overlay64 output;

    output.longw = num;
    if (shift >= 32) {
        output.words.higher = output.words.lower << (shift - 32);
        output.words.lower = 0;
    } else {
        if (!shift)
            return num;
        output.words.higher = (output.words.higher << shift) | (output.words.lower >> (32 - shift));
        output.words.lower = output.words.lower << shift;
    }
    return output.longw;
}

u64 __lshrdi3(u64 num, unsigned int shift)
{
    union overlay64 output;

    output.longw = num;
    if (shift >= 32) {
        output.words.lower = output.words.higher >> (shift - 32);
        output.words.higher = 0;
    } else {
        if (!shift)
            return num;
        output.words.lower = output.words.lower >> shift | (output.words.higher << (32 - shift));
        output.words.higher = output.words.higher >> shift;
    }
    return output.longw;
}

#define MAX_32BIT_UINT ((((u64)1) << 32) - 1)

static u64 _64bit_divide(u64 dividend, u64 divider, u64* rem_p)
{
    u64 result = 0;

    /*
	 * If divider is zero - let the rest of the system care about the
	 * exception.
	 */
    if (!divider)
        return 1 / (u32)divider;

    /* As an optimization, let's not use 64 bit division unless we must. */
    if (dividend <= MAX_32BIT_UINT) {
        if (divider > MAX_32BIT_UINT) {
            result = 0;
            if (rem_p)
                *rem_p = divider;
        } else {
            result = (u32)dividend / (u32)divider;
            if (rem_p)
                *rem_p = (u32)dividend % (u32)divider;
        }
        return result;
    }

    while (divider <= dividend) {
        u64 locald = divider;
        u64 limit = __lshrdi3(dividend, 1);
        int shifts = 0;

        while (locald <= limit) {
            shifts++;
            locald = locald + locald;
        }
        result |= __ashldi3(1, shifts);
        dividend -= locald;
    }

    if (rem_p)
        *rem_p = dividend;

    return result;
}

u64 __udivdi3(u64 num, u64 den)
{
    return _64bit_divide(num, den, nullptr);
}

u64 __umoddi3(u64 num, u64 den)
{
    u64 v = 0;

    _64bit_divide(num, den, &v);
    return v;
}

uint64_t __udivmoddi4(uint64_t num, uint64_t den, uint64_t* rem_p)
{
    uint64_t quot = 0, qbit = 1;

    if (den == 0) {
        return 1 / ((unsigned)den); /* Intentional divide by zero, without
				 triggering a compiler warning which
				 would abort the build */
    }

    /* Left-justify denominator and count shift */
    while ((int64_t)den >= 0) {
        den <<= 1;
        qbit <<= 1;
    }

    while (qbit) {
        if (den <= num) {
            num -= den;
            quot += qbit;
        }
        den >>= 1;
        qbit >>= 1;
    }

    if (rem_p)
        *rem_p = num;

    return quot;
}

size_t strlen(const char* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
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
} // End of "extern C"

int dbgprintf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal([](char*&, char ch) { dbgputc(ch); }, nullptr, fmt, ap);
    va_end(ap);
    return ret;
}

static void buffer_putch(char*& bufptr, char ch)
{
    *bufptr++ = ch;
}

int vsprintf(char* buffer, const char* fmt, va_list ap)
{
    int ret = printf_internal(buffer_putch, buffer, fmt, ap);
    buffer[ret] = '\0';
    return ret;
}

int sprintf(char* buffer, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vsprintf(buffer, fmt, ap);
    va_end(ap);
    return ret;
}

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

[[noreturn]] void hang()
{
    dbgprintf("hang\n");
    while (1) {
        sleep(100);
    }
}

int strcmp(const char* s1, const char* s2)
{
    while (*s1 == *s2++)
        if (*s1++ == 0)
            return 0;
    return *(const unsigned char*)s1 - *(const unsigned char*)--s2;
}
