/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Function.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/ArrayBufferPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ArrayBufferPrototype::ArrayBufferPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void ArrayBufferPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.slice, slice, 2, attr);
    // FIXME: This should be an accessor property
    define_native_property(vm.names.byteLength, byte_length_getter, {}, Attribute::Configurable);

    define_property(vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "ArrayBuffer"), Attribute::Configurable);
}

ArrayBufferPrototype::~ArrayBufferPrototype()
{
}

static ArrayBuffer* array_buffer_object_from(VM& vm, GlobalObject& global_object)
{
    // ArrayBuffer.prototype.* deliberately don't coerce |this| value to object.
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object())
        return nullptr;
    auto& this_object = this_value.as_object();
    if (!is<ArrayBuffer>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAn, "ArrayBuffer");
        return nullptr;
    }
    return static_cast<ArrayBuffer*>(&this_object);
}

JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::slice)
{
    auto array_buffer_object = array_buffer_object_from(vm, global_object);
    if (!array_buffer_object)
        return {};
    TODO();
}

JS_DEFINE_NATIVE_GETTER(ArrayBufferPrototype::byte_length_getter)
{
    auto array_buffer_object = array_buffer_object_from(vm, global_object);
    if (!array_buffer_object)
        return {};
    // FIXME: Check for shared buffer
    // FIXME: Check for detached buffer
    return Value((double)array_buffer_object->byte_length());
}

}
