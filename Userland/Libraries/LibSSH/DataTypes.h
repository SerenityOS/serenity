/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/MemoryStream.h>
#include <AK/StringView.h>

namespace SSH {

// 5.  Data Type Representations Used in the SSH Protocols
// https://datatracker.ietf.org/doc/html/rfc4251#section-5

ErrorOr<ByteBuffer> decode_string(FixedMemoryStream& stream);

ErrorOr<ByteBuffer> as_mpint(ReadonlyBytes);
ErrorOr<void> encode_mpint(AllocatingMemoryStream& stream, ReadonlyBytes);
ErrorOr<void> encode_string(AllocatingMemoryStream& stream, StringView);
ErrorOr<void> encode_name_list(AllocatingMemoryStream& stream, Span<StringView const> names);

// This is used for signatures and public keys.
struct TypedBlob {
    enum class Type : u8 {
        SSH_ED25519,
    };

    static StringView type_to_string(Type t);

    static ErrorOr<TypedBlob> decode(FixedMemoryStream& stream);
    static ErrorOr<TypedBlob> read_from_string(StringView);

    Type type { Type::SSH_ED25519 };
    ByteBuffer key {};

    ErrorOr<void> encode(AllocatingMemoryStream& stream) const;
};

} // SSH
