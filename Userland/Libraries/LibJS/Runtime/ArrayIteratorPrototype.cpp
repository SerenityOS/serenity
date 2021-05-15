/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    auto& vm = this->vm();
    Object::initialize(global_object);

    define_native_function(vm.names.next, next, 0, Attribute::Configurable | Attribute::Writable);
    define_property(global_object.vm().well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Array Iterator"), Attribute::Configurable);
}

ArrayIteratorPrototype::~ArrayIteratorPrototype()
{
}

JS_DEFINE_NATIVE_FUNCTION(ArrayIteratorPrototype::next)
{
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object() || !is<ArrayIterator>(this_value.as_object())) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAn, "Array Iterator");
        return {};
    }
    auto& this_object = this_value.as_object();
    auto& iterator = static_cast<ArrayIterator&>(this_object);
    auto target_array = iterator.array();
    if (target_array.is_undefined())
        return create_iterator_result_object(global_object, js_undefined(), true);
    VERIFY(target_array.is_object());
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
