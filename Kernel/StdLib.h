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

#include <AK/Checked.h>
#include <AK/Forward.h>
#include <AK/Time.h>
#include <AK/Userspace.h>
#include <Kernel/UnixTypes.h>

namespace Syscall {
struct StringArgument;
}

[[nodiscard]] String copy_string_from_user(const char*, size_t);
[[nodiscard]] String copy_string_from_user(Userspace<const char*>, size_t);
[[nodiscard]] Optional<Time> copy_time_from_user(const timespec*);
[[nodiscard]] Optional<Time> copy_time_from_user(const timeval*);
template<typename T>
[[nodiscard]] Optional<Time> copy_time_from_user(Userspace<T*> src);

[[nodiscard]] Optional<u32> user_atomic_fetch_add_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_exchange_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_load_relaxed(volatile u32* var);
[[nodiscard]] bool user_atomic_store_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<bool> user_atomic_compare_exchange_relaxed(volatile u32* var, u32& expected, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_and_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_and_not_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_or_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_xor_relaxed(volatile u32* var, u32 val);

extern "C" {

[[nodiscard]] bool copy_to_user(void*, const void*, size_t);
[[nodiscard]] bool copy_from_user(void*, const void*, size_t);
[[nodiscard]] bool memset_user(void*, int, size_t);

void* memcpy(void*, const void*, size_t);
[[nodiscard]] int strncmp(const char* s1, const char* s2, size_t n);
[[nodiscard]] char* strstr(const char* haystack, const char* needle);
[[nodiscard]] int strcmp(char const*, const char*);
[[nodiscard]] size_t strlen(const char*);
[[nodiscard]] size_t strnlen(const char*, size_t);
void* memset(void*, int, size_t);
[[nodiscard]] int memcmp(const void*, const void*, size_t);
void* memmove(void* dest, const void* src, size_t n);
const void* memmem(const void* haystack, size_t, const void* needle, size_t);

[[nodiscard]] inline u16 ntohs(u16 w) { return (w & 0xff) << 8 | ((w >> 8) & 0xff); }
[[nodiscard]] inline u16 htons(u16 w) { return (w & 0xff) << 8 | ((w >> 8) & 0xff); }
}

template<typename T>
[[nodiscard]] inline bool copy_from_user(T* dest, const T* src)
{
    static_assert(is_trivially_copyable<T>());
    return copy_from_user(dest, src, sizeof(T));
}

template<typename T>
[[nodiscard]] inline bool copy_to_user(T* dest, const T* src)
{
    static_assert(is_trivially_copyable<T>());
    return copy_to_user(dest, src, sizeof(T));
}

template<typename T>
[[nodiscard]] inline bool copy_from_user(T* dest, Userspace<const T*> src)
{
    static_assert(is_trivially_copyable<T>());
    return copy_from_user(dest, src.unsafe_userspace_ptr(), sizeof(T));
}

template<typename T>
[[nodiscard]] inline bool copy_from_user(T* dest, Userspace<T*> src)
{
    static_assert(is_trivially_copyable<T>());
    return copy_from_user(dest, src.unsafe_userspace_ptr(), sizeof(T));
}

#define DEPRECATE_COPY_FROM_USER_TYPE(T, REPLACEMENT)                                                                                \
    template<>                                                                                                                       \
    [[nodiscard]] inline __attribute__((deprecated("use " #REPLACEMENT " instead"))) bool copy_from_user<T>(T*, const T*)            \
    {                                                                                                                                \
        VERIFY_NOT_REACHED();                                                                                                        \
    }                                                                                                                                \
    template<>                                                                                                                       \
    [[nodiscard]] inline __attribute__((deprecated("use " #REPLACEMENT " instead"))) bool copy_from_user<T>(T*, Userspace<const T*>) \
    {                                                                                                                                \
        VERIFY_NOT_REACHED();                                                                                                        \
    }                                                                                                                                \
    template<>                                                                                                                       \
    [[nodiscard]] inline __attribute__((deprecated("use " #REPLACEMENT " instead"))) bool copy_from_user<T>(T*, Userspace<T*>)       \
    {                                                                                                                                \
        VERIFY_NOT_REACHED();                                                                                                        \
    }

DEPRECATE_COPY_FROM_USER_TYPE(timespec, copy_time_from_user)
DEPRECATE_COPY_FROM_USER_TYPE(timeval, copy_time_from_user)

template<typename T>
[[nodiscard]] inline bool copy_to_user(Userspace<T*> dest, const T* src)
{
    static_assert(is_trivially_copyable<T>());
    return copy_to_user(dest.unsafe_userspace_ptr(), src, sizeof(T));
}

template<typename T>
[[nodiscard]] inline bool copy_to_user(Userspace<T*> dest, const void* src, size_t size)
{
    static_assert(is_trivially_copyable<T>());
    return copy_to_user(dest.unsafe_userspace_ptr(), src, size);
}

template<typename T>
[[nodiscard]] inline bool copy_from_user(void* dest, Userspace<const T*> src, size_t size)
{
    static_assert(is_trivially_copyable<T>());
    return copy_from_user(dest, src.unsafe_userspace_ptr(), size);
}

template<typename T>
[[nodiscard]] inline bool copy_n_from_user(T* dest, const T* src, size_t count)
{
    static_assert(is_trivially_copyable<T>());
    Checked size = sizeof(T);
    size *= count;
    if (size.has_overflow())
        return false;
    return copy_from_user(dest, src, sizeof(T) * count);
}

template<typename T>
[[nodiscard]] inline bool copy_n_to_user(T* dest, const T* src, size_t count)
{
    static_assert(is_trivially_copyable<T>());
    Checked size = sizeof(T);
    size *= count;
    if (size.has_overflow())
        return false;
    return copy_to_user(dest, src, sizeof(T) * count);
}

template<typename T>
[[nodiscard]] inline bool copy_n_from_user(T* dest, Userspace<const T*> src, size_t count)
{
    static_assert(is_trivially_copyable<T>());
    Checked size = sizeof(T);
    size *= count;
    if (size.has_overflow())
        return false;
    return copy_from_user(dest, src.unsafe_userspace_ptr(), sizeof(T) * count);
}

template<typename T>
[[nodiscard]] inline bool copy_n_to_user(Userspace<T*> dest, const T* src, size_t count)
{
    static_assert(is_trivially_copyable<T>());
    Checked size = sizeof(T);
    size *= count;
    if (size.has_overflow())
        return false;
    return copy_to_user(dest.unsafe_userspace_ptr(), src, sizeof(T) * count);
}
