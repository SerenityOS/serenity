/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace AK {

struct ByteReader {
    template<typename T>
    requires(IsTriviallyCopyable<T>) ALWAYS_INLINE static void store(u8* addr, T value)
    {
        __builtin_memcpy(addr, &value, sizeof(T));
    }
    template<typename T>
    requires(IsTriviallyCopyable<T>) ALWAYS_INLINE static void load(const u8* addr, T& value)
    {
        __builtin_memcpy(&value, addr, sizeof(T));
    }

    template<typename T>
    ALWAYS_INLINE static T* load_pointer(const u8* address)
    {
        FlatPtr value;
        load<FlatPtr>(address, value);
        return reinterpret_cast<T*>(value);
    }

    ALWAYS_INLINE static u16 load16(const u8* address)
    {
        u16 value;
        load(address, value);
        return value;
    }

    ALWAYS_INLINE static u32 load32(const u8* address)
    {
        u32 value;
        load(address, value);
        return value;
    }

    ALWAYS_INLINE static u64 load64(const u8* address)
    {
        u64 value;
        load(address, value);
        return value;
    }
};

}

using AK::ByteReader;
