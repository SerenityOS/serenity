/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayConstructor.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/Shape.h>

namespace JS {

ArrayConstructor::ArrayConstructor(GlobalObject& global_object)
    : NativeFunction("Array", *global_object.function_prototype())
{
}

ArrayConstructor::~ArrayConstructor()
{
}

void ArrayConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    define_property("prototype", global_object.array_prototype(), 0);
    define_property("length", Value(1), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function("from", from, 1, attr);
    define_native_function("isArray", is_array, 1, attr);
    define_native_function("of", of, 0, attr);
}

Value ArrayConstructor::call()
{
    if (vm().argument_count() <= 0)
        return Array::create(global_object());

    if (vm().argument_count() == 1 && vm().argument(0).is_number()) {
        auto array_length_value = vm().argument(0);
        if (!array_length_value.is_integer() || array_length_value.as_i32() < 0) {
            vm().throw_exception<TypeError>(global_object(), ErrorType::ArrayInvalidLength);
            return {};
        }
        auto* array = Array::create(global_object());
        array->indexed_properties().set_array_like_size(array_length_value.as_i32());
        return array;
    }

    auto* array = Array::create(global_object());
    for (size_t i = 0; i < vm().argument_count(); ++i)
        array->indexed_properties().append(vm().argument(i));
    return array;
}

Value ArrayConstructor::construct(Function&)
{
    return call();
}

JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::from)
{
    auto value = vm.argument(0);
    auto object = value.to_object(global_object);
    if (!object)
        return {};

    auto* array = Array::create(global_object);

    // Array.from() lets you create Arrays from:
    if (auto size = object->indexed_properties().array_like_size()) {
        // * array-like objects (objects with a length property and indexed elements)
        MarkedValueList elements(vm.heap());
        elements.ensure_capacity(size);
        for (size_t i = 0; i < size; ++i) {
            elements.append(object->get(i));
            if (vm.exception())
                return {};
        }
        array->set_indexed_property_elements(move(elements));
    } else {
        // * iterable objects
        get_iterator_values(global_object, value, [&](Value element) {
            if (vm.exception())
                return IterationDecision::Break;
            array->indexed_properties().append(element);
            return IterationDecision::Continue;
        });
        if (vm.exception())
            return {};
    }

    // FIXME: if interpreter.argument_count() >= 2: mapFn
    // FIXME: if interpreter.argument_count() >= 3: thisArg

    return array;
}

JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::is_array)
{
    auto value = vm.argument(0);
    return Value(value.is_array());
}

JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::of)
{
    auto* array = Array::create(global_object);
    for (size_t i = 0; i < vm.argument_count(); ++i)
        array->indexed_properties().append(vm.argument(i));
    return array;
}

}
