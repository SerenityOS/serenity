/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/ByteBuffer.h>
#include <AK/StringView.h>
#include <AK/Types.h>

#ifdef KERNEL
#    include <Kernel/KString.h>
#else
#    include <AK/String.h>
#endif

namespace AK {

class UUID {
public:
    enum class Endianness {
        Mixed,
        Little
    };

    UUID() = default;
    UUID(Array<u8, 16> uuid_buffer);
    UUID(StringView, Endianness endianness = Endianness::Little);
    ~UUID() = default;

    bool operator==(const UUID&) const;
    bool operator!=(const UUID& other) const { return !(*this == other); }
    bool operator<=(const UUID&) const = delete;
    bool operator>=(const UUID&) const = delete;
    bool operator<(const UUID&) const = delete;
    bool operator>(const UUID&) const = delete;

#ifdef KERNEL
    ErrorOr<NonnullOwnPtr<Kernel::KString>> to_string() const;
#else
    String to_string() const;
#endif
    bool is_zero() const;

private:
    void convert_string_view_to_little_endian_uuid(StringView);
    void convert_string_view_to_mixed_endian_uuid(StringView);

    Array<u8, 16> m_uuid_buffer {};
};

}

using AK::UUID;
