/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>

namespace JS {

ArrayBufferConstructor::ArrayBufferConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.ArrayBuffer.as_string(), *global_object.function_prototype())
{
}

void ArrayBufferConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 25.1.4.2 ArrayBuffer.prototype, https://tc39.es/ecma262/#sec-arraybuffer.prototype
    define_direct_property(vm.names.prototype, global_object.array_buffer_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.isView, is_view, 1, attr);

    // 25.1.5.4 ArrayBuffer.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-arraybuffer.prototype-@@tostringtag
    define_native_accessor(*vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

// 25.1.3.1 ArrayBuffer ( length ), https://tc39.es/ecma262/#sec-arraybuffer-length
ThrowCompletionOr<Value> ArrayBufferConstructor::call()
{
    auto& vm = this->vm();
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.ArrayBuffer);
}

// 1.1.6 GetArrayBufferMaxByteLengthOption ( options ), https://tc39.es/proposal-resizablearraybuffer/#sec-getarraybuffermaxbytelengthoption
static ThrowCompletionOr<Optional<size_t>> get_array_buffer_max_byte_length_option(VM& vm, GlobalObject& global_object, Value options)
{
    // 1. If Type(options) is not Object, return empty.
    if (!options.is_object())
        return Optional<size_t>();

    // 2. Let maxByteLength be ? Get(options, "maxByteLength").
    auto max_byte_length = TRY(options.get(global_object, vm.names.maxByteLength));

    // 3. If maxByteLength is undefined, return empty.
    if (max_byte_length.is_undefined())
        return Optional<size_t>();

    // 4. Return ? ToIndex(maxByteLength).
    return TRY(max_byte_length.to_index(global_object));
}

// 25.1.3.1 ArrayBuffer ( length ), https://tc39.es/ecma262/#sec-arraybuffer-length
// 1.2.1 ArrayBuffer ( length [, options ] ), https://tc39.es/proposal-resizablearraybuffer/#sec-arraybuffer-constructor
ThrowCompletionOr<Object*> ArrayBufferConstructor::construct(FunctionObject& new_target)
{
    auto& global_object = this->global_object();
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, throw a TypeError exception.
    // NOTE: See `ArrayBufferConstructor::call()`

    // 2. Let byteLength be ? ToIndex(length).
    auto byte_length_or_error = vm.argument(0).to_index(global_object);
    if (byte_length_or_error.is_error()) {
        auto error = byte_length_or_error.release_error();
        if (error.value()->is_object() && is<RangeError>(error.value()->as_object())) {
            // Re-throw more specific RangeError
            return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidLength, "array buffer");
        }
        return error;
    }
    auto byte_length = byte_length_or_error.release_value();

    // 3. Let requestedMaxByteLength be ? GetArrayBufferMaxByteLengthOption(options).
    auto options = vm.argument(1);
    auto requested_max_byte_length_value = TRY(get_array_buffer_max_byte_length_option(vm, global_object, options));

    // 4. If requestedMaxByteLength is empty, then
    if (!requested_max_byte_length_value.has_value()) {
        // a. Return ? AllocateArrayBuffer(NewTarget, byteLength).
        return TRY(allocate_array_buffer(global_object, new_target, byte_length));
    }
    auto requested_max_byte_length = requested_max_byte_length_value.release_value();

    // 5. If byteLength > requestedMaxByteLength, throw a RangeError exception.
    if (byte_length > requested_max_byte_length)
        return vm.throw_completion<RangeError>(global_object, ErrorType::ByteLengthBeyondRequestedMax);

    // 6. Return ? AllocateArrayBuffer(NewTarget, byteLength, requestedMaxByteLength).
    return TRY(allocate_array_buffer(global_object, new_target, byte_length, requested_max_byte_length));
}

// 25.1.4.1 ArrayBuffer.isView ( arg ), https://tc39.es/ecma262/#sec-arraybuffer.isview
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferConstructor::is_view)
{
    auto arg = vm.argument(0);
    if (!arg.is_object())
        return Value(false);
    if (arg.as_object().is_typed_array())
        return Value(true);
    if (is<DataView>(arg.as_object()))
        return Value(true);
    return Value(false);
}

// 25.1.4.3 get ArrayBuffer [ @@species ], https://tc39.es/ecma262/#sec-get-arraybuffer-@@species
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

}
