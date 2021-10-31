/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Checked.h>
#include <AK/Error.h>
#include <AK/Forward.h>
#include <AK/Time.h>
#include <AK/Userspace.h>
#include <Kernel/KString.h>
#include <Kernel/UnixTypes.h>

ErrorOr<NonnullOwnPtr<Kernel::KString>> try_copy_kstring_from_user(Userspace<const char*>, size_t);
ErrorOr<Time> copy_time_from_user(timespec const*);
ErrorOr<Time> copy_time_from_user(timeval const*);
template<typename T>
ErrorOr<Time> copy_time_from_user(Userspace<T*>);

[[nodiscard]] Optional<u32> user_atomic_fetch_add_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_exchange_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_load_relaxed(volatile u32* var);
[[nodiscard]] bool user_atomic_store_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<bool> user_atomic_compare_exchange_relaxed(volatile u32* var, u32& expected, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_and_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_and_not_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_or_relaxed(volatile u32* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_xor_relaxed(volatile u32* var, u32 val);

ErrorOr<void> copy_to_user(void*, const void*, size_t);
ErrorOr<void> copy_from_user(void*, const void*, size_t);
ErrorOr<void> memset_user(void*, int, size_t);

extern "C" {

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
[[nodiscard]] inline ErrorOr<void> copy_from_user(T* dest, const T* src)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_from_user(dest, src, sizeof(T));
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_to_user(T* dest, const T* src)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_to_user(dest, src, sizeof(T));
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_from_user(T* dest, Userspace<const T*> src)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_from_user(dest, src.unsafe_userspace_ptr(), sizeof(T));
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_from_user(T* dest, Userspace<T*> src)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_from_user(dest, src.unsafe_userspace_ptr(), sizeof(T));
}

#define DEPRECATE_COPY_FROM_USER_TYPE(T, REPLACEMENT)                                                                                         \
    template<>                                                                                                                                \
    [[nodiscard]] inline __attribute__((deprecated("use " #REPLACEMENT " instead"))) ErrorOr<void> copy_from_user<T>(T*, const T*)            \
    {                                                                                                                                         \
        VERIFY_NOT_REACHED();                                                                                                                 \
    }                                                                                                                                         \
    template<>                                                                                                                                \
    [[nodiscard]] inline __attribute__((deprecated("use " #REPLACEMENT " instead"))) ErrorOr<void> copy_from_user<T>(T*, Userspace<const T*>) \
    {                                                                                                                                         \
        VERIFY_NOT_REACHED();                                                                                                                 \
    }                                                                                                                                         \
    template<>                                                                                                                                \
    [[nodiscard]] inline __attribute__((deprecated("use " #REPLACEMENT " instead"))) ErrorOr<void> copy_from_user<T>(T*, Userspace<T*>)       \
    {                                                                                                                                         \
        VERIFY_NOT_REACHED();                                                                                                                 \
    }

DEPRECATE_COPY_FROM_USER_TYPE(timespec, copy_time_from_user)
DEPRECATE_COPY_FROM_USER_TYPE(timeval, copy_time_from_user)

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_to_user(Userspace<T*> dest, const T* src)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_to_user(dest.unsafe_userspace_ptr(), src, sizeof(T));
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_to_user(Userspace<T*> dest, const void* src, size_t size)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_to_user(dest.unsafe_userspace_ptr(), src, size);
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_from_user(void* dest, Userspace<const T*> src, size_t size)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_from_user(dest, src.unsafe_userspace_ptr(), size);
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_n_from_user(T* dest, const T* src, size_t count)
{
    static_assert(IsTriviallyCopyable<T>);
    Checked<size_t> size = sizeof(T);
    size *= count;
    if (size.has_overflow())
        return EOVERFLOW;
    return copy_from_user(dest, src, size.value());
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_n_to_user(T* dest, const T* src, size_t count)
{
    static_assert(IsTriviallyCopyable<T>);
    Checked<size_t> size = sizeof(T);
    size *= count;
    if (size.has_overflow())
        return EOVERFLOW;
    return copy_to_user(dest, src, size.value());
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_n_from_user(T* dest, Userspace<const T*> src, size_t count)
{
    static_assert(IsTriviallyCopyable<T>);
    Checked<size_t> size = sizeof(T);
    size *= count;
    if (size.has_overflow())
        return EOVERFLOW;
    return copy_from_user(dest, src.unsafe_userspace_ptr(), size.value());
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> try_copy_n_to_user(Userspace<T*> dest, const T* src, size_t count)
{
    static_assert(IsTriviallyCopyable<T>);
    Checked<size_t> size = sizeof(T);
    size *= count;
    if (size.has_overflow())
        return EOVERFLOW;
    return copy_to_user(dest.unsafe_userspace_ptr(), src, size.value());
}

template<typename T>
inline ErrorOr<T> copy_typed_from_user(Userspace<T const*> user_data)
{
    T data {};
    TRY(copy_from_user(&data, user_data));
    return data;
}
