/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayIterator.h>
#include <LibJS/Runtime/ArrayIteratorPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>

namespace JS {

ArrayIteratorPrototype::ArrayIteratorPrototype(GlobalObject& global_object)
    : Object(*global_object.iterator_prototype())
{
}

void ArrayIteratorPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    define_native_function("next", next, 0, Attribute::Configurable | Attribute::Writable);
    define_property(global_object.vm().well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Array Iterator"), Attribute::Configurable);
}

ArrayIteratorPrototype::~ArrayIteratorPrototype()
{
}

JS_DEFINE_NATIVE_FUNCTION(ArrayIteratorPrototype::next)
{
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object() || !this_value.as_object().is_array_iterator_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAn, "Array Iterator");
        return {};
    }
    auto& this_object = this_value.as_object();
    auto& iterator = static_cast<ArrayIterator&>(this_object);
    auto target_array = iterator.array();
    if (target_array.is_undefined())
        return create_iterator_result_object(global_object, js_undefined(), true);
    ASSERT(target_array.is_object());
    auto& array = target_array.as_object();

    auto index = iterator.index();
    auto iteration_kind = iterator.iteration_kind();
    // FIXME: Typed array check
    auto length = array.indexed_properties().array_like_size();

    if (index >= length) {
        iterator.m_array = js_undefined();
        return create_iterator_result_object(global_object, js_undefined(), true);
    }

    iterator.m_index++;
    if (iteration_kind == Object::PropertyKind::Key)
        return create_iterator_result_object(global_object, Value(static_cast<i32>(index)), false);

    auto value = array.get(index);
    if (vm.exception())
        return {};
    if (iteration_kind == Object::PropertyKind::Value)
        return create_iterator_result_object(global_object, value, false);

    auto* entry_array = Array::create(global_object);
    entry_array->define_property(0, Value(static_cast<i32>(index)));
    entry_array->define_property(1, value);
    return create_iterator_result_object(global_object, entry_array, false);
}

}
