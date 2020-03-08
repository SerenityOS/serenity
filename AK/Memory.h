#pragma once

#include <AK/Types.h>

#if defined(KERNEL) || defined(BOOTSTRAPPER)
#    include <LibBareMetal/StdLib.h>
#else
#    include <stdlib.h>
#    include <string.h>
#endif

#if defined(__serenity__) && !defined(KERNEL) && !defined(BOOTSTRAPPER)
extern "C" void* mmx_memcpy(void* to, const void* from, size_t);
#endif

[[gnu::always_inline]] inline void fast_u32_copy(u32* dest, const u32* src, size_t count)
{
#if defined(__serenity__) && !defined(KERNEL) && !defined(BOOTSTRAPPER)
    if (count >= 256) {
        mmx_memcpy(dest, src, count * sizeof(count));
        return;
    }
#endif
    asm volatile(
        "rep movsl\n"
        : "=S"(src), "=D"(dest), "=c"(count)
        : "S"(src), "D"(dest), "c"(count)
        : "memory");
}

[[gnu::always_inline]] inline void fast_u32_fill(u32* dest, u32 value, size_t count)
{
    asm volatile(
        "rep stosl\n"
        : "=D"(dest), "=c"(count)
        : "D"(dest), "c"(count), "a"(value)
        : "memory");
}
