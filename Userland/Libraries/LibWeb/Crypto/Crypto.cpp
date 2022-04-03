/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2022, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Random.h>
#include <AK/StringBuilder.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/Wrapper.h>
#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/Crypto/SubtleCrypto.h>

namespace Web::Crypto {

Crypto::Crypto()
    : m_subtle(SubtleCrypto::create())
{
}

// https://w3c.github.io/webcrypto/#dfn-Crypto-method-getRandomValues
DOM::ExceptionOr<JS::Value> Crypto::get_random_values(JS::Value array) const
{
    // 1. If array is not an Int8Array, Uint8Array, Uint8ClampedArray, Int16Array, Uint16Array, Int32Array, Uint32Array, BigInt64Array, or BigUint64Array, then throw a TypeMismatchError and terminate the algorithm.
    if (!array.is_object() || !(is<JS::Int8Array>(array.as_object()) || is<JS::Uint8Array>(array.as_object()) || is<JS::Uint8ClampedArray>(array.as_object()) || is<JS::Int16Array>(array.as_object()) || is<JS::Uint16Array>(array.as_object()) || is<JS::Int32Array>(array.as_object()) || is<JS::Uint32Array>(array.as_object()) || is<JS::BigInt64Array>(array.as_object()) || is<JS::BigUint64Array>(array.as_object())))
        return DOM::TypeMismatchError::create("array must be one of Int8Array, Uint8Array, Uint8ClampedArray, Int16Array, Uint16Array, Int32Array, Uint32Array, BigInt64Array, or BigUint64Array");
    auto& typed_array = static_cast<JS::TypedArrayBase&>(array.as_object());

    // 2. If the byteLength of array is greater than 65536, throw a QuotaExceededError and terminate the algorithm.
    if (typed_array.byte_length() > 65536)
        return DOM::QuotaExceededError::create("array's byteLength may not be greater than 65536");

    // IMPLEMENTATION DEFINED: If the viewed array buffer is detached, throw a InvalidStateError and terminate the algorithm.
    if (typed_array.viewed_array_buffer()->is_detached())
        return DOM::InvalidStateError::create("array is detached");
    // FIXME: Handle SharedArrayBuffers

    // 3. Overwrite all elements of array with cryptographically strong random values of the appropriate type.
    fill_with_random(typed_array.viewed_array_buffer()->buffer().data(), typed_array.viewed_array_buffer()->buffer().size());

    // 4. Return array.
    return array;
}

// https://w3c.github.io/webcrypto/#dfn-Crypto-method-randomUUID
String Crypto::random_uuid() const
{
    // 1. Let bytes be a byte sequence of length 16.
    u8 bytes[16];

    // 2. Fill bytes with cryptographically secure random bytes.
    fill_with_random(bytes, 16);

    // 3. Set the 4 most significant bits of bytes[6], which represent the UUID version, to 0100.
    bytes[6] &= ~(1 << 7);
    bytes[6] |= 1 << 6;
    bytes[6] &= ~(1 << 5);
    bytes[6] &= ~(1 << 4);

    // 4. Set the 2 most significant bits of bytes[8], which represent the UUID variant, to 10.
    bytes[8] |= 1 << 7;
    bytes[8] &= ~(1 << 6);

    /* 5. Return the string concatenation of
        «
        hexadecimal representation of bytes[0],
        hexadecimal representation of bytes[1],
        hexadecimal representation of bytes[2],
        hexadecimal representation of bytes[3],
        "-",
        hexadecimal representation of bytes[4],
        hexadecimal representation of bytes[5],
        "-",
        hexadecimal representation of bytes[6],
        hexadecimal representation of bytes[7],
        "-",
        hexadecimal representation of bytes[8],
        hexadecimal representation of bytes[9],
        "-",
        hexadecimal representation of bytes[10],
        hexadecimal representation of bytes[11],
        hexadecimal representation of bytes[12],
        hexadecimal representation of bytes[13],
        hexadecimal representation of bytes[14],
        hexadecimal representation of bytes[15]
        ».
        */
    StringBuilder builder;
    builder.appendff("{:02x}{:02x}{:02x}{:02x}-", bytes[0], bytes[1], bytes[2], bytes[3]);
    builder.appendff("{:02x}{:02x}-", bytes[4], bytes[5]);
    builder.appendff("{:02x}{:02x}-", bytes[6], bytes[7]);
    builder.appendff("{:02x}{:02x}-", bytes[8], bytes[9]);
    builder.appendff("{:02x}{:02x}{:02x}{:02x}{:02x}{:02x}", bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]);
    return builder.to_string();
}

}
