/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Random.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/Wrapper.h>
#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/Crypto/SubtleCrypto.h>

namespace Web::Crypto {

Crypto::Crypto()
    : m_subtle(SubtleCrypto::create())
{
}

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

}
