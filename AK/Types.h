/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IterationDecision.h>
#include <AK/Platform.h>
#include <AK/StdLibExtras.h>

using u64 = __UINT64_TYPE__;
using u32 = __UINT32_TYPE__;
using u16 = __UINT16_TYPE__;
using u8 = __UINT8_TYPE__;
using i64 = __INT64_TYPE__;
using i32 = __INT32_TYPE__;
using i16 = __INT16_TYPE__;
using i8 = __INT8_TYPE__;

#ifdef __serenity__

using size_t = __SIZE_TYPE__;
using ssize_t = MakeSigned<size_t>;

using ptrdiff_t = __PTRDIFF_TYPE__;

using intptr_t = __INTPTR_TYPE__;
using uintptr_t = __UINTPTR_TYPE__;

using uint8_t = u8;
using uint16_t = u16;
using uint32_t = u32;
using uint64_t = u64;

using int8_t = i8;
using int16_t = i16;
using int32_t = i32;
using int64_t = i64;

using pid_t = int;

#else
#    include <stddef.h>
#    include <stdint.h>
#    include <sys/types.h>

#    ifdef __ptrdiff_t
using __ptrdiff_t = __PTRDIFF_TYPE__;
#    endif

#endif

using FlatPtr = Conditional<sizeof(void*) == 8, u64, u32>;

constexpr u64 KiB = 1024;
constexpr u64 MiB = KiB * KiB;
constexpr u64 GiB = KiB * KiB * KiB;
constexpr u64 TiB = KiB * KiB * KiB * KiB;
constexpr u64 PiB = KiB * KiB * KiB * KiB * KiB;
constexpr u64 EiB = KiB * KiB * KiB * KiB * KiB * KiB;

namespace std {
using nullptr_t = decltype(nullptr);
}

static constexpr FlatPtr explode_byte(u8 b)
{
#if ARCH(I386)
    return (u32)b << 24 | (u32)b << 16 | (u32)b << 8 | (u32)b;
#else
    return (u64)b << 56 | (u64)b << 48 | (u64)b << 40 | (u64)b << 32 | (u64)b << 24 | (u64)b << 16 | (u64)b << 8 | (u64)b;
#endif
}

#if ARCH(I386)
static_assert(explode_byte(0xff) == 0xffffffff);
static_assert(explode_byte(0x80) == 0x80808080);
static_assert(explode_byte(0x7f) == 0x7f7f7f7f);
#else
static_assert(explode_byte(0xff) == 0xffffffffffffffff);
static_assert(explode_byte(0x80) == 0x8080808080808080);
static_assert(explode_byte(0x7f) == 0x7f7f7f7f7f7f7f7f);
#endif
static_assert(explode_byte(0) == 0);

constexpr size_t align_up_to(const size_t value, const size_t alignment)
{
    return (value + (alignment - 1)) & ~(alignment - 1);
}

enum class [[nodiscard]] TriState : u8 {
    False,
    True,
    Unknown
};

namespace AK {

enum MemoryOrder {
    memory_order_relaxed = __ATOMIC_RELAXED,
    memory_order_consume = __ATOMIC_CONSUME,
    memory_order_acquire = __ATOMIC_ACQUIRE,
    memory_order_release = __ATOMIC_RELEASE,
    memory_order_acq_rel = __ATOMIC_ACQ_REL,
    memory_order_seq_cst = __ATOMIC_SEQ_CST
};

}
