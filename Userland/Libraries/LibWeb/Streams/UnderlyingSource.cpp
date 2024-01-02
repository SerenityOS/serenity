/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/VM.h>
#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/UnderlyingSource.h>
#include <LibWeb/WebIDL/AbstractOperations.h>
#include <LibWeb/WebIDL/CallbackType.h>
#include <LibWeb/WebIDL/Types.h>

namespace Web::Streams {

JS::ThrowCompletionOr<UnderlyingSource> UnderlyingSource::from_value(JS::VM& vm, JS::Value value)
{
    if (!value.is_object())
        return UnderlyingSource {};

    auto& object = value.as_object();

    UnderlyingSource underlying_source {
        .start = TRY(property_to_callback(vm, value, "start", WebIDL::OperationReturnsPromise::No)),
        .pull = TRY(property_to_callback(vm, value, "pull", WebIDL::OperationReturnsPromise::Yes)),
        .cancel = TRY(property_to_callback(vm, value, "cancel", WebIDL::OperationReturnsPromise::Yes)),
        .type = {},
        .auto_allocate_chunk_size = {},
    };

    auto type_value = TRY(object.get("type"));
    if (!type_value.is_undefined()) {
        auto type_string = TRY(type_value.to_string(vm));
        if (type_string == "bytes"sv)
            underlying_source.type = ReadableStreamType::Bytes;
        else
            return vm.throw_completion<JS::TypeError>(ByteString::formatted("Unknown stream type '{}'", type_value));
    }

    if (TRY(object.has_property("autoAllocateChunkSize"))) {
        auto value = TRY(object.get("autoAllocateChunkSize"));
        underlying_source.auto_allocate_chunk_size = TRY(WebIDL::convert_to_int<WebIDL::UnsignedLongLong>(vm, value, WebIDL::EnforceRange::Yes));
    }

    return underlying_source;
}

}
