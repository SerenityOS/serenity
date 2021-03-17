/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
using ssize_t = MakeSigned<size_t>::Type;

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

using FlatPtr = Conditional<sizeof(void*) == 8, u64, u32>::Type;

constexpr u64 KiB = 1024;
constexpr u64 MiB = KiB * KiB;
constexpr u64 GiB = KiB * KiB * KiB;
constexpr u64 TiB = KiB * KiB * KiB * KiB;
constexpr u64 PiB = KiB * KiB * KiB * KiB * KiB;
constexpr u64 EiB = KiB * KiB * KiB * KiB * KiB * KiB;

namespace std {
using nullptr_t = decltype(nullptr);
}

static constexpr u32 explode_byte(u8 b)
{
    return b << 24 | b << 16 | b << 8 | b;
}

static_assert(explode_byte(0xff) == 0xffffffff);
static_assert(explode_byte(0x80) == 0x80808080);
static_assert(explode_byte(0x7f) == 0x7f7f7f7f);
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
