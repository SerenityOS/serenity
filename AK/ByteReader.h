/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace AK {

struct ByteReader {
    static void store(u8* address, u16 value)
    {
        union {
            u16 _16;
            u8 _8[2];
        } const v { ._16 = value };
        __builtin_memcpy(address, v._8, 2);
    }

    static void store(u8* address, u32 value)
    {
        union {
            u32 _32;
            u8 _8[4];
        } const v { ._32 = value };
        __builtin_memcpy(address, v._8, 4);
    }

    static void load(const u8* address, u16& value)
    {
        union {
            u16 _16;
            u8 _8[2];
        } v { ._16 = 0 };
        __builtin_memcpy(&v._8, address, 2);
        value = v._16;
    }

    static void load(const u8* address, u32& value)
    {
        union {
            u32 _32;
            u8 _8[4];
        } v { ._32 = 0 };
        __builtin_memcpy(&v._8, address, 4);
        value = v._32;
    }

    static void load(const u8* address, u64& value)
    {
        union {
            u64 _64;
            u8 _8[8];
        } v { ._64 = 0 };
        __builtin_memcpy(&v._8, address, 8);
        value = v._64;
    }

    template<typename T>
    static T* load_pointer(const u8* address)
    {
        if constexpr (sizeof(T*) == 4) {
            return reinterpret_cast<T*>(load32(address));
        } else {
            static_assert(sizeof(T*) == 8, "sizeof(T*) must be either 4 or 8");
            return reinterpret_cast<T*>(load64(address));
        }
    }

    static u16 load16(const u8* address)
    {
        u16 value;
        load(address, value);
        return value;
    }

    static u32 load32(const u8* address)
    {
        u32 value;
        load(address, value);
        return value;
    }

    static u64 load64(const u8* address)
    {
        u64 value;
        load(address, value);
        return value;
    }
};

}

using AK::ByteReader;
