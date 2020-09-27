/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

Array* Array::create(GlobalObject& global_object)
{
    return global_object.heap().allocate<Array>(global_object, *global_object.array_prototype());
}

Array::Array(Object& prototype)
    : Object(prototype)
{
    define_native_property("length", length_getter, length_setter, Attribute::Writable);
}

Array::~Array()
{
}

Array* Array::typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!this_object->is_array()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAn, "Array");
        return nullptr;
    }
    return static_cast<Array*>(this_object);
}

JS_DEFINE_NATIVE_GETTER(Array::length_getter)
{
    auto* array = typed_this(vm, global_object);
    if (!array)
        return {};
    return Value(static_cast<i32>(array->indexed_properties().array_like_size()));
}

JS_DEFINE_NATIVE_SETTER(Array::length_setter)
{
    auto* array = typed_this(vm, global_object);
    if (!array)
        return;
    auto length = value.to_number(global_object);
    if (vm.exception())
        return;
    if (length.is_nan() || length.is_infinity() || length.as_double() < 0) {
        vm.throw_exception<RangeError>(global_object, ErrorType::ArrayInvalidLength);
        return;
    }
    array->indexed_properties().set_array_like_size(length.as_double());
}

}
