/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayIteratorPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/TypedArray.h>

namespace JS {

ArrayIteratorPrototype::ArrayIteratorPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.iterator_prototype())
{
}

void ArrayIteratorPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    define_native_function(vm.names.next, next, 0, Attribute::Configurable | Attribute::Writable);

    // 23.1.5.2.2 %ArrayIteratorPrototype% [ @@toStringTag ], https://tc39.es/ecma262/#sec-%arrayiteratorprototype%-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Array Iterator"), Attribute::Configurable);
}

ArrayIteratorPrototype::~ArrayIteratorPrototype()
{
}

// 23.1.5.2.1 %ArrayIteratorPrototype%.next ( ), https://tc39.es/ecma262/#sec-%arrayiteratorprototype%.next
// FIXME: This seems to be CreateArrayIterator (https://tc39.es/ecma262/#sec-createarrayiterator) instead of %ArrayIteratorPrototype%.next.
JS_DEFINE_NATIVE_FUNCTION(ArrayIteratorPrototype::next)
{
    auto* iterator = TRY(typed_this_value(global_object));
    auto target_array = iterator->array();
    if (target_array.is_undefined())
        return create_iterator_result_object(global_object, js_undefined(), true);
    VERIFY(target_array.is_object());
    auto& array = target_array.as_object();

    auto index = iterator->index();
    auto iteration_kind = iterator->iteration_kind();

    size_t length;

    if (array.is_typed_array()) {
        auto& typed_array = static_cast<TypedArrayBase&>(array);

        if (typed_array.viewed_array_buffer()->is_detached())
            return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

        length = typed_array.array_length();
    } else {
        length = TRY(length_of_array_like(global_object, array));
    }

    if (index >= length) {
        iterator->m_array = js_undefined();
        return create_iterator_result_object(global_object, js_undefined(), true);
    }

    iterator->m_index++;
    if (iteration_kind == Object::PropertyKind::Key)
        return create_iterator_result_object(global_object, Value(static_cast<i32>(index)), false);

    auto value = TRY(array.get(index));

    if (iteration_kind == Object::PropertyKind::Value)
        return create_iterator_result_object(global_object, value, false);

    return create_iterator_result_object(global_object, Array::create_from(global_object, { Value(static_cast<i32>(index)), value }), false);
}

}
