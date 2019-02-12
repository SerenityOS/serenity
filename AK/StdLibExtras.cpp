#include <AK/StdLibExtras.h>
#include <AK/Assertions.h>
#include <AK/Types.h>
#include <AK/kstdio.h>

void* mmx_memcpy(void* dest, const void* src, size_t len)
{
    ASSERT(len >= 1024);

    auto* dest_ptr = (byte*)dest;
    auto* src_ptr = (const byte*)src;

    if ((dword)dest_ptr & 7) {
        dword prologue = 8 - ((dword)dest_ptr & 7);
        len -= prologue;
        asm volatile(
            "rep movsb\n"
            : "=S"(src_ptr), "=D"(dest_ptr), "=c"(prologue)
            : "0"(src_ptr), "1"(dest_ptr), "2"(prologue)
            : "memory"
        );
    }
    for (dword i = len / 64; i; --i) {
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
                    "movq %%mm7, 56(%1)\n"
                    :: "r" (src_ptr), "r" (dest_ptr) : "memory");
        src_ptr += 64;
        dest_ptr += 64;
    }
    asm volatile("emms":::"memory");
    // Whatever remains we'll have to memcpy.
    len %= 64;
    if (len)
        memcpy(dest_ptr, src_ptr, len);
    return dest;
}
