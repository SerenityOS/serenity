/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Forward.h>

namespace AK::Detail {

class StringData;

static constexpr size_t MAX_SHORT_STRING_BYTE_COUNT = sizeof(StringData*) - 1;

struct ShortString {
    ReadonlyBytes bytes() const;
    size_t byte_count() const;

    // NOTE: This is the byte count shifted left 1 step and or'ed with a 1 (the SHORT_STRING_FLAG)
    u8 byte_count_and_short_string_flag { 0 };
    u8 storage[MAX_SHORT_STRING_BYTE_COUNT] = { 0 };
};

static_assert(HostIsLittleEndian, "Order of fields in ShortString assumes LE.");
static_assert(sizeof(ShortString) >= sizeof(StringData*));
static_assert(__builtin_offsetof(ShortString, byte_count_and_short_string_flag) == 0);

class StringBase {
public:
    StringBase(StringBase const&);

    constexpr StringBase(StringBase&& other)
        : m_short_string(other.m_short_string)
    {
        other.m_short_string = ShortString {};
        other.m_short_string.byte_count_and_short_string_flag = SHORT_STRING_FLAG;
    }

    StringBase& operator=(StringBase&&);
    StringBase& operator=(StringBase const&);

    // NOTE: This is primarily interesting to unit tests.
    [[nodiscard]] bool is_short_string() const;

protected:
    // NOTE: If the least significant bit of the pointer is set, this is a short string.
    static constexpr uintptr_t SHORT_STRING_FLAG = 1;

    static constexpr bool has_short_string_bit(uintptr_t data)
    {
        return (data & SHORT_STRING_FLAG) != 0;
    }

    explicit StringBase(NonnullRefPtr<Detail::StringData const>);

    explicit constexpr StringBase(ShortString short_string)
        : m_short_string(short_string)
    {
    }

    union {
        ShortString m_short_string;
        Detail::StringData const* m_data { nullptr };
    };
};

}
