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

namespace AK {

class UUID {
public:
    UUID();
    UUID(Array<u8, 16> uuid_buffer);
    UUID(const StringView&);
    ~UUID() = default;

    bool operator==(const UUID&) const;
    bool operator!=(const UUID& other) const { return !(*this == other); }
    bool operator<=(const UUID&) const = delete;
    bool operator>=(const UUID&) const = delete;
    bool operator<(const UUID&) const = delete;
    bool operator>(const UUID&) const = delete;

    String to_string() const;
    bool is_zero() const;

private:
    void convert_string_view_to_uuid(const StringView&);

    Array<u8, 16> m_uuid_buffer {};
};

}

using AK::UUID;
