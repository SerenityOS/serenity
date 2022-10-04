/*
 * Copyright (c) 2022, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <cpuid.h>
#include <string.h>

extern "C" {

extern void* memset_sse2(void*, int, size_t);
extern void* memset_sse2_erms(void*, int, size_t);

constexpr u32 tcg_signature_ebx = 0x54474354;
constexpr u32 tcg_signature_ecx = 0x43544743;
constexpr u32 tcg_signature_edx = 0x47435447;

// Bit 9 of ebx in cpuid[eax = 7] indicates support for "Enhanced REP MOVSB/STOSB"
constexpr u32 cpuid_7_ebx_bit_erms = 1 << 9;

namespace {
[[gnu::used]] decltype(&memset) resolve_memset()
{
    u32 eax, ebx, ecx, edx;

    __cpuid(0x40000000, eax, ebx, ecx, edx);
    bool is_tcg = ebx == tcg_signature_ebx && ecx == tcg_signature_ecx && edx == tcg_signature_edx;

    // Although TCG reports ERMS support, testing shows that rep stosb performs strictly worse than
    // SSE copies on all data sizes except <= 4 bytes.
    if (is_tcg)
        return memset_sse2;

    __cpuid_count(7, 0, eax, ebx, ecx, edx);
    if (ebx & cpuid_7_ebx_bit_erms)
        return memset_sse2_erms;

    return memset_sse2;
}
}

#if !defined(AK_COMPILER_CLANG) && !defined(_DYNAMIC_LOADER)
[[gnu::ifunc("resolve_memset")]] void* memset(void*, int, size_t);
#else
// DynamicLoader can't self-relocate IFUNCs.
// FIXME: There's a circular dependency between LibC and libunwind when built with Clang,
// so the IFUNC resolver could be called before LibC has been relocated, returning bogus addresses.
void* memset(void* dest_ptr, int c, size_t n)
{
    static decltype(&memset) s_impl = nullptr;
    if (s_impl == nullptr)
        s_impl = resolve_memset();

    return s_impl(dest_ptr, c, n);
}
#endif
}
