/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Checked.h>
#include <AK/Error.h>
#include <AK/FixedStringBuffer.h>
#include <AK/Forward.h>
#include <AK/Time.h>
#include <AK/Userspace.h>
#include <Kernel/Library/KString.h>
#include <Kernel/UnixTypes.h>
#include <stddef.h>

ErrorOr<NonnullOwnPtr<Kernel::KString>> try_copy_kstring_from_user(Userspace<char const*>, size_t);

template<size_t Size>
ErrorOr<void> try_copy_string_from_user_into_fixed_string_buffer(Userspace<char const*> user_str, FixedStringBuffer<Size>& buffer, size_t user_str_size)
{
    if (user_str_size > Size)
        return E2BIG;
    TRY(buffer.copy_characters_from_user(user_str, user_str_size));
    return {};
}

template<size_t Size>
ErrorOr<void> try_copy_name_from_user_into_fixed_string_buffer(Userspace<char const*> user_str, FixedStringBuffer<Size>& buffer, size_t user_str_size)
{
    if (user_str_size > Size)
        return ENAMETOOLONG;
    TRY(buffer.copy_characters_from_user(user_str, user_str_size));
    return {};
}

ErrorOr<Duration> copy_time_from_user(timespec const*);
ErrorOr<Duration> copy_time_from_user(timeval const*);
template<typename T>
ErrorOr<Duration> copy_time_from_user(Userspace<T*>);

[[nodiscard]] Optional<u32> user_atomic_fetch_add_relaxed(u32 volatile* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_exchange_relaxed(u32 volatile* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_load_relaxed(u32 volatile* var);
[[nodiscard]] bool user_atomic_store_relaxed(u32 volatile* var, u32 val);
[[nodiscard]] Optional<bool> user_atomic_compare_exchange_relaxed(u32 volatile* var, u32& expected, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_and_relaxed(u32 volatile* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_and_not_relaxed(u32 volatile* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_or_relaxed(u32 volatile* var, u32 val);
[[nodiscard]] Optional<u32> user_atomic_fetch_xor_relaxed(u32 volatile* var, u32 val);

ErrorOr<void> copy_to_user(void*, void const*, size_t);
ErrorOr<void> copy_from_user(void*, void const*, size_t);
ErrorOr<void> memset_user(void*, int, size_t);

extern "C" {

void* memcpy(void*, void const*, size_t);
[[nodiscard]] int strncmp(char const* s1, char const* s2, size_t n);
[[nodiscard]] char* strstr(char const* haystack, char const* needle);
[[nodiscard]] int strcmp(char const*, char const*);
[[nodiscard]] size_t strlen(char const*);
[[nodiscard]] size_t strnlen(char const*, size_t);
void* memset(void*, int, size_t);
[[nodiscard]] int memcmp(void const*, void const*, size_t);
void* memmove(void* dest, void const* src, size_t n);
void const* memmem(void const* haystack, size_t, void const* needle, size_t);

[[nodiscard]] inline u16 ntohs(u16 w) { return (w & 0xff) << 8 | ((w >> 8) & 0xff); }
[[nodiscard]] inline u16 htons(u16 w) { return (w & 0xff) << 8 | ((w >> 8) & 0xff); }
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_from_user(T* dest, T const* src)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_from_user(dest, src, sizeof(T));
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_to_user(T* dest, T const* src)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_to_user(dest, src, sizeof(T));
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_from_user(T* dest, Userspace<T const*> src)
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
[[nodiscard]] inline ErrorOr<void> copy_to_user(Userspace<T*> dest, T const* src)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_to_user(dest.unsafe_userspace_ptr(), src, sizeof(T));
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_to_user(Userspace<T*> dest, void const* src, size_t size)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_to_user(dest.unsafe_userspace_ptr(), src, size);
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_from_user(void* dest, Userspace<T const*> src, size_t size)
{
    static_assert(IsTriviallyCopyable<T>);
    return copy_from_user(dest, src.unsafe_userspace_ptr(), size);
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_n_from_user(T* dest, T const* src, size_t count)
{
    static_assert(IsTriviallyCopyable<T>);
    Checked<size_t> size = sizeof(T);
    size *= count;
    if (size.has_overflow())
        return EOVERFLOW;
    return copy_from_user(dest, src, size.value());
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_n_to_user(T* dest, T const* src, size_t count)
{
    static_assert(IsTriviallyCopyable<T>);
    Checked<size_t> size = sizeof(T);
    size *= count;
    if (size.has_overflow())
        return EOVERFLOW;
    return copy_to_user(dest, src, size.value());
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_n_from_user(T* dest, Userspace<T const*> src, size_t count)
{
    static_assert(IsTriviallyCopyable<T>);
    Checked<size_t> size = sizeof(T);
    size *= count;
    if (size.has_overflow())
        return EOVERFLOW;
    return copy_from_user(dest, src.unsafe_userspace_ptr(), size.value());
}

template<typename T>
[[nodiscard]] inline ErrorOr<void> copy_n_to_user(Userspace<T*> dest, T const* src, size_t count)
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

template<typename T>
inline ErrorOr<T> copy_typed_from_user(Userspace<T*> user_data)
{
    T data {};
    TRY(copy_from_user(&data, user_data));
    return data;
}
