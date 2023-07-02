/*
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>

namespace JS {

ThrowCompletionOr<Optional<size_t>> get_max_byte_length_option(VM& vm, Value options);

ArrayBufferConstructor::ArrayBufferConstructor(Realm& realm)
    : NativeFunction(realm.vm().names.ArrayBuffer.as_string(), realm.intrinsics().function_prototype())
{
}

ThrowCompletionOr<void> ArrayBufferConstructor::initialize(Realm& realm)
{
    auto& vm = this->vm();
    MUST_OR_THROW_OOM(NativeFunction::initialize(realm));

    // 25.1.4.2 ArrayBuffer.prototype, https://tc39.es/ecma262/#sec-arraybuffer.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().array_buffer_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.isView, is_view, 1, attr);

    // 25.1.5.4 ArrayBuffer.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-arraybuffer.prototype-@@tostringtag
    define_native_accessor(realm, vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);

    return {};
}

// 25.1.3.1 ArrayBuffer ( length ), https://tc39.es/ecma262/#sec-arraybuffer-length
// 1.2.1 ArrayBuffer ( length [ , options ] ), https://tc39.es/proposal-resizablearraybuffer/#sec-arraybuffer-constructor
ThrowCompletionOr<Value> ArrayBufferConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm.throw_completion<TypeError>(ErrorType::ConstructorWithoutNew, vm.names.ArrayBuffer);
}

// 25.1.3.1 ArrayBuffer ( length ), https://tc39.es/ecma262/#sec-arraybuffer-length
// 1.2.1 ArrayBuffer ( length [ , options ] ), https://tc39.es/proposal-resizablearraybuffer/#sec-arraybuffer-constructor
ThrowCompletionOr<NonnullGCPtr<Object>> ArrayBufferConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    auto options = vm.argument(1);

    // 2. Let byteLength be ? ToIndex(length).
    auto byte_length_or_error = vm.argument(0).to_index(vm);

    if (byte_length_or_error.is_error()) {
        auto error = byte_length_or_error.release_error();
        if (error.value()->is_object() && is<RangeError>(error.value()->as_object())) {
            // Re-throw more specific RangeError
            return vm.throw_completion<RangeError>(ErrorType::InvalidLength, "array buffer");
        }
        return error;
    }

    // 3. Let requestedMaxByteLength be ? GetArrayBufferMaxByteLengthOption(options).
    auto requested_max_byte_length = TRY(get_max_byte_length_option(vm, options));

    // 4. Return ? AllocateArrayBuffer(NewTarget, byteLength, requestedMaxByteLength).
    return *TRY(allocate_array_buffer(vm, new_target, byte_length_or_error.release_value(), requested_max_byte_length));
}

// 25.1.4.1 ArrayBuffer.isView ( arg ), https://tc39.es/ecma262/#sec-arraybuffer.isview
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferConstructor::is_view)
{
    auto arg = vm.argument(0);

    // 1. If arg is not an Object, return false.
    if (!arg.is_object())
        return Value(false);

    // 2. If arg has a [[ViewedArrayBuffer]] internal slot, return true.
    if (arg.as_object().is_typed_array())
        return Value(true);
    if (is<DataView>(arg.as_object()))
        return Value(true);

    // 3. Return false.
    return Value(false);
}

// 25.1.4.3 get ArrayBuffer [ @@species ], https://tc39.es/ecma262/#sec-get-arraybuffer-@@species
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferConstructor::symbol_species_getter)
{
    // 1. Return the this value.
    return vm.this_value();
}

// 1.1.6 GetArrayBufferMaxByteLengthOption ( options ), https://tc39.es/proposal-resizablearraybuffer/#sec-getarraybuffermaxbytelengthoption
ThrowCompletionOr<Optional<size_t>> get_max_byte_length_option(VM& vm, Value options)
{
    // 1. If Type(options) is not Object, return empty
    if (!options.is_object())
        return Optional<size_t> {};

    // 2. Let maxByteLength be ? Get(options, "maxByteLength").
    auto max_byte_length = TRY(options.get(vm, vm.names.maxByteLength));

    // 3. If maxByteLength is undefined, return empty.
    if (max_byte_length.is_undefined())
        return Optional<size_t> {};

    // 4. Return ? ToIndex(maxByteLength).
    return TRY(max_byte_length.to_index(vm));
}

}
