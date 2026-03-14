/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DataTypes.h"
#include <AK/Endian.h>

namespace SSH {

ErrorOr<ByteBuffer> decode_string(FixedMemoryStream& stream)
{
    auto length = TRY(stream.read_value<NetworkOrdered<u32>>());
    auto data = TRY(ByteBuffer::create_uninitialized(length));
    TRY(stream.read_until_filled(data));
    return data;
}

ErrorOr<void> encode_mpint(AllocatingMemoryStream& stream, ReadonlyBytes bytes)
{
    VERIFY(bytes.size() > 0);

    // FIXME: Remove leading zeros.
    VERIFY(bytes.size() > 0 || bytes[0] == 0);

    bool should_add_leading_zero = bytes.size() > 0 && bytes[0] & 0x80;

    u32 total_length = bytes.size();

    // "If the most significant bit would be set for a positive number,
    // the number MUST be preceded by a zero byte."
    if (should_add_leading_zero)
        total_length += 1;

    TRY(stream.write_value<NetworkOrdered<u32>>(total_length));
    if (should_add_leading_zero)
        TRY(stream.write_value<NetworkOrdered<u8>>(0));
    TRY(stream.write_until_depleted(bytes));
    return {};
}

ErrorOr<void> encode_string(AllocatingMemoryStream& stream, StringView string)
{
    TRY(stream.write_value<NetworkOrdered<u32>>(string.length()));
    TRY(stream.write_until_depleted(string));
    return {};
}

ErrorOr<void> encode_name_list(AllocatingMemoryStream& stream, Span<StringView const> names)
{
    u64 total_length {};
    for (auto name : names) {
        total_length += name.length();
    }
    if (names.size() > 1)
        total_length += names.size() - 1;

    if (total_length > NumericLimits<u32>::max())
        return Error::from_string_literal("Name list is too long");

    TRY(stream.write_value<NetworkOrdered<u32>>(total_length));
    for (u32 i = 0; i < names.size(); i++) {
        TRY(stream.write_until_depleted(names[i]));
        if (i != names.size() - 1)
            TRY(stream.write_until_depleted(","sv));
    }
    return {};
}

// 6.6.  Public Key Algorithms
// https://datatracker.ietf.org/doc/html/rfc4253#section-6.6
ErrorOr<void> TypedBlob::encode(AllocatingMemoryStream& stream) const
{
    // "Signatures are encoded as follows:
    //      string    signature format identifier (as specified by the
    //                public key/certificate format)
    //      byte[n]   signature blob in format specific encoding."
    AllocatingMemoryStream local_stream;

    switch (type) {
    case Type::SSH_ED25519:
        TRY(encode_string(local_stream, "ssh-ed25519"sv));
        break;
    }

    TRY(encode_string(local_stream, key));

    auto encoded = TRY(local_stream.read_until_eof());
    TRY(stream.write_value<NetworkOrdered<u32>>(encoded.size()));
    TRY(stream.write_until_depleted(encoded));

    return {};
}

} // SSH
