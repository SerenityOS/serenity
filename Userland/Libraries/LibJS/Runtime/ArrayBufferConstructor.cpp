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
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_property(vm.names.prototype, global_object.array_buffer_prototype(), 0);
    define_property(vm.names.length, Value(1), Attribute::Configurable);
    define_native_function(vm.names.isView, is_view, 1, attr);
}

ArrayBufferConstructor::~ArrayBufferConstructor()
{
}

Value ArrayBufferConstructor::call()
{
    auto& vm = this->vm();
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.ArrayBuffer);
    return {};
}

Value ArrayBufferConstructor::construct(Function&)
{
    auto& vm = this->vm();
    auto byte_length = vm.argument(0).to_index(global_object());
    if (vm.exception()) {
        // Re-throw more specific RangeError
        vm.clear_exception();
        vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "array buffer");
        return {};
    }
    return ArrayBuffer::create(global_object(), byte_length);
}

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

}
