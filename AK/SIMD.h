/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace AK::SIMD {

#if !defined(KERNEL) && ARCH(X86_64)
#    define SIMD_CAN_POSSIBLY_SUPPORT_AVX2 1
#else
#    define SIMD_CAN_POSSIBLY_SUPPORT_AVX2 0
#endif

// FIXME: Support at least x86-64 SSE4.2, AVX512; Aarch64 SIMD
enum class UnrollingMode {
    NONE = 0,
#if SIMD_CAN_POSSIBLY_SUPPORT_AVX2
    AVX2 = 1 << 0,
#else
    AVX2 = NONE,
#endif
};
using enum UnrollingMode;

inline u32 get_supported_unrolling_modes()
{
#ifdef KERNEL
    // It looks as if we do not want to use SIMD registers in the kernel code.
    return static_cast<u32>(NONE);
#elif ARCH(X86_64)
    static u32 s_cached = [] {
        if (__builtin_cpu_supports("avx2"))
            return static_cast<u32>(AVX2);
        return static_cast<u32>(NONE);
    }();
    return s_cached;
#else
    return static_cast<u32>(NONE);
#endif
}

inline size_t get_load_store_alignment(UnrollingMode mode) // in bytes
{
    switch (mode) {
    case NONE:
        return 1;
#if SIMD_CAN_POSSIBLY_SUPPORT_AVX2
    case AVX2:
        return 32;
#endif
    default:
        VERIFY_NOT_REACHED();
    }
}

template<typename T>
inline void align_up(T*& ptr, UnrollingMode mode)
{
    auto aligned = align_up_to(reinterpret_cast<FlatPtr>(ptr), get_load_store_alignment(mode));
    ptr = reinterpret_cast<T*>(aligned);
}

template<UnrollingMode... modes>
inline void use_last_supported_unrolling_mode_from(auto func, u32 mode_mask = get_supported_unrolling_modes())
{
    bool found = false;

    // Iterate over modes in reversed order
    [[maybe_unused]] int dummy;
    (dummy = ... = ([&] {
        u32 current_mask = static_cast<u32>(modes);
        if (!found && current_mask && (mode_mask & current_mask) == current_mask) {
            found = true;
            func.template operator()<modes>();
        }
    }(),
         0));

    if (!found) {
        func.template operator()<NONE>();
    }
}

using i8x2 = i8 __attribute__((vector_size(2)));
using i8x4 = i8 __attribute__((vector_size(4)));
using i8x8 = i8 __attribute__((vector_size(8)));
using i8x16 = i8 __attribute__((vector_size(16)));
using i8x32 = i8 __attribute__((vector_size(32)));

using i16x2 = i16 __attribute__((vector_size(4)));
using i16x4 = i16 __attribute__((vector_size(8)));
using i16x8 = i16 __attribute__((vector_size(16)));
using i16x16 = i16 __attribute__((vector_size(32)));

// the asm intrinsics demand chars as  the 8-bit type, and do not allow
// (un-)signed ones to be used
using c8x2 = char __attribute__((vector_size(2)));
using c8x4 = char __attribute__((vector_size(4)));
using c8x8 = char __attribute__((vector_size(8)));
using c8x16 = char __attribute__((vector_size(16)));
using c8x32 = char __attribute__((vector_size(32)));

using i32x2 = i32 __attribute__((vector_size(8)));
using i32x4 = i32 __attribute__((vector_size(16)));
using i32x8 = i32 __attribute__((vector_size(32)));

using i64x2 = i64 __attribute__((vector_size(16)));
using i64x4 = i64 __attribute__((vector_size(32)));

using u8x2 = u8 __attribute__((vector_size(2)));
using u8x4 = u8 __attribute__((vector_size(4)));
using u8x8 = u8 __attribute__((vector_size(8)));
using u8x16 = u8 __attribute__((vector_size(16)));
using u8x32 = u8 __attribute__((vector_size(32)));

using u16x2 = u16 __attribute__((vector_size(4)));
using u16x4 = u16 __attribute__((vector_size(8)));
using u16x8 = u16 __attribute__((vector_size(16)));
using u16x16 = u16 __attribute__((vector_size(32)));

using u32x2 = u32 __attribute__((vector_size(8)));
using u32x4 = u32 __attribute__((vector_size(16)));
using u32x8 = u32 __attribute__((vector_size(32)));

using u64x2 = u64 __attribute__((vector_size(16)));
using u64x4 = u64 __attribute__((vector_size(32)));

using f32x2 = float __attribute__((vector_size(8)));
using f32x4 = float __attribute__((vector_size(16)));
using f32x8 = float __attribute__((vector_size(32)));

using f64x2 = double __attribute__((vector_size(16)));
using f64x4 = double __attribute__((vector_size(32)));

}
