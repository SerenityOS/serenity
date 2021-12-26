/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>

namespace JS {

ArrayBufferConstructor::ArrayBufferConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.ArrayBuffer, *global_object.function_prototype())
{
}

void ArrayBufferConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 25.1.4.2 ArrayBuffer.prototype, https://tc39.es/ecma262/#sec-arraybuffer.prototype
    define_property(vm.names.prototype, global_object.array_buffer_prototype(), 0);

    define_property(vm.names.length, Value(1), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.isView, is_view, 1, attr);

    // 25.1.5.4 ArrayBuffer.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-arraybuffer.prototype-@@tostringtag
    define_native_accessor(vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);
}

ArrayBufferConstructor::~ArrayBufferConstructor()
{
}

// 25.1.3.1 ArrayBuffer ( length ), https://tc39.es/ecma262/#sec-arraybuffer-length
Value ArrayBufferConstructor::call()
{
    auto& vm = this->vm();
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.ArrayBuffer);
    return {};
}

// 25.1.3.1 ArrayBuffer ( length ), https://tc39.es/ecma262/#sec-arraybuffer-length
Value ArrayBufferConstructor::construct(Function&)
{
    auto& vm = this->vm();
    auto byte_length = vm.argument(0).to_index(global_object());
    if (vm.exception()) {
        if (vm.exception()->value().is_object() && is<RangeError>(vm.exception()->value().as_object())) {
            // Re-throw more specific RangeError
            vm.clear_exception();
            vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "array buffer");
        }
        return {};
    }
    return ArrayBuffer::create(global_object(), byte_length);
}

// 25.1.4.1 ArrayBuffer.isView ( arg ), https://tc39.es/ecma262/#sec-arraybuffer.isview
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferConstructor::is_view)
{
    auto arg = vm.argument(0);
    if (!arg.is_object())
        return Value(false);
    if (arg.as_object().is_typed_array())
        return Value(true);
    // FIXME: Check for DataView as well
    return Value(false);
}

// 25.1.4.3 get ArrayBuffer [ @@species ], https://tc39.es/ecma262/#sec-get-arraybuffer-@@species
JS_DEFINE_NATIVE_GETTER(ArrayBufferConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

}
