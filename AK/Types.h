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

typedef __UINT64_TYPE__ u64;
typedef __UINT32_TYPE__ u32;
typedef __UINT16_TYPE__ u16;
typedef __UINT8_TYPE__ u8;
typedef __INT64_TYPE__ i64;
typedef __INT32_TYPE__ i32;
typedef __INT16_TYPE__ i16;
typedef __INT8_TYPE__ i8;

#ifdef __serenity__

typedef __SIZE_TYPE__ size_t;
typedef MakeSigned<size_t>::Type ssize_t;

typedef __PTRDIFF_TYPE__ ptrdiff_t;

typedef __INTPTR_TYPE__ intptr_t;
typedef __UINTPTR_TYPE__ uintptr_t;

typedef u8 uint8_t;
typedef u16 uint16_t;
typedef u32 uint32_t;
typedef u64 uint64_t;

typedef i8 int8_t;
typedef i16 int16_t;
typedef i32 int32_t;
typedef i64 int64_t;

typedef int pid_t;

#else
#    include <stddef.h>
#    include <stdint.h>
#    include <sys/types.h>

#    ifdef __ptrdiff_t
typedef __PTRDIFF_TYPE__ __ptrdiff_t;
#    endif

#endif

typedef Conditional<sizeof(void*) == 8, u64, u32>::Type FlatPtr;

constexpr unsigned KiB = 1024;
constexpr unsigned MiB = KiB * KiB;
constexpr unsigned GiB = KiB * KiB * KiB;

namespace std {
typedef decltype(nullptr) nullptr_t;
}

static constexpr u32 explode_byte(u8 b)
{
    return b << 24 | b << 16 | b << 8 | b;
}

static_assert(explode_byte(0xff) == 0xffffffff);
static_assert(explode_byte(0x80) == 0x80808080);
static_assert(explode_byte(0x7f) == 0x7f7f7f7f);
static_assert(explode_byte(0) == 0);

inline constexpr size_t align_up_to(const size_t value, const size_t alignment)
{
    return (value + (alignment - 1)) & ~(alignment - 1);
}

enum class [[nodiscard]] TriState : u8 {
    False,
    True,
    Unknown
};
