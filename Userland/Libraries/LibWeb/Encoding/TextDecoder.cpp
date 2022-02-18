/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FlyString.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Bindings/Wrapper.h>
#include <LibWeb/Encoding/TextDecoder.h>

namespace Web::Encoding {

// https://encoding.spec.whatwg.org/#dom-textdecoder-decode
DOM::ExceptionOr<String> TextDecoder::decode(JS::Handle<JS::Object> const& input) const
{
    // FIXME: Implement the streaming stuff.

    auto data_buffer = Bindings::IDL::get_buffer_source_copy(*input.cell());
    if (!data_buffer.has_value())
        return DOM::OperationError::create("Failed to copy bytes from ArrayBuffer");

    return m_decoder.to_utf8({ data_buffer->data(), data_buffer->size() });
}

}
