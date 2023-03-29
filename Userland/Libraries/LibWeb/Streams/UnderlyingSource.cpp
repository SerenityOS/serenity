/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/VM.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/Streams/UnderlyingSource.h>
#include <LibWeb/WebIDL/CallbackType.h>

namespace Web::Streams {

// Non-standard function to aid in converting a user-provided function into a WebIDL::Callback. This is essentially
// what the Bindings generator would do at compile time, but at runtime instead.
static JS::ThrowCompletionOr<JS::Handle<WebIDL::CallbackType>> property_to_callback(JS::VM& vm, JS::Value value, JS::PropertyKey const& property_key)
{
    auto property = TRY(value.get(vm, property_key));

    if (property.is_undefined())
        return JS::Handle<WebIDL::CallbackType> {};

    if (!property.is_function())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAFunction, TRY_OR_THROW_OOM(vm, property.to_string_without_side_effects()));

    return vm.heap().allocate_without_realm<WebIDL::CallbackType>(property.as_object(), HTML::incumbent_settings_object());
}

JS::ThrowCompletionOr<UnderlyingSource> UnderlyingSource::from_value(JS::VM& vm, JS::Value value)
{
    if (!value.is_object())
        return UnderlyingSource {};

    auto& object = value.as_object();

    UnderlyingSource underlying_source {
        .start = TRY(property_to_callback(vm, value, "start")),
        .pull = TRY(property_to_callback(vm, value, "pull")),
        .cancel = TRY(property_to_callback(vm, value, "cancel")),
        .type = {},
        .auto_allocate_chunk_size = {},
    };

    if (TRY(object.has_property("type"))) {
        auto type_value = TRY(TRY(object.get("type")).to_string(object.vm()));
        if (type_value == "bytes"sv) {
            underlying_source.type = ReadableStreamType::Bytes;
        } else {
            return vm.throw_completion<JS::TypeError>(DeprecatedString::formatted("Unknown stream type '{}'", type_value));
        }
    }

    if (TRY(object.has_property("autoAllocateChunkSize")))
        underlying_source.auto_allocate_chunk_size = TRY(TRY(object.get("autoAllocateChunkSize")).to_bigint_int64(object.vm()));

    return underlying_source;
}

}
