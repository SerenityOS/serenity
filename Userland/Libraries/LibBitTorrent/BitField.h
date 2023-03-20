/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <AK/Forward.h>

namespace BitTorrent {

class BitField {
public:
    BitField(u64 size);
    BitField(ReadonlyBytes data, u64 size);

    bool get(u64 index) const;
    void set(u64 index, bool value);
    u64 ones() const;
    u64 zeroes() const;
    float progress() const;

    u64 size() const;
    u64 data_size() const;

    ReadonlyBytes bytes() const;
    ErrorOr<void> write_to_stream(Stream& stream) const;
    static ErrorOr<BitField> read_from_stream(Stream& stream, u64 size);

private:
    u64 m_size;
    ByteBuffer m_data {};
    u64 m_ones = 0;
};
}

template<>
struct AK::Formatter<BitTorrent::BitField> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, BitTorrent::BitField const& value)
    {
        return Formatter<FormatString>::format(builder, "{}/{} ({:.2}%), storage: {}b, "sv, value.ones(), value.size(), value.progress(), value.data_size());
    }
};
