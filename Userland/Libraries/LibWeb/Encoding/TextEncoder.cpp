/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FlyString.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/Wrapper.h>
#include <LibWeb/Encoding/TextEncoder.h>

namespace Web::Encoding {

// https://encoding.spec.whatwg.org/#dom-textencoder-encode
JS::Uint8Array* TextEncoder::encode(String const& input) const
{
    auto& global_object = wrapper()->global_object();

    // NOTE: The AK::String returned from PrimitiveString::string() is always UTF-8, regardless of the internal string type, so most of these steps are no-ops.

    // 1. Convert input to an I/O queue of scalar values.
    // 2. Let output be the I/O queue of bytes « end-of-queue ».
    // 3. While true:
    //     1. Let item be the result of reading from input.
    //     2. Let result be the result of processing an item with item, an instance of the UTF-8 encoder, input, output, and "fatal".
    //     3. Assert: result is not an error.
    //     4. If result is finished, then convert output into a byte sequence and return a Uint8Array object wrapping an ArrayBuffer containing output.

    auto byte_buffer = input.to_byte_buffer();
    auto array_length = byte_buffer.size();
    auto* array_buffer = JS::ArrayBuffer::create(global_object, move(byte_buffer));
    return JS::Uint8Array::create(global_object, array_length, *array_buffer);
}

// https://encoding.spec.whatwg.org/#dom-textencoder-encoding
FlyString const& TextEncoder::encoding()
{
    static FlyString encoding = "utf-8"sv;
    return encoding;
}

}
