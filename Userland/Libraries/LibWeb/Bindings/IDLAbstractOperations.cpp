/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/NumericLimits.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/PropertyKey.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>

namespace Web::Bindings::IDL {

// https://webidl.spec.whatwg.org/#dfn-get-buffer-source-copy
Optional<ByteBuffer> get_buffer_source_copy(JS::Object const& buffer_source)
{
    // 1. Let esBufferSource be the result of converting bufferSource to an ECMAScript value.

    // 2. Let esArrayBuffer be esBufferSource.
    JS::ArrayBuffer* es_array_buffer;

    // 3. Let offset be 0.
    u32 offset = 0;

    // 4. Let length be 0.
    u32 length = 0;

    // 5. If esBufferSource has a [[ViewedArrayBuffer]] internal slot, then:
    if (is<JS::TypedArrayBase>(buffer_source)) {
        auto const& es_buffer_source = static_cast<JS::TypedArrayBase const&>(buffer_source);

        // 1. Set esArrayBuffer to esBufferSource.[[ViewedArrayBuffer]].
        es_array_buffer = es_buffer_source.viewed_array_buffer();

        // 2. Set offset to esBufferSource.[[ByteOffset]].
        offset = es_buffer_source.byte_offset();

        // 3. Set length to esBufferSource.[[ByteLength]].
        length = es_buffer_source.byte_length();
    } else if (is<JS::DataView>(buffer_source)) {
        auto const& es_buffer_source = static_cast<JS::DataView const&>(buffer_source);

        // 1. Set esArrayBuffer to esBufferSource.[[ViewedArrayBuffer]].
        es_array_buffer = es_buffer_source.viewed_array_buffer();

        // 2. Set offset to esBufferSource.[[ByteOffset]].
        offset = es_buffer_source.byte_offset();

        // 3. Set length to esBufferSource.[[ByteLength]].
        length = es_buffer_source.byte_length();
    }
    // 6. Otherwise:
    else {
        // 1. Assert: esBufferSource is an ArrayBuffer or SharedArrayBuffer object.
        auto const& es_buffer_source = static_cast<JS::ArrayBuffer const&>(buffer_source);
        es_array_buffer = &const_cast<JS ::ArrayBuffer&>(es_buffer_source);

        // 2. Set length to esBufferSource.[[ArrayBufferByteLength]].
        length = es_buffer_source.byte_length();
    }

    // 7. If ! IsDetachedBuffer(esArrayBuffer) is true, then return the empty byte sequence.
    if (es_array_buffer->is_detached())
        return ByteBuffer {};

    // 8. Let bytes be a new byte sequence of length equal to length.
    auto bytes = ByteBuffer::create_zeroed(length);
    if (bytes.is_error())
        return {};

    // 9. For i in the range offset to offset + length − 1, inclusive, set bytes[i − offset] to ! GetValueFromBuffer(esArrayBuffer, i, Uint8, true, Unordered).
    for (u64 i = offset; i <= offset + length - 1; ++i) {
        auto value = es_array_buffer->get_value<u8>(i, true, JS::ArrayBuffer::Unordered);
        bytes.value()[i - offset] = (u8)value.as_u32();
    }

    // 10. Return bytes.
    return bytes.release_value();
}

}
